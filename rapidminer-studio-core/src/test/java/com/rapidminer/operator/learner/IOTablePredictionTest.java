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
package com.rapidminer.operator.learner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.TableCapability;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.metadata.TableModelMetaData;
import com.rapidminer.operator.ports.metadata.TablePredictionModelMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.tools.documentation.OperatorDocumentation;


/**
 * Tests the {@link IOTablePredictionModel} and the {@link AbstractIOTableLearner}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class IOTablePredictionTest {

	private static class TestPredictionModel extends IOTablePredictionModel {

		private final double value;

		private TestPredictionModel(IOTable trainingTable, double value) {
			super(trainingTable, Tables.ColumnSetRequirement.SUPERSET, Tables.TypeRequirement.REQUIRE_MATCHING_TYPES,
					Tables.TypeRequirement.ALLOW_INT_FOR_REAL);
			this.value = value;
		}

		private TestPredictionModel() {
			//default constructor for json
			this.value = Double.NaN;
		}

		@Override
		protected Column performPrediction(Table adapted, Map<String, Column> confidences, Operator operator) throws OperatorException {
			Column labelColumn = getLabelColumn();
			if (labelColumn.type().id() == Column.TypeId.NOMINAL) {
				String s = labelColumn.getDictionary().get((int) value);
				Map<String, NumericBuffer> confidenceBuffers = createConfidenceBuffers(adapted.height());
				for (Map.Entry<String, NumericBuffer> entry : confidenceBuffers.entrySet()) {
					boolean equals = entry.getKey().equals(s);
					NumericBuffer buffer = entry.getValue();
					for (int i = 0; i < buffer.size(); i++) {
						buffer.set(i, equals ? 1 : 0);
					}
					confidences.put(entry.getKey(), buffer.toColumn());
				}
				return Buffers.sparseNominalBuffer(s, adapted.height()).toColumn();
			} else {
				return Buffers.sparseRealBuffer(value, adapted.height()).toColumn();
			}
		}

	}

	private static class TestPredictionOperator extends AbstractIOTableLearner{

		public TestPredictionOperator(OperatorDescription description) {
			super(description);
		}

		@Override
		public IOTableModel learn(IOTable trainingTable) throws OperatorException {
			return new TestPredictionModel(trainingTable, 42.53);
		}

		@Override
		public boolean canCalculateWeights() {
			return true;
		}

		@Override
		public AttributeWeights getWeights(IOTable table) throws OperatorException {
			return new AttributeWeights(table);
		}

		@Override
		public Class<? extends IOTablePredictionModel> getModelClass() {
			return TestPredictionModel.class;
		}

		@Override
		public Set<TableCapability> supported() {
			return null;
		}

		@Override
		public Set<TableCapability> unsupported() {
			return null;
		}
	}


	@Test
	public void testApplyNominal() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOTable apply = model.apply(new IOTable(table), null);
		assertEquals(Arrays.asList("label", "rand", "prediction(label)", "confidence(Yes)", "confidence(NO)"),
				apply.getTable().labels());
		assertEquals(ColumnRole.PREDICTION, apply.getTable().getFirstMetaData("prediction(label)", ColumnRole.class));
		assertEquals(ColumnRole.SCORE, apply.getTable().getFirstMetaData("confidence(Yes)", ColumnRole.class));
		assertEquals(ColumnRole.SCORE, apply.getTable().getFirstMetaData("confidence(NO)", ColumnRole.class));
		assertEquals(new ColumnReference("prediction(label)", "NO"), apply.getTable().getFirstMetaData("confidence(NO)"
				, ColumnReference.class));
		assertEquals(new ColumnReference("prediction(label)", "Yes"), apply.getTable().getFirstMetaData("confidence" +
				"(Yes)", ColumnReference.class));
	}

	@Test
	public void testApplyReal() throws OperatorException {
		Table table =
				Builders.newTableBuilder(11).addReal("label", i -> 0).addMetaData("label", ColumnRole.LABEL).addReal(
						"rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOTable apply = model.apply(new IOTable(table), null);
		assertEquals(Arrays.asList("label", "rand", "prediction(label)"), apply.getTable().labels());
		assertEquals(ColumnRole.PREDICTION, apply.getTable().getFirstMetaData("prediction(label)", ColumnRole.class));
	}

	@Test
	public void testApplyRealInt() throws OperatorException {
		Table table =
				Builders.newTableBuilder(11).addReal("label", i -> 0).addMetaData("label", ColumnRole.LABEL).addReal(
						"rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		Table table2 =
				Builders.newTableBuilder(15).addInt53Bit("rand", i -> 0).addReal("rand2", i -> Math.random()).build(Belt.defaultContext());
		IOTable apply = model.apply(new IOTable(table2), null);
		assertEquals(Arrays.asList("rand", "rand2", "prediction(label)"), apply.getTable().labels());
		assertEquals(ColumnRole.PREDICTION, apply.getTable().getFirstMetaData("prediction(label)", ColumnRole.class));
	}

	@Test
	public void testApplyRemapped() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).addNominal("bla", i ->
				i % 2 == 0 ? "bla" : "blup").build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		Table table2 =
				Builders.newTableBuilder(15).addInt53Bit("rand2", i -> 0).addBoolean("bla", i -> i % 3 == 0 ? "blup" :
						"bla", "bla").addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		IOTable apply = model.apply(new IOTable(table2), null);
		assertEquals(Arrays.asList("rand2", "bla", "rand", "prediction(label)", "confidence(Yes)", "confidence(NO)"), apply.getTable().labels());
		assertEquals(ColumnRole.PREDICTION, apply.getTable().getFirstMetaData("prediction(label)", ColumnRole.class));
	}

	@Test
	public void testApplytable() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).addNominal("bla", i ->
				i % 2 == 0 ? "bla" : "blup").build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2){
			@Override
			protected Column performPrediction(Table adapted, Map<String, Column> confidences, Operator operator) throws OperatorException {
				assertEquals(Arrays.asList("rand", "bla"), adapted.labels());
				assertEquals(table.column("bla").getDictionary(), adapted.column("bla").getDictionary());
				return super.performPrediction(adapted, confidences, operator);
			}
		};
		Table table2 =
				Builders.newTableBuilder(15).addInt53Bit("rand2", i -> 0).addBoolean("bla", i -> i % 3 == 0 ? "blup" :
						"bla", "bla").addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		model.apply(new IOTable(table2), null);
	}

	@Test
	public void testDoWork() throws OperatorException {
		Table table =
				Builders.newTableBuilder(11).addReal("label", i -> 0).addMetaData("label", ColumnRole.LABEL).addReal(
						"rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionOperator testPredictionOperator = getTestPredictionOperator();
		IOTableModel model = testPredictionOperator.doWork(new IOTable(table));

		assertEquals(TestPredictionModel.class, model.getClass());
		assertEquals(Arrays.asList("label", "rand"), model.getTrainingHeader().getTable().labels());
	}

	@Test(expected = UserError.class)
	public void testWrongInput() throws OperatorException {
		Table table =
				Builders.newTableBuilder(11).addReal("label", i -> 0).addMetaData("label", ColumnRole.LABEL).build(Belt.defaultContext());
		TestPredictionOperator testPredictionOperator = getTestPredictionOperator();
		testPredictionOperator.doWork(new IOTable(table));
	}

	@Test
	public void testMetaData() throws IncompatibleMDClassException {
		Table table =
				Builders.newTableBuilder(11).addReal("label", i -> 0).addMetaData("label", ColumnRole.LABEL).addReal(
						"rand", i -> Math.random()).build(Belt.defaultContext());

		TestPredictionOperator testPredictionOperator = getTestPredictionOperator();
		assertEquals(TestPredictionModel.class, testPredictionOperator.getModelClass());

		testPredictionOperator.getTableInputPort().receiveMD(new TableMetaData(new IOTable(table), false));

		testPredictionOperator.getInputPorts().checkPreconditions();
		testPredictionOperator.getTransformer().transformMetaData();

		TableModelMetaData model =
				testPredictionOperator.getOutputPorts().getPortByName("model").getMetaData(TableModelMetaData.class);
		assertEquals(TestPredictionModel.class, model.getObjectClass());
		assertTrue(model.isModelKind(GeneralModel.ModelKind.SUPERVISED));
		assertEquals(new HashSet<>(Arrays.asList("label", "rand")), model.getTrainingMetaData().labels());
	}

	@Test
	public void testModelMetaDataMatches() throws IncompatibleMDClassException, OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());

		TestPredictionOperator testPredictionOperator = getTestPredictionOperator();
		assertEquals(TestPredictionModel.class, testPredictionOperator.getModelClass());

		TableMetaData metaData = new TableMetaData(new IOTable(table), true);
		testPredictionOperator.getTableInputPort().receiveMD(metaData);

		testPredictionOperator.getInputPorts().checkPreconditions();
		testPredictionOperator.getTransformer().transformMetaData();

		IOTableModel model = testPredictionOperator.doWork(new IOTable(table));
		TablePredictionModelMetaData modelMD =
				testPredictionOperator.getOutputPorts().getPortByName("model").getMetaData(TablePredictionModelMetaData.class);

		TablePredictionModelMetaData realMD = new TablePredictionModelMetaData((Class<?
				extends IOTablePredictionModel>) model.getClass(), new TableMetaData(model.getTrainingHeader(),true));

		assertEquals(realMD.getPredictedLabelMetaData(), modelMD.getPredictedLabelMetaData());
		assertEquals(realMD.getPredictionMetaData().toString(), modelMD.getPredictionMetaData().toString());
		assertEquals(realMD.getPredictionColumnSetRelation(), modelMD.getPredictionColumnSetRelation());
		assertEquals(realMD.getDescription(), modelMD.getDescription());
	}

	@Test
	public void testAppliedMetaDataMatches() throws IncompatibleMDClassException, OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());

		TestPredictionOperator testPredictionOperator = getTestPredictionOperator();
		assertEquals(TestPredictionModel.class, testPredictionOperator.getModelClass());

		IOTable ioTable = new IOTable(table);
		TableMetaData metaData = new TableMetaData(ioTable, true);
		testPredictionOperator.getTableInputPort().receiveMD(metaData);

		testPredictionOperator.getInputPorts().checkPreconditions();
		testPredictionOperator.getTransformer().transformMetaData();

		IOTableModel model = testPredictionOperator.doWork(ioTable);
		TablePredictionModelMetaData modelMD =
				testPredictionOperator.getOutputPorts().getPortByName("model").getMetaData(TablePredictionModelMetaData.class);

		IOTable applied = model.apply(ioTable, null);
		TableMetaData appliedMD = modelMD.apply(metaData, testPredictionOperator.getTableInputPort());
		TableMetaData realMD = new TableMetaData(applied, true);

		assertEquals(realMD.labels(), appliedMD.labels());
		assertEquals(realMD.getColumns().stream().map(c -> c.getType().get()).collect(Collectors.toList()),
				appliedMD.getColumns().stream().map(c -> c.getType().get()).collect(Collectors.toList()));
		assertEquals(realMD.labels().stream().map(realMD::getColumnMetaData).collect(Collectors.toSet()),
				appliedMD.labels().stream().map(appliedMD::getColumnMetaData).collect(Collectors.toSet()));
	}

	private TestPredictionOperator getTestPredictionOperator() {
		OperatorDocumentation documentation = mock(OperatorDocumentation.class);
		when(documentation.getShortName()).thenReturn("name");
		OperatorDescription description = mock(OperatorDescription.class);
		doReturn(documentation).when(description).getOperatorDocumentation();
		return new TestPredictionOperator(description);
	}
}
