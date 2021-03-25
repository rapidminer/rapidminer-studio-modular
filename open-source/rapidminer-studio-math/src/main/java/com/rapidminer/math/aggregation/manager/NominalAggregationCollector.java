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
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;


/**
 * An {@link AggregationCollector} that collects String values and creates a new nominal column.
 *
 * @author Gisa Meier
 * @since 9.7
 */
class NominalAggregationCollector implements AggregationCollector {

	/**
	 * An aggregation function that returns a String value.
	 */
	interface NominalAggregationFunction extends AggregationFunction {

		/**
		 * @return the String to add
		 */
		String getResult();
	}

	private final NominalBuffer buffer;
	private final Dictionary dictionary;

	/**
	 * @param numberOfRows
	 * 		the number of rows in the new column to build
	 * @param dictionary
	 * 		the old dictionary to get boolean information from, can be {@code null}
	 */
	NominalAggregationCollector(int numberOfRows, Dictionary dictionary) {
		buffer = Buffers.nominalBuffer(numberOfRows);
		this.dictionary = dictionary;
	}

	@Override
	public void set(int index, AggregationFunction function) {
		buffer.set(index, ((NominalAggregationFunction) function).getResult());
	}

	@Override
	public Column getResult(Context context) {
		if (dictionary != null && dictionary.isBoolean()) {
			//keep positive value
			String positiveValue = dictionary.get(dictionary.getPositiveIndex());
			if (buffer.differentValues() == dictionary.size()
					|| buffer.differentValues() == 1 && buffer.size() > 0 && buffer.get(0).equals(positiveValue)) {
				return buffer.toBooleanColumn(positiveValue);
			} else {
				return buffer.toBooleanColumn(null);
			}
		}
		return buffer.toColumn();
	}
}

