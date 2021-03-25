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

import com.rapidminer.belt.reader.MixedRow;
import com.rapidminer.belt.reader.NumericRow;


/**
 * An aggregation function that is applied to several rows. For every row either {@link #accept(MixedRow)} or {@link
 * #accept(NumericRow)} is called. When working in parallel, several aggregation managers can be {@link #merge}d into
 * one. {@link AggregationFunction}s are used in a {@link AggregationTreeNode}.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public interface AggregationFunction {

	/**
	 * Apply the aggregation function to the given row.
	 *
	 * @param row
	 * 		the row to consider
	 */
	void accept(NumericRow row);

	/**
	 * Apply the aggregation function to the given row.
	 *
	 * @param row
	 * 		the row to consider
	 */
	void accept(MixedRow row);

	/**
	 * Merges with another aggregation function of the same type.
	 *
	 * @param function
	 * 		the function to merge with
	 */
	void merge(AggregationFunction function);

}