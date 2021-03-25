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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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


/**
 * Tests the {@link TablePrecondition}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class TablePreconditionTest {

	@Test
	public void testNull() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition = new TablePrecondition(generateDummyInputPort(errors));
		tablePrecondition.check(null);
		assertEquals(1, errors.size());
	}

	@Test
	public void testOther() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition = new TablePrecondition(generateDummyInputPort(errors));
		tablePrecondition.check(new MetaData(PredictionModel.class));
		assertEquals(1, errors.size());
	}

	@Test
	public void testNullOptional() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition = new TablePrecondition(generateDummyInputPort(errors));
		tablePrecondition.setOptional(true);
		tablePrecondition.check(null);
		assertEquals(0, errors.size());
	}

	@Test
	public void testExampleSetMD() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition = new TablePrecondition(generateDummyInputPort(errors));
		tablePrecondition.check(new ExampleSetMetaData());
		assertEquals(0, errors.size());
	}

	@Test
	public void testRegularPlusRoleWrong() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Column.Category.NUMERIC, ColumnRole.LABEL);
		tablePrecondition.check(new ExampleSetMetaData());
		assertEquals(1, errors.size());
	}

	@Test
	public void testRegularPlusRoleWrongId() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Column.Category.NUMERIC, ColumnRole.ID);
		tablePrecondition.check(new ExampleSetMetaData());
		assertEquals(1, errors.size());
	}

	@Test
	public void testRegularPlusRoleWrongPrediction() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Column.Category.NUMERIC, ColumnRole.PREDICTION);
		tablePrecondition.check(new ExampleSetMetaData());
		assertEquals(1, errors.size());
	}

	@Test
	public void testRegularPlusRole() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Column.Category.NUMERIC, ColumnRole.LABEL);
		tablePrecondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null,  SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testRegularNamesPlusRoleWrong() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Arrays.asList("bla", "blup"),
						Column.Category.NUMERIC, ColumnRole.LABEL);
		tablePrecondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				.addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(2, errors.size());
	}

	@Test
	public void testRegularNamesPlusRole() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Arrays.asList("real", "nominal"),
						Column.Category.NUMERIC, ColumnRole.LABEL);
		tablePrecondition.check(new TableMetaDataBuilder(10)
				.addReal("real", null, SetRelation.EQUAL, null)
				 .addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testIgnore() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Arrays.asList("real", "nominal"),
						Column.Category.NUMERIC, Collections.singleton("date"), Column.Category.CATEGORICAL,
						ColumnRole.LABEL);
		tablePrecondition.check(new TableMetaDataBuilder(10)
				 .addReal("real", null, SetRelation.EQUAL, null)
				.addDateTime("date", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testIgnoreWrong() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Arrays.asList("real", "nominal"),
						Column.Category.NUMERIC, Collections.singleton("blup"), Column.Category.CATEGORICAL,
						ColumnRole.LABEL);
		tablePrecondition.check(new TableMetaDataBuilder(10)
				 .addReal("real", null, SetRelation.EQUAL, null)
				.addDateTime("date", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testRoleUnknown() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), Arrays.asList("real", "nominal"),
						Column.Category.NUMERIC, Collections.singleton("date"), Column.Category.CATEGORICAL,
						ColumnRole.LABEL, ColumnRole.BATCH);
		tablePrecondition.check(new TableMetaDataBuilder(10)
				 .addReal("real", null, SetRelation.EQUAL, null)
				.addDateTime("date", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.columnsAreSubset()
				.build());
		assertEquals(2, errors.size());
	}

	@Test
	public void testRoleTypeWrong() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), ColumnRole.LABEL, Column.Category.NUMERIC);
		tablePrecondition.check(new TableMetaDataBuilder(10)
				 .addReal("real", null, SetRelation.EQUAL, null)
				 .addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(1, errors.size());
	}

	@Test
	public void testRoleType() {
		List<MetaDataError> errors = new ArrayList<>();
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(errors), ColumnRole.LABEL, Column.Category.CATEGORICAL);
		tablePrecondition.check(new TableMetaDataBuilder(10)
				 .addReal("real", null, SetRelation.EQUAL, null)
				 .addInteger("int", null, SetRelation.EQUAL, null)
				.add("nominal", ColumnType.NOMINAL, null)
				.addColumnMetaData("nominal", ColumnRole.LABEL)
				.build());
		assertEquals(0, errors.size());
	}

	@Test
	public void testCompatible() {
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(new ArrayList<>()), ColumnRole.LABEL,
						Column.Category.CATEGORICAL);
		assertTrue(tablePrecondition.isCompatible(new TableMetaData(), CompatibilityLevel.VERSION_5));
		assertTrue(tablePrecondition.isCompatible(new ExampleSetMetaData(), CompatibilityLevel.VERSION_5));
		assertFalse(tablePrecondition.isCompatible(new PredictionModelMetaData(PredictionModel.class),
				CompatibilityLevel.VERSION_5));
	}

	@Test
	public void testExpected() {
		final TablePrecondition tablePrecondition =
				new TablePrecondition(generateDummyInputPort(new ArrayList<>()), ColumnRole.LABEL,
						Column.Category.CATEGORICAL);
		assertTrue(tablePrecondition.getExpectedMetaData() instanceof TableMetaData);
	}

	static InputPort generateDummyInputPort(List<MetaDataError> errorList) {
		final Ports portsMock = Mockito.mock(Ports.class);
		final InputPort mock = Mockito.mock(InputPort.class);
		doAnswer(invocation -> {
			final Object argument = invocation.getArgument(0);
			errorList.add((MetaDataError) argument);
			return null;
		}).when(mock).addError(any(MetaDataError.class));
		when(mock.getPorts()).thenReturn(portsMock);
		return mock;
	}
}
