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
package com.rapidminer.tools.belt;

import java.util.List;
import java.util.Map;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.reader.CategoricalReader;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.ColumnSelector;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.studio.internal.Resources;


/**
 * Provides some convenience methods for using the belt framework.
 *
 * @author Kevin Majchrzak
 * @since 9.7.0
 */
public final class BeltTools {

	/**
	 * Disallow instances of this class.
	 */
	private BeltTools() {
		throw new AssertionError("No com.rapidminer.tools.belt.BeltTools instances for you!");
	}

	/**
	 * Returns the subtable of the given table holing all columns without a {@link ColumnRole}).
	 *
	 * @param table
	 * 		the given table
	 * @return a {@link Table} holding exactly the columns without a role of the given table
	 * @since 9.8.0
	 */
	public static Table regularSubtable(Table table) {
		return table.columns(table.select().withoutMetaData(ColumnRole.class).labels());
	}

	/**
	 * Returns a column selector containing the regular columns of the given table (columns without a {@link
	 * ColumnRole}).
	 *
	 * @param table
	 * 		the given table
	 * @return a {@link ColumnSelector}
	 */
	public static ColumnSelector selectRegularColumns(Table table) {
		return table.select().withoutMetaData(ColumnRole.class);
	}

	/**
	 * Returns a column selector containing the special columns of the given table (columns with a {@link ColumnRole}).
	 *
	 * @param table
	 * 		the given table
	 * @return a {@link ColumnSelector}
	 */
	public static ColumnSelector selectSpecialColumns(Table table) {
		return table.select().withMetaData(ColumnRole.class);
	}

	/**
	 * Return {@code true} iff the given label is the label of a special column (column with a {@link ColumnRole}) in
	 * the given table.
	 *
	 * @param table
	 * 		the table that contains the column
	 * @param label
	 * 		the column name
	 * @return {@code true} iff the column is a special column
	 */
	public static boolean isSpecial(Table table, String label) {
		return table.getFirstMetaData(label, ColumnRole.class) != null;
	}

	/**
	 * Throws an user error if any column in the given table is not nominal.
	 *
	 * @param table
	 * 		the table containing the columns to be checked
	 * @param origin
	 * 		this will be shown as the origin of the error
	 * @throws UserError
	 * 		with error code {@code 103} if any column in the given table is not nominal
	 * @deprecated relocated, please use {@link BeltErrorTools#onlyNominal(Table, String, Operator)} instead
	 */
	@Deprecated
	public static void onlyNominal(Table table, String origin) throws UserError {
		List<String> labels = table.select().notOfTypeId(ColumnType.NOMINAL.id()).labels();
		if (!labels.isEmpty()) {
			throw new UserError(null, 103, origin, labels.iterator().next());
		}
	}

	/**
	 * Returns whether the type is an advanced column type, i.e. not part of the standard set of column types.
	 *
	 * @param type
	 * 		the column type
	 * @return {@code true} iff given type is advanced
	 */
	public static boolean isAdvanced(ColumnType<?> type) {
		return !BeltConverter.STANDARD_TYPES.contains(type.id());
	}

