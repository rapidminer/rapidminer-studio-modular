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
package com.rapidminer.math.aggregation.manager.aggregator;

/**
 * A container for a numeric aggregation.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public interface NumericAggregator {

	/**
	 * Adds the value to the aggregation. This is only called for non-NaN values.
	 *
	 * @param value
	 * 		the value to add to the aggregation
	 */
	void accept(double value);

	/**
	 * Merges with another aggregator of the same kind.
	 *
	 * @param other
	 * 		the aggregator to merge with
	 */
	void merge(NumericAggregator other);

	/**
	 * @return the resulting double value
	 */
	double getValue();

	/**
	 * Sets optional aggregation parameter value(s). Some aggregation functions use it (e.g. the percentile aggregation), others
	 * don't.
	 *
	 * @param parameterValue the values, can be empty but must not be {@code null}
	 * @since 9.9
	 */
	default void setAggregationParameter(Object... parameterValue) {
		// does nothing by default
	}
}

