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
import static com.rapidminer.tools.belt.expression.internal.function.date.AbstractDateManipulationFunction.MILLI_TO_NANO;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;


/**
 * A {@link com.rapidminer.tools.belt.expression.Function} for setting a value of a given time.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TimeSet extends AbstractTimeManipulationFunction {

	private static final int MIN_MILLI = 0;
	private static final int MAX_MILLI = 999;
	private static final int MIN_NANO = 0;
	private static final int MAX_NANO = 999_999;

	private static final String I18N_OUT_OF_RANGE = "expression_parser.out_of_range.date_time";

	public TimeSet() {
		super("time.time_set", 3, ExpressionType.LOCAL_TIME);
	}

	@Override
	protected LocalTime compute(Callable<Void> stopChecker, LocalTime time, double value, String unit) {
		if (time == null || Double.isNaN(value) || unit == null) {
			return null;
		}
		int intValue = (int) value;
		try {
			switch (unit) {
				case DATE_TIME_UNIT_HOUR:
					return time.withHour(intValue);
				case DATE_TIME_UNIT_MINUTE:
					return time.withMinute(intValue);
				case DATE_TIME_UNIT_SECOND:
					return time.withSecond(intValue);
				case DATE_TIME_UNIT_MILLISECOND:
					if (intValue < MIN_MILLI || intValue > MAX_MILLI) {
						throw new FunctionInputExceptionWrapper(I18N_OUT_OF_RANGE,
								"milliseconds", MIN_MILLI, MAX_MILLI, intValue);
					}
					return time.withNano(intValue * MILLI_TO_NANO
							+ time.getNano() % MILLI_TO_NANO);
				case DATE_TIME_UNIT_NANOSECOND:
					if (intValue < MIN_NANO || intValue > MAX_NANO) {
						throw new FunctionInputExceptionWrapper(I18N_OUT_OF_RANGE,
								"nanoseconds", MIN_NANO, MAX_NANO, intValue);
					}
					return time.withNano(time.getNano() / MILLI_TO_NANO * MILLI_TO_NANO
							+ intValue);
				default:
					throw ExpressionParserUtils.getInvalidTimeUnitException(getFunctionName(), unit);
			}
		} catch (DateTimeException e) {
			throw new FunctionInputExceptionWrapper(e.getMessage());
		}
	}
}
