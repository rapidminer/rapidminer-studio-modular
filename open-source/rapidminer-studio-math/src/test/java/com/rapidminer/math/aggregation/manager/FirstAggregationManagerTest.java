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
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.math.aggregation.manager.FirstAggregationManager;
import com.rapidminer.math.aggregation.manager.MappingAggregationCollector;


/**
 * Test the {@link FirstAggregationManager}.
 *
 * @author Gisa Meier
 */
public class FirstAggregationManagerTest {

	@Test
	public void testReturnType() {
		for (ColumnType type : new ColumnType[]{ColumnType.REAL, ColumnType.INTEGER_53_BIT, ColumnType.NOMINAL,
				ColumnType.DATETIME, ColumnType.TIME}) {
			assertEquals(type, new FirstAggregationManager().checkColumnType(type));
		}
	}

	@Test
	public void testAllMissing() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		FirstAggregationManager manager = new FirstAggregationManager();
		manager.initialize(column, 0);
		FirstAggregationManager.FirstAggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(-1, function.getMappingIndex());
	}

	@Test
	public void testSomeValues() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "x");
		Column column = buffer.toColumn();

		FirstAggregationManager manager = new FirstAggregationManager();
		manager.initialize(column, 0);
		FirstAggregationManager.FirstAggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("bla", buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testSomeValuesMixed() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "x");
		Column column = buffer.toColumn();

		FirstAggregationManager manager = new FirstAggregationManager();
		manager.initialize(column, 0);
		FirstAggregationManager.FirstAggregationFunction function = manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("bla", buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testSomeValuesDatetime() {
		DateTimeBuffer buffer = Buffers.dateTimeBuffer(23, true);
		buffer.set(3, Instant.EPOCH);
		buffer.set(11, Instant.MAX);
		buffer.set(17, Instant.MIN);
		Column column = buffer.toColumn();

		FirstAggregationManager manager = new FirstAggregationManager();
		manager.initialize(column, 0);
		FirstAggregationManager.FirstAggregationFunction function = manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(Instant.EPOCH, buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testMerge() {
		Table table =
				Builders.newTableBuilder(20).addNominal("test", i -> i % 3 == 0 ? "bla" : i % 2 == 0 ? "x" : "blup")
						.build(Belt.defaultContext());
		FirstAggregationManager manager = new FirstAggregationManager();
		manager.initialize(table.column(0), 0);
		FirstAggregationManager.FirstAggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(table);
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		int expected = function.getMappingIndex();

		FirstAggregationManager.FirstAggregationFunction function2 = manager.newFunction();
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		for (int i = 0; i < 10; i++) {
			reader.move();
			function2.accept(reader);
		}

		FirstAggregationManager.FirstAggregationFunction function3 = manager.newFunction();
		while (reader.hasRemaining()) {
			reader.move();
			function3.accept(reader);
		}

		function2.merge(function3);

		assertEquals(expected, function2.getMappingIndex());
	}

	@Test
	public void testMergeStartWithMissings() {
		Table table =
				Builders.newTableBuilder(20)
						.addNominal("test", i -> i < 12 ? null : i % 3 == 0 ? "bla" : i % 2 == 0 ? "x" : "blup")
						.build(Belt.defaultContext());
		FirstAggregationManager manager = new FirstAggregationManager();
		manager.initialize(table.column(0), 0);
		FirstAggregationManager.FirstAggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(table);
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		int expected = function.getMappingIndex();

		FirstAggregationManager.FirstAggregationFunction function2 = manager.newFunction();
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		for (int i = 0; i < 10; i++) {
			reader.move();
			function2.accept(reader);
		}

		FirstAggregationManager.FirstAggregationFunction function3 = manager.newFunction();
		while (reader.hasRemaining()) {
			reader.move();
			function3.accept(reader);
		}

		function2.merge(function3);

		assertEquals(expected, function2.getMappingIndex());
	}

	@Test
	public void testCollector() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		for(int i = 0; i< buffer.size();i++){
			buffer.set(i, "val"+i);
		}
		Column column = buffer.toColumn();

		FirstAggregationManager manager = new FirstAggregationManager();
		manager.initialize(column, 0);

		MappingAggregationCollector collector = new MappingAggregationCollector(7, column);
		
		
		FirstAggregationManager.FirstAggregationFunction dummy =
				mock(FirstAggregationManager.FirstAggregationFunction.class);
		when(dummy.getMappingIndex()).thenReturn(1);
		collector.set(3, dummy);
		when(dummy.getMappingIndex()).thenReturn(7);
		collector.set(5, dummy);
		when(dummy.getMappingIndex()).thenReturn(15);
		collector.set(6, dummy);

		Column result = collector.getResult(Belt.defaultContext());
		assertArrayEquals(new String[]{null, null, null, "val1", null, "val7", "val15"}, columnToArray(result));
	}

	private static String[] columnToArray(Column column) {
		ObjectReader<String> reader = Readers.objectReader(column, String.class);
		String[] array = new String[column.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = reader.read();
		}
		return array;
	}
}
