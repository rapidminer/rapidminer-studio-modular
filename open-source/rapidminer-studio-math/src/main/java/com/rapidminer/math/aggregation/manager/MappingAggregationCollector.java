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
package com.rapidminer.math.aggregation.manager;

import java.util.Arrays;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Columns;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;


/**
 * An {@link AggregationCollector} that collects mapping indices and then maps the input column.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class MappingAggregationCollector implements AggregationCollector {

	/**
	 * An aggregation function that returns a mapping index.
	 */
	interface MappingIndexAggregationFunction extends AggregationFunction {

		/**
		 * @return the entry for the mapping
		 */
		int getMappingIndex();
	}

	private final int[] mapping;
	private final Column column;

	MappingAggregationCollector(int numberOfRows, Column column) {
		this.column = column;
		this.mapping = new int[numberOfRows];
		Arrays.fill(mapping, -1);
	}

	@Override
	public void set(int index, AggregationFunction function) {
		mapping[index] = ((MappingIndexAggregationFunction) function).getMappingIndex();
	}

	@Override
	public Column getResult(Context context) {
		return Columns.removeUnusedDictionaryValues(column.rows(mapping, false), Columns.CleanupOption.COMPACT,
				context);
	}
}

