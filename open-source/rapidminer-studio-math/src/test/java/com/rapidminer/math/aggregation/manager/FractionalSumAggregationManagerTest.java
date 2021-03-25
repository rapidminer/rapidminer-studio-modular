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

import org.junit.Test;

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.math.aggregation.manager.FractionalSumAggregationManager;


/**
 * Test the {@link FractionalSumAggregationManager}.
 *
 * @author Gisa Meier
 */
public class FractionalSumAggregationManagerTest {

	@Test
	public void testReturnType() {
		assertEquals(ColumnType.REAL, new FractionalSumAggregationManager().checkColumnType(ColumnType.REAL));
		assertEquals(ColumnType.REAL, new FractionalSumAggregationManager().checkColumnType(ColumnType.INTEGER_53_BIT));
	}

	@Test
	public void testReturnTypeWrong() {
		assertNull(new FractionalSumAggregationManager().checkColumnType(ColumnType.NOMINAL));
		assertNull(new FractionalSumAggregationManager().checkColumnType(ColumnType.TIME));
		assertNull(new FractionalSumAggregationManager().checkColumnType(ColumnType.DATETIME));
	}


	@Test(expected = IllegalArgumentException.class)
	public void testInitialize() {
		new FractionalSumAggregationManager()
				.initialize(Buffers.timeBuffer(3).toColumn(), 1);
	}

	@Test
	public void testInitializeRow() {
		FractionalSumAggregationManager manager = new FractionalSumAggregationManager();
		manager.initialize(Buffers.integer53BitBuffer(3).toColumn(), 42);
		assertEquals(42, manager.getIndex());
	}

	@Test
	public void testCollector() {
		FractionalSumAggregationManager manager = new FractionalSumAggregationManager();
		manager.initialize(Buffers.integer53BitBuffer(4).toColumn(), 2);
		assertEquals(ColumnType.REAL, manager.getCollector(3).getResult(Belt.defaultContext()).type());
	}
}
