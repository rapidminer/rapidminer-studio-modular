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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.rapidminer.math.aggregation.AggregationCollector;
import com.rapidminer.math.aggregation.AggregationFunction;
import com.rapidminer.math.aggregation.AggregationManager;
import com.rapidminer.math.aggregation.AggregationTreeNode;
import com.rapidminer.math.aggregation.manager.aggregator.AverageAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.LeastAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.LogProductAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.MedianAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.ModeAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.PercentileAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.ProductAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.StandardDeviationAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.SumAggregator;
import com.rapidminer.math.aggregation.manager.aggregator.VarianceAggregator;


/**
 * Enum that handles {@link AggregationManager}s that are used for aggregations.
 * The {@link AggregationFunction}s and the {@link
 * AggregationCollector} of the {@link AggregationManager} are used by the
 * {@link AggregationTreeNode}.
 *
 * @author Gisa Meier
 * @since 9.1
 */
public enum AggregationManagers {

	INSTANCE;

	/**
	 * Constants for the aggregation function names. They reuse the constants of the Aggregate operator whenever
	 * possible to ensure consistent naming.
	 */
	public static final String FUNCTION_NAME_AVERAGE = "average";
	public static final String FUNCTION_NAME_SUM = "sum";
	public static final String FUNCTION_NAME_MEDIAN = "median";
	public static final String FUNCTION_NAME_VARIANCE = "variance";
	public static final String FUNCTION_NAME_STANDARD_DEVIATION = "standard deviation";
	public static final String FUNCTION_NAME_FRACTIONAL_SUM = "sum (fractional)";
	public static final String FUNCTION_NAME_PRODUCT = "product";
	public static final String FUNCTION_NAME_LOG_PRODUCT = "log product";
	public static final String FUNCTION_NAME_MODE = "mode";
	public static final String FUNCTION_NAME_LEAST = "least";
	public static final String FUNCTION_NAME_CONCATENATION = "concatenation";
	public static final String FUNCTION_NAME_MINIMUM = "minimum";
	public static final String FUNCTION_NAME_MAXIMUM = "maximum";
	public static final String FUNCTION_NAME_FIRST = FirstAggregationManager.NAME;
	public static final String FUNCTION_NAME_COUNT = "count";
	public static final String FUNCTION_NAME_COUNT_INCLUDING_MISSINGS = "count (including missings)";
	public static final String FUNCTION_NAME_COUNT_FRACTIONAL = "count (fractional)";
	public static final String FUNCTION_NAME_COUNT_PERCENTAGE = "count (percentage)";
	/**
	 * The percentile function, takes the optional percentile parameter (a double between (0, 100]). If not specified, defaults to 75.
	 *
	 * @since 9.9
	 */
	public static final String FUNCTION_NAME_PERCENTILE = "percentile";

	/**
	 * Indicates when to use a map instead of full array for nominal mapping counting
	 *
	 * @since 9.7
	 */
	public static final int MAX_MAPPING_SIZE = 1000;

	/**
	 * Fill ratio of {@link #MAX_MAPPING_SIZE} maps. When a map get's too big, switch back to full array.
	 *
	 * @since 9.7
	 */
	public static final int MAP_FILL_RATIO = 5;


	private final Map<String, Supplier<AggregationManager>> aggregationManagerMap;
	private final Map<String, Supplier<AggregationManager>> newAggregationManagerMap;
	private final Map<String, Supplier<AggregationManager>> includingOnlyDirectlyAccessibleNumericAggregationManagerMap;

	AggregationManagers() {
		Map<String, Supplier<AggregationManager>> tempAggregationManagerMap = new TreeMap<>();

		//numeric
		tempAggregationManagerMap
				.put(FUNCTION_NAME_AVERAGE, () -> new NumericAggregationManager(FUNCTION_NAME_AVERAGE,
						AverageAggregator::new, false));
		tempAggregationManagerMap.put(FUNCTION_NAME_SUM,
				() -> new NumericAggregationManager(FUNCTION_NAME_SUM, SumAggregator::new, true));
		tempAggregationManagerMap
				.put(FUNCTION_NAME_MEDIAN, () -> new NumericAggregationManager(FUNCTION_NAME_MEDIAN,
						MedianAggregator::new, false));
		tempAggregationManagerMap
				.put(FUNCTION_NAME_VARIANCE, () -> new NumericAggregationManager(FUNCTION_NAME_VARIANCE,
						VarianceAggregator::new, false));
		tempAggregationManagerMap.put(FUNCTION_NAME_STANDARD_DEVIATION,
				() -> new NumericAggregationManager("standard_deviation", StandardDeviationAggregator::new, false));
		tempAggregationManagerMap.put(FUNCTION_NAME_FRACTIONAL_SUM, FractionalSumAggregationManager::new);
		tempAggregationManagerMap
				.put(FUNCTION_NAME_PRODUCT, () -> new NumericAggregationManager(FUNCTION_NAME_PRODUCT,
						ProductAggregator::new, false));
		tempAggregationManagerMap
				.put(FUNCTION_NAME_LOG_PRODUCT,
						() -> new NumericAggregationManager("log_product", LogProductAggregator::new, false));

		// categorical
		tempAggregationManagerMap.put(FUNCTION_NAME_MODE,
				() -> new CategoricalAppearanceAggregationManager(CategoricalAppearanceAggregationManager.Mode.MOST));
		tempAggregationManagerMap.put(FUNCTION_NAME_LEAST,
				() -> new CategoricalAppearanceAggregationManager(CategoricalAppearanceAggregationManager.Mode.LEAST));

		// nominal
		tempAggregationManagerMap.put(FUNCTION_NAME_CONCATENATION, ConcatAggregationManager::new);

		//all numeric or sortable object readable
		tempAggregationManagerMap.put(FUNCTION_NAME_MINIMUM,
				() -> new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MIN));
		tempAggregationManagerMap.put(FUNCTION_NAME_MAXIMUM,
				() -> new MinMaxAggregationManager(MinMaxAggregationManager.Mode.MAX));

