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

import java.util.List;

import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.tools.TableSubsetSelector;
import com.rapidminer.parameter.MetaDataProvider;
import com.rapidminer.parameter.ParameterType;


/**
 * Interface for filters that filter columns from a Belt table. Primarily used by the {@link TableSubsetSelector}.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
public interface TableSubsetSelectorFilter {

	/**
	 * Takes a table and returns a filtered table.
	 *
	 * @param table                the table that will be filtered
	 * @param filterSpecialColumns If this is {@code true}, then the special columns (columns with a role) are also filtered.
	 *                             Otherwise special columns are always kept.
	 * @param invertFilter         inverts the result of the filter (keeps the columns that would usually be removed and
	 *                             vice versa)
	 * @return the filtered table
	 * @throws UserError if the user input and / or the table is incompatible with the filter
	 */
	Table filterTable(Table table, boolean filterSpecialColumns, boolean invertFilter) throws UserError;

	/**
	 * Takes belt metadata and returns filtered belt metadata.
	 *
	 * @param metaData
	 * 		the metadata that will be filtered
	 * @param filterSpecialColumns
	 * 		If this is true, then the special columns (columns with a role) are also filtered. Otherwise special columns
	 * 		are always kept.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa)
	 * @return the filtered metadata
	 */
	TableMetaData filterMetaData(TableMetaData metaData, boolean filterSpecialColumns, boolean invertFilter);

	/**
	 * Returns parameter types that should be exposed to the user. The filter will automatically make use of these
	 * parameter types in the methods {@link #filterTable(Table, boolean, boolean)} and {@link
	 * #filterMetaData(TableMetaData, boolean, boolean)} to configure itself.
	 *
	 * @param metaDataProvider A {@link MetaDataProvider} holding metadata of the table that will be filtered. This will
	 *                         be used e.g. to add suggestions for the user to the parameter types.
	 * @return the parameter types needed to configure this filter
	 */
	List<ParameterType> getParameterTypes(MetaDataProvider metaDataProvider);

}
