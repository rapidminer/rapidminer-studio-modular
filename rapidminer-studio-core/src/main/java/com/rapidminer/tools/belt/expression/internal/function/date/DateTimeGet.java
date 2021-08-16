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

import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_DAY;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_HOUR;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_MILLISECOND;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_MINUTE;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_MONTH;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_NANOSECOND;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_SECOND;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_WEEK;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_YEAR;
import static com.rapidminer.tools.belt.expression.internal.function.date.AbstractDateManipulationFunction.MILLI_TO_NANO;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;

import java.time.ZonedDateTime;
import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FatalExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionEvaluatorFactory;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;
import com.rapidminer.tools.belt.expression.internal.function.AbstractFunction;


/**
 * A Function for getting the value of a single unit from a date-time.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateTimeGet extends AbstractFunction {

	private static final String I18N_WRONG_TYPE_AT = "expression_parser.function_wrong_type_at";

	public DateTimeGet() {
		super("date.date_time_get", 3, ExpressionType.INTEGER);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		ExpressionType type = getResultType(inputEvaluators);
		ExpressionEvaluator date = inputEvaluators[0];
		ExpressionEvaluator unit = inputEvaluators[1];
		ExpressionEvaluator timeZone = inputEvaluators[2];
		return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(date, unit, timeZone),
				isResultConstant(inputEvaluators), type);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes.length != 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input",
					getFunctionName(), "3", inputTypes.length);
		}
		if (inputTypes[0] != ExpressionType.INSTANT) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "date-time", "1.");
		}
		if (inputTypes[1] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "string", "2.");
		}
		if (inputTypes[2] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "string", "3.");
		}
		return ExpressionType.INTEGER;
	}

	private double compute(ZonedDateTime dateTime, String unit) {
		if(dateTime == null || unit == null){
			return Double.NaN;
		}
		switch (unit) {
			case DATE_TIME_UNIT_YEAR:
				return dateTime.getYear();
			case DATE_TIME_UNIT_MONTH:
				// shifting the range to [0,11] for backwards compatibility
				return dateTime.getMonthValue() - 1;
			case DATE_TIME_UNIT_WEEK:
				return dateTime.get(ALIGNED_WEEK_OF_YEAR);
			case DATE_TIME_UNIT_DAY:
				return dateTime.getDayOfMonth();
			case DATE_TIME_UNIT_HOUR:
				return dateTime.getHour();
			case DATE_TIME_UNIT_MINUTE:
				return dateTime.getMinute();
			case DATE_TIME_UNIT_SECOND:
				return dateTime.getSecond();
			case DATE_TIME_UNIT_MILLISECOND:
				return dateTime.get(MILLI_OF_SECOND);
			case DATE_TIME_UNIT_NANOSECOND:
				return dateTime.getNano() % MILLI_TO_NANO;
			default:
				throw ExpressionParserUtils.getInvalidTimeUnitException(getFunctionName(), unit);
		}
	}

	private DoubleCallable makeDoubleCallable(ExpressionEvaluator instant, ExpressionEvaluator unit,
											  ExpressionEvaluator timeZone) {
		try {
			if (instant.isConstant() && timeZone.isConstant()) {
				ZonedDateTime constantZonedDateTime = ExpressionParserUtils.getZonedDateTime(instant, timeZone);
				if (unit.isConstant()) {
					String constantUnit = unit.getStringFunction().call();
					double constantResult = compute(constantZonedDateTime, constantUnit);
					return () -> constantResult;
				} else {
					return () -> compute(constantZonedDateTime, unit.getStringFunction().call());
				}
			} else {
				return () -> {
					ZonedDateTime zonedDateTime = ExpressionParserUtils.getZonedDateTime(instant, timeZone);
					return compute(zonedDateTime, unit.getStringFunction().call());
				};
			}
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

}
