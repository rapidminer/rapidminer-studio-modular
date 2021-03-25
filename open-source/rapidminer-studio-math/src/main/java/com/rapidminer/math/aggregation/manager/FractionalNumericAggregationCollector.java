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

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;


/**
 * An {@link AggregationCollector} that collects numeric values and then divides by their sum.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class FractionalNumericAggregationCollector implements AggregationCollector {

	private final NumericBuffer buffer;
	private double totalSum;

	FractionalNumericAggregationCollector(int numberOfRows) {
		buffer = Buffers.realBuffer(numberOfRows);
	}

	@Override
	public void set(int index, AggregationFunction function) {
		double value = ((NumericAggregationFunction) function).getValue();
		if (!Double.isNaN(value)) {
			if (value < 0) {
				totalSum = Double.NaN;
			} else {
				totalSum += value;
			}
		}
		buffer.set(index, value);
	}

	@Override
	public Column getResult(Context context) {
		for (int i = 0; i < buffer.size(); i++) {
			buffer.set(i, buffer.get(i) / totalSum);
		}
		return buffer.toColumn();
	}
}

