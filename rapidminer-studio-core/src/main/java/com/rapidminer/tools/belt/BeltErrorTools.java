/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.tools.belt;

import java.util.List;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.ExecutionUtils;
import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;


/**
 * Provides some convenience methods for error handling when using the belt framework.
 *
 * @author Kevin Majchrzak
 * @since 9.8.0
 */
public final class BeltErrorTools {

	/**
	 * Disallow instances of this class.
	 */
	private BeltErrorTools() {
		throw new AssertionError("No com.rapidminer.tools.belt.BeltErrorTools instances for you!");
	}

	/**
	 * Throws an user error if any column in the given table is not nominal.
	 *
	 * @param table
	 * 		the table containing the columns to be checked
	 * @param origin
	 * 		this will be shown as the origin of the error
	 * @param operator
	 * 		the offending operator. Can be {@code null}
	 * @throws UserError
	 * 		with error id {@code non_nominal_attribute}
	 */
	public static void onlyNominal(Table table, String origin, Operator operator) throws UserError {
		List<String> labels = table.select().notOfTypeId(ColumnType.NOMINAL.id()).labels();
		if (!labels.isEmpty()) {
			throw new UserError(operator, "non_nominal_attribute", origin, labels.iterator().next());
		}
	}

	/**
	 * Throws an user error if any column in the given table is not numerical.
	 *
	 * @param table
	 * 		the table containing the columns to be checked
	 * @param origin
	 * 		this will be shown as the origin of the error
	 * @param operator
	 * 		the offending operator. Can be {@code null}
	 * @throws UserError
	 * 		with error id {@code non_numerical_attribute}
	 */
	public static void onlyNumeric(Table table, String origin, Operator operator) throws UserError {
		List<String> labels = table.select().notOfCategory(Column.Category.NUMERIC).labels();
		if (!labels.isEmpty()) {
			throw new UserError(operator, "non_numerical_attribute", origin, labels.iterator().next());
		}
	}

	/**
	 * Throws a user error if the table contains any missing values.
	 *
	 * @param table
	 * 		the {@link Table} to check
	 * @param origin
	 * 		will be shown as the origin of the error
	 * @param context
	 * 		belt context used for parallel computation
	 * @param operator
	 * 		the offending operator. Can be {@code null}.
	 * @throws UserError
	 * 		with error id {@code missing_values}
	 **/
	public static void onlyNonMissingValues(Table table, String origin, Context context, Operator operator) throws OperatorException {
		try {
			ExecutionUtils.parallel(0, table.width(), i -> {
				try {
					onlyNonMissingValues(table, origin, operator, i);
				} catch (OperatorException e) {
					// wrap cause
					throw new RuntimeException(e);
				}
			}, context);
		} catch (RuntimeException e) {
			Throwable cause = e.getCause();
			if (cause instanceof OperatorException) {
				// unwrap cause
				throw (OperatorException) cause;
			} else {
				throw e;
			}
		}
	}

	/**
	 * The data set is not allowed to contain infinite and, if indicated, missing values. If it does, a corresponding
	 * {@link UserError} is thrown.
	 *
	 * @param table
	 * 		the {@link Table} to check
	 * @param allowMissingValues
	 * 		indicates whether missing values are allowed
	 * @param origin
	 * 		will be shown as the origin of the error
	 * @param context
	 * 		belt context used for parallel computation
	 * @param operator
	 * 		the offending operator. Can be {@code null}
	 * @throws UserError
	 * 		with error id {@code infinite_values} if {@code allowMissingValues} is {@code true} and with error id {@code
	 * 		non_finite_values}, otherwise.
	 **/
	public static void onlyFiniteValues(Table table, boolean allowMissingValues, String origin, Context context,
										Operator operator) throws OperatorException {
		try {
			ExecutionUtils.parallel(0, table.width(), i -> {
				try {
					onlyFiniteValues(table, allowMissingValues, origin, operator, i);
				} catch (OperatorException e) {
					// wrap cause
					throw new RuntimeException(e);
				}
			}, context);
		} catch (RuntimeException e) {
			Throwable cause = e.getCause();
			if (cause instanceof OperatorException) {
				// unwrap cause
				throw (OperatorException) cause;
			} else {
				throw e;
			}
		}
	}

	/**
	 * Throws a UserError if the table holds no regular columns (columns without a role).
	 *
	 * @param table
	 * 		the table to check
	 * @param operator
	 * 		the offending operator. Can be {@code null}
	 * @throws UserError
	 * 		with error id {@code no_attributes}
	 */
	public static void hasRegularColumns(Table table, Operator operator) throws UserError {
		if (BeltTools.selectRegularColumns(table).labels().isEmpty()) {
			throw new UserError(operator, "no_attributes");
		}
	}

	/**
	 * Throws a UserError if the given table contains zero rows.
	 *
	 * @param table
	 * 		the table to check
	 * @param operator
	 * 		the offending operator. Can be {@code null}
	 * @throws UserError
	 * 		with error id {@code empty_exampleset}
	 */
	public static void nonEmpty(Table table, Operator operator) throws UserError {
		if (table.height() == 0) {
			throw new UserError(operator, "empty_exampleset");
		}
	}

	/**
	 * Helper method used in {@link #onlyFiniteValues(Table, boolean, String, Context, Operator)} that checks the i-th
	 * column of the given table for infinite values.
	 */
	private static void onlyFiniteValues(Table table, boolean allowMissingValues, String origin,
										 Operator operator, int i) throws OperatorException {
		if (operator != null) {
			operator.checkForStop();
		}
		Column column = table.column(i);
		if (allowMissingValues && BeltTools.containsInfiniteValues(column)) {
			throw new UserError(operator, "infinite_values", origin);
		} else if (!allowMissingValues && BeltTools.containsNonFiniteValues(column)) {
			throw new UserError(operator, "non_finite_values", origin);
		}
	}

	/**
	 * Helper method used in {@link #onlyNonMissingValues(Table, String, Context, Operator)} that checks the i-th column
	 * of the given table for missing values.
	 */
	private static void onlyNonMissingValues(Table table, String origin, Operator operator,
											 int i) throws OperatorException {
		if (operator != null) {
			operator.checkForStop();
		}
		Column column = table.column(i);
		if (BeltTools.containsMissingValues(column)) {
			throw new UserError(operator, "missing_values", origin);
		}
	}

}
