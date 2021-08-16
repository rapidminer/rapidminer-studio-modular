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
 * Takes a LocalTime, value and unit as arguments and returns a LocalTime.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public abstract class AbstractTimeManipulationFunction extends AbstractFunction {

	private static final String I18N_WRONG_TYPE_AT = "expression_parser.function_wrong_type_at";

	public AbstractTimeManipulationFunction(String i18nKey, int numberOfArgumentsToCheck, ExpressionType returnType) {
		super(i18nKey, numberOfArgumentsToCheck, returnType);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		getResultType(inputEvaluators);
		ExpressionEvaluator time = inputEvaluators[0];
		ExpressionEvaluator value = inputEvaluators[1];
		ExpressionEvaluator unit = inputEvaluators[2];
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be null, therefore, early exit
				return ExpressionEvaluatorFactory.ofLocalTime(null);
			}
			return ExpressionEvaluatorFactory.ofLocalTime(makeTimeCallable(stopChecker, time, value, unit),
					isResultConstant(inputEvaluators));
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (DateTimeException e) {
			throw new FunctionInputExceptionWrapper(e.getMessage());
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	/**
	 * Updates the value of the given LocalTime based on the given value and unit.
	 *
	 * @param stopChecker
	 * 		optional callable to check for stop
	 * @param time
	 * 		time to manipulate
	 * @param value
	 * 		the amount of which the time should change
	 * @param unit
	 * 		the unit constant which should be changed
	 * @return the result of the computation
	 */
	protected abstract LocalTime compute(Callable<Void> stopChecker, LocalTime time, double value, String unit);

	private Callable<LocalTime> makeTimeCallable(Callable<Void> stopChecker, ExpressionEvaluator time,
												 ExpressionEvaluator value, ExpressionEvaluator unit) throws Exception {
		if (time.isConstant() && value.isConstant() && unit.isConstant()) {
			LocalTime constantResult = compute(stopChecker, time.getLocalTimeFunction().call(),
					value.getDoubleFunction().call(), unit.getStringFunction().call());
			return () -> constantResult;
		} else if (value.isConstant() && unit.isConstant()) {
			double constantValue = value.getDoubleFunction().call();
			String constantUnit = unit.getStringFunction().call();
			return () -> compute(stopChecker, time.getLocalTimeFunction().call(), constantValue, constantUnit);
		} else if (unit.isConstant()) {
			String constantUnit = unit.getStringFunction().call();
			return () -> compute(stopChecker, time.getLocalTimeFunction().call(),
					value.getDoubleFunction().call(), constantUnit);
		}
		return () -> compute(stopChecker, time.getLocalTimeFunction().call(),
				value.getDoubleFunction().call(), unit.getStringFunction().call());

	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes.length != 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input",
					getFunctionName(), "3", inputTypes.length);
		}
		if (inputTypes[0] != ExpressionType.LOCAL_TIME) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT,
					getFunctionName(), "time", "1.");
		}
		if (inputTypes[1] != ExpressionType.DOUBLE && inputTypes[1] != ExpressionType.INTEGER) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT,
					getFunctionName(), "double or integer", "2.");
		}
		if (inputTypes[2] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT,
					getFunctionName(), "string", "3.");
		}
		return ExpressionType.LOCAL_TIME;
	}
}
