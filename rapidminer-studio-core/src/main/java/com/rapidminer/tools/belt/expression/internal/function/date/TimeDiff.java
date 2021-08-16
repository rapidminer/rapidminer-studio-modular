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

import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_HOUR;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_MILLISECOND;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_MINUTE;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_NANOSECOND;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_SECOND;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.LocalTime;
import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;


/**
 * A Function for calculating the difference between two LocalTimes in various units.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TimeDiff extends Abstract2TimeInputIntegerOutputFunction {

	public TimeDiff() {
		super("time.time_diff", 3);
	}

	@Override
	protected double compute(Callable<Void> stopChecker, LocalTime left, LocalTime right, String unit) {
		if (left == null || right == null || unit == null) {
			return Double.NaN;
		} else {
			switch (unit) {
				case DATE_TIME_UNIT_HOUR:
					return left.until(right, HOURS);
				case DATE_TIME_UNIT_MINUTE:
					return left.until(right, MINUTES);
				case DATE_TIME_UNIT_SECOND:
					return left.until(right, SECONDS);
				case DATE_TIME_UNIT_MILLISECOND:
					return left.until(right, MILLIS);
				case DATE_TIME_UNIT_NANOSECOND:
					return left.until(right, NANOS);
				default:
					throw ExpressionParserUtils.getInvalidTimeUnitException(getFunctionName(), unit);
			}
		}
	}

}
