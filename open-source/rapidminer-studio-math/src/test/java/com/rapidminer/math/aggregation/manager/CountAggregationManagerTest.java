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

import java.time.Instant;
import java.util.Collections;

import org.junit.Test;

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.buffer.DateTimeBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.reader.NumericRowReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationManager;
import com.rapidminer.math.aggregation.manager.CountAggregationManager;
import com.rapidminer.math.aggregation.manager.NumericAggregationFunction;


/**
 * Test the {@link CountAggregationManager}.
 *
 * @author Gisa Meier
 */
public class CountAggregationManagerTest {

	@Test
	public void testReturnType() {
		assertEquals(ColumnType.INTEGER_53_BIT,
				new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL).checkColumnType(ColumnType.NOMINAL));
		assertEquals(ColumnType.INTEGER_53_BIT,
				new CountAggregationManager(false, CountAggregationManager.Mode.NORMAL).checkColumnType(ColumnType.DATETIME));
		assertEquals(ColumnType.REAL,
				new CountAggregationManager(false, CountAggregationManager.Mode.FRACTIONAL).checkColumnType(ColumnType.TIME));
		assertEquals(ColumnType.REAL,
				new CountAggregationManager(false, CountAggregationManager.Mode.PERCENTAGE).checkColumnType(ColumnType.INTEGER_53_BIT));
	}


	@Test
	public void testAllMissing() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		manager.initialize(column, 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 4; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(0.0, function.getValue(), 0);
	}

	@Test
	public void testAllMissingMixed() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		manager.initialize(column, 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 4; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(0.0, function.getValue(), 0);
	}

	@Test
	public void testAllMissingMixedDateTime() {
		DateTimeBuffer buffer = Buffers.dateTimeBuffer(33, true);
		Column column = buffer.toColumn();

		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		manager.initialize(column, 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 4; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(0.0, function.getValue(), 0);
	}

	@Test
	public void testSomeValues() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		manager.initialize(column, 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(3, function.getValue(), 0);
	}

	@Test
	public void testSomeValuesMixed() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		manager.initialize(column, 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(3, function.getValue(), 0);
	}

	@Test
	public void testSomeValuesDateTime() {
		DateTimeBuffer buffer = Buffers.dateTimeBuffer(33, true);
		buffer.set(3, Instant.EPOCH);
		buffer.set(11, Instant.EPOCH);
		buffer.set(17, Instant.EPOCH);
		Column column = buffer.toColumn();

		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		manager.initialize(column, 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(3, function.getValue(), 0);
	}

	@Test
	public void testSomeValuesWithMissings() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		CountAggregationManager manager = new CountAggregationManager(false, CountAggregationManager.Mode.NORMAL);
		manager.initialize(column, 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(18, function.getValue(), 0);
	}

	@Test
	public void testSomeValuesMixedWithMissings() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		CountAggregationManager manager = new CountAggregationManager(false, CountAggregationManager.Mode.NORMAL);
		manager.initialize(column, 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(18, function.getValue(), 0);
	}

	@Test
	public void testMerge() {
		Table table = Builders.newTableBuilder(20).addNominal("test", i -> i % 3 == 0 ? "bla" : i % 2 == 0 ? null :
				"blup").build(
				Belt.defaultContext());
		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		manager.initialize(table.column(0), 0);
		NumericAggregationFunction function = (NumericAggregationFunction) manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(table);
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		double expected = function.getValue();

		NumericAggregationFunction function2 = (NumericAggregationFunction) manager.newFunction();
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		for (int i = 0; i < 10; i++) {
			reader.move();
			function2.accept(reader);
		}

		NumericAggregationFunction function3 = (NumericAggregationFunction) manager.newFunction();
		while (reader.hasRemaining()) {
			reader.move();
			function3.accept(reader);
		}

		function2.merge(function3);

		assertEquals(expected, function2.getValue(), 0);
	}

	@Test
	public void testCollector() {
		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);

		AggregationCollector collector = manager.getCollector(7);
		NumericAggregationFunction dummy =
				mock(NumericAggregationFunction.class);
		when(dummy.getValue()).thenReturn(1.0);
		collector.set(3, dummy);
		when(dummy.getValue()).thenReturn(10.0);
		collector.set(5, dummy);
		when(dummy.getValue()).thenReturn(0.0);
		collector.set(6, dummy);

		Column result = collector.getResult(null);
		double[] resultArray = new double[result.size()];
		result.fill(resultArray, 0);
		assertArrayEquals(new double[]{Double.NaN, Double.NaN, Double.NaN, 1.0, Double.NaN, 10.0, 0.0}, resultArray,
				0);
	}

	@Test
	public void testCollectorFractional() {
		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.FRACTIONAL);

		AggregationCollector collector = manager.getCollector(7);
		NumericAggregationFunction dummy =
				mock(NumericAggregationFunction.class);
		when(dummy.getValue()).thenReturn(1.0);
		collector.set(3, dummy);
		when(dummy.getValue()).thenReturn(9.0);
		collector.set(5, dummy);
		when(dummy.getValue()).thenReturn(0.0);
		collector.set(6, dummy);

		Column result = collector.getResult(null);
		double[] resultArray = new double[result.size()];
		result.fill(resultArray, 0);
		assertArrayEquals(new double[]{Double.NaN, Double.NaN, Double.NaN, 0.1, Double.NaN, 0.9, 0.0}, resultArray, 0);
	}

	@Test
	public void testCollectorPercentage() {
		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.PERCENTAGE);

		AggregationCollector collector = manager.getCollector(7);
		NumericAggregationFunction dummy =
				mock(NumericAggregationFunction.class);
		when(dummy.getValue()).thenReturn(1.0);
		collector.set(3, dummy);
		when(dummy.getValue()).thenReturn(9.0);
		collector.set(5, dummy);
		when(dummy.getValue()).thenReturn(0.0);
		collector.set(6, dummy);

		Column result = collector.getResult(null);
		double[] resultArray = new double[result.size()];
		result.fill(resultArray, 0);
		assertArrayEquals(new double[]{Double.NaN, Double.NaN, Double.NaN, 10.0, Double.NaN, 90.0, 0.0}, resultArray,
				0);
	}

	@Test
	public void testName() {
		CountAggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		assertEquals("count", manager.getAggregationName());

		manager = new CountAggregationManager(true, CountAggregationManager.Mode.FRACTIONAL);
		assertEquals("fractional_count", manager.getAggregationName());

		manager = new CountAggregationManager(true, CountAggregationManager.Mode.PERCENTAGE);
		assertEquals("percentage_count", manager.getAggregationName());

		manager = new CountAggregationManager(false, CountAggregationManager.Mode.NORMAL);
		assertEquals("count_with_missings", manager.getAggregationName());

		manager = new CountAggregationManager(false, CountAggregationManager.Mode.FRACTIONAL);
		assertEquals("fractional_count_with_missings", manager.getAggregationName());

		manager = new CountAggregationManager(false, CountAggregationManager.Mode.PERCENTAGE);
		assertEquals("percentage_count_with_missings", manager.getAggregationName());
	}

	@Test
	public void testInitializeRow() {
		AggregationManager manager = new CountAggregationManager(true, CountAggregationManager.Mode.NORMAL);
		manager.initialize(Buffers.integer53BitBuffer(3).toColumn(), 42);
		assertEquals(42, manager.getIndex());
	}
}
