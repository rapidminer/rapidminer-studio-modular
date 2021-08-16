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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.parameter.MetaDataProvider;
import com.rapidminer.parameter.ParameterType;


/**
 * Column filter that keeps all columns.
 *
 * @author Kevin Majchrzak
 * @since 9.9.1
 */
public class AllColumnFilter implements TableSubsetSelectorFilter {

	/**
	 * Expects the {@link Operator} using the filter. It is used to access the user input needed to configure the filter
	 * and to access the Operators's {@link com.rapidminer.core.concurrency.ConcurrencyContext}.
	 *
	 * @param operator
	 * 		The Operator using the filter.
	 */
	public AllColumnFilter(Operator operator) {
	}

	@Override
	public Table filterTable(Table table, SpecialFilterStrategy filterSpecialColumns, boolean invertFilter) {
		return filterTableWithSettings(table, filterSpecialColumns, invertFilter);
	}

	@Override
	public TableMetaData filterMetaData(TableMetaData metaData, SpecialFilterStrategy filterSpecialColumns, boolean invertFilter) {
		return filterMetaDataWithSettings(metaData, filterSpecialColumns, invertFilter);
	}

	@Override
	public List<ParameterType> getParameterTypes(MetaDataProvider metaDataProvider) {
		return Collections.emptyList();
	}

	/**
	 * Keeps all columns (if the filter is not negated). See the parameter documentation below for further information.
	 *
	 * @param table
	 * 		the table to be filtered
	 * @param strategy
	 * 		describes how the filter should handle special columns. See {@link SpecialFilterStrategy}.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa).
	 * @return the filtered table
	 */
	public static Table filterTableWithSettings(Table table, SpecialFilterStrategy strategy, boolean invertFilter) {
		Predicate<String> filter = FilterUtils.addDefaultFilters(table, strategy, invertFilter, columnName -> true);
		return table.columns(table.labels().stream().filter(filter).collect(Collectors.toList()));
	}

	/**
	 * Keeps all columns (if the filter is not negated). See the parameter documentation below for further information.
	 *
	 * @param metaData
	 * 		the meta data to be filtered
	 * @param strategy
	 * 		describes how the filter should handle special columns. See {@link SpecialFilterStrategy}.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa).
	 * @return the filtered meta data
	 */
	public static TableMetaData filterMetaDataWithSettings(TableMetaData metaData, SpecialFilterStrategy strategy,
														   boolean invertFilter) {
		Predicate<String> filter = FilterUtils.addDefaultFilters(metaData, strategy, invertFilter, columnName -> true);
		return metaData.columns(metaData.labels().stream().filter(filter).collect(Collectors.toList()));
	}
}
