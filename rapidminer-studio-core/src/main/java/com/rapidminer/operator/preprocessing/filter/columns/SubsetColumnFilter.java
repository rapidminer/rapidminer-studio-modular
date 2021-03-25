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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.parameter.MetaDataProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributeSubset;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.belt.BeltMetaDataTools;
import com.rapidminer.tools.belt.BeltTools;


/**
 * Column filter that keeps the selected subset of columns of the given table.
 *
 * @author Kevin Majchrzak
 * @since 9.9.0
 */
public class SubsetColumnFilter implements TableSubsetSelectorFilter {

	public static final String PARAMETER_SELECT_SUBSET = "select_subset";

	/**
	 * The Operator using the filter.
	 */
	private Operator operator;

	/**
	 * Expects the {@link Operator} using the filter. It is used to access the user input needed to configure the filter
	 * and to access the Operators's {@link com.rapidminer.core.concurrency.ConcurrencyContext}.
	 *
	 * @param operator
	 * 		The Operator using the filter.
	 */
	public SubsetColumnFilter(Operator operator) {
		this.operator = operator;
	}

	@Override
	public Table filterTable(Table table, boolean filterSpecialColumns, boolean invertFilter) throws UserError {
		Set<String> columnNames = subsetStringToSet(operator.getParameterAsString(PARAMETER_SELECT_SUBSET));
		return filterTableWithSettings(table, filterSpecialColumns, invertFilter, columnNames);
	}

	@Override
	public TableMetaData filterMetaData(TableMetaData metaData, boolean filterSpecialColumns, boolean invertFilter) {
		Set<String> columnNames = null;
		if(operator.isParameterSet(PARAMETER_SELECT_SUBSET)) {
			try {
				columnNames = subsetStringToSet(operator.getParameterAsString(PARAMETER_SELECT_SUBSET));
			} catch (UndefinedParameterError undefinedParameterError) {
				// should never happen
			}
		}
		return filterMetaDataWithSettings(metaData, filterSpecialColumns, invertFilter, columnNames);
	}

	@Override
	public List<ParameterType> getParameterTypes(MetaDataProvider metaDataProvider) {
		List<ParameterType> types = new ArrayList<>();
		ParameterType type = new ParameterTypeAttributeSubset(PARAMETER_SELECT_SUBSET,
				"Click to select the attribute subset.", metaDataProvider, false,
				Ontology.ATTRIBUTE_VALUE);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	/**
	 * Keeps the columns with the given column names and filters out the rest.
	 *
	 * @param table
	 * 		the table to be filtered
	 * @param filterSpecialColumns
	 * 		If this is {@code true}, then the special columns (columns with a role) are also filtered. Otherwise special
	 * 		columns are always kept.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa) or empty it
	 * 		accepts all columns.
	 * @param columnNames
	 * 		the names of the columns that should be kept.
	 * @return the filtered table
	 */
	public static Table filterTableWithSettings(Table table, boolean filterSpecialColumns, boolean invertFilter, Set<String> columnNames) {
		Predicate<String> filter;
		if (columnNames == null || columnNames.isEmpty()) {
			filter = columnName -> false;
		} else {
			filter = columnNames::contains;
		}
		if (invertFilter) {
			filter = filter.negate();
		}
		if (!filterSpecialColumns) {
			Predicate<String> isSpecialColumn = columnName -> BeltTools.isSpecial(table, columnName);
			filter = isSpecialColumn.or(filter);
		}
		return table.columns(table.labels().stream().filter(filter).collect(Collectors.toList()));
	}

	/**
	 * Keeps the columns with the given column names and filters out the rest.
	 *
	 * @param metaData
	 * 		the meta data to be filtered
	 * @param filterSpecialColumns
	 * 		If this is {@code true}, then the special columns (columns with a role) are also filtered. Otherwise special
	 * 		columns are always kept.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa) or empty it
	 * 		accepts all columns.
	 * @param columnNames
	 * 		the names of the columns that should be kept.
	 * @return the filtered meta data
	 */
	public static TableMetaData filterMetaDataWithSettings(TableMetaData metaData, boolean filterSpecialColumns,
														   boolean invertFilter, Set<String> columnNames) {
		Predicate<String> filter;
		if (columnNames == null || columnNames.isEmpty()) {
			filter = columnName -> false;
		} else {
			filter = columnNames::contains;
		}
		if (invertFilter) {
			filter = filter.negate();
		}
		if (!filterSpecialColumns) {
			Predicate<String> isSpecialColumn = columnName -> BeltMetaDataTools.isSpecial(metaData, columnName);
			filter = isSpecialColumn.or(filter);
		}
		return metaData.columns(metaData.labels().stream().filter(filter).collect(Collectors.toList()));
	}

	/**
	 * Takes the string representation of the user-specified attribute names and converts it to a set of string.
	 */
	private static Set<String> subsetStringToSet(String parameterAsString) {
		if (parameterAsString == null) {
			return Collections.emptySet();
		}
		String[] attributeNames = parameterAsString
				.split(String.valueOf(ParameterTypeAttributeSubset.ATTRIBUTE_SEPARATOR_CHARACTER));
		if (attributeNames.length == 0) {
			return Collections.emptySet();
		} else {
			return new HashSet<>(Arrays.asList(attributeNames));
		}
	}
}
