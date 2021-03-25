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
package com.rapidminer.math.aggregation.manager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.math.aggregation.manager.FractionalNumericAggregationCollector;
import com.rapidminer.math.aggregation.manager.NumericAggregationFunction;
import com.rapidminer.math.aggregation.manager.NumericBufferAggregationCollector;
import com.rapidminer.math.aggregation.manager.PercentageNumericAggregationCollector;


/**
 * Test the {@link NumericBufferAggregationCollector}, the {@link PercentageNumericAggregationCollector} and the {@link
 * FractionalNumericAggregationCollector}.
 *
 * @author Gisa Meier
 */
public class NumericAggregationCollectorTest {

	@Test
	public void testNumericBuffer() {
		NumericBufferAggregationCollector collector = new NumericBufferAggregationCollector(33, false);
		NumericAggregationFunction dummy = mock(NumericAggregationFunction.class);
		when(dummy.getValue()).thenReturn(1.0);
		for (int i = 0; i < 31; i++) {
			collector.set(i, dummy);
		}
		Column column = collector.getResult(null);
		double[] expected = new double[33];
		Arrays.fill(expected, 1.0);
		expected[31] = Double.NaN;
		expected[32] = Double.NaN;
		assertArrayEquals(expected, columnToArray(column), 1e-10);
		assertEquals(ColumnType.REAL, column.type());
	}

	@Test
	public void testNumericBufferInteger() {
		NumericBufferAggregationCollector collector = new NumericBufferAggregationCollector(33, true);
		NumericAggregationFunction dummy = mock(NumericAggregationFunction.class);
		when(dummy.getValue()).thenReturn(1.0);
		for (int i = 0; i < 31; i++) {
			collector.set(i, dummy);
		}
		Column column = collector.getResult(null);
		double[] expected = new double[33];
		Arrays.fill(expected, 1.0);
		expected[31] = Double.NaN;
		expected[32] = Double.NaN;
		assertArrayEquals(expected, columnToArray(column), 1e-10);
		assertEquals(ColumnType.INTEGER_53_BIT, column.type());
	}

	@Test
	public void testFractionalAndPercentageBuffer() {
		double[] input = new double[111];
		Arrays.setAll(input, i -> Math.random());

		FractionalNumericAggregationCollector fractional = new FractionalNumericAggregationCollector(input.length);
		PercentageNumericAggregationCollector percentage = new PercentageNumericAggregationCollector(input.length);
		for (int i = 0; i < input.length; i++) {
			NumericAggregationFunction dummy = mock(NumericAggregationFunction.class);
			when(dummy.getValue()).thenReturn(input[i]);
			fractional.set(i, dummy);
			percentage.set(i, dummy);
		}
		Column column = fractional.getResult(null);
		double[] expected = columnToArray(column);
		Arrays.setAll(expected, i -> 100 * expected[i]);
		assertArrayEquals(expected, columnToArray(percentage.getResult(null)), 1e-10);
		assertEquals(ColumnType.REAL, column.type());
	}

	@Test
	public void testFractionalAndMissing() {
		double[] input = new double[103];
		Arrays.fill(input, 1);
		input[3] = Double.NaN;
		input[17] = Double.NaN;
		input[52] = Double.NaN;

		FractionalNumericAggregationCollector fractional = new FractionalNumericAggregationCollector(input.length);
		for (int i = 0; i < input.length; i++) {
			NumericAggregationFunction dummy = mock(NumericAggregationFunction.class);
			when(dummy.getValue()).thenReturn(input[i]);
			fractional.set(i, dummy);
		}
		double[] expected = new double[103];
		Arrays.fill(expected, 1 / 100.0);
		expected[3] = Double.NaN;
		expected[17] = Double.NaN;
		expected[52] = Double.NaN;
		assertArrayEquals(expected, columnToArray(fractional.getResult(null)), 1e-10);
	}

	@Test
	public void testPercentageAndMissing() {
		double[] input = new double[103];
		Arrays.fill(input, 1);
		input[3] = Double.NaN;
		input[17] = Double.NaN;
		input[52] = Double.NaN;

		PercentageNumericAggregationCollector percentage = new PercentageNumericAggregationCollector(input.length);
		for (int i = 0; i < input.length; i++) {
			NumericAggregationFunction dummy = mock(NumericAggregationFunction.class);
			when(dummy.getValue()).thenReturn(input[i]);
			percentage.set(i, dummy);
		}
		double[] expected = new double[103];
		Arrays.fill(expected, 1.0);
		expected[3] = Double.NaN;
		expected[17] = Double.NaN;
		expected[52] = Double.NaN;
		assertArrayEquals(expected, columnToArray(percentage.getResult(null)), 1e-10);
	}

	@Test
	public void testFractionalAndNegative() {
		double[] input = new double[103];
		Arrays.fill(input, 1);
		input[52] = -1;

		FractionalNumericAggregationCollector fractional = new FractionalNumericAggregationCollector(input.length);
		for (int i = 0; i < input.length; i++) {
			NumericAggregationFunction dummy = mock(NumericAggregationFunction.class);
			when(dummy.getValue()).thenReturn(input[i]);
			fractional.set(i, dummy);
		}
		double[] expected = new double[103];
		Arrays.fill(expected, Double.NaN);
		assertArrayEquals(expected, columnToArray(fractional.getResult(null)), 1e-10);
	}

	@Test
	public void testPercentageAndNegative() {
		double[] input = new double[103];
		Arrays.fill(input, 1);
		input[52] = -1;

		PercentageNumericAggregationCollector percentage = new PercentageNumericAggregationCollector(input.length);
		for (int i = 0; i < input.length; i++) {
			NumericAggregationFunction dummy = mock(NumericAggregationFunction.class);
			when(dummy.getValue()).thenReturn(input[i]);
			percentage.set(i, dummy);
		}
		double[] expected = new double[103];
		Arrays.fill(expected, Double.NaN);
		assertArrayEquals(expected, columnToArray(percentage.getResult(null)), 1e-10);
	}


	private static double[] columnToArray(Column column) {
		NumericReader reader = Readers.numericReader(column);
		double[] array = new double[column.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = reader.read();
		}
		return array;
	}
}
