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

import java.util.StringJoiner;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.reader.MixedRow;
import com.rapidminer.belt.reader.NumericRow;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.AggregationManager;


/**
 * An {@link AggregationManager} to concatenate nominal values.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class ConcatAggregationManager implements AggregationManager {

	private static final String NAME = "concat";
	private static final String DELIMITER = "|";
	private int rowIndex;
	private Dictionary dictionary;

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		if (inputType.id() != Column.TypeId.NOMINAL) {
			return null;
		}else{
			return inputType;
		}
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		if (column.type().id() != Column.TypeId.NOMINAL) {
			throw new IllegalArgumentException("Only nominal columns for concatenation");
		}
		this.dictionary = column.getDictionary();
		this.rowIndex = indexInRowReader;
	}

	@Override
	public ConcatenationAggregationFunction newFunction() {
		return new ConcatenationAggregationFunction();
	}

	@Override
	public NominalAggregationCollector getCollector(int numberOfRows) {
		return new NominalAggregationCollector(numberOfRows, null);
	}

	@Override
	public int getIndex() {
		return rowIndex;
	}

	@Override
	public String getAggregationName() {
		return NAME;
	}

	/**
	 * Joins the String values at the given nominal column of the rows together.
	 */
	class ConcatenationAggregationFunction implements NominalAggregationCollector.NominalAggregationFunction {

		private StringJoiner joiner = new StringJoiner(DELIMITER);
		private boolean added = false;

		@Override
		public void accept(NumericRow row) {
			int index = (int) row.get(rowIndex);
			String value = dictionary.get(index);
			if(value != null){
				joiner.add(value);
				added = true;
			}
		}

		@Override
		public void accept(MixedRow row) {
			String value = row.getObject(rowIndex, String.class);
			if(value != null){
				joiner.add(value);
				added = true;
			}
		}

		@Override
		public void merge(AggregationFunction function) {
			ConcatenationAggregationFunction other = (ConcatenationAggregationFunction) function;
			joiner.merge(other.joiner);
		}

		@Override
		public String getResult(){
			if (added) {
				return joiner.toString();
			} else {
				return null;
			}
		}
	}
}
