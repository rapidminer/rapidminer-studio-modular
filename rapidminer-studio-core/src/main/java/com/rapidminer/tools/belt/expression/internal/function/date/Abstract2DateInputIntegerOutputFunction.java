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


import static com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils.getZonedDateTime;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
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
 * Abstract class for a Function that has two instant, one unit and one time zone arguments and returns an integer.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public abstract class Abstract2DateInputIntegerOutputFunction extends AbstractFunction {

	private static final String I18N_WRONG_TYPE_AT = "expression_parser.function_wrong_type_at";

	public Abstract2DateInputIntegerOutputFunction(String i18nKey, int numberOfArgumentsToCheck) {
		super(i18nKey, numberOfArgumentsToCheck, ExpressionType.INTEGER);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		ExpressionType type = getResultType(inputEvaluators);
		ExpressionEvaluator left = inputEvaluators[0];
		ExpressionEvaluator right = inputEvaluators[1];
		ExpressionEvaluator unit = inputEvaluators[2];
		ExpressionEvaluator timeZone = inputEvaluators[3];
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be NaN, therefore, early exit
				return ExpressionEvaluatorFactory.ofDouble(Double.NaN, ExpressionType.INTEGER);
			}
			return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(stopChecker, left, right, unit, timeZone),
					isResultConstant(inputEvaluators), type);
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (DateTimeException e) {
			throw new FunctionInputExceptionWrapper(e.getMessage());
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes.length != 4) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), "4",
					inputTypes.length);
		}
		if (inputTypes[0] != ExpressionType.INSTANT || inputTypes[1] != ExpressionType.INSTANT) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type", getFunctionName(), "date-time");
		}
		if (inputTypes[2] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "string", "3.");
		}
		if (inputTypes[3] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "string", "4.");
		}
		return ExpressionType.INTEGER;

	}

	/**
	 * Computes the result for two input date-times and the unit.
	 *
	 * @param left
	 * 		first date
	 * @param right
	 * 		second date
	 * @param unit
	 * 		the date unit
	 * @return the result of the computation.
	 */
	protected abstract double compute(Callable<Void> stopChecker, ZonedDateTime left, ZonedDateTime right, String unit);

	private DoubleCallable makeDoubleCallable(Callable<Void> stopChecker, ExpressionEvaluator left,
											  ExpressionEvaluator right, ExpressionEvaluator unit,
											  ExpressionEvaluator timeZone) throws Exception {
		if (timeZone.isConstant()) {
			if (left.isConstant() && right.isConstant() && unit.isConstant()) {
				double constantResult = compute(stopChecker, getZonedDateTime(left, timeZone),
						getZonedDateTime(right, timeZone), unit.getStringFunction().call());
				return () -> constantResult;
			} else if (left.isConstant() && right.isConstant()) {
				ZonedDateTime leftConstant = getZonedDateTime(left, timeZone);
				ZonedDateTime rightConstant = getZonedDateTime(right, timeZone);
				return () -> compute(stopChecker, leftConstant, rightConstant,
						unit.getStringFunction().call());
			} else if (left.isConstant()) {
				ZonedDateTime leftConstant = getZonedDateTime(left, timeZone);
				return () -> compute(stopChecker, leftConstant, getZonedDateTime(right, timeZone),
						unit.getStringFunction().call());
			} else if (right.isConstant()) {
				ZonedDateTime rightConstant = getZonedDateTime(right, timeZone);
				return () -> compute(stopChecker, getZonedDateTime(right, timeZone), rightConstant,
						unit.getStringFunction().call());
			}
		}
		return () -> compute(stopChecker, getZonedDateTime(left, timeZone), getZonedDateTime(right, timeZone),
				unit.getStringFunction().call());
	}

}
