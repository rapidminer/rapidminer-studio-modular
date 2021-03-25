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
package com.rapidminer.math.aggregation;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;


/**
 * A manager for the aggregation done inside {@link com.rapidminer.extension.blending.operator.pivot.PivotOperator}
 * using a {@link AggregationTreeNode}.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public interface AggregationManager {

	/**
	 * Checks if the given input column type is compatible with this aggregation manager and if yes, returns the result
	 * type of the aggregation.
	 *
	 * @param inputType
	 * 		the type to check
	 * @return the result type of this aggregation with the given input type or {@code null} if the input type is not
	 * compatible with the aggregation
	 */
	ColumnType<?> checkColumnType(ColumnType<?> inputType);

	/**
	 * Initializes the manager. Use {@link #checkColumnType(ColumnType)} before calling this to check if initialization
	 * with this column type is possible. Called once before {@link #newFunction()}, {@link #getCollector(int)} or
	 * {@link #getIndex()} is called.
	 *
	 * @param column
	 * 		the column to aggregate
	 * @param indexInRowReader
	 * 		the index the column will have in the row reader
	 * @throws IllegalArgumentException
	 * 		in case the column type is not compatible with this manager, i.e., when
	 * 		{@link #checkColumnType(ColumnType)}
	 * 		returns {@code null}
	 */
	void initialize(Column column, int indexInRowReader);

	/**
	 * Sets optional aggregation parameter value(s). Some aggregation functions use it (e.g. the percentile aggregation), others
	 * don't.
	 *
	 * @param parameterValue the values, can be empty
	 * @since 9.9
	 */
	default void setAggregationParameter(Object... parameterValue) {
		// does nothing by default
	}

	/**
	 * @return a new aggregation function for the initialized manager
	 */
	AggregationFunction newFunction();

	/**
	 * Creates an aggregation collector for the given number of rows.
	 *
	 * @param numberOfRows
	 * 		the number of rows in the result column
	 * @return the aggregation collector
	 */
	AggregationCollector getCollector(int numberOfRows);

	/**
	 * Returns the index in the row reader this manager was initialized with.
	 *
	 * @return the index of the column which is aggregated by this manager in the row reader
	 */
	int getIndex();

	/**
	 * @return the name of the aggregation
	 */
	String getAggregationName();

}