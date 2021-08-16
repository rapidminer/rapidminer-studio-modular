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
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;

import java.time.LocalTime;
import java.util.Arrays;
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
 * A Function for getting the value of a single unit from a time.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TimeGet extends AbstractFunction {

	private static final String I18N_WRONG_TYPE_AT = "expression_parser.function_wrong_type_at";

	public TimeGet() {
		super("time.time_get", 2, ExpressionType.INTEGER);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		ExpressionType type = getResultType(inputEvaluators);
		ExpressionEvaluator time = inputEvaluators[0];
		ExpressionEvaluator unit = inputEvaluators[1];
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be NaN, therefore, early exit
				return ExpressionEvaluatorFactory.ofDouble(Double.NaN, ExpressionType.INTEGER);
			}
			return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(time, unit),
					isResultConstant(inputEvaluators), type);
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes.length != 2) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input",
					getFunctionName(), "2", inputTypes.length);
		}
		if (inputTypes[0] != ExpressionType.LOCAL_TIME) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "time", "1.");
		}
		if (inputTypes[1] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "string", "2.");
		}
		return ExpressionType.INTEGER;
	}

	private double compute(LocalTime time, String unit) {
		if (time == null || unit == null) {
			return Double.NaN;
		}
		switch (unit) {
			case DATE_TIME_UNIT_HOUR:
				return time.getHour();
			case DATE_TIME_UNIT_MINUTE:
				return time.getMinute();
			case DATE_TIME_UNIT_SECOND:
				return time.getSecond();
			case DATE_TIME_UNIT_MILLISECOND:
				return time.get(MILLI_OF_SECOND);
			case DATE_TIME_UNIT_NANOSECOND:
				return time.getNano() % MILLI_TO_NANO;
			default:
				throw ExpressionParserUtils.getInvalidTimeUnitException(getFunctionName(), unit);
		}
	}

	private DoubleCallable makeDoubleCallable(ExpressionEvaluator localTime, ExpressionEvaluator unit) throws Exception {
		if (localTime.isConstant() && unit.isConstant()) {
			double constantResult = compute(localTime.getLocalTimeFunction().call(), unit.getStringFunction().call());
			return () -> constantResult;
		} else if (localTime.isConstant()) {
			LocalTime constantLocalTime = localTime.getLocalTimeFunction().call();
			return () -> compute(constantLocalTime, unit.getStringFunction().call());
		} else if (unit.isConstant()) {
			String constantUnit = unit.getStringFunction().call();
			return () -> compute(localTime.getLocalTimeFunction().call(), constantUnit);
		} else {
			return () -> compute(localTime.getLocalTimeFunction().call(), unit.getStringFunction().call());
		}
	}

}
