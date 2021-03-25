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
 * An {@link AggregationManager} with a function that always takes the first appearing element.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class FirstAggregationManager implements AggregationManager {

	static final String NAME = "first";
	private int rowIndex;
	private Column column;
	private boolean notNumericReadable;

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		return inputType;
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		this.rowIndex = indexInRowReader;
		this.column = column;
		this.notNumericReadable = !column.type().hasCapability(Column.Capability.NUMERIC_READABLE);
	}

	@Override
	public FirstAggregationFunction newFunction() {
		return new FirstAggregationFunction();
	}

	/**
	 * An aggregation function that stores the first row index it encounters.
	 */
	class FirstAggregationFunction implements MappingAggregationCollector.MappingIndexAggregationFunction {

		private int mappingIndex = -1;

		@Override
		public void accept(NumericRow row) {
			if (mappingIndex < 0 && !Double.isNaN(row.get(rowIndex))) {
				mappingIndex = row.position();
			}
		}

		@Override
		public void accept(MixedRow row) {
			if (mappingIndex < 0 && isNotMissing(row)) {
				mappingIndex = row.position();
			}
		}

		private boolean isNotMissing(MixedRow row) {
			if (notNumericReadable) {
				return row.getObject(rowIndex) != null;
			} else {
				return !Double.isNaN(row.getNumeric(rowIndex));
			}
		}

		@Override
		public void merge(AggregationFunction function) {
			FirstAggregationFunction other = (FirstAggregationFunction) function;
			if (mappingIndex < 0) {
				mappingIndex = other.mappingIndex;
			}
		}

		@Override
		public int getMappingIndex() {
			return mappingIndex;
		}
	}

	@Override
	public AggregationCollector getCollector(int numberOfRows) {
		return new MappingAggregationCollector(numberOfRows, column);
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