	/**
	 * Returns whether the table contains advanced columns.
	 *
	 * @param table
	 * 		the table to check
	 * @return {@code true} iff the table contains a column that is advanced
	 * @see #isAdvanced(ColumnType)
	 */
	public static boolean hasAdvanced(Table table) {
		for (int i = 0; i < table.width(); i++) {
			if (isAdvanced(table.column(i).type())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Creates a {@link Context} for the given operator. Calling this method is equivalent to calling {@code
	 * ContextAdapter.adapt(Resources.getConcurrencyContext(operator))}.
	 *
	 * @param operator
	 * 		will be used to create a {@link ConcurrencyContext} and then wrap it into a {@link Context}.
	 * @return the generated {@link Context}
	 * @since 9.8.0
	 */
	public static Context getContext(Operator operator) {
		return ContextAdapter.adapt(Resources.getConcurrencyContext(operator));
	}

	/**
	 * Returns true iff the given table holds at least one column with the given column type.
	 *
	 * @param table
	 * 		the table to check
	 * @param type
	 * 		the column type to check for
	 * @return true iff the table holds the given column type
	 * @since 9.8.0
	 */
	public static boolean containsColumnType(Table table, Column.TypeId type) {
		return !table.select().ofTypeId(type).labels().isEmpty();
	}

	/**
	 * Return {@code true} iff the given column contains missing values.
	 *
	 * @param column
	 * 		the column that should be checked
	 * @return {@code true} iff the column contains missing values
	 * @since 9.8.0
	 */
	public static boolean containsMissingValues(Column column) {
		if (column.type().category() == Column.Category.CATEGORICAL) {
			CategoricalReader reader = Readers.categoricalReader(column);
			for (int i = 0; i < column.size(); i++) {
				if (reader.read() == CategoricalReader.MISSING_CATEGORY) {
					// found a missing value
					return true;
				}
			}
		} else if (column.type().hasCapability(Column.Capability.NUMERIC_READABLE)) {
			NumericReader reader = Readers.numericReader(column);
			while (reader.hasRemaining()) {
				if (Double.isNaN(reader.read())) {
					// found a missing value
					return true;
				}
			}
		} else {
			ObjectReader<Object> reader = Readers.objectReader(column, Object.class);
			while (reader.hasRemaining()) {
				if (reader.read() == null) {
					// found a missing value
					return true;
				}
			}
		}
		// scanned all lines and did not find a missing value
		return false;
	}

	/**
	 * Returns {@code true} iff the given column contains infinite values (according to {@link
	 * Double#isInfinite(double)}). If the column is not {@link Column.Category#NUMERIC} this method returns {@code
	 * false}. Please note, {@link Double#NaN} is neither a finite nor an infinite value. To also check for {@code NaN}
	 * use {@link #containsNonFiniteValues(Column)}.
	 *
	 * @param column
	 * 		the column that should be checked
	 * @return {@code true} iff the column is numeric and contains infinite values
	 * @since 9.8.0
	 */
	public static boolean containsInfiniteValues(Column column) {
		if (column.type().category() == Column.Category.NUMERIC) {
			NumericReader reader = Readers.numericReader(column);
			while (reader.hasRemaining()) {
				if (Double.isInfinite(reader.read())) {
					// found an infinite value
					return true;
				}
			}
		}
		// not numeric or no infinite value found
		return false;
	}

	/**
	 * Returns {@code true} iff the given column contains non-finite values (according to {@link
	 * Double#isFinite(double)}). If the column is not {@link Column.Category#NUMERIC} this method returns {@code
	 * false}. Please note, {@link Double#NaN} is neither a finite nor an infinite value. Therefore, this method will
	 * also return true if the given column contains missing values.
	 *
	 * @param column
	 * 		the column that should be checked
	 * @return {@code true} iff the column is numeric and contains infinite or {@link Double#NaN} values
	 * @since 9.8.0
	 */
	public static boolean containsNonFiniteValues(Column column) {
		if (column.type().category() == Column.Category.NUMERIC) {
			NumericReader reader = Readers.numericReader(column);
			while (reader.hasRemaining()) {
				if (!Double.isFinite(reader.read())) {
					// found a non-finite value
					return true;
				}
			}
		}
		// not numeric or no non-finite value found
		return false;
	}

	/**
	 * Finds the regular columns in the table that are incompatible with regular columns of the schema according to the
	 * further parameters. If {@link UserError}s in case of incompatibilities are required, use {@link
	 * BeltErrorTools#requireCompatibleRegulars} instead.
	 *
	 * @param table
	 * 		the table to check
	 * @param schema
	 * 		the table to compare to
	 * @param columnSetRequirement
	 * 		the column set requirement to check
	 * @param typeRequirements
	 * 		the type requirements to check, can be empty for no type check
	 * @return a map from incompatible column name to reason for incompatibility
	 * @since 9.9
	 */
	public static Map<String, Tables.Incompatibility> findIncompatibleRegulars(Table table, Table schema,
																			   Tables.ColumnSetRequirement columnSetRequirement,
																			   Tables.TypeRequirement... typeRequirements){
		return Tables.findIncompatible(regularSubtable(table), regularSubtable(schema), columnSetRequirement, typeRequirements);
	}
}
