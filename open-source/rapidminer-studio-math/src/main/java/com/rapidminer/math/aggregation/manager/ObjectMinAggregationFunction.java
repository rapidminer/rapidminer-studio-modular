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

import java.util.Comparator;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.reader.MixedRow;
import com.rapidminer.belt.reader.NumericRow;
import com.rapidminer.math.aggregation.AggregationFunction;


/**
 * Finds the position of the minimal value in a column with respect to a comparator.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class ObjectMinAggregationFunction<T> implements MappingAggregationCollector.MappingIndexAggregationFunction {

	private final int rowIndex;
	private final Class<T> type;
	private final Comparator<T> comparator;
	private final Dictionary dictionary;

	private T min = null;
	private int mappingIndex = -1;

	ObjectMinAggregationFunction(ColumnType<T> columnType, Column column, int rowIndex) {
		this.type = columnType.elementType();
		this.comparator = Comparator.nullsLast(columnType.comparator());
		if (columnType.category() == Column.Category.CATEGORICAL) {
			this.dictionary = column.getDictionary();
		} else {
			this.dictionary = null;
		}
		this.rowIndex = rowIndex;
	}

	@Override
	public void accept(NumericRow row) {
		int index = (int) row.get(rowIndex);
		String value = dictionary.get(index);
		//if there is a dictionary, the type T must be String
		@SuppressWarnings("unchecked")
		T typedValue = (T) value;
		accept(typedValue, row.position());
	}

	@Override
	public void accept(MixedRow row) {
		T value = row.getObject(rowIndex, type);
		accept(value, row.position());
	}

	private void accept(T value, int rowPosition) {
		if (comparator.compare(min, value) > 0) {
			min = value;
			mappingIndex = rowPosition;
		}
	}

	@Override
	public void merge(AggregationFunction function) {
		ObjectMinAggregationFunction<T> other = (ObjectMinAggregationFunction<T>) function;
		accept(other.min, other.mappingIndex);
	}

	@Override
	public int getMappingIndex() {
		return mappingIndex;
	}
}

