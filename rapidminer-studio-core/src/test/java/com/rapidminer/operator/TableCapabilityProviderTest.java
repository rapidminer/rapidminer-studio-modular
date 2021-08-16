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
package com.rapidminer.operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.learner.AbstractIOTableLearner;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.TableCapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.preprocessing.discretization.AbsoluteDiscretization;
import com.rapidminer.operator.preprocessing.discretization.BinDiscretization;
import com.rapidminer.operator.preprocessing.discretization.FrequencyDiscretization;
import com.rapidminer.operator.preprocessing.discretization.MinMaxBinDiscretization;
import com.rapidminer.operator.preprocessing.discretization.MinimalEntropyDiscretization;
import com.rapidminer.operator.preprocessing.discretization.UserBasedDiscretization;
import com.rapidminer.tools.documentation.OperatorDocumentation;


/**
 * Tests the {@link TableCapabilityProvider}, the {@link TableCapabilityCheck} and the {@link
 * com.rapidminer.operator.ports.metadata.TableCapabilityPrecondition}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class TableCapabilityProviderTest {

	private static class TestLearner extends AbstractIOTableLearner {

		private final Set<TableCapability> supported;
		private final Set<TableCapability> unsupported;

		public TestLearner(Set<TableCapability> supported, Set<TableCapability> unsupported) {
			super(mockOperatorDescription());
			this.supported = supported;
			this.unsupported = unsupported;
		}

		@Override
		public Set<TableCapability> supported() {
			return supported;
		}

		@Override
		public Set<TableCapability> unsupported() {
			return unsupported;
		}

		@Override
		public IOTableModel learn(IOTable trainingTable) throws OperatorException {
			return new IOTablePredictionModel() {
				@Override
				protected Column performPrediction(Table adapted, Map<String, Column> confidences, Operator operator) throws OperatorException {
					return Buffers.sparseRealBuffer(42, adapted.height()).toColumn();
				}
			};
		}
	}

	private static class TestOperator extends Operator implements TableCapabilityProvider {

		private final Set<TableCapability> supported;
		private final Set<TableCapability> unsupported;

		public TestOperator(Set<TableCapability> supported, Set<TableCapability> unsupported) {
			super(mockOperatorDescription());
			this.supported = supported;
			this.unsupported = unsupported;
		}

		@Override
		public Set<TableCapability> supported() {
			return supported;
		}

		@Override
		public Set<TableCapability> unsupported() {
			return unsupported;
		}

	}

	@BeforeClass
	public static void initRapidMiner() {
		// trigger some static initializers
		try {
			Class.forName(AbsoluteDiscretization.class.getName());
			Class.forName(BinDiscretization.class.getName());
			Class.forName(FrequencyDiscretization.class.getName());
			Class.forName(MinimalEntropyDiscretization.class.getName());
			Class.forName(MinMaxBinDiscretization.class.getName());
			Class.forName(UserBasedDiscretization.class.getName());
		} catch (ClassNotFoundException e) {
			//ignore
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testOverlap() throws OperatorException {
		Table table = Builders.newTableBuilder(1).addReal("reg", i -> 0).build(Belt.defaultContext());
		testDoWork(table, EnumSet.allOf(TableCapability.class), EnumSet.of(TableCapability.UPDATABLE));
	}

	@Test(expected = IllegalStateException.class)
	public void testOverlapNonLearner() throws OperatorException {
		Table table = Builders.newTableBuilder(1).addReal("reg", i -> 0).build(Belt.defaultContext());
		testDoWork(table, EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NUMERIC_COLUMNS,
				TableCapability.DATE_TIME_COLUMNS, TableCapability.TIME_COLUMNS,
				TableCapability.ADVANCED_COLUMNS, TableCapability.MISSING_VALUES,
				TableCapability.NOMINAL_COLUMNS), EnumSet.of(TableCapability.MISSING_VALUES));
	}

	@Test(expected = IllegalStateException.class)
	public void testNotAll() {
		TestLearner testLearner =
				new TestLearner(EnumSet.of(TableCapability.NOMINAL_COLUMNS, TableCapability.NOMINAL_LABEL),
						EnumSet.of(TableCapability.UPDATABLE));
		testLearner.checkCompatible(true);
	}

	@Test(expected = IllegalStateException.class)
	public void testNotAllNonLearner() {
		TestOperator operator = new TestOperator(EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NUMERIC_COLUMNS,
				TableCapability.DATE_TIME_COLUMNS, TableCapability.TIME_COLUMNS,
				TableCapability.NOMINAL_COLUMNS), EnumSet.of(TableCapability.MISSING_VALUES));
		operator.checkCompatible(true);
	}

	@Test(expected = UserError.class)
	public void testNominalColumnsError() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExceptNominalColumns =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NUMERIC_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS, TableCapability.MISSING_VALUES,
						TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExceptNominalColumns, EnumSet.of(TableCapability.NOMINAL_COLUMNS));
		assertEquals(1, processSetupErrors.size());
		assertEquals(2, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExceptNominalColumns, EnumSet.of(TableCapability.NOMINAL_COLUMNS));
	}

	@Test
	public void test2ClassColumnsOk() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addNominal("nom", i -> "bla" + (i%2)).build(Belt.defaultContext());
		EnumSet<TableCapability> allExceptNominalColumns =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NUMERIC_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS, TableCapability.MISSING_VALUES,
						TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExceptNominalColumns, EnumSet.of(TableCapability.NOMINAL_COLUMNS));
		assertEquals(0, processSetupErrors.size());
		testDoWork(table, allExceptNominalColumns, EnumSet.of(TableCapability.NOMINAL_COLUMNS));
	}

	@Test(expected = UserError.class)
	public void test2ClassColumnsError() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addNominal("nom", i -> "bla" + i).addNominal("nom2", i -> "bla" + (i%2)).build(Belt.defaultContext());
		EnumSet<TableCapability> allExceptNominalAnd2ClassColumns =
				EnumSet.of(TableCapability.NUMERIC_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS, TableCapability.MISSING_VALUES,
						TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExceptNominalAnd2ClassColumns, EnumSet.of(TableCapability.NOMINAL_COLUMNS, TableCapability.TWO_CLASS_COLUMNS));
		assertEquals(1, processSetupErrors.size());
		assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExceptNominalAnd2ClassColumns, EnumSet.of(TableCapability.NOMINAL_COLUMNS, TableCapability.TWO_CLASS_COLUMNS));
	}

	@Test(expected = UserError.class)
	public void test2ClassColumnsErrorOnly() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addNominal("nom", i -> "bla" + (i%2)).build(Belt.defaultContext());
		EnumSet<TableCapability> allExceptNominalAnd2ClassColumns =
				EnumSet.of(TableCapability.NUMERIC_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS, TableCapability.MISSING_VALUES,
						TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExceptNominalAnd2ClassColumns, EnumSet.of(TableCapability.NOMINAL_COLUMNS, TableCapability.TWO_CLASS_COLUMNS));
		assertEquals(1, processSetupErrors.size());
		assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExceptNominalAnd2ClassColumns, EnumSet.of(TableCapability.NOMINAL_COLUMNS, TableCapability.TWO_CLASS_COLUMNS));
	}

	@Test(expected = UserError.class)
	public void testNumericColumns() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random())
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExceptNumericColumns =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS, TableCapability.MISSING_VALUES,
						TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExceptNumericColumns, EnumSet.of(TableCapability.NUMERIC_COLUMNS));
		assertEquals(1, processSetupErrors.size());
		assertEquals(5, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExceptNumericColumns, EnumSet.of(TableCapability.NUMERIC_COLUMNS));

	}

	@Test(expected = UserError.class)
	public void testDateTimeColumns() throws OperatorException {
			Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
					"NO").addMetaData("label", ColumnRole.LABEL).addDateTime("rand", i -> null)
					.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
			EnumSet<TableCapability> allExcept =
					EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
							TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
							TableCapability.ADVANCED_COLUMNS, TableCapability.MISSING_VALUES,
							TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
							TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
							TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
							TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
							TableCapability.WEIGHTED_ROWS);
			List<ProcessSetupError> processSetupErrors =
					testPrecondition(table, allExcept, EnumSet.of(TableCapability.DATE_TIME_COLUMNS));
			assertEquals(1, processSetupErrors.size());
			assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
			testDoWork(table, allExcept, EnumSet.of(TableCapability.DATE_TIME_COLUMNS));
	}

	@Test(expected = UserError.class)
	public void testTimeColumns() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.DATE_TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS, TableCapability.MISSING_VALUES,
						TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.TIME_COLUMNS));
		assertEquals(1, processSetupErrors.size());
		assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.TIME_COLUMNS));
	}

	@Test(expected = UserError.class)
	public void testAdvancedColumns() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addText("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.MISSING_VALUES,
						TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.ADVANCED_COLUMNS));
		assertEquals(1, processSetupErrors.size());
		assertEquals(0, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.ADVANCED_COLUMNS));
	}

	@Test(expected = UserError.class)
	public void testMissingInRegulars() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.NOMINAL_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.MISSING_VALUES));
		assertEquals(1, processSetupErrors.size());
		assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.MISSING_VALUES));
	}

	@Test(expected = UserError.class)
	public void testNominalLabel() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> "Yes"+i)
				.addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL));
		assertEquals(1, processSetupErrors.size());
		assertEquals(4, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL));

	}

	@Test
	public void testOneClassLabelOk() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> "Yes")
				.addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL, TableCapability.TWO_CLASS_LABEL));
		assertEquals(0, processSetupErrors.size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL, TableCapability.TWO_CLASS_LABEL));
	}

	@Test(expected = UserError.class)
	public void testOneClassLabelError() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i ->  "Yes")
				.addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NUMERIC_LABEL,
						TableCapability.TWO_CLASS_LABEL, TableCapability.NOMINAL_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.ONE_CLASS_LABEL));
		assertEquals(1, processSetupErrors.size());
		assertEquals(0, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.ONE_CLASS_LABEL));
	}

	@Test
	public void testTwoClassLabelOk() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NUMERIC_LABEL,
						TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL, TableCapability.ONE_CLASS_LABEL));
		assertEquals(0, processSetupErrors.size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL, TableCapability.ONE_CLASS_LABEL));
	}

	@Test(expected = UserError.class)
	public void testTwoClassLabelError() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NUMERIC_LABEL,
						TableCapability.ONE_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL, TableCapability.TWO_CLASS_LABEL));
		assertEquals(1, processSetupErrors.size());
		assertEquals(0, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL, TableCapability.TWO_CLASS_LABEL));
	}

	@Test(expected = UserError.class)
	public void testTwoAndOneClassLabelError() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NUMERIC_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL,
						TableCapability.TWO_CLASS_LABEL, TableCapability.ONE_CLASS_LABEL));
		assertEquals(1, processSetupErrors.size());
		assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL,
				TableCapability.TWO_CLASS_LABEL, TableCapability.ONE_CLASS_LABEL));
	}

	@Test(expected = UserError.class)
	public void testNumericLabel() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addInt53Bit("label", i -> i)
				.addMetaData("label", ColumnRole.LABEL).addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NOMINAL_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NUMERIC_LABEL));
		assertEquals(1, processSetupErrors.size());
		assertEquals(5, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NUMERIC_LABEL));
	}

	@Test(expected = UserError.class)
	public void testOtherLabel() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addInt53Bit("label", i -> i)
				.addTime("rand", i -> null).addMetaData("rand", ColumnRole.LABEL)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NOMINAL_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS, TableCapability.NUMERIC_LABEL);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.noneOf(TableCapability.class));
		assertEquals(1, processSetupErrors.size());
		assertEquals(0, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.noneOf(TableCapability.class));
	}

	@Test
	public void testNoLabelOk() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addInt53Bit("label", i -> i)
				.addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NOMINAL_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NUMERIC_LABEL));
		assertEquals(0, processSetupErrors.size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NUMERIC_LABEL));
	}

	@Test(expected = UserError.class)
	public void testNoLabelError() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addInt53Bit("label", i -> i)
				.addTime("rand", i -> null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NOMINAL_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NUMERIC_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NO_LABEL));
		assertEquals(1, processSetupErrors.size());
		assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NO_LABEL));
	}

	@Test
	public void testMultipleLabelsOk() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addInt53Bit("rand", i -> Double.NaN)
				.addMetaData("rand", ColumnRole.LABEL)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NUMERIC_LABEL,
						TableCapability.TWO_CLASS_LABEL, TableCapability.MULTIPLE_LABELS,
						TableCapability.NO_LABEL,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL, TableCapability.ONE_CLASS_LABEL));
		assertEquals(0, processSetupErrors.size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.NOMINAL_LABEL, TableCapability.ONE_CLASS_LABEL));
	}

	@Test(expected = UserError.class)
	public void testMultipleLabelsError() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addInt53Bit("rand", i -> i)
				.addMetaData("rand", ColumnRole.LABEL)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NOMINAL_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.MULTIPLE_LABELS));
		assertEquals(1, processSetupErrors.size());
		assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.MULTIPLE_LABELS));
	}

	@Test(expected = UserError.class)
	public void testMissingsInLabel() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addInt53Bit("rand", i ->Double.NaN)
				.addMetaData("rand", ColumnRole.LABEL)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NOMINAL_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.MULTIPLE_LABELS, TableCapability.UPDATABLE,
						TableCapability.WEIGHTED_ROWS);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.MISSINGS_IN_LABEL));
		assertEquals(1, processSetupErrors.size());
		assertEquals(1, processSetupErrors.get(0).getQuickFixes().size());
		testDoWork(table, allExcept, EnumSet.of(TableCapability.MISSINGS_IN_LABEL));
	}

	@Test
	public void testWeightedRows() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addInt53Bit("rand", i ->i)
				.addMetaData("rand", ColumnRole.WEIGHT)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.DATE_TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES, TableCapability.NOMINAL_LABEL,
						TableCapability.ONE_CLASS_LABEL, TableCapability.TWO_CLASS_LABEL,
						TableCapability.NO_LABEL, TableCapability.NUMERIC_LABEL,
						TableCapability.MULTIPLE_LABELS, TableCapability.UPDATABLE,
						TableCapability.MISSINGS_IN_LABEL);
		List<ProcessSetupError> processSetupErrors =
				testPrecondition(table, allExcept, EnumSet.of(TableCapability.WEIGHTED_ROWS));
		assertEquals(1, processSetupErrors.size());
		assertEquals(0, processSetupErrors.get(0).getQuickFixes().size());
		// warning but no user error
		testDoWork(table, allExcept, EnumSet.of(TableCapability.WEIGHTED_ROWS));
	}


	@Test
	public void testNonLearnerNoLabel() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addDateTime("rand", i ->null)
				.addMetaData("rand", ColumnRole.LABEL)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES);
		TestOperator operator = new TestOperator(allExcept, EnumSet.of(TableCapability.DATE_TIME_COLUMNS));
		new TableCapabilityCheck(operator).checkCapabilities(table, operator);
	}

	@Test(expected = UserError.class)
	public void testNonLearnerError() throws OperatorException {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addDateTime("rand", i ->null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES);
		TestOperator operator = new TestOperator(allExcept, EnumSet.of(TableCapability.DATE_TIME_COLUMNS));
		new TableCapabilityCheck(operator).checkCapabilities(table, operator);
	}

	@Test
	public void testNonLearnerNoLabelPrecondition() {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addDateTime("rand", i ->null)
				.addMetaData("rand", ColumnRole.LABEL)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES);
		TestOperator operator = new TestOperator(allExcept, EnumSet.of(TableCapability.DATE_TIME_COLUMNS));
		List<MetaDataError> errorList = new ArrayList<>();
		final Ports portsMock = Mockito.mock(Ports.class);
		final InputPort mock = Mockito.mock(InputPort.class);
		doAnswer(invocation -> {
			final Object argument = invocation.getArgument(0);
			errorList.add((MetaDataError) argument);
			return null;
		}).when(mock).addError(any(MetaDataError.class));
		when(mock.getPorts()).thenReturn(portsMock);
		new TableCapabilityPrecondition(operator, mock).makeAdditionalChecks(new TableMetaData(new IOTable(table), false));
		assertTrue(errorList.isEmpty());
	}

	@Test
	public void testNonLearnerPreconditionCheck() {
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addDateTime("rand", i ->null)
				.addNominal("nom", i -> "bla" + i).build(Belt.defaultContext());
		EnumSet<TableCapability> allExcept =
				EnumSet.of(TableCapability.TWO_CLASS_COLUMNS, TableCapability.NOMINAL_COLUMNS,
						TableCapability.NUMERIC_COLUMNS, TableCapability.TIME_COLUMNS,
						TableCapability.ADVANCED_COLUMNS,
						TableCapability.MISSING_VALUES);
		TestOperator operator = new TestOperator(allExcept, EnumSet.of(TableCapability.DATE_TIME_COLUMNS));
		List<MetaDataError> errorList = new ArrayList<>();
		final Ports portsMock = Mockito.mock(Ports.class);
		final InputPort mock = Mockito.mock(InputPort.class);
		doAnswer(invocation -> {
			final Object argument = invocation.getArgument(0);
			errorList.add((MetaDataError) argument);
			return null;
		}).when(mock).addError(any(MetaDataError.class));
		when(mock.getPorts()).thenReturn(portsMock);
		new TableCapabilityPrecondition(operator, mock).makeAdditionalChecks(new TableMetaData(new IOTable(table), false));
		assertEquals(1, errorList.size());
	}


	private static OperatorDescription mockOperatorDescription() {
		OperatorDocumentation documentation = mock(OperatorDocumentation.class);
		when(documentation.getShortName()).thenReturn("name");
		OperatorDescription description = mock(OperatorDescription.class);
		doReturn(documentation).when(description).getOperatorDocumentation();
		doReturn("name").when(description).getName();
		return description;
	}

	private static void testDoWork(Table table, Set<TableCapability> supported,
								   Set<TableCapability> unsupported) throws OperatorException {
		TestLearner testLearner = new TestLearner(supported, unsupported);
		testLearner.doWork(new IOTable(table));
	}

	private static List<ProcessSetupError> testPrecondition(Table table, Set<TableCapability> supported,
															Set<TableCapability> unsupported) {
		TestLearner testLearner = new TestLearner(supported, unsupported);
		IOTable ioTable = new IOTable(table);
		TableMetaData metaData = new TableMetaData(ioTable, false);
		testLearner.getInputPorts().getPortByName("training set").receiveMD(metaData);

		testLearner.getInputPorts().checkPreconditions();
		return testLearner.getErrorList();
	}
}
