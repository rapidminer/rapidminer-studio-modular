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
package com.rapidminer.operator.preprocessing;

import static com.rapidminer.operator.preprocessing.IOTablePreprocessingOperator.PREPROCESSING_MODEL_OUTPUT_PORT_NAME;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.operator.learner.IOTablePredictionTest;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.metadata.GeneralModelMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.TableModelMetaData;
import com.rapidminer.operator.ports.metadata.TablePredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.preprocessing.filter.columns.ValueTypeColumnFilter;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.documentation.OperatorDocumentation;
import com.rapidminer.tools.math.container.Range;


/**
 * Tests the {@link IOTablePreprocessingModel} and {@link IOTablePreprocessingOperator}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class IOTablePreprocessingTest {

	private static class TestPreprocessingModel extends IOTablePreprocessingModel {

		private final double value;

		private TestPreprocessingModel(IOTable trainingTable, double value) {
			super(trainingTable);
			this.value = value;
		}

		@Override
		public void applyOnData(Table adjusted, TableBuilder builder, Operator operator) throws OperatorException {
			for (String label : adjusted.labels()) {
				builder.replace(label, Buffers.sparseRealBuffer(value, adjusted.height()).toColumn());
			}
		}

		@Override
		protected boolean needsRemapping() {
			return false;
		}
	}

	private static class TestPreprocessingOperator extends IOTablePreprocessingOperator {

		public TestPreprocessingOperator(OperatorDescription description) {
			super(description);
		}

		@Override
		protected Collection<Pair<String, ColumnInfo>> modifyColumnMetaData(TableMetaData tmd, String name) throws UndefinedParameterError {
			ColumnInfoBuilder builder = new ColumnInfoBuilder(tmd.column(name));
			builder.setNumericRange(new Range(42.53, 42.53), SetRelation.EQUAL);
			return Collections.singleton(new Pair<>(name, builder.build()));
		}

		@Override
		public IOTablePreprocessingModel createPreprocessingModel(IOTable ioTable) throws OperatorException {
			return new TestPreprocessingModel(ioTable, 42.53);
		}

		@Override
		protected String[] getAllowedTypes() {
			return new String[]{ValueTypeColumnFilter.TYPE_REAL};
		}

		@Override
		public Class<? extends IOTablePreprocessingModel> getPreprocessingModelClass() {
			return TestPreprocessingModel.class;
		}

	}

	@BeforeClass
	public static void init() {
		RapidMiner.initAsserters();
	}


	@Test
	public void testModel() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("nom", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("nom", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addInt53Bit("int", i -> Math.random()).addReal("rand2", i -> Math.random())
				.addTime("time", i -> LocalTime.NOON)
				.build(Belt.defaultContext());

		TestPreprocessingOperator operator = getTestPreprocessingOperator();

		operator.getTableInputPort().receive(new IOTable(table));
		operator.doWork();
		IOTableModel model = operator.getPreprocessingModelOutputPort().getData(IOTableModel.class);
		assertEquals(TestPreprocessingModel.class, model.getClass());
		assertEquals(Arrays.asList("rand", "rand2"), model.getTrainingHeader().getTable().labels());
	}

	@Test
	public void testModelApply() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("nom", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("nom", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addInt53Bit("int", i -> Math.random()).addReal("rand2", i -> Math.random())
				.addTime("time", i -> LocalTime.NOON)
				.build(Belt.defaultContext());

		TestPreprocessingOperator operator = getTestPreprocessingOperator();

		IOTable result = operator.doWork(new IOTable(table));
		assertEquals(table.labels(), result.getTable().labels());
		RapidAssert.assertEquals(new IOTable(table.columns(Arrays.asList("nom", "int", "time"))),
				new IOTable(result.getTable().columns(Arrays.asList("nom", "int", "time"))));
		RapidAssert.assertEquals("column", "rand", result.getTable().column("rand"), result.getTable().column("rand2"
		));
		assertEquals(42.53, Readers.numericReader(result.getTable().column("rand")).read(), 0);
	}

	@Test
	public void testModelString() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("nom", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("nom", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addInt53Bit("int", i -> Math.random()).addReal("rand2", i -> Math.random())
				.addTime("time", i -> LocalTime.NOON)
				.build(Belt.defaultContext());

		TestPreprocessingOperator operator = getTestPreprocessingOperator();

		operator.getTableInputPort().receive(new IOTable(table));
		operator.doWork();
		IOTableModel model = operator.getPreprocessingModelOutputPort().getData(IOTableModel.class);
		assertEquals("TestPreprocessing" + Tools.getLineSeparator()+
				Tools.getLineSeparator() +
				"Model covering 2 attributes:" +Tools.getLineSeparator()+
				" - rand" +Tools.getLineSeparator()+
				" - rand2"+Tools.getLineSeparator(), model.toResultString());
	}

	@Test
	public void testMetaData() throws IncompatibleMDClassException {
		Table table = Builders.newTableBuilder(11).addNominal("nom", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("nom", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addInt53Bit("int", i -> Math.random()).addReal("rand2", i -> Math.random())
				.addTime("time", i -> LocalTime.NOON)
				.build(Belt.defaultContext());

		TestPreprocessingOperator operator = getTestPreprocessingOperator();
		assertEquals(TestPreprocessingModel.class, operator.getPreprocessingModelClass());

		operator.getTableInputPort().receiveMD(new TableMetaData(new IOTable(table), false));

		operator.getInputPorts().checkPreconditions();
		operator.getTransformer().transformMetaData();

		TableModelMetaData model =
				operator.getOutputPorts().getPortByName(PREPROCESSING_MODEL_OUTPUT_PORT_NAME).getMetaData(TableModelMetaData.class);
		assertEquals(TestPreprocessingModel.class, model.getObjectClass());
		assertTrue(model.isModelKind(GeneralModel.ModelKind.PREPROCESSING));
	}

	@Test
	public void testModelMetaDataMatches() throws IncompatibleMDClassException, OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("nom", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("nom", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addInt53Bit("int", i -> Math.random()).addReal("rand2", i -> Math.random())
				.addTime("time", i -> LocalTime.NOON)
				.build(Belt.defaultContext());

		TestPreprocessingOperator operator = getTestPreprocessingOperator();

		TableMetaData metaData = new TableMetaData(new IOTable(table), false);
		operator.getTableInputPort().receiveMD(metaData);

		operator.getInputPorts().checkPreconditions();
		operator.getTransformer().transformMetaData();

		TableModelMetaData modelMD =
				operator.getOutputPorts().getPortByName(PREPROCESSING_MODEL_OUTPUT_PORT_NAME).getMetaData(TableModelMetaData.class);

		operator.getTableInputPort().receive(new IOTable(table));
		operator.doWork();
		IOTableModel model = operator.getPreprocessingModelOutputPort().getData(IOTableModel.class);

		TableModelMetaData realMD = new TableModelMetaData(model, false);

		assertEquals(realMD.getObjectClass(), modelMD.getObjectClass());
		assertArrayEquals(GeneralModelMetaData.modelKindsAsArray(realMD), GeneralModelMetaData.modelKindsAsArray(modelMD));
		assertEquals(realMD.getDescription(), modelMD.getDescription());
	}

	@Test
	public void testAppliedMetaDataMatches() throws IncompatibleMDClassException, OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("nom", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("nom", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addInt53Bit("int", i -> Math.random()).addReal("rand2", i -> Math.random())
				.addTime("time", i -> LocalTime.NOON)
				.build(Belt.defaultContext());

		TestPreprocessingOperator operator = getTestPreprocessingOperator();

		IOTable ioTable = new IOTable(table);
		TableMetaData metaData = new TableMetaData(ioTable, false);
		operator.getTableInputPort().receiveMD(metaData);

		operator.getInputPorts().checkPreconditions();
		operator.getTransformer().transformMetaData();

		IOTable result = operator.doWork(new IOTable(table));

		TableMetaData appliedMD = operator.getTableOutputPort().getMetaData(TableMetaData.class);
		TableMetaData realMD = new TableMetaData(result, false);

		assertEquals(realMD.toString(), appliedMD.toString());
	}

	private TestPreprocessingOperator getTestPreprocessingOperator() {
		OperatorDocumentation documentation = mock(OperatorDocumentation.class);
		when(documentation.getShortName()).thenReturn("name");
		OperatorDescription description = mock(OperatorDescription.class);
		doReturn(documentation).when(description).getOperatorDocumentation();
		return new TestPreprocessingOperator(description);
	}

}
