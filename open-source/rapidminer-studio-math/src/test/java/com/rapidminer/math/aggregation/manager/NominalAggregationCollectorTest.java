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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.math.aggregation.manager.NominalAggregationCollector;


/**
 * Test the {@link NominalAggregationCollector}.
 *
 * @author Gisa Meier
 */
public class NominalAggregationCollectorTest {

	@Test
	public void testNominal() {
		Table table = Builders.newTableBuilder(10).addNominal("nominal", i->"val"+(i%5)).build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		when(dummy.getResult()).thenReturn("val2");
		collector.set(0, dummy);
		when(dummy.getResult()).thenReturn("val4");
		collector.set(2, dummy);
		Column column = collector.getResult(null);
		assertFalse(column.getDictionary().isBoolean());
	}

	@Test
	public void testBooleanBoth() {
		Table table = Builders.newTableBuilder(10).addBoolean("nominal", i -> "val" + (i % 2), "val1").build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		when(dummy.getResult()).thenReturn("val1");
		collector.set(0, dummy);
		when(dummy.getResult()).thenReturn("val0");
		collector.set(2, dummy);
		Column column = collector.getResult(null);
		assertTrue(column.getDictionary().isBoolean());
		assertEquals(table.column(0).getDictionary().get(table.column(0).getDictionary().getPositiveIndex()),
				column.getDictionary().get(column.getDictionary().getPositiveIndex()));
		assertEquals(table.column(0).getDictionary().get(table.column(0).getDictionary().getNegativeIndex()),
				column.getDictionary().get(column.getDictionary().getNegativeIndex()));
	}

	@Test
	public void testBooleanPositive() {
		Table table = Builders.newTableBuilder(10).addBoolean("nominal", i -> "val" + (i % 2), "val1").build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		when(dummy.getResult()).thenReturn("val1");
		collector.set(0, dummy);
		collector.set(2, dummy);
		Column column = collector.getResult(null);
		assertTrue(column.getDictionary().isBoolean());
		assertEquals(table.column(0).getDictionary().get(table.column(0).getDictionary().getPositiveIndex()),
				column.getDictionary().get(column.getDictionary().getPositiveIndex()));
		assertFalse(column.getDictionary().hasNegative());
	}

	@Test
	public void testBooleanNegative() {
		Table table = Builders.newTableBuilder(10).addBoolean("nominal", i -> "val" + (i % 2), "val1").build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		when(dummy.getResult()).thenReturn("val0");
		collector.set(0, dummy);
		collector.set(2, dummy);
		Column column = collector.getResult(null);
		assertTrue(column.getDictionary().isBoolean());
		assertEquals(table.column(0).getDictionary().get(table.column(0).getDictionary().getNegativeIndex()),
				column.getDictionary().get(column.getDictionary().getNegativeIndex()));
		assertFalse(column.getDictionary().hasPositive());
	}

	@Test
	public void testBooleanOnePositive() {
		Table table = Builders.newTableBuilder(10).addBoolean("nominal", i -> "val1", "val1").build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		when(dummy.getResult()).thenReturn("val1");
		collector.set(0, dummy);
		collector.set(2, dummy);
		Column column = collector.getResult(null);
		assertTrue(column.getDictionary().isBoolean());
		assertEquals(table.column(0).getDictionary().get(table.column(0).getDictionary().getPositiveIndex()),
				column.getDictionary().get(column.getDictionary().getPositiveIndex()));
		assertFalse(column.getDictionary().hasNegative());
	}

	@Test
	public void testBooleanOneNegative() {
		Table table = Builders.newTableBuilder(10).addBoolean("nominal", i -> "val0", null).build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		when(dummy.getResult()).thenReturn("val0");
		collector.set(0, dummy);
		collector.set(2, dummy);
		Column column = collector.getResult(null);
		assertTrue(column.getDictionary().isBoolean());
		assertEquals(table.column(0).getDictionary().get(table.column(0).getDictionary().getNegativeIndex()),
				column.getDictionary().get(column.getDictionary().getNegativeIndex()));
		assertFalse(column.getDictionary().hasPositive());
	}

	@Test
	public void testBooleanNone() {
		Table table = Builders.newTableBuilder(10).addBoolean("nominal", i -> "val" + (i % 2), "val1").build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		when(dummy.getResult()).thenReturn(null);
		collector.set(0, dummy);
		Column column = collector.getResult(null);
		assertTrue(column.getDictionary().isBoolean());
		assertFalse(column.getDictionary().hasNegative());
		assertFalse(column.getDictionary().hasPositive());
	}

	@Test
	public void testBooleanOnePositiveNone() {
		Table table = Builders.newTableBuilder(10).addBoolean("nominal", i -> "val1", "val1").build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		Column column = collector.getResult(null);
		assertTrue(column.getDictionary().isBoolean());
		assertFalse(column.getDictionary().hasNegative());
		assertFalse(column.getDictionary().hasPositive());
	}

	@Test
	public void testBooleanOneNegativeNone() {
		Table table = Builders.newTableBuilder(10).addBoolean("nominal", i -> "val0", null).build(Belt.defaultContext());
		NominalAggregationCollector collector = new NominalAggregationCollector(3, table.column(0).getDictionary());
		NominalAggregationCollector.NominalAggregationFunction dummy = mock(NominalAggregationCollector.NominalAggregationFunction.class);
		Column column = collector.getResult(null);
		assertTrue(column.getDictionary().isBoolean());
		assertFalse(column.getDictionary().hasNegative());
		assertFalse(column.getDictionary().hasPositive());
	}
}
