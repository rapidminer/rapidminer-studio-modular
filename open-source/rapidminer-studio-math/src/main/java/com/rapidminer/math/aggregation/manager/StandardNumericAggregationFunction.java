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

import com.rapidminer.belt.reader.MixedRow;
import com.rapidminer.belt.reader.NumericRow;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.manager.aggregator.NumericAggregator;


/**
 * The standard {@link NumericAggregationFunction} used with different {@link NumericAggregator}s. It only pipes
 * non-missing values to the underlying aggregator.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class StandardNumericAggregationFunction implements NumericAggregationFunction {

	private final int rowIndex;
	private final NumericAggregator aggregator;

	private boolean foundNonNan = false;

	StandardNumericAggregationFunction(NumericAggregator aggregator, int rowIndex) {
		this.aggregator = aggregator;
		this.rowIndex = rowIndex;
	}

	@Override
	public void accept(NumericRow row) {
		accept(row.get(rowIndex));
	}

	@Override
	public void accept(MixedRow row) {
		accept(row.getNumeric(rowIndex));
	}

	@Override
	public void accept(double value) {
		if (!Double.isNaN(value)) {
			aggregator.accept(value);
			foundNonNan = true;
		}
	}

	@Override
	public void merge(AggregationFunction function) {
		StandardNumericAggregationFunction other = (StandardNumericAggregationFunction) function;
		foundNonNan &= other.foundNonNan;
		aggregator.merge(other.aggregator);
	}

	@Override
	public double getValue() {
		if (foundNonNan) {
			return aggregator.getValue();
		} else {
			return Double.NaN;
		}
	}
}

