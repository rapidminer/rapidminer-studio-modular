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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import com.rapidminer.tools.LogService;


/**
 * Calculates the n-th percentile.
 *
 * @author Marco Boeck
 * @since 9.9
 */
public class PercentileAggregator implements NumericAggregator {

	private double percentile = 75d;
	private final List<Double> elements = new ArrayList<>();


	/**
	 * Set the percentile to be calculated
	 *
	 * @param parameterValue if set, must be of type double, between (0, 100]
	 */
	@Override
	public void setAggregationParameter(Object... parameterValue) {
		try {
			if (parameterValue.length > 0) {
				this.percentile = (double) parameterValue[0];
			}
		} catch (ClassCastException e) {
			this.percentile = 75d;
			LogService.getRoot().log(Level.WARNING, "Cannot set percentile parameter, not a double. Defaulting to 75.", e);
		}
	}

	@Override
	public void accept(double value) {
		elements.add(value);
	}

	@Override
	public void merge(NumericAggregator other) {
		PercentileAggregator varOther = (PercentileAggregator) other;
		elements.addAll(varOther.elements);
	}

	@Override
	public double getValue() {
		Percentile percentileCalc = new Percentile();
		percentileCalc.setData(ArrayUtils.toPrimitive(elements.toArray(new Double[0])));
		return percentileCalc.evaluate(percentile);
	}

}

