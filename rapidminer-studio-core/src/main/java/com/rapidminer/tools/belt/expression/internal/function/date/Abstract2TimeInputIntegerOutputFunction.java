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


import java.time.DateTimeException;
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
 * Abstract class for a Function that has two local time and one unit arguments and returns an integer.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public abstract class Abstract2TimeInputIntegerOutputFunction extends AbstractFunction {

	private static final String I18N_WRONG_TYPE_AT = "expression_parser.function_wrong_type_at";

	public Abstract2TimeInputIntegerOutputFunction(String i18nKey, int numberOfArgumentsToCheck) {
		super(i18nKey, numberOfArgumentsToCheck, ExpressionType.INTEGER);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		ExpressionType type = getResultType(inputEvaluators);
		ExpressionEvaluator left = inputEvaluators[0];
		ExpressionEvaluator right = inputEvaluators[1];
		ExpressionEvaluator unit = inputEvaluators[2];
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be NaN, therefore, early exit
				return ExpressionEvaluatorFactory.ofDouble(Double.NaN, ExpressionType.INTEGER);
			}
			return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(stopChecker, left, right, unit),
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
		if (inputTypes.length != 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input",
					getFunctionName(), "3", inputTypes.length);
		}
		if (inputTypes[0] != ExpressionType.LOCAL_TIME || inputTypes[1] != ExpressionType.LOCAL_TIME) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type",
					getFunctionName(), "time");
		}
		if (inputTypes[2] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT,
					getFunctionName(), "string", "3.");
		}
		return ExpressionType.INTEGER;

	}

	/**
	 * Computes the result for two input times and the unit.
	 *
	 * @param left
	 * 		first time
	 * @param right
	 * 		second time
	 * @param unit
	 * 		the time unit
	 * @return the result of the computation.
	 */
	protected abstract double compute(Callable<Void> stopChecker, LocalTime left, LocalTime right, String unit);

	private DoubleCallable makeDoubleCallable(Callable<Void> stopChecker, ExpressionEvaluator left,
											  ExpressionEvaluator right, ExpressionEvaluator unit) throws Exception {
		if (left.isConstant() && right.isConstant() && unit.isConstant()) {
			double constantResult = compute(stopChecker, left.getLocalTimeFunction().call(),
					right.getLocalTimeFunction().call(), unit.getStringFunction().call());
			return () -> constantResult;
		} else if (unit.isConstant()) {
			String constantUnit = unit.getStringFunction().call();
			return () -> compute(stopChecker, left.getLocalTimeFunction().call(), right.getLocalTimeFunction().call(),
					constantUnit);
		}
		return () -> compute(stopChecker, left.getLocalTimeFunction().call(), right.getLocalTimeFunction().call(),
				unit.getStringFunction().call());
	}

}
