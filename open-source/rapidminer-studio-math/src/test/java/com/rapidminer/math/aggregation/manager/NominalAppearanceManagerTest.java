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

import static com.rapidminer.math.aggregation.manager.AggregationManagers.MAP_FILL_RATIO;
import static com.rapidminer.math.aggregation.manager.AggregationManagers.MAX_MAPPING_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.reader.NumericRow;
import com.rapidminer.belt.reader.NumericRowReader;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.math.aggregation.manager.NominalAppearanceAggregationManager;
import com.rapidminer.math.aggregation.manager.NominalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction;
import com.rapidminer.math.aggregation.manager.NominalAppearanceAggregationManager.Mode;


/**
 * Test the {@link NominalAppearanceAggregationManager}.
 *
 * @author Gisa Meier, Jan Czogalla
 * @since 9.7
 */
public class NominalAppearanceManagerTest {

	@Test
	public void testReturnType() {
		assertEquals(ColumnType.NOMINAL,
				new NominalAppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.NOMINAL));
	}

	@Test
	public void testReturnTypeWrong() {
		assertNull(new NominalAppearanceAggregationManager(Mode.MOST).checkColumnType(ColumnType.INTEGER_53_BIT));
		assertNull(new NominalAppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.REAL));
		assertNull(new NominalAppearanceAggregationManager(Mode.MOST).checkColumnType(ColumnType.TIME));
		assertNull(new NominalAppearanceAggregationManager(Mode.LEAST).checkColumnType(ColumnType.DATETIME));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalInitialize() {
		new NominalAppearanceAggregationManager(Mode.LEAST).initialize(Buffers.realBuffer(3).toColumn(), 1);
	}

	@Test
	public void testAllMissingLeast() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		NominalAppearanceAggregationManager manager = new NominalAppearanceAggregationManager(Mode.LEAST);
		manager.initialize(column, 0);
		CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertNull(function.getResult());
	}

	@Test
	public void testAllMissingMode() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		NominalAppearanceAggregationManager manager = new NominalAppearanceAggregationManager(Mode.LEAST);
		manager.initialize(column, 0);
		CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertNull(function.getResult());
	}

	@Test
	public void testSomeValuesLeast() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		NominalAppearanceAggregationManager manager = new NominalAppearanceAggregationManager(Mode.LEAST);
		manager.initialize(column, 0);
		CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("blup", function.getResult());
	}

	@Test
	public void testSomeValuesMode() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		NominalAppearanceAggregationManager manager = new NominalAppearanceAggregationManager(Mode.MOST);
		manager.initialize(column, 0);
		CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("bla", function.getResult());
	}

	@Test
	public void testSomeValuesMixed() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		NominalAppearanceAggregationManager manager = new NominalAppearanceAggregationManager(Mode.MOST);
		manager.initialize(column, 0);
		CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("bla", function.getResult());
	}

	@Test
	public void testMerge() {
		Table table =
				Builders.newTableBuilder(20).addNominal("test", i -> i % 3 == 0 ? "bla" : i % 2 == 0 ? "x" : "blup")
						.build(
								Belt.defaultContext());
		NominalAppearanceAggregationManager manager = new NominalAppearanceAggregationManager(Mode.MOST);
		manager.initialize(table.column(0), 0);
		CategoricalModeLeastAggregationFunction function =
				manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(table);
		while (reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		String expected = function.getResult();

		CategoricalModeLeastAggregationFunction function2 =
				manager.newFunction();
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		for (int i = 0; i < 10; i++) {
			reader.move();
			function2.accept(reader);
		}

		CategoricalModeLeastAggregationFunction function3 =
				manager.newFunction();
		while (reader.hasRemaining()) {
			reader.move();
			function3.accept(reader);
		}

		function2.merge(function3);

		assertEquals(expected, function2.getResult());
	}

	/**
	 * Test behavior for sparse and dense nominal most counter when merging
	 *
	 */
	@Test
	public void testMapArrayMergeMost() {
		int dictSize = MAX_MAPPING_SIZE + 1;
		// number to cross to get into array territory
		int threshold = (dictSize + MAP_FILL_RATIO - 1) / MAP_FILL_RATIO + 1;
		// allow to keep map, even if merging two non-overlapping maps
		int minFill = Math.min(10, threshold / 3);
		Table table = Builders.newTableBuilder(dictSize)
				.addNominal("test", i -> i + "")
				.build(Belt.defaultContext());

		NominalAppearanceAggregationManager manager = new NominalAppearanceAggregationManager(Mode.MOST);
		manager.initialize(table.column(0), 0);

		// fill with values from upper indices; don't overfill
		CategoricalModeLeastAggregationFunction rightMap = prepareFunction(manager, dictSize / 2, minFill);
		// make this the most index
		rightMap.accept(new DummyNumRow(dictSize / 2 + minFill - 1));
		assertEquals("Error filling right map", table.column(0).getDictionary().get(dictSize / 2 + minFill - 1), rightMap.getResult());
		assertNotNull(rightMap.appearingMap);
		assertNull(rightMap.appearingCounter);

		// fill with values from upper indices; fill to reach array status
		CategoricalModeLeastAggregationFunction rightArray = prepareFunction(manager, dictSize / 2, threshold);
		// make this the most index
		rightArray.accept(new DummyNumRow(dictSize / 2 + threshold - 1));
		assertEquals("Error filling right array", table.column(0).getDictionary().get(dictSize / 2 + threshold - 1), rightArray.getResult());
		assertNull(rightArray.appearingMap);
		assertNotNull(rightArray.appearingCounter);

		CategoricalModeLeastAggregationFunction left;
		String leftResult;

		// left empty, right map
		left = manager.newFunction();
		left.merge(rightMap);
		assertEquals("Error left empty, right map", rightMap.getResult(), left.getResult());

		// left empty, right array
		left = manager.newFunction();
		left.merge(rightArray);
		assertEquals("Error left empty, right array", rightArray.getResult(), left.getResult());

		// left map, right empty
		left = prepareFunction(manager, 0, minFill);
		left.accept(new DummyNumRow(minFill - 1));
		leftResult = left.getResult();
		left.merge(manager.newFunction());
		assertEquals("Error left map, right empty", leftResult, left.getResult());

		// both maps, no overlap, left dominates
		left = prepareFunction(manager, 0, minFill);
		left.accept(new DummyNumRow(minFill - 1));
		leftResult = left.getResult();
		left.merge(rightMap);
		assertEquals("Error both maps, no overlap, left dominates", leftResult, left.getResult());

		// both maps, no overlap, right dominates
		left = prepareFunction(manager, 0, minFill);
		left.merge(rightMap);
		assertEquals("Error both maps, no overlap, right dominates", rightMap.getResult(), left.getResult());

		// both maps, overlap, overlap creates max
		left = prepareFunction(manager, 0, minFill);
		left.accept(new DummyNumRow(minFill - 1));
		left.accept(new DummyNumRow(dictSize / 2));
		left.accept(new DummyNumRow(dictSize / 2));
		left.merge(rightMap);
		assertEquals("Error both maps, overlap, overlap creates max", table.column(0).getDictionary().get(dictSize / 2), left.getResult());

		// left map, right array, no overlap, left dominates
		left = prepareFunction(manager, 0, minFill);
		left.accept(new DummyNumRow(minFill - 1));
		leftResult = left.getResult();
		left.merge(rightArray);
		assertEquals("Error left map, right array, no overlap, left dominates", leftResult, left.getResult());

		// left map, right array, no overlap, right dominates
		left = prepareFunction(manager, 0, minFill);
		left.merge(rightArray);
		assertEquals("Error left map, right array, no overlap, right dominates", rightArray.getResult(), left.getResult());

		// left map, right array, overlap, overlap creates max
		left = prepareFunction(manager, 0, minFill);
		left.accept(new DummyNumRow(minFill - 1));
		left.accept(new DummyNumRow(dictSize / 2));
		left.accept(new DummyNumRow(dictSize / 2));
		left.merge(rightArray);
		assertEquals("Error left map, right array, overlap, overlap creates max", table.column(0).getDictionary().get(dictSize / 2), left.getResult());

		// left array, right empty
		left = prepareFunction(manager, 0, threshold);
		left.accept(new DummyNumRow(threshold - 1));
		left.merge(manager.newFunction());
		assertEquals("Error left array, right empty", table.column(0).getDictionary().get(threshold - 1), left.getResult());

		// left array, right map, no overlap, left dominates
		left = prepareFunction(manager, 0, threshold);
		left.accept(new DummyNumRow(threshold - 1));
		leftResult = left.getResult();
		left.merge(rightMap);
		assertEquals("Error left array, right map, no overlap, left dominates", leftResult, left.getResult());

		// left array, right map, no overlap, right dominates
		left = prepareFunction(manager, 0, threshold);
		left.merge(rightMap);
		assertEquals("Error left array, right map, no overlap, right dominates", rightMap.getResult(), left.getResult());

		// left array, right map, overlap creates max
		left = prepareFunction(manager, 0, threshold);
		left.accept(new DummyNumRow(threshold - 1));
		left.accept(new DummyNumRow(dictSize / 2));
		left.accept(new DummyNumRow(dictSize / 2));
		left.merge(rightMap);
		assertEquals("Error left array, right map, overlap creates max", table.column(0).getDictionary().get(dictSize / 2), left.getResult());

		// both arrays, no overlap, left dominates
		left = prepareFunction(manager, 0, threshold);
		left.accept(new DummyNumRow(threshold - 1));
		leftResult = left.getResult();
		left.merge(rightArray);
		assertEquals("Error both arrays, no overlap, left dominates", leftResult, left.getResult());

		// both arrays, no overlap, right dominates
		left = prepareFunction(manager, 0, threshold);
		left.merge(rightArray);
		assertEquals("Error both arrays, no overlap, right dominates", rightArray.getResult(), left.getResult());

		// both arrays, overlap creates max
		left = prepareFunction(manager, 0, threshold);
		left.accept(new DummyNumRow(threshold - 1));
		left.accept(new DummyNumRow(dictSize / 2));
		left.accept(new DummyNumRow(dictSize / 2));
		left.merge(rightArray);
		assertEquals("Error both arrays, overlap creates max", table.column(0).getDictionary().get(dictSize / 2), left.getResult());
	}

	/**
	 * Test behavior for sparse and dense nominal least counter when merging
	 */
	@Test
	public void testMapArrayMergeLeast() {
		int dictSize = MAX_MAPPING_SIZE + 1;
		// number to cross to get into array territory
		int threshold = (dictSize + MAP_FILL_RATIO - 1) / MAP_FILL_RATIO + 1;
		// allow to keep map, even if merging two non-overlapping maps
		int minFill = Math.min(10, threshold / 3);
		Table table = Builders.newTableBuilder(dictSize)
				.addNominal("test", i -> i + "")
				.build(Belt.defaultContext());

		NominalAppearanceAggregationManager manager = new NominalAppearanceAggregationManager(Mode.LEAST);
		manager.initialize(table.column(0), 0);

		// fill with values from upper indices; don't overfill
		CategoricalModeLeastAggregationFunction rightMap = prepareFunction(manager, dictSize / 2, minFill);
		// make the last index the least
		fillFunction(rightMap, dictSize / 2, minFill - 1);
		assertEquals("Error filling right map", table.column(0).getDictionary().get(dictSize / 2 + minFill - 1), rightMap.getResult());
		assertNotNull(rightMap.appearingMap);
		assertNull(rightMap.appearingCounter);

		// fill with values from upper indices; fill to reach array status
		CategoricalModeLeastAggregationFunction rightArray = prepareFunction(manager, dictSize / 2, threshold);
		// make the last index the least
		fillFunction(rightArray, dictSize / 2, threshold - 1);
		assertEquals("Error filling right array", table.column(0).getDictionary().get(dictSize / 2 + threshold - 1), rightArray.getResult());
		assertNull(rightArray.appearingMap);
		assertNotNull(rightArray.appearingCounter);

		CategoricalModeLeastAggregationFunction left;
		String leftResult;

		// left empty, right map
		left = manager.newFunction();
		left.merge(rightMap);
		assertEquals("Error left empty, right map", rightMap.getResult(), left.getResult());

		// left empty, right array
		left = manager.newFunction();
		left.merge(rightArray);
		assertEquals("Error left empty, right array", rightArray.getResult(), left.getResult());

		// left map, right empty
		left = prepareFunction(manager, 0, minFill);
		fillFunction(left, 0, minFill - 1);
		leftResult = left.getResult();
		left.merge(manager.newFunction());
		assertEquals("Error left map, right empty", leftResult, left.getResult());

		// both maps, no overlap, left dominates
		left = prepareFunction(manager, 0, minFill);
		fillFunction(left, 0, minFill - 1);
		leftResult = left.getResult();
		left.merge(rightMap);
		assertEquals("Error both maps, no overlap, left dominates", leftResult, left.getResult());

		// both maps, no overlap, right dominates
		left = prepareFunction(manager, 0, minFill);
		fillFunction(left, 0, minFill);
		left.merge(rightMap);
		assertEquals("Error both maps, no overlap, right dominates", rightMap.getResult(), left.getResult());

		// both maps, overlap, overlap creates min
		left = prepareFunction(manager, 0, minFill);
		fillFunction(left, 0, minFill);
		fillFunction(left, 0, minFill);
		fillFunction(left, dictSize / 2 + 1, minFill - 1);
		left.merge(rightMap);
		assertEquals("Error both maps, overlap, overlap creates min", table.column(0).getDictionary().get(dictSize / 2), left.getResult());

		// left map, right array, no overlap, left dominates
		left = prepareFunction(manager, 0, minFill);
		fillFunction(left, 0, minFill - 1);
		leftResult = left.getResult();
		left.merge(rightArray);
		assertEquals("Error left map, right array, no overlap, left dominates", leftResult, left.getResult());

		// left map, right array, no overlap, right dominates
		left = prepareFunction(manager, 0, minFill);
		fillFunction(left, 0, minFill);
		left.merge(rightArray);
		assertEquals("Error left map, right array, no overlap, right dominates", rightArray.getResult(), left.getResult());

		// left map, right array, overlap, overlap creates min
		left = prepareFunction(manager, 0, minFill);
		fillFunction(left, 0, minFill);
		fillFunction(left, 0, minFill);
		fillFunction(left, dictSize / 2 + 1, threshold - 1);
		left.merge(rightArray);
		assertEquals("Error left map, right array, overlap, overlap creates min", table.column(0).getDictionary().get(dictSize / 2), left.getResult());

		// left array, right empty
		left = prepareFunction(manager, 0, threshold);
		fillFunction(left, 0, threshold - 1);
		left.merge(manager.newFunction());
		assertEquals("Error left array, right empty", table.column(0).getDictionary().get(threshold - 1), left.getResult());

		// left array, right map, no overlap, left dominates
		left = prepareFunction(manager, 0, threshold);
		fillFunction(left, 0, threshold - 1);
		leftResult = left.getResult();
		left.merge(rightMap);
		assertEquals("Error left array, right map, no overlap, left dominates", leftResult, left.getResult());

		// left array, right map, no overlap, right dominates
		left = prepareFunction(manager, 0, threshold);
		fillFunction(left, 0, threshold);
		left.merge(rightMap);
		assertEquals("Error left array, right map, no overlap, right dominates", rightMap.getResult(), left.getResult());

		// left array, right map, overlap creates min
		left = prepareFunction(manager, 0, threshold);
		fillFunction(left, 0, threshold);
		fillFunction(left, 0, threshold);
		fillFunction(left, dictSize / 2 + 1, minFill - 1);
		left.merge(rightMap);
		assertEquals("Error left array, right map, overlap creates min", table.column(0).getDictionary().get(dictSize / 2), left.getResult());

		// both arrays, no overlap, left dominates
		left = prepareFunction(manager, 0, threshold);
		fillFunction(left, 0, threshold - 1);
		leftResult = left.getResult();
		left.merge(rightArray);
		assertEquals("Error both arrays, no overlap, left dominates", leftResult, left.getResult());

		// both arrays, no overlap, right dominates
		left = prepareFunction(manager, 0, threshold);
		fillFunction(left, 0, threshold);
		left.merge(rightArray);
		assertEquals("Error both arrays, no overlap, right dominates", rightArray.getResult(), left.getResult());

		// both arrays, overlap creates min
		left = prepareFunction(manager, 0, threshold);
		fillFunction(left, 0, threshold);
		fillFunction(left, 0, threshold);
		fillFunction(left, dictSize / 2 + 1, threshold - 1);
		left.merge(rightArray);
		assertEquals("Error both arrays, overlap creates min", table.column(0).getDictionary().get(dictSize / 2), left.getResult());
	}

	private static String[] columnToArray(Column column) {
		ObjectReader<String> reader = Readers.objectReader(column, String.class);
		String[] array = new String[column.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = reader.read();
		}
		return array;
	}

	private static CategoricalModeLeastAggregationFunction prepareFunction(NominalAppearanceAggregationManager manager, int offset, int count) {
		CategoricalModeLeastAggregationFunction function = manager.newFunction();
		fillFunction(function, offset, count);
		return function;
	}

	private static void fillFunction(CategoricalModeLeastAggregationFunction function, int offset, int count) {
		for (int i = 0; i < count; i++) {
			function.accept(new DummyNumRow(offset + i));
		}
	}

	private static class DummyNumRow implements NumericRow {
		private int index;

		private DummyNumRow(int index) {
			this.index = index;
		}

		@Override
		public double get(int i) {
			return index;
		}

		@Override
		public int width() {
			return 0;
		}

		@Override
		public int position() {
			return index;
		}
	}
}
