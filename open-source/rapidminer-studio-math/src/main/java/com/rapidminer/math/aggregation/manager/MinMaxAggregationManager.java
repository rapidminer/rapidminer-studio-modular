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
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.AggregationManager;
import com.rapidminer.math.aggregation.manager.aggregator.MaxAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.MinAggregator;


/**
 * An {@link AggregationManager} that allows to calculate the min/max of a group in a numeric or sortable
 * object-readable column.
 *
 * @author Gisa Meier
 * @since 9.1
 */
class MinMaxAggregationManager implements AggregationManager {

	enum Mode {
		MIN, MAX;
	}

	private final Mode mode;

	MinMaxAggregationManager(
			Mode mode) {
		this.mode = mode;
	}

	private int rowIndex;
	private Column column;
	private boolean asSortableObjects;

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		//integer, real or sortable
		if (inputType.id() == Column.TypeId.INTEGER_53_BIT || inputType.id() == Column.TypeId.REAL ||
				((inputType.category() == Column.Category.CATEGORICAL || inputType
						.category() == Column.Category.OBJECT) && inputType.comparator() != null)) {
			return inputType;
		}
		return null;
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		this.rowIndex = indexInRowReader;
		this.column = column;
		if (column.type().hasCapability(Column.Capability.OBJECT_READABLE) && column.type().comparator() != null) {
			asSortableObjects = true;
		}
		if (!asSortableObjects && column.type().id() != Column.TypeId.INTEGER_53_BIT && column.type()
				.id() != Column.TypeId.REAL) {
			throw new IllegalArgumentException("Min and max not defined for this type of column");
		}
	}

	@Override
	public AggregationFunction newFunction() {
		if (asSortableObjects) {
			return mode == Mode.MAX ? new ObjectMaxAggregationFunction<>(column.type(), column, rowIndex) :
					new ObjectMinAggregationFunction<>(column.type(), column, rowIndex);
		} else {
			return new StandardNumericAggregationFunction(mode == Mode.MAX ? new MaxAggregator() : new MinAggregator(),
					rowIndex);
		}
	}

	@Override
	public AggregationCollector getCollector(int numberOfRows) {
		if (asSortableObjects) {
			return new MappingAggregationCollector(numberOfRows, column);
		} else {
			return new NumericBufferAggregationCollector(numberOfRows,
					column.type().id() == Column.TypeId.INTEGER_53_BIT);
		}
	}

	@Override
	public int getIndex() {
		return rowIndex;
	}

	@Override
	public String getAggregationName() {
		return mode == Mode.MAX ? AggregationManagers.FUNCTION_NAME_MAXIMUM : AggregationManagers.FUNCTION_NAME_MINIMUM;
	}
}

