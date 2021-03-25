/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.ports.metadata.table;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.LegacyRole;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.test.ExampleTestTools;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.gui.processeditor.results.DisplayContext;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.ToTableMetaDataConverter;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Tests for {@link FromTableMetaDataConverter} and {@link ToTableMetaDataConverter}.
 *
 * @author Kevin Majchrzak
 * @since 9.9.0
 */
@RunWith(Enclosed.class)
public class TableMetaDataConverterTest {

	private static final String TIMEZONE =
			Tools.getPreferredTimeZone().toZoneId().getDisplayName(TextStyle.SHORT, Locale.getDefault());
	private static final DateTimeFormatter FORMATTER =
			DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);

	/**
	 * Converts TableMetaData to ExampleSetMetaData and back to TableMetaData and checks that the TMD is valid.
	 */
	public static class DoubleConvertedTableMD {

		private static final TableMetaData converted;
		private static final TableMetaData original;

		static {
			original = original();
			converted = converted();
		}

		private static TableMetaData converted() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);
			return ToTableMetaDataConverter.convert(emd);
		}

		private static TableMetaData original() {
			TableMetaData tmd = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean with pos", "positive value", null, new MDInteger(0))
					.addBoolean("boolean with pos/neg", "positive value", "negative value", new MDInteger(0))
					.addBoolean("boolean without pos/neg", new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
							ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("text", ColumnType.TEXT, new MDInteger(5))
					.add("unknown", null, null)
					.add("real2", ColumnType.REAL, new MDInteger(10))
					.addColumnMetaData("nominal", ColumnRole.SCORE)
					.addColumnMetaData("nominal", new ColumnReference("unknown", "YES"))
					.addColumnMetaData("date_time", ColumnRole.SCORE)
					.addColumnMetaData("date_time", new ColumnReference("whatever", null))
					.addColumnMetaData("int", ColumnRole.SOURCE)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("real2", ColumnRole.METADATA)
					.addColumnMetaData("unknown", ColumnRole.PREDICTION)
					.addColumnMetaData("time", new ColumnAnnotation("bla"))
					.columnsAreKnown().build();
			tmd.addToHistory(generateDummyOutputPort());
			tmd.addToHistory(generateDummyOutputPort());
			tmd.addToHistory(generateDummyOutputPort());
			Annotations annotations = new Annotations();
			annotations.put("key", "myValue");
			tmd.setAnnotations(annotations);
			tmd.getKeyValueMap().put("addDataKey1", 1.2);
			tmd.getKeyValueMap().put("addDataKey2", "Test");
			return tmd;
		}

		@Test
		public void testTableMetaInformation() {
			assertEquals(original.height().getNumber(), converted.height().getNumber());
			assertEquals(original.getGenerationHistory(), converted.getGenerationHistory());
			assertEquals(original.getAnnotations(), converted.getAnnotations());
			assertEquals(original.getKeyValueMap().get("addDataKey1"), converted.getKeyValueMap().get("addDataKey1"));
			assertEquals(original.getKeyValueMap().get("addDataKey2"), converted.getKeyValueMap().get("addDataKey2"));

			assertSame(original.height().getRelation(), converted.height().getRelation());
			assertNotEquals(original.getDescription(), converted.getDescription());
			assertNotEquals(original.getShortDescription(), converted.getShortDescription());

			TableMetaData withOutAdvancedColumns = new TableMetaDataBuilder(original)
					.remove("set")
					.remove("text").build();
			// need to re-add the additional data cause TableMetaDataBuilder ignores it
			withOutAdvancedColumns.getKeyValueMap().put("addDataKey1", 1.2);
			withOutAdvancedColumns.getKeyValueMap().put("addDataKey2", "Test");

			assertEquals(withOutAdvancedColumns.getDescription(), converted.getDescription());
			assertEquals(withOutAdvancedColumns.getShortDescription(), converted.getShortDescription());
			assertEquals(withOutAdvancedColumns.labels().size(), converted.labels().size());
		}

		@Test
		public void testBoolean() {
			assertEquals("positive value",
					converted.column("boolean with pos/neg").getDictionary().getPositiveValue().get());
			assertEquals("negative value",
					converted.column("boolean with pos/neg").getDictionary().getNegativeValue().get());

			assertEquals(original.column("boolean with pos/neg").getDictionary().getPositiveValue(),
					converted.column("boolean with pos/neg").getDictionary().getPositiveValue());
			assertEquals(original.column("boolean with pos/neg").getDictionary().getNegativeValue(),
					converted.column("boolean with pos/neg").getDictionary().getNegativeValue());

			assertFalse(converted.column("boolean without pos/neg").getDictionary().getPositiveValue().isPresent());
			assertFalse(converted.column("boolean without pos/neg").getDictionary().getNegativeValue().isPresent());
		}

		@Test
		public void testColumnMetaData() {
			TableMetaData withOutLegacyRoles = new TableMetaDataBuilder(converted)
					.removeColumnMetaData("real", LegacyRole.class)
					.removeColumnMetaData("real2", LegacyRole.class)
					.removeColumnMetaData("boolean with pos/neg", LegacyType.class)
					.removeColumnMetaData("boolean without pos/neg", LegacyType.class).build();
			assertEquals(original.getMetaDataMap(), withOutLegacyRoles.getMetaDataMap());
		}

		@Test
		public void testContains() {
			assertSame(MetaDataInfo.NO, converted.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.YES, converted.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.YES, converted.hasColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.NO, converted.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.NO, converted.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.NO, converted.hasUniqueColumnMetaData(ColumnRole.ID));
			assertSame(MetaDataInfo.YES, converted.hasUniqueColumnMetaData(ColumnRole.SOURCE));
			assertSame(MetaDataInfo.YES, converted.contains("real"));
			assertSame(MetaDataInfo.NO, converted.contains("integer"));
			assertSame(MetaDataInfo.YES, converted.containsType(ColumnType.NOMINAL, true));
			assertSame(MetaDataInfo.NO, converted.containsType(ColumnType.REAL, false));
			assertSame(MetaDataInfo.NO, converted.containsType(null, false));
			assertSame(MetaDataInfo.YES, converted.containsType(null, true));
			assertSame(MetaDataInfo.YES, converted.column("nominal").hasMissingValues());
			assertSame(MetaDataInfo.NO, converted.column("boolean with pos/neg").hasMissingValues());
			assertSame(MetaDataInfo.NO, converted.column("int").hasMissingValues());
			assertSame(MetaDataInfo.YES, converted.column("real").hasMissingValues());
		}

		@Test
		public void testDuplicateRole(){
			TableMetaData withDuplicate = new TableMetaDataBuilder(original)
					.addColumnMetaData("real", ColumnRole.ID)
					.addColumnMetaData("real2", ColumnRole.ID).build();
			TableMetaData convertedTMD = ToTableMetaDataConverter
					.convert(FromTableMetaDataConverter.convert(withDuplicate));
			Set<String> expected = new HashSet<>();
			expected.add("real");
			expected.add("real2");
			assertEquals(expected, new HashSet<>(convertedTMD.selectByColumnMetaData(ColumnRole.ID)));
		}

		@Test
		public void testShrunkValueSet(){
			String[] values = new String[AttributeMetaData.getMaximumNumberOfNominalValues() + 1];
			Arrays.setAll(values, i -> "val" + i);
			final ColumnInfo info =
					new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(0).setDictionaryValues(Arrays.asList(values),
							SetRelation.EQUAL).build();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(AttributeMetaData.getMaximumNumberOfNominalValues() * 2);
			builder.add("large nominal", info);
			builder.addNominal("nominal", Collections.singletonList("value"), SetRelation.EQUAL, new MDInteger(0));

			TableMetaData tmd = builder.build();
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(tmd);
			TableMetaData convertedTMD = ToTableMetaDataConverter.convert(emd);

			assertTrue(tmd.column("large nominal").getDictionary().valueSetWasShrunk());
			assertTrue(emd.getAttributeByName("large nominal").valueSetWasShrunk());
			assertTrue(convertedTMD.column("large nominal").getDictionary().valueSetWasShrunk());

			assertFalse(tmd.column("nominal").getDictionary().valueSetWasShrunk());
			assertFalse(emd.getAttributeByName("nominal").valueSetWasShrunk());
			assertFalse(convertedTMD.column("nominal").getDictionary().valueSetWasShrunk());

			emd.getAttributeByName("nominal").valueSetIsShrunk(true);
			TableMetaData modifiedTMD = ToTableMetaDataConverter.convert(emd);

			assertTrue(emd.getAttributeByName("nominal").valueSetWasShrunk());
			assertTrue(modifiedTMD.column("nominal").getDictionary().valueSetWasShrunk());
		}

	}

	/**
	 * Converts ExampleSetMetaData to TableMetaData and back to ExampleSetMetaData and checks that the EMD is valid.
	 */
	public static class DoubleConvertedExampleSetMD {

		private static final ExampleSetMetaData converted;
		private static final ExampleSetMetaData original;

		static {
			original = original();
			converted = converted();
		}

		private static ExampleSetMetaData converted() {
			TableMetaData tmd = ToTableMetaDataConverter.convert(original);
			return FromTableMetaDataConverter.convert(tmd);
		}

		private static ExampleSetMetaData original() {
			ExampleSetMetaData emd = new ExampleSetMetaData();
			emd.setNumberOfExamples(20);
			emd.addAttribute(new AttributeMetaData("nominal", Attributes.CONFIDENCE_NAME + "_YES", "value"));
			AttributeMetaData shrunk = new AttributeMetaData("shrunk", null, "value");
			shrunk.valueSetIsShrunk(true);
			emd.addAttribute(shrunk);
			AttributeMetaData binominal = new AttributeMetaData("boolean", null, Ontology.BINOMINAL);
			Set<String> valueSet = new HashSet<>();
			valueSet.add("negative value");
			valueSet.add("positive value");
			binominal.setValueSet(valueSet, SetRelation.EQUAL);
			emd.addAttribute(new AttributeMetaData("integer", "source", Ontology.INTEGER));
			AttributeMetaData real = new AttributeMetaData("real", "cool_role", Ontology.REAL);
			real.setNumberOfMissingValues(new MDInteger(10));
			real.setValueRange(new Range(2,2), SetRelation.EQUAL);
			emd.addAttribute(real);
			emd.addAttribute(new AttributeMetaData("real2", "cool_role_2", Ontology.REAL));
			AttributeMetaData dateTime = new AttributeMetaData("date_time", Ontology.DATE_TIME);
			dateTime.setValueRange(new Range(Instant.EPOCH.toEpochMilli(), Instant.EPOCH.toEpochMilli()), SetRelation.SUBSET);
			emd.addAttribute(dateTime);
			AttributeMetaData time = new AttributeMetaData("time", Ontology.TIME);
			time.setValueRange(new Range(BeltConverter.nanoOfDayToLegacyTime(LocalTime.MIDNIGHT.toNanoOfDay()), BeltConverter.nanoOfDayToLegacyTime(LocalTime.NOON.toNanoOfDay())), SetRelation.EQUAL);
			time.getAnnotations().put(Annotations.KEY_COMMENT, "bla");
			emd.addAttribute(time);
			AttributeMetaData unknown = new AttributeMetaData("unknown", Attributes.PREDICTION_NAME, Ontology.ATTRIBUTE_VALUE);
			unknown.setValueSetRelation(SetRelation.UNKNOWN);
			emd.addAttribute(unknown);
			emd.addAttribute(new AttributeMetaData("file", Ontology.FILE_PATH));

			emd.attributesAreSuperset();
			emd.addToHistory(generateDummyOutputPort());
			emd.addToHistory(generateDummyOutputPort());
			emd.addToHistory(generateDummyOutputPort());
			Annotations annotations = new Annotations();
			annotations.put("key", "myValue");
			emd.setAnnotations(annotations);
			emd.addAdditionalData("addDataKey1", 1.2);
			emd.addAdditionalData("addDataKey2", "Test");
			return emd;
		}

		@Test
		public void testMetaInformation() {
			assertEquals(original.getNumberOfExamples().getNumber(), converted.getNumberOfExamples().getNumber());
			assertEquals(original.getGenerationHistory(), converted.getGenerationHistory());
			assertEquals(original.getAnnotations(), converted.getAnnotations());
			assertEquals(original.getAdditionalData("addDataKey1"), converted.getAdditionalData("addDataKey1"));
			assertEquals(original.getAdditionalData("addDataKey2"), converted.getAdditionalData("addDataKey2"));

			assertSame(original.getNumberOfExamples().getRelation(), converted.getNumberOfExamples().getRelation());

			assertEquals(original.getNumberOfRegularAttributes(), converted.getNumberOfRegularAttributes());
			assertSame(SetRelation.SUPERSET, converted.getAttributeSetRelation());
		}

		@Test
		public void testEquals() {
			assertEquals(original.getShortDescription(), converted.getShortDescription());
			assertEquals(original.getDescription(), converted.getDescription());
			for(AttributeMetaData amd : original.getAllAttributes()){
				assertEquals(amd.getDescription(), converted.getAttributeByName(amd.getName()).getDescription());
			}
			for(AttributeMetaData amd : converted.getAllAttributes()){
				assertEquals(amd.getDescription(), original.getAttributeByName(amd.getName()).getDescription());
			}
		}

		@Test
		public void testShrunkValueSet(){
			assertFalse(converted.getAttributeByName("nominal").valueSetWasShrunk());
			assertTrue(converted.getAttributeByName("shrunk").valueSetWasShrunk());
		}
	}

	/**
	 * Converts ExampleSetMetaData to TableMetaData and checks that the TMD is valid.
	 */
	public static class ConvertedEMD {

		private static final TableMetaData converted;
		private static final ExampleSetMetaData original;

		static {
			original = original();
			converted = converted();
		}

		private static TableMetaData converted() {
			return ToTableMetaDataConverter.convert(original);
		}

		private static ExampleSetMetaData original() {
			ExampleSetMetaData emd = new ExampleSetMetaData();
			emd.setNumberOfExamples(20);
			emd.addAttribute(new AttributeMetaData("nominal", Attributes.CONFIDENCE_NAME + "_YES", "value"));
			AttributeMetaData shrunk = new AttributeMetaData("shrunk", null, "value");
			shrunk.valueSetIsShrunk(true);
			emd.addAttribute(shrunk);
			AttributeMetaData binominal = new AttributeMetaData("boolean", null, Ontology.BINOMINAL);
			Set<String> valueSet = new HashSet<>();
			valueSet.add("negative value");
			valueSet.add("positive value");
			binominal.setValueSet(valueSet, SetRelation.EQUAL);
			emd.addAttribute(binominal);
			emd.addAttribute(new AttributeMetaData("integer", "source", Ontology.INTEGER));
			AttributeMetaData real = new AttributeMetaData("real", "cool_role", Ontology.REAL);
			real.setNumberOfMissingValues(new MDInteger(10));
			real.setValueRange(new Range(2, 2), SetRelation.EQUAL);
			emd.addAttribute(real);
			emd.addAttribute(new AttributeMetaData("real2", "cool_role_2", Ontology.REAL));
			AttributeMetaData dateTime = new AttributeMetaData("date_time", Ontology.DATE_TIME);
			dateTime.setValueRange(new Range(Instant.EPOCH.toEpochMilli(), Instant.EPOCH.toEpochMilli()),
					SetRelation.SUBSET);
			emd.addAttribute(dateTime);
			AttributeMetaData time = new AttributeMetaData("time name", Ontology.TIME);
			time.setValueRange(new Range(BeltConverter.nanoOfDayToLegacyTime(LocalTime.NOON.toNanoOfDay()),
					BeltConverter.nanoOfDayToLegacyTime(LocalTime.MIDNIGHT.toNanoOfDay())), SetRelation.EQUAL);
			time.getAnnotations().put(Annotations.KEY_COMMENT, "bla");
			emd.addAttribute(time);
			AttributeMetaData unknown =
					new AttributeMetaData("unknown", Attributes.PREDICTION_NAME, Ontology.ATTRIBUTE_VALUE);
			unknown.setValueSetRelation(SetRelation.UNKNOWN);
			emd.addAttribute(unknown);
			emd.addAttribute(new AttributeMetaData("file", Ontology.FILE_PATH));

			emd.addToHistory(generateDummyOutputPort());
			emd.addToHistory(generateDummyOutputPort());
			emd.addToHistory(generateDummyOutputPort());
			Annotations annotations = new Annotations();
			annotations.put("key", "myValue");
			emd.setAnnotations(annotations);
			emd.addAdditionalData("addDataKey1", 1.2);
			emd.addAdditionalData("addDataKey2", "Test");
			return emd;
		}

		@Test
		public void testTableMetaInformation() {
			assertEquals(original.getNumberOfExamples().getNumber(), converted.height().getNumber());
			assertEquals(original.getGenerationHistory(), converted.getGenerationHistory());
			assertEquals(original.getAnnotations(), converted.getAnnotations());
			assertEquals(original.getAdditionalData("addDataKey1"), converted.getKeyValueMap().get("addDataKey1"));
			assertEquals(original.getAdditionalData("addDataKey2"), converted.getKeyValueMap().get("addDataKey2"));

			assertSame(original.getNumberOfExamples().getRelation(), converted.height().getRelation());
			assertEquals(original.getAllAttributes().size(), converted.labels().size());
			assertEquals(original.getNumberOfRegularAttributes(),
					converted.labels().size() - converted.selectByColumnMetaData(ColumnRole.class).size());
		}

		@Test
		public void testEquals() {
			assertEquals(original.getDescription()
							.replace(">time</td><td>= [" + LocalTime.NOON.format(FORMATTER) + " " + TIMEZONE + "..." +
									LocalTime.MIDNIGHT.format(FORMATTER) + " " + TIMEZONE +
									"]", ">time</td><td>= [00:00...00:00]")
							.replace("ExampleSet", "IOTable")
							.replace("confidence_YES", "score")
							.replace("file</td><td>file_path", "file</td><td>nominal")
							.replace(">cool_role<", ">metadata<")
							.replace(">cool_role_2<", ">metadata<")
					, converted.getDescription());
			assertEquals(original.getShortDescription()
							.replace("ExampleSet", "IOTable")
					, converted.getShortDescription());
		}

		@Test
		public void testContains() {
			assertSame(MetaDataInfo.NO, converted.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.YES, converted.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.YES, converted.hasColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.NO, converted.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.NO, converted.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.NO, converted.hasUniqueColumnMetaData(ColumnRole.ID));
			assertSame(MetaDataInfo.YES, converted.hasUniqueColumnMetaData(ColumnRole.SOURCE));
			assertSame(MetaDataInfo.YES, converted.contains("real"));
			assertSame(MetaDataInfo.YES, converted.contains("integer"));
			assertSame(MetaDataInfo.YES, converted.containsType(ColumnType.NOMINAL, true));
			assertSame(MetaDataInfo.NO, converted.containsType(ColumnType.REAL, false));
			assertSame(MetaDataInfo.NO, converted.containsType(null, false));
			assertSame(MetaDataInfo.YES, converted.containsType(null, true));
			assertSame(MetaDataInfo.NO, converted.column("nominal").hasMissingValues());
			assertSame(MetaDataInfo.NO, converted.column("integer").hasMissingValues());
			assertSame(MetaDataInfo.YES, converted.column("real").hasMissingValues());
		}

		@Test
		public void testShrunkValueSet() {
			assertTrue(converted.column("shrunk").getDictionary().valueSetWasShrunk());
			assertFalse(converted.column("boolean").getDictionary().valueSetWasShrunk());
			assertFalse(converted.column("file").getDictionary().valueSetWasShrunk());
		}

		@Test
		public void testInvalidDateRange() {
			ObjectRange<LocalTime> range = converted.column("time name").getObjectRange(LocalTime.class).get();
			assertEquals(range.getLower(), range.getUpper());
			// this does not really make a lot of sense
			// - just done to check that that lower is set to upper if lower originally was > upper
			assertEquals(range.getLower(), LocalTime.MIDNIGHT);
		}
	}

	/**
	 * Converts TableMetaData to ExampleSetMetaData and checks that the EMD is valid.
	 */
	public static class ConvertedTMD {
		private static final ExampleSetMetaData converted;
		private static final TableMetaData original;

		static {
			original = original();
			converted = converted();
		}

		private static ExampleSetMetaData converted() {
			return FromTableMetaDataConverter.convert(original);
		}

		private static TableMetaData original() {
			TableMetaData tmd = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addNominal("pseudo binominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(0) )
					.addBoolean("boolean with pos", "positive value", null, new MDInteger(0))
					.addBoolean("boolean with pos/neg", "positive value", "negative value", new MDInteger(0))
					.addBoolean("boolean without pos/neg", new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
							ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("text", ColumnType.TEXT, new MDInteger(5))
					.add("unknown", null, null)
					.add("real2", ColumnType.REAL, new MDInteger(10))
					.addColumnMetaData("pseudo binominal", LegacyType.BINOMINAL)
					.addColumnMetaData("nominal", ColumnRole.SCORE)
					.addColumnMetaData("nominal", new ColumnReference("unknown", "YES"))
					.addColumnMetaData("int", ColumnRole.SOURCE)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("real2", ColumnRole.METADATA)
					.addColumnMetaData("unknown", ColumnRole.PREDICTION)
					.addColumnMetaData("time", new ColumnAnnotation("bla"))
					.columnsAreKnown().build();
			tmd.addToHistory(generateDummyOutputPort());
			tmd.addToHistory(generateDummyOutputPort());
			tmd.addToHistory(generateDummyOutputPort());
			Annotations annotations = new Annotations();
			annotations.put("key", "myValue");
			tmd.setAnnotations(annotations);
			tmd.getKeyValueMap().put("addDataKey1", 1.2);
			tmd.getKeyValueMap().put("addDataKey2", "Test");
			return tmd;
		}

		@Test
		public void testTableMetaInformation() {
			assertEquals(original.height().getNumber(), converted.getNumberOfExamples().getNumber());
			assertEquals(original.getGenerationHistory(), converted.getGenerationHistory());
			assertEquals(original.getAnnotations(), converted.getAnnotations());
			assertEquals(original.getKeyValueMap().get("addDataKey1"), converted.getAdditionalData("addDataKey1"));
			assertEquals(original.getKeyValueMap().get("addDataKey2"), converted.getAdditionalData("addDataKey2"));

			assertSame(original.height().getRelation(), converted.getNumberOfExamples().getRelation());
			assertNotEquals(original.getDescription(), converted.getDescription());
			assertNotEquals(original.getShortDescription(), converted.getShortDescription());

			TableMetaData withOutAdvancedColumns = new TableMetaDataBuilder(original)
					.remove("set")
					.remove("text").build();
			// need to re-add the additional data cause TableMetaDataBuilder ignores it
			withOutAdvancedColumns.getKeyValueMap().put("addDataKey1", 1.2);
			withOutAdvancedColumns.getKeyValueMap().put("addDataKey2", "Test");

			assertEquals(withOutAdvancedColumns.getDescription()
							.replace("IOTable", "ExampleSet")
							.replace("score", "confidence_YES")
							.replace("boolean with pos</td><td>binominal", "boolean with pos</td><td>nominal")
							.replace("time</td><td>= [12:00...12:00]",
									"time</td><td>= [" + LocalTime.NOON.format(FORMATTER) + " " + TIMEZONE +
											"..." + LocalTime.NOON.format(FORMATTER) + " " + TIMEZONE + "]")
							.replace("metadata</td><td>real2", "metadata_2</td><td>real2")
							.replace("pseudo binominal</td><td>nominal", "pseudo binominal</td><td>binominal"),
					converted.getDescription());
			assertEquals(withOutAdvancedColumns.getShortDescription().replace("IOTable", "ExampleSet"),
					converted.getShortDescription());
			assertArrayEquals(withOutAdvancedColumns.labels().toArray(),
					converted.getAllAttributes().stream().map(AttributeMetaData::getName).toArray());
		}

		@Test
		public void testBoolean() {
			Set<String> posNeg = new HashSet<>();
			posNeg.add("positive value");
			posNeg.add("negative value");
			assertEquals(posNeg, converted.getAttributeByName("boolean with pos/neg").getValueSet());
			assertTrue(converted.getAttributeByName("boolean with pos/neg").isBinominal());

			Set<String> pos = new HashSet<>();
			pos.add("positive value");
			assertEquals(pos, converted.getAttributeByName("boolean with pos").getValueSet());
			assertFalse(converted.getAttributeByName("boolean with pos").isBinominal());

			assertEquals(Collections.emptySet(), converted.getAttributeByName("boolean without pos/neg").getValueSet());
			assertTrue(converted.getAttributeByName("boolean without pos/neg").isBinominal());
		}

		@Test
		public void testRoles() {
			assertEquals(Attributes.CONFIDENCE_NAME + "_YES", converted.getAttributeByName("nominal").getRole());
			assertEquals("source", converted.getAttributeByName("int").getRole());
			assertEquals(Attributes.PREDICTION_NAME, converted.getAttributeByName("unknown").getRole());
			assertEquals("metadata", converted.getAttributeByName("real").getRole());
			assertEquals("metadata_2", converted.getAttributeByName("real2").getRole());
			assertEquals(new HashSet<>(original.selectByColumnMetaData(ColumnRole.class)),
					converted.getAllAttributes().stream().filter(a -> a.getRole() != null)
							.map(AttributeMetaData::getName).collect(Collectors.toSet()));
		}
	}

	/**
	 * Tests that meta data conversion is consistent with data conversion.
	 */
	public static class DerivedMD {

		/**
		 * Converts {@link ExampleSet} to {@link IOTable} and then derives metadata from both and checks that they are
		 * equal after conversion.
		 */
		@Test
		public void testExampleSet() {
			ExampleSetBuilder builder = ExampleSets.from(ExampleTestTools.createFourAttributes());
			builder.addRow(new double[] {1, 1, 10, -20.3})
			.addRow(new double[] {2, 0, 9, 10.1});
			ExampleSet exampleSet = builder.build();
			exampleSet.getAnnotations().put("myKey", "myValue");
			IOTable ioTable = BeltConverter.convert(exampleSet, new SequentialConcurrencyContext());
			ExampleSetMetaData directEMD = new ExampleSetMetaData(exampleSet);
			directEMD.setAnnotations(exampleSet.getAnnotations());
			TableMetaData tmd = new TableMetaData(ioTable, false);
			ExampleSetMetaData indirectEMD = FromTableMetaDataConverter.convert(tmd);

			// * need to remove mean because we do not store mean in TableMetaData
			// * need to remove underline for mode in value set
			// because we do not store mode in TableMetaData
			assertEquals(directEMD.getDescription()
					.replace("; mean =9.500", "")
					.replace("; mean =-5.100", "")
					.replace("<span style=\"text-decoration:underline\">", "")
					.replace("</span>", "")
					, indirectEMD.getDescription());
			assertEquals(directEMD.getShortDescription(), indirectEMD.getShortDescription());
		}

		/**
		 * Converts {@link IOTable} to {@link ExampleSet} and then derives metadata from both and checks that they are
		 * equal after conversion.
		 */
		@Test
		public void testTable() {
			Table table = Builders.newTableBuilder(10)
					.addNominal("nominal", i -> "i")
					.addBoolean("boolean", i -> null, null)
					.addInt53Bit("int", i -> i)
					.addDateTime("date_time", i -> Instant.ofEpochMilli(i * 1000))
					.addTime("time", i -> LocalTime.ofNanoOfDay(i * 1_000_000))
					.addMetaData("nominal", ColumnRole.SCORE)
					.addMetaData("nominal", new ColumnReference("unknown", "YES"))
					.addMetaData("date_time", ColumnRole.SCORE)
					.addMetaData("date_time", new ColumnReference("whatever", null))
					.addMetaData("int", ColumnRole.SOURCE).build(new DisplayContext());
			IOTable ioTable = new IOTable(table);
			ioTable.getAnnotations().put("key", "myValue");

			TableMetaData directTMD = new TableMetaData(ioTable, false);

			ExampleSet exampleSet = BeltConverter.convert(ioTable, new SequentialConcurrencyContext());
			ExampleSetMetaData emd = new ExampleSetMetaData(exampleSet);
			emd.setAnnotations(exampleSet.getAnnotations());
			TableMetaData indirectTMD = ToTableMetaDataConverter.convert(emd);

			// * need to replace equals with superset because of heuristic for conversion that
			// fails in this case but that we want to keep
			assertEquals(directTMD.getDescription()
							.replace("binominal</td><td>=", "binominal</td><td>\u2287"),
					indirectTMD.getDescription());
			assertEquals(directTMD.getShortDescription(), indirectTMD.getShortDescription());
		}

	}

	/**
	 * Converts TableMetaData to ExampleSetMetaData, changes the value sets, and back to TableMetaData.
	 */
	public static class CopyOnWrite {

		private static final TableMetaData original;

		static {
			original = original();
		}

		private static TableMetaData converted() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);
			return ToTableMetaDataConverter.convert(emd);
		}

		private static TableMetaData original() {
			return new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean with pos", "positive value", null, new MDInteger(0))
					.addBoolean("boolean with pos/neg", "positive value", "negative value", new MDInteger(0))
					.addBoolean("boolean without pos/neg", new MDInteger(0))
					.addNominal("more", Arrays.asList("one", "two", "three"), SetRelation.EQUAL, null)
					.add("type", ColumnType.NOMINAL, new MDInteger(0))
					.addColumnMetaData("nominal", ColumnRole.SCORE)
					.addColumnMetaData("nominal", new ColumnReference("unknown", "YES"))
					.columnsAreKnown().build();
		}

		@Test
		public void testBackAndForth() {
			TableMetaData converted = converted();
			assertSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testAdd() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);
			final Set<String> nominalValueSet = emd.getAttributeByName("nominal").getValueSet();
			assertTrue(nominalValueSet instanceof CopyOnWriteValueSet);
			assertTrue(((CopyOnWriteValueSet) nominalValueSet).isUnchanged());
			assertFalse(nominalValueSet.containsAll(Arrays.asList("value", "blup")));
			nominalValueSet.add("blup");
			assertFalse(((CopyOnWriteValueSet) nominalValueSet).isUnchanged());

			final TableMetaData converted = ToTableMetaDataConverter.convert(emd);
			assertNotSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testAddAll() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);

			final Set<String> nominalValueSet = emd.getAttributeByName("nominal").getValueSet();
			assertTrue(nominalValueSet instanceof CopyOnWriteValueSet);
			assertTrue(((CopyOnWriteValueSet) nominalValueSet).isUnchanged());
			assertTrue(nominalValueSet.contains("value"));
			nominalValueSet.addAll(Arrays.asList("bla", "blup"));
			assertFalse(((CopyOnWriteValueSet) nominalValueSet).isUnchanged());

			final TableMetaData converted = ToTableMetaDataConverter.convert(emd);
			assertNotSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testRemove() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);

			final Set<String> nominalValueSet = emd.getAttributeByName("nominal").getValueSet();
			assertTrue(nominalValueSet instanceof CopyOnWriteValueSet);
			assertTrue(((CopyOnWriteValueSet) nominalValueSet).isUnchanged());
			assertFalse(nominalValueSet.isEmpty());
			nominalValueSet.remove("value");
			assertFalse(((CopyOnWriteValueSet) nominalValueSet).isUnchanged());

			final TableMetaData converted = ToTableMetaDataConverter.convert(emd);
			assertNotSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testClear() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);

			final Set<String> nominalValueSet = emd.getAttributeByName("nominal").getValueSet();
			assertTrue(nominalValueSet instanceof CopyOnWriteValueSet);
			assertTrue(((CopyOnWriteValueSet) nominalValueSet).isUnchanged());
			assertEquals(1, nominalValueSet.size());
			nominalValueSet.clear();
			assertFalse(((CopyOnWriteValueSet) nominalValueSet).isUnchanged());

			final TableMetaData converted = ToTableMetaDataConverter.convert(emd);
			assertNotSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testIterator() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);

			final Set<String> moreValueSet = emd.getAttributeByName("more").getValueSet();
			assertTrue(moreValueSet instanceof CopyOnWriteValueSet);
			assertTrue(((CopyOnWriteValueSet) moreValueSet).isUnchanged());
			assertArrayEquals(new String[]{"one", "three", "two"}, moreValueSet.toArray(new String[0]));
			final Iterator<String> iterator = moreValueSet.iterator();
			assertEquals("one", iterator.next());
			assertTrue(iterator.hasNext());
			iterator.next();
			iterator.remove();
			assertTrue(iterator.hasNext());
			assertEquals("two", iterator.next());
			assertEquals("one", moreValueSet.iterator().next());
			assertFalse(((CopyOnWriteValueSet) moreValueSet).isUnchanged());

			final TableMetaData converted = ToTableMetaDataConverter.convert(emd);
			assertSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertNotSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testRemoveAll() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);

			final Set<String> moreValueSet = emd.getAttributeByName("more").getValueSet();
			assertTrue(moreValueSet instanceof CopyOnWriteValueSet);
			assertTrue(((CopyOnWriteValueSet) moreValueSet).isUnchanged());
			assertArrayEquals(moreValueSet.toArray(), moreValueSet.stream().toArray());
			moreValueSet.removeAll(Arrays.asList("one", "two"));
			List<String> testList = new ArrayList<>();
			moreValueSet.forEach(testList::add);
			assertEquals(Collections.singletonList("three"), testList);
			assertFalse(((CopyOnWriteValueSet) moreValueSet).isUnchanged());

			final TableMetaData converted = ToTableMetaDataConverter.convert(emd);
			assertSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertNotSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testRetainAll() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);

			final Set<String> moreValueSet = emd.getAttributeByName("more").getValueSet();
			assertTrue(moreValueSet instanceof CopyOnWriteValueSet);
			assertTrue(((CopyOnWriteValueSet) moreValueSet).isUnchanged());
			assertArrayEquals(moreValueSet.toArray(), moreValueSet.parallelStream().toArray());
			moreValueSet.retainAll(Arrays.asList("one", "two", "blup"));
			assertEquals(new TreeSet<>(Arrays.asList("one", "two")), moreValueSet);
			assertFalse(((CopyOnWriteValueSet) moreValueSet).isUnchanged());

			final TableMetaData converted = ToTableMetaDataConverter.convert(emd);
			assertSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertNotSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testRemoveIf() {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);

			final Set<String> moreValueSet = emd.getAttributeByName("more").getValueSet();
			assertTrue(moreValueSet instanceof CopyOnWriteValueSet);
			assertTrue(((CopyOnWriteValueSet) moreValueSet).isUnchanged());
			moreValueSet.removeIf(o -> o.contains("o"));
			List<String> test = new ArrayList<>();
			moreValueSet.spliterator().tryAdvance(test::add);
			assertEquals(Collections.singletonList("three"), test);
			assertFalse(((CopyOnWriteValueSet) moreValueSet).isUnchanged());

			final TableMetaData converted = ToTableMetaDataConverter.convert(emd);
			assertSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean without pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertNotSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		@Test
		public void testSerialization() throws IOException, ClassNotFoundException {
			ExampleSetMetaData emd = FromTableMetaDataConverter.convert(original);

			ExampleSetMetaData deserialized = (ExampleSetMetaData) readFromArray(writeToArray(emd));

			final TableMetaData converted = ToTableMetaDataConverter.convert(deserialized);
			assertNotSame(original.column("nominal").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"nominal").getDictionary().getAsCopyOnWrite().getValueSet());
			assertNotSame(original.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos").getDictionary().getAsCopyOnWrite().getValueSet());
			assertNotSame(original.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet(),
					converted.column("boolean with pos/neg").getDictionary().getAsCopyOnWrite().getValueSet());
			assertNotSame(original.column("more").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"more").getDictionary().getAsCopyOnWrite().getValueSet());
			assertNotSame(original.column("type").getDictionary().getAsCopyOnWrite().getValueSet(), converted.column(
					"type").getDictionary().getAsCopyOnWrite().getValueSet());
		}

		private byte[] writeToArray(Object object) throws IOException {
			try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
				 ObjectOutputStream out = new ObjectOutputStream(stream)) {
				out.writeObject(object);
				return stream.toByteArray();
			}
		}

		private Object readFromArray(byte[] array) throws IOException, ClassNotFoundException {
			try (ByteArrayInputStream stream = new ByteArrayInputStream(array);
				 ObjectInputStream in = new ObjectInputStream(stream)) {
				return in.readObject();
			}
		}
	}

	/**
	 * Helper for testing the history.
	 */
	static OutputPort generateDummyOutputPort() {
		return Mockito.mock(OutputPort.class);
	}

}
