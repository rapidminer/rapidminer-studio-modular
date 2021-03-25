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

import org.apache.commons.math3.util.OpenIntToDoubleHashMap;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.reader.CategoricalReader;
import com.rapidminer.belt.reader.MixedRow;
import com.rapidminer.belt.reader.NumericRow;
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.AggregationManager;


/**
 * An {@link AggregationManager} to compute the mode or least of a nominal column.
 *
 * @author Gisa Meier, Jan Czogalla
 * @since 9.7
 */
class NominalAppearanceAggregationManager implements AggregationManager {

	enum Mode {
		MOST, LEAST;
	}

	private static final String NAME_MOST = AggregationManagers.FUNCTION_NAME_MODE;
	private static final String NAME_LEAST = AggregationManagers.FUNCTION_NAME_LEAST;

	private final Mode mode;

	private Dictionary dictionary;
	private int dictionarySize;
	private int rowIndex;

	NominalAppearanceAggregationManager(Mode mode) {
		this.mode = mode;
	}

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		if (inputType.id() != Column.TypeId.NOMINAL) {
			return null;
		} else {
			return inputType;
		}
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		if (column.type().id() != Column.TypeId.NOMINAL) {
			throw new IllegalArgumentException("Only nominal columns for mode/least");
		}
		this.dictionary = column.getDictionary();
		this.dictionarySize = column.getDictionary().maximalIndex() + 1;
		this.rowIndex = indexInRowReader;
	}

	@Override
	public NominalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction newFunction() {
		return new NominalAppearanceAggregationManager.CategoricalModeLeastAggregationFunction();
	}

	@Override
	public AggregationCollector getCollector(int numberOfRows) {
		return new NominalAggregationCollector(numberOfRows, dictionary);
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
			implements NominalAggregationCollector.NominalAggregationFunction {

		// all fields kept package-private for tests
		// counts how often a category index appears
		int[] appearingCounter;

		// stores count and row index sparsely
		OpenIntToDoubleHashMap appearingMap;

		CategoricalModeLeastAggregationFunction() {
			if (dictionarySize > AggregationManagers.MAX_MAPPING_SIZE) {
				appearingMap = new OpenIntToDoubleHashMap();
				appearingCounter = null;
			} else {
				appearingCounter = new int[dictionarySize];
				appearingMap = null;
			}
		}

		@Override
		public void accept(NumericRow row) {
			accept((int) row.get(rowIndex));
		}

		@Override
		public void accept(MixedRow row) {
			accept(row.getIndex(rowIndex));
		}

		@Override
		public void merge(AggregationFunction function) {
			CategoricalModeLeastAggregationFunction other = (CategoricalModeLeastAggregationFunction) function;
			if (other.appearingCounter != null) {
				for (int i = 0; i < other.appearingCounter.length; i++) {
					update(i, other.appearingCounter[i]);
				}
			} else {
				for (OpenIntToDoubleHashMap.Iterator iterator = other.appearingMap.iterator(); iterator.hasNext(); ) {
					iterator.advance();
					update(iterator.key(), (int) iterator.value());
				}
			}
		}

		@Override
		public String getResult() {
			if (mode == Mode.LEAST) {
				return least();
			}
			return most();
		}

		private String least() {
			int resultIndex = -1;
			int minCount = Integer.MAX_VALUE;
			// find the min non-missing that appears at least once, else return missing
			if (appearingCounter != null) {
				for (int i = 1; i < appearingCounter.length; i++) {
					if (appearingCounter[i] < minCount && appearingCounter[i] > 0) {
						minCount = appearingCounter[i];
						resultIndex = i;
						if (minCount == 1) {
							//cannot get smaller than this
							break;
						}
					}
				}
			} else if (appearingMap.size() > 0) {
				int minIndex = -1;
				// no zero entries
				for (OpenIntToDoubleHashMap.Iterator iterator = appearingMap.iterator(); iterator.hasNext(); ) {
					iterator.advance();
					int valueIndex = iterator.key();
					int occurrence = (int) iterator.value();
					if (occurrence < minCount || occurrence == minCount && valueIndex < minIndex) {
						minCount = occurrence;
						minIndex = valueIndex;
					}
				}
				resultIndex = minIndex;
			}
			return dictionary.get(resultIndex);
		}

		private String most() {
			int resultIndex = -1;
			int maxCount = 0;
			// find the max non-missing, else return missing
			if (appearingCounter != null) {
				for (int i = 1; i < appearingCounter.length; i++) {
					if (appearingCounter[i] > maxCount) {
						maxCount = appearingCounter[i];
						resultIndex = i;
					}
				}
			} else if (appearingMap.size() > 0) {
				int maxIndex = -1;
				for (OpenIntToDoubleHashMap.Iterator iterator = appearingMap.iterator(); iterator.hasNext(); ) {
					iterator.advance();
					int valueIndex = iterator.key();
					int occurrence = (int) iterator.value();
					if (occurrence > maxCount || occurrence == maxCount && valueIndex < maxIndex) {
						maxCount = occurrence;
						maxIndex = valueIndex;
					}
				}
				resultIndex = maxIndex;
			}
			return dictionary.get(resultIndex);
		}

		/**
		 * Accept an index and row position
		 */
		private void accept(int index) {
			//don't count missings
			if (index != CategoricalReader.MISSING_CATEGORY) {
				if (appearingCounter != null) {
					appearingCounter[index]++;
				} else {
					double current = appearingMap.get(index);
					boolean isNew = Double.isNaN(current);
					if (isNew) {
						appearingMap.put(index, 1);
						checkMapFillStatus();
					} else {
						appearingMap.put(index, current + 1);
					}
				}
			}
		}


		/**
		 * Update the given index with the provided occurrence. Will ignore {@code 0} occurrences and if the
		 * occurrence represents a zero count.
		 */
		private void update(int index, int occurrence) {
			if (occurrence == 0 || index == CategoricalReader.MISSING_CATEGORY) {
				return;
			}
			if (appearingCounter != null) {
				appearingCounter[index] += occurrence;
			} else {
				double left = appearingMap.get(index);
				if (Double.isNaN(left)) {
					appearingMap.put(index, occurrence);
					checkMapFillStatus();
				} else {
					appearingMap.put(index, left + occurrence);
				}
			}
		}

		/**
		 * Checks if the {@link #appearingMap} is getting too big (threshold: map's size is a third of {@link
		 * #dictionarySize}). If the map is too big, it will be replaced with an array again.
		 * <p>
		 * <strong>Note:</strong> The {@link #appearingMap} must not be {@code null} when this is called!
		 */
		private void checkMapFillStatus() {
			if (appearingMap.size() * AggregationManagers.MAP_FILL_RATIO > dictionarySize) {
				appearingCounter = new int[dictionarySize];
				OpenIntToDoubleHashMap.Iterator iterator = appearingMap.iterator();
				while (iterator.hasNext()) {
					iterator.advance();
					appearingCounter[iterator.key()] = (int) iterator.value();
				}
				appearingMap = null;
			}
		}

	}
}
