/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.math.aggregation.manager.aggregator;

import java.util.Map;


/**
 * Calculates the least appearing value of numeric values.
 *
 * @author Gisa Meier
 * @since 9.9
 */
public class LeastAggregator extends NumericAppearanceAggregator {

	@Override
	public double getValue() {
		int min = Integer.MAX_VALUE;
		double minValue = 0;
		for (Map.Entry<Double, Integer> entry : frequencies.entrySet()) {
			int count = entry.getValue();
			//to break ties consistently take the one with the smallest value
			if (count < min || (count == min && entry.getKey() < minValue)) {
				min = count;
				minValue = entry.getKey();
			}
		}
		return minValue;
	}
}

