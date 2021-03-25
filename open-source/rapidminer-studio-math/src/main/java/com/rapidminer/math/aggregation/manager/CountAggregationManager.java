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

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.reader.MixedRow;
import com.rapidminer.belt.reader.NumericRow;
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.AggregationManager;


/**
 * An {@link AggregationManager} to count the number of values in a group. It can include or ignore missing values and
 * the result can be given as count, fractional count or percentage.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class CountAggregationManager implements AggregationManager {

	enum Mode {
		NORMAL, FRACTIONAL, PERCENTAGE;
	}

	private final boolean ignoreMissings;
	private final Mode mode;

	private int rowIndex;
	private boolean notNumericReadable;

	CountAggregationManager(boolean ignoreMissings, Mode mode) {
		this.ignoreMissings = ignoreMissings;
		this.mode = mode;
	}

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		if (mode == Mode.NORMAL) {
			return ColumnType.INTEGER_53_BIT;
		} else {
			return ColumnType.REAL;
		}
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		this.rowIndex = indexInRowReader;
		this.notNumericReadable = !column.type().hasCapability(Column.Capability.NUMERIC_READABLE);
	}

	@Override
	public AggregationFunction newFunction() {
		return new CountAggregationFunction();
	}

	@Override
	public AggregationCollector getCollector(int numberOfRows) {
		switch (mode) {
			case FRACTIONAL:
				return new FractionalNumericAggregationCollector(numberOfRows);
			case PERCENTAGE:
				return new PercentageNumericAggregationCollector(numberOfRows);
			case NORMAL:
			default:
				return new NumericBufferAggregationCollector(numberOfRows, true);
		}
	}

	@Override
	public int getIndex() {
		return rowIndex;
	}

	@Override
	public String getAggregationName() {
		String name;
		switch (mode) {
			case FRACTIONAL:
				name = "fractional_count";
				break;
			case PERCENTAGE:
				name = "percentage_count";
				break;
			case NORMAL:
			default:
				name = AggregationManagers.FUNCTION_NAME_COUNT;
				break;

		}
		if (!ignoreMissings) {
			name += "_with_missings";
		}
		return name;
	}

	/**
	 * Counts the accepted values, either ignoring missings or not.
	 */
	class CountAggregationFunction implements NumericAggregationFunction {

		private int count;

		@Override
		public void accept(NumericRow row) {
			if (ignoreMissings) {
				if (!Double.isNaN(row.get(rowIndex))) {
					count++;
				}
			} else {
				count++;
			}
		}

		@Override
		public void accept(MixedRow row) {
			if (ignoreMissings) {
				if (notNumericReadable) {
					if (row.getObject(rowIndex) != null) {
						count++;
					}
				} else {
					if (!Double.isNaN(row.getNumeric(rowIndex))) {
						count++;
					}
				}
			} else {
				count++;
			}
		}

		@Override
		public void accept(double value) {
			if (ignoreMissings) {
				if (!Double.isNaN(value)) {
					count++;
				}
			} else {
				count++;
			}
		}

		@Override
		public void merge(AggregationFunction function) {
			CountAggregationFunction other = (CountAggregationFunction) function;
			count += other.count;
		}


		@Override
		public double getValue() {
			return count;
		}
	}

}
