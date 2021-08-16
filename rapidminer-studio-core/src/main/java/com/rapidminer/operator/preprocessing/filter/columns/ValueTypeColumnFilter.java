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
package com.rapidminer.operator.preprocessing.filter.columns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.rapidminer.belt.column.Column.TypeId;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.table.DictionaryInfo;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.parameter.MetaDataProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCheckBoxGroup;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * Column filter that keeps the columns of the selected value types.
 *
 * @author Kevin Majchrzak
 * @since 9.9.1
 */
public class ValueTypeColumnFilter implements TableSubsetSelectorFilter {

	public static final String PARAMETER_VALUE_TYPES = "type_of_value";

	private static final String NUMERIC_GROUP = "Numeric";
	public static final String TYPE_REAL = "real";
	public static final String TYPE_INTEGER = "integer";

	private static final String DATE_TIME_GROUP = "Time / Date";
	public static final String TYPE_DATE_TIME = "date-time";
	public static final String TYPE_TIME = "time";

	private static final String NOMINAL_GROUP = "Nominal";
	public static final String TYPE_BINOMINAL = "binominal";
	public static final String TYPE_NON_BINOMINAL = "non-binominal";

	/**
	 * The Operator using the filter.
	 */
	private Operator operator;

	/**
	 * Expects the {@link Operator} using the filter. It is used to access the user input needed to configure the filter
	 * and to access the Operators's {@link com.rapidminer.core.concurrency.ConcurrencyContext}.
	 *
	 * @param operator The Operator using the filter.
	 */
	public ValueTypeColumnFilter(Operator operator) {
		this.operator = operator;
	}

	@Override
	public Table filterTable(Table table, SpecialFilterStrategy strategy, boolean invertFilter) throws UserError {
		String valueTypeString = operator.getParameterAsString(PARAMETER_VALUE_TYPES);
		if (valueTypeString == null || valueTypeString.isEmpty()) {
			return filterTableWithSettings(table, strategy, invertFilter);
		}
		return filterTableWithSettings(table, strategy, invertFilter,
				valueTypeString.split(String.valueOf(ParameterTypeCheckBoxGroup.CHECKBOX_SEPARATOR)));

	}

	@Override
	public TableMetaData filterMetaData(TableMetaData metaData, SpecialFilterStrategy strategy, boolean invertFilter) {
		if (operator.isParameterSet(PARAMETER_VALUE_TYPES)) {
			String valueTypeString = null;
			try {
				valueTypeString = operator.getParameterAsString(PARAMETER_VALUE_TYPES);
			} catch (UndefinedParameterError undefinedParameterError) {
				// should never happen
			}
			if (valueTypeString != null && !valueTypeString.isEmpty()) {
				return filterMetaDataWithSettings(metaData, strategy, invertFilter,
						valueTypeString.split(String.valueOf(ParameterTypeCheckBoxGroup.CHECKBOX_SEPARATOR)));
			}
		}

		return filterMetaDataWithSettings(metaData, strategy, invertFilter);
	}

	@Override
	public List<ParameterType> getParameterTypes(MetaDataProvider metaDataProvider) {
		List<ParameterType> types = new ArrayList<>();
		ParameterTypeCheckBoxGroup type = new ParameterTypeCheckBoxGroup(PARAMETER_VALUE_TYPES,
				"Select the attribute types to keep.");
		type.add(NUMERIC_GROUP, TYPE_REAL);
		type.add(NUMERIC_GROUP, TYPE_INTEGER);
		type.add(DATE_TIME_GROUP, TYPE_DATE_TIME);
		type.add(DATE_TIME_GROUP, TYPE_TIME);
		type.add(NOMINAL_GROUP, TYPE_BINOMINAL);
		type.add(NOMINAL_GROUP, TYPE_NON_BINOMINAL);
		type.setExpert(false);
		type.setOptional(true);
		types.add(type);
		return types;
	}

