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
package com.rapidminer.operator.ports.metadata.table;

import static com.rapidminer.operator.ports.metadata.table.TableMetaDataConverterTest.generateDummyOutputPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * Tests the {@link TablePassThroughRule}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class TablePassThroughRuleTest {

	@Test
	public void testRelation() {
		final TablePassThroughRule tablePassThroughRule =
				new TablePassThroughRule(generateDummyInputPort(), generateDummyOutputPort(), SetRelation.SUBSET);
		final ExampleSetMetaData metaData = new ExampleSetMetaData();
		final MetaData result = tablePassThroughRule.modifyMetaData(metaData);
		assertTrue(result instanceof TableMetaData);
		final TableMetaData tableMetaData = (TableMetaData) result;
		assertEquals(SetRelation.SUBSET, tableMetaData.getColumnSetRelation());
	}

	@Test
	public void testSubclass() {
		final TablePassThroughRule tablePassThroughRule =
				new TablePassThroughRule(generateDummyInputPort(), generateDummyOutputPort(), SetRelation.EQUAL) {
					@Override
					public TableMetaData modifyTableMetaData(TableMetaData metaData) throws UndefinedParameterError {
						return new TableMetaDataBuilder(metaData).add("new", ColumnType.REAL, new MDInteger(5)).build();
					}
				};
		TableMetaData tmd = new TableMetaDataBuilder(10).add("bla", ColumnType.NOMINAL, MDInteger.newPossible()).build();
		final MetaData result = tablePassThroughRule.modifyMetaData(tmd);
		assertTrue(result instanceof TableMetaData);
		final TableMetaData tableMetaData = (TableMetaData) result;
		assertEquals(SetRelation.EQUAL, tableMetaData.getColumnSetRelation());
		assertEquals(new HashSet<>(Arrays.asList("bla", "new")), tableMetaData.labels());
	}

	@Test
	public void testOther() {
		final TablePassThroughRule tablePassThroughRule =
				new TablePassThroughRule(generateDummyInputPort(), generateDummyOutputPort(), SetRelation.SUBSET);
		final PredictionModelMetaData modelMetaData = new PredictionModelMetaData(PredictionModel.class);
		final MetaData result = tablePassThroughRule.modifyMetaData(modelMetaData);
		assertSame(modelMetaData, result);
	}

	static InputPort generateDummyInputPort() {
		return Mockito.mock(InputPort.class);
	}
}
