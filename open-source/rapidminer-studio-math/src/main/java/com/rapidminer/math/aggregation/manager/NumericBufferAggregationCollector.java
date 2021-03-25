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
 * An aggregation collector that collects numeric values, either as integers or as reals.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class NumericBufferAggregationCollector implements AggregationCollector {

	private final NumericBuffer buffer;

	NumericBufferAggregationCollector(int numberOfRows, boolean integer) {
		buffer = integer ? Buffers.integer53BitBuffer(numberOfRows) : Buffers.realBuffer(numberOfRows);
	}

	@Override
	public void set(int index, AggregationFunction function) {
		buffer.set(index, ((NumericAggregationFunction) function).getValue());
	}

	@Override
	public Column getResult(Context context) {
		return buffer.toColumn();
	}
}

