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
package com.rapidminer.math.aggregation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.reader.NumericRowReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.manager.NominalAppearanceAggregationManager.Mode;


/**
 * Test the {@link AppearanceAggregationManager}.
 *
 * @author Gisa Meier
 */
public class AppearanceAggregationManagerTest {

	@Test
	public void testReturnType() {
		assertEquals(ColumnType.NOMINAL,
				new AppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.NOMINAL));
		assertEquals(ColumnType.REAL,
				new AppearanceAggregationManager(Mode.MOST).checkColumnType(ColumnType.REAL));
		assertEquals(ColumnType.INTEGER_53_BIT,
				new AppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.INTEGER_53_BIT));
	}

	@Test
	public void testReturnTypeWrong() {
		assertNull(new AppearanceAggregationManager(Mode.MOST).checkColumnType(ColumnType.TIME));
		assertNull(new AppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.DATETIME));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalInitialize() {
		new AppearanceAggregationManager(Mode.LEAST).initialize(Buffers.timeBuffer(3).toColumn(), 1);
	}

	@Test
	public void testName() {
		AppearanceAggregationManager aggregationManager = new AppearanceAggregationManager(Mode.LEAST);
		aggregationManager.initialize(Buffers.realBuffer(3).toColumn(), 1);
		assertEquals(AggregationManagers.FUNCTION_NAME_LEAST, aggregationManager.getAggregationName());
		aggregationManager = new AppearanceAggregationManager(Mode.MOST);
		aggregationManager.initialize(Buffers.nominalBuffer(3).toColumn(), 1);
		assertEquals(AggregationManagers.FUNCTION_NAME_MODE, aggregationManager.getAggregationName());
	}

	@Test
	public void testIndex() {
		AppearanceAggregationManager aggregationManager = new AppearanceAggregationManager(Mode.LEAST);
		aggregationManager.initialize(Buffers.realBuffer(3).toColumn(), 1);
		assertEquals(1, aggregationManager.getIndex());
		aggregationManager = new AppearanceAggregationManager(Mode.MOST);
		aggregationManager.initialize(Buffers.nominalBuffer(3).toColumn(), 42);
		assertEquals(42, aggregationManager.getIndex());
	}

	@Test
	public void testCollector() {
		AppearanceAggregationManager aggregationManager = new AppearanceAggregationManager(Mode.LEAST);
		aggregationManager.initialize(Buffers.realBuffer(3).toColumn(), 1);
		assertTrue(aggregationManager.getCollector(3) instanceof NumericBufferAggregationCollector);
		aggregationManager = new AppearanceAggregationManager(Mode.MOST);
		aggregationManager.initialize(Buffers.nominalBuffer(3).toColumn(), 42);
		assertTrue(aggregationManager.getCollector(3) instanceof NominalAggregationCollector);
	}

	@Test
	public void testSomeValuesLeast() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		AppearanceAggregationManager manager = new AppearanceAggregationManager(Mode.LEAST);
		manager.initialize(column, 0);
		AggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertTrue(function instanceof NominalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction);
		assertEquals("blup",
				((NominalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction) function).getResult());
	}

	@Test
	public void testSomeValuesMode() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		AppearanceAggregationManager manager = new AppearanceAggregationManager(Mode.MOST);
		manager.initialize(column, 0);
		AggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertTrue(function instanceof NominalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction);
		assertEquals("bla",
				((NominalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction) function).getResult());
	}

	@Test
	public void testSomeValuesLeastNumeric() {
		NumericBuffer buffer = Buffers.realBuffer(23);
		buffer.set(3, 0.3);
		buffer.set(11, 0.42);
		buffer.set(17, 0.3);
		Column column = buffer.toColumn();

		AppearanceAggregationManager manager = new AppearanceAggregationManager(Mode.LEAST);
		manager.initialize(column, 0);
		AggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertTrue(function instanceof StandardNumericAggregationFunction);
		assertEquals(0.42, ((StandardNumericAggregationFunction) function).getValue(), 0);
	}

	@Test
	public void testSomeValuesModeNumeric() {
		NumericBuffer buffer = Buffers.integer53BitBuffer(23);
		buffer.set(3, 1);
		buffer.set(11, 42);
		buffer.set(17, 1);
		Column column = buffer.toColumn();

		AppearanceAggregationManager manager = new AppearanceAggregationManager(Mode.MOST);
		manager.initialize(column, 0);
		AggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertTrue(function instanceof StandardNumericAggregationFunction);
		assertEquals(1, ((StandardNumericAggregationFunction) function).getValue(), 0);
	}
}