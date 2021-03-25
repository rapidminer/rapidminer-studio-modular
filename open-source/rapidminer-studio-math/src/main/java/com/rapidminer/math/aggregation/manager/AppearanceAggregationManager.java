/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.math.aggregation.manager;

import static com.rapidminer.math.aggregation.manager.AggregationManagers.FUNCTION_NAME_LEAST;
import static com.rapidminer.math.aggregation.manager.AggregationManagers.FUNCTION_NAME_MODE;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.AggregationManager;
import com.rapidminer.math.aggregation.manager.aggregator.LeastAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.ModeAggregator;


/**
 * Wrapper aggregation manager for {@link NumericAggregationManager} and {@link NominalAppearanceAggregationManager}.
 *
 * @author Gisa Meier
 * @since 9.9
 */
class AppearanceAggregationManager implements AggregationManager {

	private Column.Category category;

	private final NumericAggregationManager numericAggregationManager;
	private final NominalAppearanceAggregationManager nominalAggregationManager;

	AppearanceAggregationManager(NominalAppearanceAggregationManager.Mode mode) {
		nominalAggregationManager = new NominalAppearanceAggregationManager(mode);
		numericAggregationManager = new NumericAggregationManager(
				mode == NominalAppearanceAggregationManager.Mode.MOST ? FUNCTION_NAME_MODE : FUNCTION_NAME_LEAST,
				mode == NominalAppearanceAggregationManager.Mode.MOST ? ModeAggregator::new :
						LeastAggregator::new, true);
	}

	@Override
	public ColumnType<?> checkColumnType(ColumnType<?> inputType) {
		if (inputType.category() == Column.Category.NUMERIC) {
			return numericAggregationManager.checkColumnType(inputType);
		} else {
			return nominalAggregationManager.checkColumnType(inputType);
		}
	}

	@Override
	public void initialize(Column column, int indexInRowReader) {
		category = column.type().category();
		if (category == Column.Category.NUMERIC) {
			numericAggregationManager.initialize(column, indexInRowReader);
		} else if (category == Column.Category.CATEGORICAL) {
			nominalAggregationManager.initialize(column, indexInRowReader);
		} else {
			throw new IllegalArgumentException("Only numeric and nominal columns for mode/least");
		}
	}

	@Override
	public AggregationFunction newFunction() {
		if (category == Column.Category.NUMERIC) {
			return numericAggregationManager.newFunction();
		} else {
			return nominalAggregationManager.newFunction();
		}
	}

	@Override
	public AggregationCollector getCollector(int numberOfRows) {
		if (category == Column.Category.NUMERIC) {
			return numericAggregationManager.getCollector(numberOfRows);
		} else {
			return nominalAggregationManager.getCollector(numberOfRows);
		}
	}

	@Override
	public int getIndex() {
		if (category == Column.Category.NUMERIC) {
			return numericAggregationManager.getIndex();
		} else {
			return nominalAggregationManager.getIndex();
		}
	}

	@Override
	public String getAggregationName() {
		if (category == Column.Category.NUMERIC) {
			return numericAggregationManager.getAggregationName();
		} else {
			return nominalAggregationManager.getAggregationName();
		}
	}


}