	/**
	 * Keeps the columns with the specified value types and removes all other columns from the table.
	 *
	 * @param table
	 * 		the table to be filtered
	 * @param strategy
	 * 		describes how the filter should handle special columns. See {@link SpecialFilterStrategy}.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa)
	 * @param valueTypes
	 * 		the value types to keep. Possible values are {@link #TYPE_REAL}, {@link #TYPE_INTEGER}, {@link #TYPE_TIME},
	 *        {@link #TYPE_DATE_TIME}, {@link #TYPE_BINOMINAL} and {@link #TYPE_NON_BINOMINAL}.
	 * @return the filtered table
	 */
	public static Table filterTableWithSettings(Table table, SpecialFilterStrategy strategy, boolean invertFilter,
												String... valueTypes) {
		Predicate<String> filter;
		if (valueTypes == null || valueTypes.length == 0) {
			filter = columnName -> false;
		} else {
			final HashSet<String> selectedTypes = new HashSet<>(Arrays.asList(valueTypes));
			filter = columnName -> isOfSelectedType(table, columnName, selectedTypes);
		}
		filter = FilterUtils.addDefaultFilters(table, strategy, invertFilter, filter);
		return table.columns(table.labels().stream().filter(filter).collect(Collectors.toList()));
	}

	/**
	 * Keeps the columns with the specified value types and removes all other columns from the meta data.
	 *
	 * @param metaData
	 * 		the meta data to be filtered
	 * @param strategy
	 * 		describes how the filter should handle special columns. See {@link SpecialFilterStrategy}.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa)
	 * @param valueTypes
	 * 		the value types to keep. Possible values are {@link #TYPE_REAL}, {@link #TYPE_INTEGER}, {@link #TYPE_TIME},
	 *        {@link #TYPE_DATE_TIME}, {@link #TYPE_BINOMINAL} and {@link #TYPE_NON_BINOMINAL}.
	 * @return the filtered meta data
	 */
	public static TableMetaData filterMetaDataWithSettings(TableMetaData metaData, SpecialFilterStrategy strategy,
														   boolean invertFilter, String... valueTypes) {
		Predicate<String> filter;
		if (valueTypes == null || valueTypes.length == 0) {
			filter = columnName -> false;
		} else {
			final HashSet<String> selectedTypes = new HashSet<>(Arrays.asList(valueTypes));
			filter = columnName -> isOfSelectedType(metaData, columnName, selectedTypes);
		}
		filter = FilterUtils.addDefaultFilters(metaData, strategy, invertFilter, filter);
		return metaData.columns(metaData.labels().stream().filter(filter).collect(Collectors.toList()));
	}

	/**
	 * Returns true iff the given column is of one of the selected types.
	 */
	private static boolean isOfSelectedType(Table table, String columnName, Set<String> selectedTypes) {
		TypeId type = table.column(columnName).type().id();
		switch (type) {
			case REAL:
				return selectedTypes.contains(TYPE_REAL);
			case INTEGER_53_BIT:
				return selectedTypes.contains(TYPE_INTEGER);
			case TIME:
				return selectedTypes.contains(TYPE_TIME);
			case DATE_TIME:
				return selectedTypes.contains(TYPE_DATE_TIME);
			case NOMINAL:
				boolean isBinominal = table.column(columnName).getDictionary().isBoolean();
				return isBinominal && selectedTypes.contains(TYPE_BINOMINAL) ||
						!isBinominal && selectedTypes.contains(TYPE_NON_BINOMINAL);
			default:
				return false;
		}
	}

	/**
	 * Returns true iff the given column is of one of the selected types.
	 */
	private static boolean isOfSelectedType(TableMetaData metaData, String columnName, Set<String> selectedTypes) {
		Optional<ColumnType<?>> type = metaData.column(columnName).getType();
		if (type.isPresent()) {
			switch (type.get().id()) {
				case REAL:
					return selectedTypes.contains(TYPE_REAL);
				case INTEGER_53_BIT:
					return selectedTypes.contains(TYPE_INTEGER);
				case TIME:
					return selectedTypes.contains(TYPE_TIME);
				case DATE_TIME:
					return selectedTypes.contains(TYPE_DATE_TIME);
				case NOMINAL:
					DictionaryInfo dictionary = metaData.column(columnName).getDictionary();
					boolean isBinominal = dictionary != null && dictionary.isBoolean();
					return isBinominal && selectedTypes.contains(TYPE_BINOMINAL) ||
							!isBinominal && selectedTypes.contains(TYPE_NON_BINOMINAL);
				default:
					return false;
			}
		}
		return false;
	}
}
