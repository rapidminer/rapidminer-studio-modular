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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.rapidminer.math.aggregation.manager.ConcatAggregationManager;
import com.rapidminer.math.aggregation.manager.NominalAggregationCollector;


/**
 * Test the {@link ConcatAggregationManager}.
 *
 * @author Gisa Meier
 */
public class ConcatAggregationManagerTest {

	@Test
	public void testReturnType() {
		assertEquals(ColumnType.NOMINAL, new ConcatAggregationManager().checkColumnType(ColumnType.NOMINAL));
	}

	@Test
	public void testReturnTypeWrong() {
		assertNull(new ConcatAggregationManager().checkColumnType(ColumnType.INTEGER_53_BIT));
		assertNull(new ConcatAggregationManager().checkColumnType(ColumnType.REAL));
		assertNull(new ConcatAggregationManager().checkColumnType(ColumnType.TIME));
		assertNull(new ConcatAggregationManager().checkColumnType(ColumnType.DATETIME));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalInitialize() {
		new ConcatAggregationManager().initialize(Buffers.realBuffer(3).toColumn(), 1);
	}

	@Test
	public void testAllMissing() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		ConcatAggregationManager manager = new ConcatAggregationManager();
		manager.initialize(column, 0);
		ConcatAggregationManager.ConcatenationAggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 4; i < 17; i++) {
			reader.move();
			function.accept(reader);
		}
		assertNull(function.getResult());
	}

	@Test
	public void testSomeValues() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		ConcatAggregationManager manager = new ConcatAggregationManager();
		manager.initialize(column, 0);
		ConcatAggregationManager.ConcatenationAggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("bla|blup|bla", function.getResult());
	}

	@Test
	public void testSomeValuesMixed() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(3, "bla");
		buffer.set(11, "blup");
		buffer.set(17, "bla");
		Column column = buffer.toColumn();

		ConcatAggregationManager manager = new ConcatAggregationManager();
		manager.initialize(column, 0);
		ConcatAggregationManager.ConcatenationAggregationFunction function = manager.newFunction();
		MixedRowReader reader = Readers.mixedRowReader(Collections.singletonList(column));
		for (int i = 0; i < 18; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("bla|blup|bla", function.getResult());
	}

	@Test
	public void testOneValue() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		buffer.set(11, "blup");
		Column column = buffer.toColumn();

		ConcatAggregationManager manager = new ConcatAggregationManager();
		manager.initialize(column, 0);
		ConcatAggregationManager.ConcatenationAggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(Collections.singletonList(column));
		for (int i = 0; i < 13; i++) {
			reader.move();
			function.accept(reader);
		}
		assertEquals("blup", function.getResult());
	}

	@Test
	public void testMerge() {
		Table table = Builders.newTableBuilder(20).addNominal("test", i-> i%3==0? "bla": i%2==0?"x":"blup").build(
				Belt.defaultContext());
		ConcatAggregationManager manager = new ConcatAggregationManager();
		manager.initialize(table.column(0), 0);
		ConcatAggregationManager.ConcatenationAggregationFunction function = manager.newFunction();
		NumericRowReader reader = Readers.numericRowReader(table);
		while(reader.hasRemaining()) {
			reader.move();
			function.accept(reader);
		}
		String expected = function.getResult();

		ConcatAggregationManager.ConcatenationAggregationFunction function2 = manager.newFunction();
		reader.setPosition(Readers.BEFORE_FIRST_ROW);
		for(int i = 0; i< 10; i++){
			reader.move();
			function2.accept(reader);
		}

		ConcatAggregationManager.ConcatenationAggregationFunction function3 = manager.newFunction();
		while(reader.hasRemaining()) {
			reader.move();
			function3.accept(reader);
		}

		function2.merge(function3);

		assertEquals(expected, function2.getResult());
	}

	@Test
	public void testCollector() {
		NominalBuffer buffer = Buffers.nominalBuffer(23);
		Column column = buffer.toColumn();

		ConcatAggregationManager manager = new ConcatAggregationManager();
		manager.initialize(column, 0);

		NominalAggregationCollector collector = manager.getCollector(7);
		ConcatAggregationManager.ConcatenationAggregationFunction dummy =
				mock(ConcatAggregationManager.ConcatenationAggregationFunction.class);
		when(dummy.getResult()).thenReturn("bla");
		collector.set(3, dummy);
		when(dummy.getResult()).thenReturn("blup|blup");
		collector.set(5, dummy);
		when(dummy.getResult()).thenReturn("");
		collector.set(6, dummy);

		Column result = collector.getResult(null);
		assertArrayEquals(new String[]{null, null, null, "bla", null, "blup|blup", ""}, columnToArray(result));
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
