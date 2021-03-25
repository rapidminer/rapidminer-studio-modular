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

import static com.rapidminer.operator.ports.metadata.table.TablePreconditionTest.generateDummyInputPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.PredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * Tests the {@link ColumnParameterPrecondition}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ColumnParameterPreconditionTest {

	@Test
	public void testNameWrong() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real2");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter");
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testName() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter");
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testNameUnknown() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real2");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter");
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.columnsAreSuperset()
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testExampleSetMD() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter");
		final ExampleSetMetaData emd = FromTableMetaDataConverter.convert(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		precondition.check(emd);
		assertEquals(0, errors.size());
	}

	@Test
	public void testTypeWrong() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", ColumnType.INTEGER_53_BIT);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testType() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", ColumnType.REAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testTypeUnknown() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("unknown");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", ColumnType.NOMINAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.add("unknown", null, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testRoleTypeWrong() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("nominal");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", ColumnRole.PREDICTION, ColumnType.NOMINAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testRoleType() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("nominal");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", ColumnRole.LABEL, ColumnType.NOMINAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testCategoryWrong() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", ColumnRole.PREDICTION, Column.Category.CATEGORICAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testCategoryUnknown() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("unknown");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", null, Column.Category.NUMERIC);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.add("unknown", null, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testCategory() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("nominal");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", null, Column.Category.CATEGORICAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testRoleCategoryWrong() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("nominal");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", ColumnRole.PREDICTION, Column.Category.CATEGORICAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testRoleCategory() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("nominal");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(errors), handler,
						"some_parameter", ColumnRole.LABEL, Column.Category.CATEGORICAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testCompatible() {
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(new ArrayList<>()), null,
						null);
		assertTrue(precondition.isCompatible(new TableMetaData(), CompatibilityLevel.VERSION_5));
		assertTrue(precondition.isCompatible(new ExampleSetMetaData(), CompatibilityLevel.VERSION_5));
		assertFalse(precondition.isCompatible(new PredictionModelMetaData(PredictionModel.class),
				CompatibilityLevel.VERSION_5));
	}

	@Test
	public void testExpected() {
		final ColumnParameterPrecondition precondition =
				new ColumnParameterPrecondition(generateDummyInputPort(new ArrayList<>()), null,
						null);
		assertTrue(precondition.getExpectedMetaData() instanceof TableMetaData);
	}

}
