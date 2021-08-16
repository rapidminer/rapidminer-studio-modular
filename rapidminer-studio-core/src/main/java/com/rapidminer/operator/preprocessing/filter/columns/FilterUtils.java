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

import static com.rapidminer.operator.preprocessing.filter.columns.TableSubsetSelectorFilter.SpecialFilterStrategy;

import java.util.function.Predicate;

import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.tools.belt.BeltMetaDataTools;
import com.rapidminer.tools.belt.BeltTools;


/**
 * Util methods used by the {@link TableSubsetSelectorFilter}s in this package.
 *
 * @author Kevin Majchrzak
 * @since 9.9.1
 */
enum FilterUtils {

	; // no instance enum

	/**
	 * Adds the default filters (invert and strategy) to the given base filter.
	 *
	 * @param metaData
	 * 		the meta data that should be filtered
	 * @param strategy
	 * 		the base filter
	 * @param invertFilter
	 *        {@code true} if the base filter should be inverted
	 * @param filter
	 * 		specified how special attributes should be handles (see {@link SpecialFilterStrategy})
	 * @return the base filter combined with the defaults filters
	 */
	static Predicate<String> addDefaultFilters(TableMetaData metaData, SpecialFilterStrategy strategy,
											   boolean invertFilter, Predicate<String> filter) {
		if (invertFilter) {
			filter = filter.negate();
		}
		if (strategy == SpecialFilterStrategy.KEEP) {
			Predicate<String> isSpecialColumn = columnName -> BeltMetaDataTools.isSpecial(metaData, columnName);
			filter = isSpecialColumn.or(filter);
		} else if (strategy == SpecialFilterStrategy.REMOVE) {
			Predicate<String> isNotSpecialColumn = columnName -> !BeltMetaDataTools.isSpecial(metaData, columnName);
			filter = isNotSpecialColumn.and(filter);
		}
		return filter;
	}

	/**
	 * Adds the default filters (invert and strategy) to the given base filter.
	 *
	 * @param table
	 * 		the table that should be filtered
	 * @param strategy
	 * 		the base filter
	 * @param invertFilter
	 *        {@code true} if the base filter should be inverted
	 * @param filter
	 * 		specified how special attributes should be handles (see {@link SpecialFilterStrategy})
	 * @return the base filter combined with the defaults filters
	 */
	static Predicate<String> addDefaultFilters(Table table, SpecialFilterStrategy strategy, boolean invertFilter,
											   Predicate<String> filter) {
		if (invertFilter) {
			filter = filter.negate();
		}
		if (strategy == SpecialFilterStrategy.KEEP) {
			Predicate<String> isSpecialColumn = columnName -> BeltTools.isSpecial(table, columnName);
			filter = isSpecialColumn.or(filter);
		} else if (strategy == SpecialFilterStrategy.REMOVE) {
			Predicate<String> isNotSpecialColumn = columnName -> !BeltTools.isSpecial(table, columnName);
			filter = isNotSpecialColumn.and(filter);
		}
		return filter;
	}

}
