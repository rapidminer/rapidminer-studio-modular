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

import java.time.Instant;
import java.util.Collections;
import java.util.Random;

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
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.Order;
import com.rapidminer.math.aggregation.manager.MinMaxAggregationManager;
import com.rapidminer.math.aggregation.manager.ObjectMaxAggregationFunction;
import com.rapidminer.math.aggregation.manager.ObjectMinAggregationFunction;


/**
 * Test the {@link MinMaxAggregationManager}, the {@link
 * ObjectMaxAggregationFunction} and the {@link
 * ObjectMinAggregationFunction}.
 *
 * @author Gisa Meier
 */
public class MinMaxAggregationManagerTest {

	@Test
	public void testReturnType() {
		for (ColumnType type : new ColumnType[]{ColumnType.REAL, ColumnType.INTEGER_53_BIT, ColumnType.NOMINAL,
				ColumnType.DATETIME, ColumnType.TIME}) {
			assertEquals(type, new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MAX).checkColumnType(type));
		}
	}

	@Test
	public void testAllMissingMin() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		ObjectMinAggregationFunction<?> function = new ObjectMinAggregationFunction<>(column.type(), column, 0);
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(-1, function.getMappingIndex());
	}

	@Test
	public void testAllMissingMax() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		ObjectMaxAggregationFunction<?> function = new ObjectMaxAggregationFunction<>(column.type(), column, 0);
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(-1, function.getMappingIndex());
	}

	@Test
	public void testSomeValuesMin() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "d");
		buffer.set(11, "c");
		buffer.set(17, "a");
		buffer.set(19, "b");
		Column column = buffer.toColumn();

		MinMaxAggregationManager manager = new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MIN);
		manager.initialize(column, 0);

		ObjectMinAggregationFunction<?> function = (ObjectMinAggregationFunction) manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("a", buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testSomeValuesMixedMin() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "d");
		buffer.set(11, "c");
		buffer.set(17, "a");
		buffer.set(19, "b");
		Column column = buffer.toColumn();

		ObjectMinAggregationFunction<?> function = new ObjectMinAggregationFunction<>(column.type(), column, 0);
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("a", buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testSomeValuesMax() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "a");
		buffer.set(11, "c");
		buffer.set(17, "d");
		buffer.set(19, "b");
		Column column = buffer.toColumn();

		MinMaxAggregationManager manager = new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MAX);
		manager.initialize(column, 0);

		ObjectMaxAggregationFunction<?> function = (ObjectMaxAggregationFunction) manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("d", buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testSomeValuesMixedMax() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "a");
		buffer.set(11, "c");
		buffer.set(17, "d");
		buffer.set(19, "b");
		Column column = buffer.toColumn();

		ObjectMaxAggregationFunction<?> function = new ObjectMaxAggregationFunction<>(column.type(), column, 0);
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("d", buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testDatetime() {
		DateTimeBuffer buffer = Buffers.dateTimeBuffer(42, false);
		Random random = new Random();
		for (int i = 0; i < buffer.size(); i++) {
			buffer.set(i, random.nextLong() % Instant.MAX.getEpochSecond());
		}
		Column column = buffer.toColumn();

		ObjectMinAggregationFunction<?> min = new ObjectMinAggregationFunction<>(column.type(), column, 0);
		ObjectMaxAggregationFunction<?> max = new ObjectMaxAggregationFunction<>(column.type(), column, 0);
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			min.accept(reader);
			max.accept(reader);
		}

		Column sorted = Builders.newTableBuilder(column.size()).add("bla", column).build(Belt.defaultContext()).sort(0,
				Order.ASCENDING, Belt.defaultContext()).column(0);
		ObjectReader<Instant> objectReader = Readers.objectReader(sorted, Instant.class);
		assertEquals(objectReader.read(), buffer.get(min.getMappingIndex()));
		objectReader.setPosition(sorted.size() - 2);
		assertEquals(objectReader.read(), buffer.get(max.getMappingIndex()));
	}

	@Test
	public void testMergeMin() {
		DateTimeBuffer buffer = Buffers.dateTimeBuffer(42, false);
		Random random = new Random();
		for (int i = 0; i < buffer.size(); i++) {
			buffer.set(i, random.nextLong() % Instant.MAX.getEpochSecond());
		}
		Column column = buffer.toColumn();

		ObjectMinAggregationFunction<?> min = new ObjectMinAggregationFunction<>(column.type(), column, 0);
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			min.accept(reader);
		}

		ObjectMinAggregationFunction<?> min2 = new ObjectMinAggregationFunction<>(column.type(), column, 0);
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		for (int i = 0; i < 10; i++) {
			reader.move();
			min2.accept(reader);
		}

		ObjectMinAggregationFunction<?> min3 = new ObjectMinAggregationFunction<>(column.type(), column, 0);
		while (reader.hasRemaining()) {
			reader.move();
			min3.accept(reader);
		}

		min2.merge(min3);

		assertEquals(min.getMappingIndex(), min2.getMappingIndex());
	}

	@Test
	public void testMergeMax() {
		DateTimeBuffer buffer = Buffers.dateTimeBuffer(42, false);
		Random random = new Random();
		for (int i = 0; i < buffer.size(); i++) {
			buffer.set(i, random.nextLong() % Instant.MAX.getEpochSecond());
		}
		Column column = buffer.toColumn();

		ObjectMaxAggregationFunction<?> max = new ObjectMaxAggregationFunction<>(column.type(), column, 0);
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		while (reader.hasRemaining()) {
			reader.move();
			max.accept(reader);
		}

		ObjectMaxAggregationFunction<?> max2 = new ObjectMaxAggregationFunction<>(column.type(), column, 0);
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		for (int i = 0; i < 10; i++) {
			reader.move();
			max2.accept(reader);
		}

		ObjectMaxAggregationFunction<?> max3 = new ObjectMaxAggregationFunction<>(column.type(), column, 0);
		while (reader.hasRemaining()) {
			reader.move();
			max3.accept(reader);
		}

		max2.merge(max3);

		assertEquals(max.getMappingIndex(), max2.getMappingIndex());
	}

	@Test
	public void testInitializeRow() {
		MinMaxAggregationManager manager = new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MIN);
		manager.initialize(Buffers.integer53BitBuffer(4).toColumn(), 42);
		assertEquals(42, manager.getIndex());
	}

	@Test
	public void testCollectorInteger() {
		MinMaxAggregationManager manager = new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MIN);
		manager.initialize(Buffers.integer53BitBuffer(4).toColumn(), 2);
		assertEquals(ColumnType.INTEGER_53_BIT, manager.getCollector(3).getResult(Belt.defaultContext()).type());
	}

	@Test
	public void testCollectorReal() {
		MinMaxAggregationManager manager = new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MIN);
		manager.initialize(Buffers.realBuffer(4).toColumn(), 2);
		assertEquals(ColumnType.REAL, manager.getCollector(3).getResult(Belt.defaultContext()).type());
	}

	@Test
	public void testCollectorDate() {
		MinMaxAggregationManager manager = new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MIN);
		manager.initialize(Buffers.dateTimeBuffer(4, false).toColumn(), 2);
		assertEquals(ColumnType.DATETIME, manager.getCollector(3).getResult(Belt.defaultContext()).type());
	}
}
