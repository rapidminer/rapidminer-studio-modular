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
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.parameter.MetaDataProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


/**
 * Column filter that keeps a single column.
 *
 * @author Kevin Majchrzak
 * @since 9.9.1
 */
public class SingleColumnFilter implements TableSubsetSelectorFilter {

	public static final String PARAMETER_ATTRIBUTE = "select_attribute";

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
	public SingleColumnFilter(Operator operator) {
		this.operator = operator;
	}

	@Override
	public Table filterTable(Table table, SpecialFilterStrategy strategy, boolean invertFilter) throws UserError {
		String columnName = operator.getParameterAsString(PARAMETER_ATTRIBUTE);
		return filterTableWithSettings(table, strategy, invertFilter, columnName);
	}

	@Override
	public TableMetaData filterMetaData(TableMetaData metaData, SpecialFilterStrategy strategy, boolean invertFilter) {
		String columnName = null;
		if (operator.isParameterSet(PARAMETER_ATTRIBUTE)) {
			try {
				columnName = operator.getParameterAsString(PARAMETER_ATTRIBUTE);
			} catch (UndefinedParameterError undefinedParameterError) {
				// should never happen
			}
		}
		return filterMetaDataWithSettings(metaData, strategy, invertFilter, columnName);
	}

	@Override
	public List<ParameterType> getParameterTypes(MetaDataProvider metaDataProvider) {
		List<ParameterType> types = new ArrayList<>();
		ParameterType type = new ParameterTypeAttribute(PARAMETER_ATTRIBUTE, "The attribute which should be selected.",
				metaDataProvider, false, Ontology.ATTRIBUTE_VALUE);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	/**
	 * Keeps the column with the given column name and filters out the rest.
	 *
	 * @param table
	 * 		the table to be filtered
	 * @param strategy
	 * 		describes how the filter should handle special columns. See {@link SpecialFilterStrategy}.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa).
	 * @param columnName
	 * 		the column that should be kept
	 * @return the filtered table
	 */
	public static Table filterTableWithSettings(Table table, SpecialFilterStrategy strategy, boolean invertFilter,
												String columnName) {
		Predicate<String> filter;
		if (columnName == null || columnName.isEmpty()) {
			filter = x -> false;
		} else {
			filter = columnName::equals;
		}
		filter = FilterUtils.addDefaultFilters(table, strategy, invertFilter, filter);
		return table.columns(table.labels().stream().filter(filter).collect(Collectors.toList()));
	}

	/**
	 * Keeps the column with the given column name and filters out the rest.
	 *
	 * @param metaData
	 * 		the meta data to be filtered
	 * @param strategy
	 * 		describes how the filter should handle special columns. See {@link SpecialFilterStrategy}.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa).
	 * @param columnName
	 * 		the column that should be kept
	 * @return the filtered meta data
	 */
	public static TableMetaData filterMetaDataWithSettings(TableMetaData metaData, SpecialFilterStrategy strategy,
														   boolean invertFilter, String columnName) {
		Predicate<String> filter;
		if (columnName == null || columnName.isEmpty()) {
			filter = x -> false;
		} else {
			filter = columnName::equals;
		}
		filter = FilterUtils.addDefaultFilters(metaData, strategy, invertFilter, filter);
		return metaData.columns(metaData.labels().stream().filter(filter).collect(Collectors.toList()));
	}
}
