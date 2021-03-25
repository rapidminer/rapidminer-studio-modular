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
import com.rapidminer.math.aggregation.AggregationManager;
import com.rapidminer.math.aggregation.manager.aggregator.NumericAggregator;


/**
 * Calculates a numeric aggregation using a {@link NumericAggregator}.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class NumericAggregationManager implements AggregationManager {

	private final String name;
	private final Supplier<NumericAggregator> supplier;
	private final boolean supportsIntegers;
	private Object[] parameterValue;

	private int rowIndex;
	private boolean integer = false;

	NumericAggregationManager(String name, Supplier<NumericAggregator> supplier, boolean supportsIntegers) {
		this.name = name;
		this.supplier = supplier;
		this.supportsIntegers = supportsIntegers;
	}

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		if (inputType.category() != Column.Category.NUMERIC) {
			return null;
		}
		if (supportsIntegers && inputType.id() == Column.TypeId.INTEGER_53_BIT) {
			return ColumnType.INTEGER_53_BIT;
		}
		return ColumnType.REAL;
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		if (column.type().category() != Column.Category.NUMERIC) {
			throw new IllegalArgumentException("Only numeric columns for numeric aggregation");
		}
		if (supportsIntegers && column.type().id() == Column.TypeId.INTEGER_53_BIT) {
			this.integer = true;
		}
		this.rowIndex = indexInRowReader;
	}

	@Override
	public void setAggregationParameter(Object... parameterValue) {
		this.parameterValue = parameterValue;
	}

	@Override
	public StandardNumericAggregationFunction newFunction() {
		NumericAggregator numericAggregator = supplier.get();
		if (numericAggregator != null && parameterValue != null) {
			numericAggregator.setAggregationParameter(parameterValue);
		}
		return new StandardNumericAggregationFunction(numericAggregator,
				rowIndex);
	}

	@Override
	public AggregationCollector getCollector(int numberOfRows) {
		return new NumericBufferAggregationCollector(numberOfRows, integer);
	}

	@Override
	public int getIndex() {
		return rowIndex;
	}

	@Override
	public String getAggregationName() {
		return name;
	}
}

