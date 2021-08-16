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
package com.rapidminer.tools.belt.expression.internal.function.date;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;


/**
 * A {@link com.rapidminer.tools.belt.expression.Function Function} for adding a value to a given time.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TimeAdd extends AbstractTimeManipulationFunction {

	public TimeAdd() {
		super("time.time_add", 3, ExpressionType.LOCAL_TIME);
	}

	@Override
	protected LocalTime compute(Callable<Void> stopChecker, LocalTime time, double value, String unit) {
		if (time == null || Double.isNaN(value) || unit == null) {
			return null;
		}
		long longValue = (long) value;
		switch (unit){
			case ExpressionParserConstants.DATE_TIME_UNIT_HOUR:
				return time.plusHours(longValue);
			case ExpressionParserConstants.DATE_TIME_UNIT_MINUTE:
				return time.plusMinutes(longValue);
			case ExpressionParserConstants.DATE_TIME_UNIT_SECOND:
				return time.plusSeconds(longValue);
			case ExpressionParserConstants.DATE_TIME_UNIT_MILLISECOND:
				return time.plus(longValue, ChronoUnit.MILLIS);
			case ExpressionParserConstants.DATE_TIME_UNIT_NANOSECOND:
				return time.plusNanos(longValue);
			default:
				throw ExpressionParserUtils.getInvalidTimeUnitException(getFunctionName(), unit);
		}
	}
}
