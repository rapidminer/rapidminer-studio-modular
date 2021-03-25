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
package com.rapidminer.tools.math.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeight;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorException;
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
import com.rapidminer.operator.performance.ParameterizedMeasuredPerformanceCriterion;
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
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.Averagable;
import com.rapidminer.tools.math.ROCBias;
import com.rapidminer.tools.math.ROCDataGenerator;


/**
 * Tests the {@link ObjectRange}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ObjectRangeTest {

	@Test
	public void testContains() {
		final ObjectRange<String> stringObjectRange = new ObjectRange<>("a", "z", String::compareTo);
		assertTrue(stringObjectRange.contains("c"));
		assertTrue(stringObjectRange.contains("y"));
		assertFalse(stringObjectRange.contains("A"));
		assertTrue(stringObjectRange.contains("a"));
	}

	@Test
	public void testUpperLower() {
		final ObjectRange<Integer> intObjectRange = new ObjectRange<>(2, 3, Integer::compareTo);
		assertEquals(2, intObjectRange.getLower(), 0);
		assertEquals(3, intObjectRange.getUpper(), 0);
	}

	@Test
	public void testContainsRange() {
		final ObjectRange<String> stringObjectRange = new ObjectRange<>("a", "z", String::compareTo);
		assertTrue(stringObjectRange.contains(new ObjectRange<>("a", "z", String::compareTo)));
		assertTrue(stringObjectRange.contains(new ObjectRange<>("b", "x", String::compareTo)));
		assertFalse(stringObjectRange.contains(new ObjectRange<>("B", "x", String::compareTo)));
	}

	@Test
	public void testEquals() {
		final ObjectRange<String> stringObjectRange = new ObjectRange<>("a", "z", String::compareTo);
		assertTrue(stringObjectRange.equals(new ObjectRange<>("a", "z", String::compareTo)));
		assertTrue(stringObjectRange.equals(stringObjectRange));
		assertFalse(stringObjectRange.equals(new ObjectRange<>("b", "x", String::compareTo)));
	}

	@Test
	public void testToString() {
		final ObjectRange<String> stringObjectRange = new ObjectRange<>("a", "z", String::compareTo);
		assertEquals("[a – z]", stringObjectRange.toString());
	}

}