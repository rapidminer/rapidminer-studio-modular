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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.belt.BeltErrorTools;
import com.rapidminer.tools.belt.BeltTools;


/**
 * Checks if the the given operator can work on the table using its {@link TableCapabilityProvider}.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public class TableCapabilityCheck {

	private final TableCapabilityProvider capabilityProvider;

	/**
	 * Constructs a checker for the given capability provider.
	 *
	 * @param provider
	 * 		the capability provider to use for checks
	 */
	public TableCapabilityCheck(TableCapabilityProvider provider) {
		this.capabilityProvider = provider;
		capabilityProvider.checkCompatible(false);
	}

	/**
	 * Checks if the table fits the capabilities of the capability provider.
	 *
	 * @param table
	 * 		the table to check
	 * @param operator
	 * 		the operator to use for exceptions and parallel calculation
	 * @throws OperatorException
	 * 		if the capabilities do not fit
	 */
	public void checkCapabilities(Table table, Operator operator) throws OperatorException {
		Set<TableCapability> unsupported = capabilityProvider.unsupported();
		if (unsupported == null) {
			unsupported = Collections.emptySet();
		}
		checkRegularColumns(operator, table, unsupported);
		if (capabilityProvider.isLearner()) {
			// label
			checkLabel(operator, table, unsupported);
		}
		// checking last because might take some time
		checkMissings(operator, table, unsupported);
	}

	/**
	 * Checks that there are no missings in the regular columns.
	 */
	private void checkMissings(Operator operator, Table table, Set<TableCapability> unsupported) throws OperatorException {
		if (unsupported.contains(TableCapability.MISSING_VALUES)) {
			BeltErrorTools.onlyNonMissingValues(BeltTools.regularSubtable(table),
					operator != null ? operator.getName() :
							"capability provider", BeltTools.getContext(operator), operator);
		}
	}


	private void checkRegularColumns(Operator operator, Table table, Set<TableCapability> unsupported) throws UserError {
		Table regularTable = BeltTools.regularSubtable(table);

		// nominal columns
		checkNominalRegulars(operator, regularTable, unsupported);

		// numerical columns
		if (unsupported.contains(TableCapability.NUMERIC_COLUMNS)
				&& !regularTable.select().ofCategory(Column.Category.NUMERIC).labels().isEmpty()) {
			throwCapabilityError(operator, TableCapability.NUMERIC_COLUMNS);
			return;
		}

		// time columns
		if (unsupported.contains(TableCapability.TIME_COLUMNS)
				&& !regularTable.select().ofTypeId(Column.TypeId.TIME).labels().isEmpty()) {
			throwCapabilityError(operator, TableCapability.TIME_COLUMNS);
		}

		// date-time columns
		if (unsupported.contains(TableCapability.DATE_TIME_COLUMNS)
				&& !regularTable.select().ofTypeId(Column.TypeId.DATE_TIME).labels().isEmpty()) {
			throwCapabilityError(operator, TableCapability.DATE_TIME_COLUMNS);
		}


		// advanced columns
		if (unsupported.contains(TableCapability.ADVANCED_COLUMNS)
				&& !regularTable.select().matchesPredicate((c, s) ->
				!BeltConverter.STANDARD_TYPES.contains(c.type().id())).labels().isEmpty()) {
			throwCapabilityError(operator, TableCapability.ADVANCED_COLUMNS);
		}

	}

	/**
	 * Checks the nominal regular columns with regard to the capabilities.
	 */
	private void checkNominalRegulars(Operator learningOperator, Table regularTable,
									  Set<TableCapability> unsupported) throws UserError {
		List<Column> nominalColumns = regularTable.select().ofTypeId(Column.TypeId.NOMINAL).columns();
		if (!nominalColumns.isEmpty() && unsupported.contains(TableCapability.NOMINAL_COLUMNS) &&
				(unsupported.contains(TableCapability.TWO_CLASS_COLUMNS) ||
						nominalColumns.stream().anyMatch(c -> c.getDictionary().size() > 2))) {
			//nominal and two-class unsupported OR not all are two-class
			throwCapabilityError(learningOperator, TableCapability.NOMINAL_COLUMNS);
		}
	}

	/**
	 * Checks the label columns with regard to the capabilities.
	 */
	private void checkLabel(Operator learningOperator, Table table, Set<TableCapability> unsupported) throws UserError {
		List<Column> labelColumns = table.select().withMetaData(ColumnRole.LABEL).columns();
		if (labelColumns.size() > 1 &&
				unsupported.contains(TableCapability.MULTIPLE_LABELS)) {
			throwCapabilityError(learningOperator, TableCapability.MULTIPLE_LABELS);
		}
		if (!labelColumns.isEmpty()) {
			for (Column labelColumn : labelColumns) {
				if (labelColumn.type().id() == Column.TypeId.NOMINAL) {
					checkNominalLabel(learningOperator, unsupported, labelColumn);
				} else if (labelColumn.type().category() == Column.Category.NUMERIC) {
					checkNumericLabel(learningOperator, unsupported);
				} else {
					//other label types not supported
					throw new UserError(learningOperator, "operator_capability.insufficient_capability", learningOperator.getName(),
							I18N.getGUIMessage("gui.non_capability.description.label_type",
									labelColumn.type().id().toString().toLowerCase(Locale.ROOT)));
				}
			}
		} else {
			if (unsupported.contains(TableCapability.NO_LABEL)) {
				throwCapabilityError(learningOperator, TableCapability.NO_LABEL);
			}
		}
		checkLabelMissings(learningOperator, labelColumns, unsupported);
	}

	/**
	 * Checks the label column is numeric and numeric labels are supported.
	 */
	private void checkNumericLabel(Operator learningOperator, Set<TableCapability> unsupported) throws UserError {
		if (unsupported.contains(TableCapability.NUMERIC_LABEL)) {
			throwCapabilityError(learningOperator, TableCapability.NUMERIC_LABEL);
		}
	}

	/**
	 * Checks the label columns have missings in case that is not supported.
	 */
	private void checkLabelMissings(Operator learningOperator, List<Column> labelColumns,
									Set<TableCapability> unsupported) throws UserError {
		if (unsupported.contains(TableCapability.MISSINGS_IN_LABEL)) {
			for (Column labelColumn : labelColumns) {
				if (BeltTools.containsMissingValues(labelColumn)) {
					throwCapabilityError(learningOperator, TableCapability.MISSINGS_IN_LABEL);
				}
			}
		}
	}

	/*
	 * Checks if the nominal label column fits the capabilities.
	 */
	private void checkNominalLabel(Operator learningOperator, Set<TableCapability> unsupported,
								   Column nominalLabel) throws UserError {
		if (nominalLabel.getDictionary().size() == 1) {
			if (unsupported.contains((TableCapability.ONE_CLASS_LABEL))) {
				throw new UserError(learningOperator, "operator_capability.insufficient_capability", learningOperator.getName());
			}
		} else {
			if (nominalLabel.getDictionary().size() == 2) {
				if (unsupported.contains(TableCapability.TWO_CLASS_LABEL)
						&& unsupported.contains(TableCapability.NOMINAL_LABEL)) {
					throwCapabilityError(learningOperator, TableCapability.TWO_CLASS_LABEL);

				}
			} else {
				if (unsupported.contains(TableCapability.NOMINAL_LABEL)) {
					throwCapabilityError(learningOperator, TableCapability.NOMINAL_LABEL);
				}
			}
		}
	}

	/**
	 * Throws an error about insufficient capabilities.
	 */
	private void throwCapabilityError(Operator operator, TableCapability numericColumns) throws UserError {
		throw new UserError(operator, "operator_capability.insufficient_capability", operator.getName(),
				numericColumns.getDescription());
	}
}