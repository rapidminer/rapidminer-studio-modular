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
 * An {@link AggregationManager} to compute the mode or least of a categorical column.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class CategoricalAppearanceAggregationManager implements AggregationManager {

	enum Mode {
		MOST, LEAST;
	}

	private static final String NAME_MOST = AggregationManagers.FUNCTION_NAME_MODE;
	private static final String NAME_LEAST = AggregationManagers.FUNCTION_NAME_LEAST;

	private final Mode mode;

	private Column column;
	private int dictionarySize;
	private int rowIndex;

	CategoricalAppearanceAggregationManager(Mode mode) {
		this.mode = mode;
	}

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		if (inputType.category() != Column.Category.CATEGORICAL) {
			return null;
		} else {
			return inputType;
		}
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		if (column.type().category() != Column.Category.CATEGORICAL) {
			throw new IllegalArgumentException("Only categorical columns for mode/least");
		}
		this.column = column;
		this.dictionarySize = column.getDictionary().maximalIndex() + 1;
		this.rowIndex = indexInRowReader;
	}

	@Override
	public CategoricalModeLeastAggregationFunction newFunction() {
		return new CategoricalModeLeastAggregationFunction();
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
		return mode == Mode.LEAST ? NAME_LEAST : NAME_MOST;
	}

	/**
	 * Calculates the categorical mode for all accepted rows.
	 */
	class CategoricalModeLeastAggregationFunction
			implements MappingAggregationCollector.MappingIndexAggregationFunction {

		//counts how often a category index appears
		private final int[] appearingCounter = new int[dictionarySize];

		//stores the row a category index appears in
		private final int[] appearingIndex = new int[dictionarySize];

		@Override
		public void accept(NumericRow row) {
			int index = (int) row.get(rowIndex);
			appearingCounter[index]++;
			appearingIndex[index] = row.position();
		}

		@Override
		public void accept(MixedRow row) {
			int index = row.getIndex(rowIndex);
			appearingCounter[index]++;
			appearingIndex[index] = row.position();
		}

		@Override
		public void merge(AggregationFunction function) {
			CategoricalModeLeastAggregationFunction other = (CategoricalModeLeastAggregationFunction) function;
			for (int i = 0; i < appearingCounter.length; i++) {
				if (appearingCounter[i] == 0) {
					appearingIndex[i] = other.appearingIndex[i];
				}
				appearingCounter[i] += other.appearingCounter[i];
			}
		}

		@Override
		public int getMappingIndex() {
			if (mode == Mode.LEAST) {
				return least();
			}
			return most();
		}

		private int least() {
			int arrayIndex = -1;
			int minCount = Integer.MAX_VALUE;
			// find the min non-missing that appears at least once, else return missing
			for (int i = 1; i < appearingCounter.length; i++) {
				if (appearingCounter[i] < minCount && appearingCounter[i] > 0) {
					minCount = appearingCounter[i];
					arrayIndex = appearingIndex[i];
				}
			}
			return arrayIndex;
		}

		private int most() {
			int arrayIndex = -1;
			int maxCount = 0;
			// find the max non-missing, else return missing
			for (int i = 1; i < appearingCounter.length; i++) {
				if (appearingCounter[i] > maxCount) {
					maxCount = appearingCounter[i];
					arrayIndex = appearingIndex[i];
				}
			}
			return arrayIndex;
		}
	}
}
