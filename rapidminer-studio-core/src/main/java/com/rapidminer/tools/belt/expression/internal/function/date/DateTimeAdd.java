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

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;


/**
 * A {@link com.rapidminer.tools.belt.expression.Function Function} for adding a value to a given date-time.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateTimeAdd extends AbstractDateManipulationFunction {

	public DateTimeAdd() {
		super("date.date_time_add", 4, ExpressionType.INSTANT);
	}

	@Override
	protected Instant compute(Callable<Void> stopChecker, ZonedDateTime dateTime, double value, String unit) {
		if (dateTime == null || Double.isNaN(value) || unit == null) {
			return null;
		}
		if (value == Double.POSITIVE_INFINITY) {
			return Instant.MAX;
		} else if (value == Double.NEGATIVE_INFINITY) {
			return Instant.MIN;
		}
		
		long longValue = (long) value;
		switch (unit) {
			case ExpressionParserConstants.DATE_TIME_UNIT_YEAR:
				return dateTime.plusYears(longValue).toInstant();
			case ExpressionParserConstants.DATE_TIME_UNIT_MONTH:
				return dateTime.plusMonths(longValue).toInstant();
			case ExpressionParserConstants.DATE_TIME_UNIT_WEEK:
				return dateTime.plusWeeks(longValue).toInstant();
			case ExpressionParserConstants.DATE_TIME_UNIT_DAY:
				return dateTime.plusDays(longValue).toInstant();
			case ExpressionParserConstants.DATE_TIME_UNIT_HOUR:
				return dateTime.plusHours(longValue).toInstant();
			case ExpressionParserConstants.DATE_TIME_UNIT_MINUTE:
				return dateTime.plusMinutes(longValue).toInstant();
			case ExpressionParserConstants.DATE_TIME_UNIT_SECOND:
				return dateTime.plusSeconds(longValue).toInstant();
			case ExpressionParserConstants.DATE_TIME_UNIT_MILLISECOND:
				return dateTime.plus(longValue, ChronoUnit.MILLIS).toInstant();
			case ExpressionParserConstants.DATE_TIME_UNIT_NANOSECOND:
				return dateTime.plusNanos(longValue).toInstant();
			default:
				throw ExpressionParserUtils.getInvalidTimeUnitException(getFunctionName(), unit);
		}
	}
}
