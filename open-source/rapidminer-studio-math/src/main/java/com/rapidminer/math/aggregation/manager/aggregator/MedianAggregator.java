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
package com.rapidminer.math.aggregation.manager.aggregator;

import java.util.Arrays;


/**
 * Calculates the median of numeric values.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public class MedianAggregator implements NumericAggregator {

	/**
	 * This class implements an array of primitive doubles and provides getter, adder and size
	 * methods. It is used by the {@link MedianAggregator} as a lightweight data structure.
	 */
	public static class VariableDoubleArray {

		private static final int INITIAL_ARRAY_SIZE = 64;

		private int size = 0;
		private double[] data;

		public VariableDoubleArray() {
			data = new double[INITIAL_ARRAY_SIZE];
		}

		public int size() {
			return size;
		}

		public double[] getArray() {
			return data;
		}

		public void add(double value) {
			if (data.length == size) {
				int newSize = size + (size >> 2);
				data = Arrays.copyOf(data, newSize);
			}
			data[size] = value;
			size++;
		}

		public void addAll(VariableDoubleArray other) {
			if (data.length < size + other.size) {
				int newSize = size + other.size;
				data = Arrays.copyOf(data, newSize);
			}
			System.arraycopy(other.data, 0, data, size, other.size);
			size +=other.size;
		}
	}


	private VariableDoubleArray values = null;
	private int count = 0;

	@Override
	public void accept(double value) {
		if (count == 0) {
			values = new VariableDoubleArray();
		}
		values.add(value);
		count++;
	}

	@Override
	public void merge(NumericAggregator other) {
		MedianAggregator medianOther = (MedianAggregator) other;
		if (values == null) {
			values = medianOther.values;
		} else if (medianOther.values != null) {
			values.addAll(medianOther.values);
		}
		count += medianOther.count;
	}

	@Override
	public double getValue() {
		if (count == 0) {
			return Double.NaN;
		}
		return quickNth(values, count / 2.0);
	}


	/**
	 * Implements a variation of quickSelect. Selects the value which contains the the nth weight.
	 * If n is the weight between two values, the middlepoint of these two values will be returned.
	 *
	 * @param values
	 *            The values as a {@link VariableDoubleArray}
	 * @param n
	 *            The nth value will be selected
	 * @return The nth value
	 * @since 9.9
	 */
	public static double quickNth(VariableDoubleArray values, double n) {
		// Choose pivot from the middle of the list
		double pivot = values.getArray()[values.size() / 2];

		// Split into smaller equal and greater list
		VariableDoubleArray smallerValues = new VariableDoubleArray();
		VariableDoubleArray greaterValues = new VariableDoubleArray();

		int equalCount = 0;

		for (int i = 0; i < values.size(); i++) {
			double currentElement = values.getArray()[i];
			if (currentElement < pivot) {
				smallerValues.add(currentElement);
			} else if (currentElement > pivot) {
				greaterValues.add(currentElement);
			} else {
				equalCount++;
			}
		}

		// Median between two different lists -> Median is midpoint of greatest value of smaller
		// list and smallest value of greater list
		if (smallerValues.size() == n) {
			double max = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < smallerValues.size(); i++) {
				if (smallerValues.getArray()[i] > max) {
					max = smallerValues.getArray()[i];
				}
			}
			return (pivot + max) / 2;
		} else if (smallerValues.size() + equalCount == n) {
			double min = Double.POSITIVE_INFINITY;
			for (int i = 0; i < greaterValues.size(); i++) {
				if (greaterValues.getArray()[i] < min) {
					min = greaterValues.getArray()[i];
				}
			}
			return (pivot + min) / 2;
		}

		// Check which of the three lists contains median and return it or adjust n
		else if (smallerValues.size() >= n) {
			return quickNth(smallerValues, n);
		} else if (smallerValues.size() + equalCount > n) {
			return pivot;
		} else {
			return quickNth(greaterValues, n - smallerValues.size() - equalCount);
		}
	}

	/**
	 * Implements a variation of quickSelect. Selects the value which contains the the nth weight.
	 * If n is the weight between two values, the middlepoint of these two values will be returned.
	 *
	 * @param values
	 *            The values as a {@link VariableDoubleArray}
	 * @param weights
	 *            The weights as a {@link VariableDoubleArray}
	 * @param n
	 *            The nth value will be selected
	 * @return The nth value
	 * @since 9.9
	 */
	public static double quickNthWeighted(VariableDoubleArray values, VariableDoubleArray weights, double n) {
		double pivot = values.getArray()[values.size() / 2];

		// Split into smaller equal and greater list
		VariableDoubleArray smallerValues = new VariableDoubleArray();
		VariableDoubleArray greaterValues = new VariableDoubleArray();
		VariableDoubleArray smallerWeights = new VariableDoubleArray();
		VariableDoubleArray greaterWeights = new VariableDoubleArray();

		double smallerWeightCount = 0;
		double equalWeightCount = 0;
		for (int i = 0; i < values.size(); i++) {
			double currentElement = values.getArray()[i];
			double currentWeight = weights.getArray()[i];
			if (currentElement < pivot) {
				smallerValues.add(currentElement);
				smallerWeights.add(currentWeight);
				smallerWeightCount += currentWeight;
			} else if (currentElement > pivot) {
				greaterValues.add(currentElement);
				greaterWeights.add(currentWeight);
			} else {
				equalWeightCount += currentWeight;
			}
		}

		// Median between two different lists -> Median is midpoint of greatest value of smaller
		// list and smallest value of greater list
		if (smallerWeightCount == n) {
			double max = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < smallerValues.size(); i++) {
				if (smallerValues.getArray()[i] > max) {
					max = smallerValues.getArray()[i];
				}
			}
			return (pivot + max) / 2;
		} else if (smallerWeightCount + equalWeightCount == n) {
			double min = Double.POSITIVE_INFINITY;
			for (int i = 0; i < greaterValues.size(); i++) {
				if (greaterValues.getArray()[i] < min) {
					min = greaterValues.getArray()[i];
				}
			}
			return (pivot + min) / 2;
		}

		// Check which of the three lists contains median and return it or adjust n
		else if (smallerWeightCount >= n) {
			return quickNthWeighted(smallerValues, smallerWeights, n);
		} else if (smallerWeightCount + equalWeightCount > n) {
			return pivot;
		} else {
			return quickNthWeighted(greaterValues, greaterWeights, n - smallerWeightCount - equalWeightCount);
		}
	}
}

