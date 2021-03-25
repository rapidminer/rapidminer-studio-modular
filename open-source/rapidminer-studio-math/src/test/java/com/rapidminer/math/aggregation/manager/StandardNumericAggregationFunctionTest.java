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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.reader.NumericRowReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.math.aggregation.manager.NumericAggregationManager;
import com.rapidminer.math.aggregation.manager.StandardNumericAggregationFunction;
import com.rapidminer.math.aggregation.manager.aggregator.SumAggregator;


/**
 * Test the {@link StandardNumericAggregationFunction}.
 *
 * @author Gisa Meier
 */
public class StandardNumericAggregationFunctionTest {

	Table table = Builders.newTableBuilder(101).addInt53Bit("missing", i -> Double.NaN).addReal("noMissing", i -> i)
			.addNominal("someMissing", i -> i % 2 == 0 ? null : "bla").build(
					Belt.defaultContext());

	@Test
	public void testAllMissingReturnsMissing() {
		StandardNumericAggregationFunction function = new StandardNumericAggregationFunction(null, 0);
		NumericRowReader reader = Readers.numericRowReader(table);
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(Double.NaN, function.getValue(), 1e-15);
	}

	@Test
	public void testAllMissingReturnsMissingWithMixed() {
		StandardNumericAggregationFunction function = new NumericAggregationManager("", () -> null, true).newFunction();
		MixedRowReader reader = Readers.mixedRowReader(table);
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(Double.NaN, function.getValue(), 1e-15);
	}

	@Test
	public void testNormalVsMixed() {
		StandardNumericAggregationFunction function = new StandardNumericAggregationFunction(new SumAggregator(), 2);
		NumericRowReader reader = Readers.numericRowReader(table);
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		StandardNumericAggregationFunction function2 = new StandardNumericAggregationFunction(new SumAggregator(), 2);
		MixedRowReader reader2 = Readers.mixedRowReader(table);
		while (reader2.hasRemaining()) {
			reader2.move();
			function2.accept(reader2);
		}
		assertEquals(function2.getValue(), function.getValue(), 1e-15);
	}

	@Test
	public void testMerge() {
		StandardNumericAggregationFunction function = new StandardNumericAggregationFunction(new SumAggregator(), 1);
		NumericRowReader reader = Readers.numericRowReader(table);
		for (int i = 0; i < table.height() / 2; i++) {
			reader.move();
			function.accept(reader);
		}

		StandardNumericAggregationFunction function2 = new StandardNumericAggregationFunction(new SumAggregator(), 1);
		reader.setPosition(table.height() / 2 - 1);
		while (reader.hasRemaining()) {
			reader.move();
			function2.accept(reader);
		}
		function.merge(function2);


		StandardNumericAggregationFunction comparison = new StandardNumericAggregationFunction(new SumAggregator(), 1);
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		while (reader.hasRemaining()) {
			reader.move();
			comparison.accept(reader);
		}

		assertEquals(comparison.getValue(), function.getValue(), 1e-15);
	}

	@Test
	public void testMergeOnlyMissing() {
		StandardNumericAggregationFunction function = new StandardNumericAggregationFunction(new SumAggregator(), 0);
		NumericRowReader reader = Readers.numericRowReader(table);
		for (int i = 0; i < table.height() / 2; i++) {
			reader.move();
			function.accept(reader);
		}

		StandardNumericAggregationFunction function2 = new StandardNumericAggregationFunction(new SumAggregator(), 0);
		reader.setPosition(table.height() / 2 - 1);
		while (reader.hasRemaining()) {
			reader.move();
			function2.accept(reader);
		}
		function.merge(function2);

		assertEquals(Double.NaN, function.getValue(), 1e-15);
	}
}