		//all
		tempAggregationManagerMap.put(FUNCTION_NAME_FIRST, FirstAggregationManager::new);
		tempAggregationManagerMap
				.put(FUNCTION_NAME_COUNT, () -> new CountAggregationManager(true,
						CountAggregationManager.Mode.NORMAL));
		tempAggregationManagerMap.put(FUNCTION_NAME_COUNT_INCLUDING_MISSINGS,
				() -> new CountAggregationManager(false, CountAggregationManager.Mode.NORMAL));
		tempAggregationManagerMap.put(FUNCTION_NAME_COUNT_FRACTIONAL,
				() -> new CountAggregationManager(true, CountAggregationManager.Mode.FRACTIONAL));
		tempAggregationManagerMap.put(FUNCTION_NAME_COUNT_PERCENTAGE,
				() -> new CountAggregationManager(true, CountAggregationManager.Mode.PERCENTAGE));

		this.aggregationManagerMap = Collections.unmodifiableMap(tempAggregationManagerMap);

		//use the new mode/least aggregations
		Map<String, Supplier<AggregationManager>> newTempMap = new TreeMap<>(tempAggregationManagerMap);
		newTempMap.put(FUNCTION_NAME_MODE,
				() -> new AppearanceAggregationManager(NominalAppearanceAggregationManager.Mode.MOST));
		newTempMap.put(FUNCTION_NAME_LEAST,
				() -> new AppearanceAggregationManager(NominalAppearanceAggregationManager.Mode.LEAST));
		this.newAggregationManagerMap = Collections.unmodifiableMap(newTempMap);

		//add hidden aggregations that are only accessible via the direct function access
		Map<String, Supplier<AggregationManager>> hiddenTempMap = new TreeMap<>(newAggregationManagerMap);
		hiddenTempMap.put(FUNCTION_NAME_PERCENTILE,
				() -> new NumericAggregationManager(FUNCTION_NAME_PERCENTILE, PercentileAggregator::new, false));
		//overwrite numeric appearance managers so that their functions are directly accessible
		hiddenTempMap.put(FUNCTION_NAME_MODE,
				() -> new NumericAggregationManager(FUNCTION_NAME_MODE, ModeAggregator::new, true));
		hiddenTempMap.put(FUNCTION_NAME_LEAST,
				() -> new NumericAggregationManager(FUNCTION_NAME_LEAST, LeastAggregator::new, true));
		this.includingOnlyDirectlyAccessibleNumericAggregationManagerMap = Collections.unmodifiableMap(hiddenTempMap);
	}

	/**
	 * Returns a map from name to managers which was used up until version 9.6.
	 *
	 * @return a map from aggregation function name to aggregation managers
	 */
	public Map<String, Supplier<AggregationManager>> getLegacyAggregationManagers() {
		return aggregationManagerMap;
	}

	/**
	 * Returns a map from name to managers which is used since version 9.7.
	 *
	 * @return a map from aggregation function name to aggregation managers
	 */
	public Map<String, Supplier<AggregationManager>> getAggregationManagers() {
		return newAggregationManagerMap;
	}

	/**
	 * @return the set of possible aggregation function names
	 */
	public Set<String> getAggregationFunctionNames() {
		return aggregationManagerMap.keySet();
	}

	/**
	 * Get an {@link NumericAggregationFunction} for the given function name.
	 *
	 * @param functionName   the function name, see class constants
	 * @param parameterValue optional parameter values, which may be used by the function. Check the function name
	 *                       JavaDoc for reference for each function
	 * @return the function instance or {@code null} if the function is unknown or not numeric
	 * @since 9.9
	 */
	public NumericAggregationFunction getNumericAggregationFunction(String functionName, Object... parameterValue) {
		AggregationManager aggregationManager = includingOnlyDirectlyAccessibleNumericAggregationManagerMap.get(functionName).get();
		aggregationManager.setAggregationParameter(parameterValue);

		AggregationFunction aggregationFunction = aggregationManager.newFunction();
		if (aggregationFunction instanceof NumericAggregationFunction) {
			return (NumericAggregationFunction) aggregationFunction;
		} else {
			return null;
		}
	}

}