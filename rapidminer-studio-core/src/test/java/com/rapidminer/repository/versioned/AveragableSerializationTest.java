/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.versioned;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeight;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.performance.AbsoluteError;
import com.rapidminer.operator.performance.AreaUnderCurve;
import com.rapidminer.operator.performance.AreaUnderCurve.Neutral;
import com.rapidminer.operator.performance.AreaUnderCurve.Optimistic;
import com.rapidminer.operator.performance.AreaUnderCurve.Pessimistic;
import com.rapidminer.operator.performance.BinaryClassificationPerformance;
import com.rapidminer.operator.performance.CorrelationCriterion;
import com.rapidminer.operator.performance.CrossEntropy;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.LenientRelativeError;
import com.rapidminer.operator.performance.LogisticLoss;
import com.rapidminer.operator.performance.MDLCriterion;
import com.rapidminer.operator.performance.Margin;
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.operator.performance.MinMaxCriterion;
import com.rapidminer.operator.performance.MultiClassificationPerformance;
import com.rapidminer.operator.performance.NormalizedAbsoluteError;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.performance.PredictionAverage;
import com.rapidminer.operator.performance.RankCorrelation;
import com.rapidminer.operator.performance.RelativeError;
import com.rapidminer.operator.performance.RootMeanSquaredError;
import com.rapidminer.operator.performance.RootRelativeSquaredError;
import com.rapidminer.operator.performance.SimpleClassificationError;
import com.rapidminer.operator.performance.SoftMarginLoss;
import com.rapidminer.operator.performance.SquaredCorrelationCriterion;
import com.rapidminer.operator.performance.SquaredError;
import com.rapidminer.operator.performance.StrictRelativeError;
import com.rapidminer.operator.performance.WeightedMultiClassPerformance;
import com.rapidminer.operator.performance.cost.ClassificationCostCriterion;
import com.rapidminer.operator.performance.cost.RankingCriterion;
import com.rapidminer.repository.versioned.JsonResolverWithRegistry.NotRegisteredException;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.Averagable;
import com.rapidminer.tools.math.ROCBias;
import com.rapidminer.tools.math.ROCData;

/**
 * Serialization tests for registered classes of {@link JsonStorableAveragableResolver}
 *
 * @author Jan Czogalla
 * @since 9.10.0
 */
public class AveragableSerializationTest {

	private static class TestAveragable extends Averagable {

		private String name;
		private double mikroAv;
		private double mikroVar;
		private int counter;
		@JsonIgnore
		private Attribute ignored;

		public TestAveragable() {
		}

		public TestAveragable(String name, double mikroAv, double mikroVar, int counter, Attribute ignored) {
			this.name = name;
			this.mikroAv = mikroAv;
			this.mikroVar = mikroVar;
			this.counter = counter;
			this.ignored = ignored;
		}

		public TestAveragable(TestAveragable o) {
			super(o);
			this.name = o.getName();
			this.mikroAv = o.getMikroAverage();
			this.mikroVar = o.getMikroVariance();
			this.counter = o.counter;
			this.ignored = o.ignored;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public double getMikroAverage() {
			return mikroAv;
		}

		@Override
		public double getMikroVariance() {
			return mikroVar;
		}

		public int getCounter() {
			return counter;
		}

		public Attribute getIgnored() {
			return ignored;
		}

		@Override
		protected void buildSingleAverage(Averagable averagable) {
			// noop
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestAveragable that = (TestAveragable) o;
			return Double.compare(that.mikroAv, mikroAv) == 0 && Double.compare(that.mikroVar, mikroVar) == 0
					&& counter == that.counter && name.equals(that.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, mikroAv, mikroVar, counter);
		}
	}

	private static ObjectWriter writer;
	private static ObjectReader reader;
	private static TestAveragable original;

	@BeforeClass
	public static void setup() throws NoSuchFieldException, IllegalAccessException {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		RapidMiner.initAsserters();
		Field writerField = JsonIOObjectEntry.class.getDeclaredField("WRITER");
		writerField.setAccessible(true);
		writer = (ObjectWriter) writerField.get(null);
		Field readerField = JsonIOObjectEntry.class.getDeclaredField("READER");
		readerField.setAccessible(true);
		reader = ((ObjectReader) readerField.get(null)).withType(Averagable.class);
		original = new TestAveragable("test averagable", -10, 20.45, 5, AttributeFactory.createAttribute(Ontology.INTEGER));
	}

