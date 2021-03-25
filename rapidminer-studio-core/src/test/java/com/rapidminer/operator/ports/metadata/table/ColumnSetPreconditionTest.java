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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.PredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * Tests the {@link ColumnSetPrecondition}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ColumnSetPreconditionTest {

	@Test
	public void testNameFixWrong() throws UndefinedParameterError {
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors), "nominal", "binominal");
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testNameFix() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors), "nominal", "real");
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testNameFixMacro() {
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors), "nominal", "%{real}2");
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testNameWrong() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real2");
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"));
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
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"), "nominal");
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testNameMultiple() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString("some_key")).thenReturn("int");
		when(handler.getParameterAsString("some_key2")).thenReturn("real");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key", "some_key2"));
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testNameMultipleWrong() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString("some_key")).thenReturn("int");
		when(handler.getParameterAsString("some_key2")).thenReturn("real2");
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key", "some_key2"));
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testNameUnknown() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real2");
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"));
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
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"));
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
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"), ColumnType.NOMINAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testTypeWrongFix() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterAsString(anyString())).thenReturn("real");
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"), ColumnType.REAL, "nominal");
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
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"), ColumnType.REAL);
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
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"), ColumnType.REAL);
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
	public void testTypeWrongList() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.isParameterSet(anyString())).thenReturn(true);
		when(handler.getParameterList(anyString())).thenReturn(Arrays.asList(new String[]{"bla","real"}, new String[]{"bla","int"}));
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameterListEntry(handler, "some_key", 1), ColumnType.NOMINAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(2, errors.size());
	}

	@Test
	public void testTypeList() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.isParameterSet(anyString())).thenReturn(true);
		when(handler.getParameterList(anyString())).thenReturn(Collections.singletonList(new String[]{"bla","real"}));
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameterListEntry(handler, "some_key", 1), ColumnType.REAL);
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
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"), Column.Category.CATEGORICAL);
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
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"), Column.Category.NUMERIC);
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
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameter(handler, "some_key"), Column.Category.CATEGORICAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testCategoryWrongList() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.isParameterSet(anyString())).thenReturn(true);
		when(handler.getParameterList(anyString())).thenReturn(Arrays.asList(new String[]{"nominal"}, new String[]{"real"}));
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameterListEntry(handler, "some_key",0), Column.Category.CATEGORICAL);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testCategoryUnknownList() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.isParameterSet(anyString())).thenReturn(true);
		when(handler.getParameterList(anyString())).thenReturn(Arrays.asList(new String[]{"bla","real"}, new String[]{"bla","unknown"}, new String[]{"bla","int"}));
		addDescription(handler);
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameterListEntry(handler, "some_key",1), Column.Category.NUMERIC);
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
	public void testCategoryList() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.isParameterSet(anyString())).thenReturn(true);
		when(handler.getParameterList(anyString())).thenReturn(Arrays.asList(new String[]{"bla","real"}, new String[]{"bla","int"}));
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameterListEntry(handler, "some_key",1), Column.Category.NUMERIC);
		precondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testCategoryListNotSet() throws UndefinedParameterError {
		ParameterHandler handler = mock(ParameterHandler.class);
		when(handler.getParameterList(anyString())).thenReturn(Arrays.asList(new String[]{"bla","real"}, new String[]{"bla","int"}));
		List<MetaDataError> errors = new ArrayList<>();
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(errors),
						ColumnSetPrecondition.getColumnsByParameterListEntry(handler, "some_key",1), Column.Category.OBJECT);
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
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(new ArrayList<>()));
		assertTrue(precondition.isCompatible(new TableMetaData(), CompatibilityLevel.VERSION_5));
		assertTrue(precondition.isCompatible(new ExampleSetMetaData(), CompatibilityLevel.VERSION_5));
		assertFalse(precondition.isCompatible(new PredictionModelMetaData(PredictionModel.class),
				CompatibilityLevel.VERSION_5));
	}

	@Test
	public void testExpected() {
		final ColumnSetPrecondition precondition =
				new ColumnSetPrecondition(generateDummyInputPort(new ArrayList<>()));
		assertTrue(precondition.getExpectedMetaData() instanceof TableMetaData);
	}

	private static void addDescription(ParameterHandler handler) {
		Parameters parameters = mock(Parameters.class);
		when(parameters.getParameterType(anyString())).thenReturn(new ParameterTypeString("bla", "blup"));
		when(handler.getParameters()).thenReturn(parameters);
	}

}
