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
package com.rapidminer.math.aggregation;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;


/**
 * An aggregation collector converts several {@link AggregationFunction}s coming from the same {@link
 * AggregationManager} into one {@link Column}.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public interface AggregationCollector {

	/**
	 * Sets the result of the function at the given row index.
	 *
	 * @param index
	 * 		the row index in the new column
	 * @param function
	 * 		the function containing the result
	 */
	void set(int index, AggregationFunction function);

	/**
	 * Returns the result column.
	 *
	 * @param context
	 * 		the context to use to create the column, this parameter might be removed as soon as not every table
	 * 		operation,
	 * 		even non-parallel ones, requires a context
	 * @return the result of the aggregations
	 */
	Column getResult(Context context);
}


