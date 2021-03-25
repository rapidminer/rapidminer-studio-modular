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
 * Calculates the variance of numeric values.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public class VarianceAggregator implements NumericAggregator {

	private double valueSum = 0d;
	private double squaredValueSum = 0d;
	private double count = 0;

	@Override
	public void accept(double value) {
		valueSum += value;
		squaredValueSum += value * value;
		count++;
	}

	@Override
	public void merge(NumericAggregator other) {
		VarianceAggregator varOther = (VarianceAggregator) other;
		valueSum += varOther.valueSum;
		squaredValueSum += varOther.squaredValueSum;
		count += varOther.count;
	}

	@Override
	public double getValue() {
		if (count > 0) {
			return variance(squaredValueSum, valueSum, count);
		} else {
			return Double.NaN;
		}
	}

	/**
	 * Calculates the variance from the input parameters.
	 *
	 * @param squaredValueSum
	 * 		the sum of the squares of the values
	 * @param valueSum
	 * 		the sum of the values
	 * @param count
	 * 		the number of values
	 * @return the variance
	 */
	static double variance(double squaredValueSum, double valueSum, double count) {
		return (squaredValueSum - valueSum * valueSum / count) / ((count - 1) / count * count);
	}

}

