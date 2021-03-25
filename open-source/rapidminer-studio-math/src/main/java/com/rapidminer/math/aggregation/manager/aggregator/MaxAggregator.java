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
 * Calculates the max of numeric values.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public class MaxAggregator implements NumericAggregator {

	private double max = Double.NEGATIVE_INFINITY;

	@Override
	public void accept(double value) {
		if (value > max) {
			max = value;
		}
	}

	@Override
	public void merge(NumericAggregator other) {
		MaxAggregator sumOther = (MaxAggregator) other;
		if (sumOther.max > max) {
			max = sumOther.max;
		}
	}

	@Override
	public double getValue() {
		return max;
	}

}

