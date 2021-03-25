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
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.reader.NumericRowReader;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.math.aggregation.manager.CategoricalAppearanceAggregationManager;
import com.rapidminer.math.aggregation.manager.CategoricalAppearanceAggregationManager.Mode;


/**
 * Test the {@link CategoricalAppearanceAggregationManager}.
 *
 * @author Gisa Meier
 */
public class CategoricalAppearanceManagerTest {

	@Test
	public void testReturnType() {
		assertEquals(ColumnType.NOMINAL,
				new CategoricalAppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.NOMINAL));
	}

	@Test
	public void testReturnTypeWrong() {
		assertNull(new CategoricalAppearanceAggregationManager(Mode.MOST).checkColumnType(ColumnType.INTEGER_53_BIT));
		assertNull(new CategoricalAppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.REAL));
		assertNull(new CategoricalAppearanceAggregationManager(Mode.MOST).checkColumnType(ColumnType.TIME));
		assertNull(new CategoricalAppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.DATETIME));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalInitialize() {
		new CategoricalAppearanceAggregationManager(Mode.LEAST).initialize(Buffers.realBuffer(3).toColumn(), 1);
	}

	@Test
	public void testAllMissingLeast() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		CategoricalAppearanceAggregationManager manager = new CategoricalAppearanceAggregationManager(Mode.LEAST);
		manager.initialize(column, 0);
		CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(-1, function.getMappingIndex());
	}

	@Test
	public void testAllMissingMode() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		CategoricalAppearanceAggregationManager manager = new CategoricalAppearanceAggregationManager(Mode.LEAST);
		manager.initialize(column, 0);
		CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals(-1, function.getMappingIndex());
	}

	@Test
	public void testSomeValuesLeast() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		CategoricalAppearanceAggregationManager manager = new CategoricalAppearanceAggregationManager(Mode.LEAST);
		manager.initialize(column, 0);
		CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("blup", buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testSomeValuesMode() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		CategoricalAppearanceAggregationManager manager = new CategoricalAppearanceAggregationManager(Mode.MOST);
		manager.initialize(column, 0);
		CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
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
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		CategoricalAppearanceAggregationManager manager = new CategoricalAppearanceAggregationManager(Mode.MOST);
		manager.initialize(column, 0);
		CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("bla", buffer.get(function.getMappingIndex()));
	}

	@Test
	public void testMerge() {
		Table table =
				Builders.newTableBuilder(20).addNominal("test", i -> i % 3 == 0 ? "bla" : i % 2 == 0 ? "x" : "blup")
						.build(
								Belt.defaultContext());
		CategoricalAppearanceAggregationManager manager = new CategoricalAppearanceAggregationManager(Mode.MOST);
		manager.initialize(table.column(0), 0);
		CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(table);
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		int expected = function.getMappingIndex();

		CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction function2 =
				manager.newFunction();
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		for (int i = 0; i < 10; i++) {
			reader.move();
			function2.accept(reader);
		}

		CategoricalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction function3 =
				manager.newFunction();
		while (reader.hasRemaining()) {
			reader.move();
			function3.accept(reader);
		}

		function2.merge(function3);

		String[] columnValues = columnToArray(table.column(0));
		assertEquals(columnValues[expected], columnValues[function2.getMappingIndex()]);
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