	@Test
	public void testUnregisteredWriting() throws JsonProcessingException {
		try {
			String written = writer.writeValueAsString(new TestAveragable());
			fail("Should not be writable");
			reader.readValue(written);
		} catch (JsonMappingException e) {
			assertNotNull(e.getCause());
			assertEquals(NotRegisteredException.class, e.getCause().getClass());
			assertEquals(TestAveragable.class.getName(), ((NotRegisteredException) e.getCause()).getClassName());
		}
	}

	@Test(expected = NotRegisteredException.class)
	public void testUnregisteredReading() throws JsonProcessingException {
		JsonStorableAveragableResolver.INSTANCE.register(TestAveragable.class);
		String written = writer.writeValueAsString(new TestAveragable());
		JsonStorableAveragableResolver.INSTANCE.unregister(TestAveragable.class);
		reader.readValue(written);
	}

	@Test
	public void testRegisteredWriteRead() throws JsonProcessingException {
		JsonStorableAveragableResolver.INSTANCE.register(TestAveragable.class);
		String written = writer.writeValueAsString(original);
		TestAveragable read = reader.readValue(written);
		assertEquals(original, read);
		assertNull(read.ignored);
		JsonStorableAveragableResolver.INSTANCE.unregister(TestAveragable.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testAllRegisteredSerialization() throws Exception {
		Map<Class<?>, Averagable> averagables = prepareAveragables();
		Field classToNameField = JsonStorableAveragableResolver.class.getDeclaredField("classToName");
		classToNameField.setAccessible(true);
		Set<Class<?>> registeredClasses = ((Map<Class<?>, String>) classToNameField.get(null)).keySet();
		for (Class<?> registeredClass : registeredClasses) {
			assertTrue("No example for " + registeredClass + " in serialization test", averagables.containsKey(registeredClass));
		}

		for (Averagable averagable : averagables.values()) {
			try {
				String written = writer.writeValueAsString(averagable);
				Averagable readValue = reader.readValue(written);
				if (averagable instanceof PerformanceCriterion) {
					assertEquals(0, ((PerformanceCriterion) averagable).compareTo((PerformanceCriterion) readValue));
				} else if (averagable instanceof AttributeWeight) {
					assertEquals(averagable, readValue);
				}
			} catch (Exception e) {
				String msg = "Error when writing/reading " + averagable.getClass();
				throw new IllegalArgumentException(msg, e);
			}
		}
	}

	@Test
	public void testAverageVectorSerialization() throws Exception {
		PerformanceVector perfVector = new PerformanceVector();
		perfVector.addCriterion(prepareObject(new Margin(), "margin", 20d));
		ObjectReader iooReader = reader.withType(JsonStorableIOObject.class);

		String written = writer.writeValueAsString(perfVector);
		JsonStorableIOObject readValue = iooReader.readValue(written);
		assertEquals(0, perfVector.compareTo(readValue));

		AttributeWeights weights = new AttributeWeights();
		weights.setWeight("a", 0.5);
		weights.setWeight("b", 0.5);

		written = writer.writeValueAsString(weights);
		readValue = iooReader.readValue(written);
		assertEquals(0, weights.compareTo(readValue));
	}

	private Map<Class<?>, Averagable> prepareAveragables() throws Exception {
		AttributeWeights weights = new AttributeWeights();
		List<Averagable> averagables = new ArrayList<>();
		averagables.add(new AttributeWeight(weights, "att1", 5));
		averagables.add(prepareObject(new RankCorrelation(RankCorrelation.TAU), "value", 20d));
		averagables.add(prepareObject(new Margin(), "margin", 20d));
		averagables.add(prepareObject(new NormalizedAbsoluteError(), "deviationSum", 20d));
		List<ROCData> rocDataList = new LinkedList<>(Collections.singletonList(prepareObject(new ROCData(), "sumPos", 20d)));
		averagables.add(prepareObject(new AreaUnderCurve(ROCBias.OPTIMISTIC), "rocData", rocDataList));
		averagables.add(prepareObject(new Optimistic(), "rocData", rocDataList));
		averagables.add(prepareObject(new Neutral(), "rocData", rocDataList));
		averagables.add(prepareObject(new Pessimistic(), "rocData", rocDataList));
		averagables.add(prepareObject(new WeightedMultiClassPerformance(WeightedMultiClassPerformance.WEIGHTED_PRECISION),
				"counter", new double[][]{{0, 1, 2}, {3, 4, 5}, {6, 7, 8}}, "classNames", new String[]{"a", "b", "c"},
				"classWeights", new double[]{.3, .3, .4}, "classNameMap", Collections.singletonMap("a", 12)));
		averagables.add(prepareObject(new MDLCriterion(MDLCriterion.MAXIMIZATION), "length", 20));
		averagables.add(prepareObject(new PredictionAverage(), "sun", 20d));
		averagables.add(prepareObject(new SoftMarginLoss(), "margin", 20d));
		averagables.add(new MinMaxCriterion((MeasuredPerformance) averagables.get(1), 0.5));
		Attribute label = AttributeFactory.createAttribute("label", Ontology.NOMINAL);
		label.getMapping().mapString("a");
		label.getMapping().mapString("b");
		averagables.add(new RankingCriterion(new int[]{0, 1, 2}, new double[]{3, 4, 5},
				ExampleSets.from(label,
						AttributeFactory.createAttribute(Attributes.CONFIDENCE_NAME + "_a", Ontology.NUMERICAL),
						AttributeFactory.createAttribute(Attributes.CONFIDENCE_NAME + "_b", Ontology.NUMERICAL)
				)
						.withRole(label, Attributes.LABEL_NAME)
						.build()));
		averagables.add(prepareObject(new LogisticLoss(), "loss", 20d));
		averagables.add(new RelativeError());
		averagables.add(new SimpleClassificationError());
		averagables.add(new LenientRelativeError());
		averagables.add(new SquaredError());
		averagables.add(new RootMeanSquaredError());
		averagables.add(new AbsoluteError());
		averagables.add(new StrictRelativeError());
		averagables.add(prepareObject(new CorrelationCriterion(), "sumLabel", 20d));
		averagables.add(prepareObject(new SquaredCorrelationCriterion(), "sumLabel", 20d));
		averagables.add(prepareObject(new RootRelativeSquaredError(), "deviationSum", 20d));
		averagables.add(prepareObject(new BinaryClassificationPerformance(BinaryClassificationPerformance.TRUE_NEGATIVE),
				"negativeClassName", "negative"));
		averagables.add(prepareObject(new CrossEntropy(), "value", 20d));
		averagables.add(prepareObject(new MultiClassificationPerformance(MultiClassificationPerformance.KAPPA),
				"classNameMap", Collections.singletonMap("a", 12),
				"counter", new double[][]{{0, 1, 2}, {3, 4, 5}, {6, 7, 8}}));
		averagables.add(prepareObject(new ClassificationCostCriterion(new double[][]{{0, 1, 2}, {3, 4, 5}}, label,
						AttributeFactory.createAttribute(Attributes.PREDICTION_NAME, Ontology.NOMINAL)),
				"classOrderMap", Collections.singletonMap("a", 1)));
		averagables.add(new EstimatedPerformance("estimate", 20d, 13, true));

		return averagables.stream().collect(Collectors.toMap(Object::getClass, a -> a, (a, b) -> a, LinkedHashMap::new));
	}

	private <T> T prepareObject(T object, Object... nameAndValue) throws Exception {
		Class<?> aClass = object.getClass();
		for (int i = 0; i < nameAndValue.length - 1; i += 2) {
			Class<?> checkClass = aClass;
			while (checkClass != Object.class) {
				try {
					Field field = aClass.getDeclaredField((String) nameAndValue[i]);
					field.setAccessible(true);
					field.set(object, nameAndValue[i + 1]);
					break;
				} catch (NoSuchFieldException e) {
					checkClass = checkClass.getSuperclass();
				}
			}
		}
		return object;
	}
}
