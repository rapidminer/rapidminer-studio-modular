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

import java.util.function.Supplier;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.AggregationManager;
import com.rapidminer.math.aggregation.manager.aggregator.NumericAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.SumAggregator;


/**
 * An {@link AggregationManager} that calculates the a fractional sum of groups.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class FractionalSumAggregationManager implements AggregationManager {

	private static final String NAME = "fractional_sum";

	private final Supplier<NumericAggregator> supplier = SumAggregator::new;

	private int rowIndex;

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		if (inputType.category() != Column.Category.NUMERIC) {
			return null;
		}
		return ColumnType.REAL;
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		if (column.type().category() != Column.Category.NUMERIC) {
			throw new IllegalArgumentException("Only numeric columns for numeric aggregation");
		}
		this.rowIndex = indexInRowReader;
	}

	@Override
	public AggregationFunction newFunction() {
		return new StandardNumericAggregationFunction(supplier.get(), rowIndex);
	}

	@Override
	public AggregationCollector getCollector(int numberOfRows) {
		return new FractionalNumericAggregationCollector(numberOfRows);
	}

	@Override
	public int getIndex() {
		return rowIndex;
	}

	@Override
	public String getAggregationName() {
		return NAME;
	}
}

