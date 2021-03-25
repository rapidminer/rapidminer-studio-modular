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
package com.rapidminer.operator.preprocessing.transformation.aggregation;

import static com.rapidminer.math.aggregation.manager.aggregator.MedianAggregator.quickNth;
import static com.rapidminer.math.aggregation.manager.aggregator.MedianAggregator.quickNthWeighted;

import com.rapidminer.math.aggregation.manager.aggregator.MedianAggregator.VariableDoubleArray;


/**
 * This is an {@link Aggregator} for the {@link MeanAggregationFunction}. It uses a variation of the
 * quickselect algorithm for computing the median in an average time of O(n). In case the number of
 * unweighted elements is even or the midpoint of the weights lies between two elements, the
 * midpoint of the both middle values will be returned as the median. The memory consumption will
 * grow linearly with the size of the dataset.
 *
 *
 * @author Marcel Seifert
 * @since 7.5
 */
public class MedianAggregator extends NumericalAggregator {


	private VariableDoubleArray values = null;
	private VariableDoubleArray weights = null;
	private int count = 0;
	private double weightCount = 0;

	public MedianAggregator(AggregationFunction function) {
		super(function);
	}

	@Override
	public void count(double value) {
		if (count == 0) {
			values = new VariableDoubleArray();
		}

		values.add(value);

		count++;
	}

	@Override
	public void count(double value, double weight) {
		if (count == 0) {
			values = new VariableDoubleArray();
			weights = new VariableDoubleArray();
		}

		values.add(value);
		weights.add(weight);

		count++;
		weightCount += weight;
	}

	@Override
	public double getValue() {
		// The Median is NaN
		if (count == 0) {
			return Double.NaN;
		}

		if (weights == null) {
			return quickNth(values, count / 2.0);
		} else {
			return quickNthWeighted(values, weights, weightCount / 2.0);
		}

	}

}
