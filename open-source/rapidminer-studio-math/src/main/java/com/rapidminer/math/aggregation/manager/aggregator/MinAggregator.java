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
 * Calculates the min of numeric values.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public class MinAggregator implements NumericAggregator {

	private double min = Double.POSITIVE_INFINITY;

	@Override
	public void accept(double value) {
		if (value < min) {
			min = value;
		}
	}

	@Override
	public void merge(NumericAggregator other) {
		MinAggregator sumOther = (MinAggregator) other;
		if (sumOther.min < min) {
			min = sumOther.min;
		}
	}

	@Override
	public double getValue() {
		return min;
	}

}

