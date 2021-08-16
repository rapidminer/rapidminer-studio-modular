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
import java.time.Instant;
import java.time.ZonedDateTime;
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
 * Takes an Instant, value, unit and time zone as arguments and returns an Instant.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public abstract class AbstractDateManipulationFunction extends AbstractFunction {

	static final int MILLI_TO_NANO = 1_000_000;

	private static final String I18N_WRONG_TYPE_AT = "expression_parser.function_wrong_type_at";

	public AbstractDateManipulationFunction(String i18nKey, int numberOfArgumentsToCheck, ExpressionType returnType) {
		super(i18nKey, numberOfArgumentsToCheck, returnType);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		getResultType(inputEvaluators);
		ExpressionEvaluator date = inputEvaluators[0];
		ExpressionEvaluator value = inputEvaluators[1];
		ExpressionEvaluator unit = inputEvaluators[2];
		ExpressionEvaluator timeZone = inputEvaluators[3];
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be null, therefore, early exit
				return ExpressionEvaluatorFactory.ofInstant(null);
			}
			return ExpressionEvaluatorFactory.ofInstant(makeInstantCallable(stopChecker, date, value, unit, timeZone),
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
	 * Updates the value of the given ZonedDateTime based on the given value and unit. The result is converted to
	 * Instant and returned.
	 *
	 * @param stopChecker
	 * 		optional callable to check for stop
	 * @param dateTime
	 * 		date time to manipulate
	 * @param value
	 * 		the amount of which the date should change
	 * @param unit
	 * 		the unit constant which should be changed
	 * @return the result of the computation
	 */
	protected abstract Instant compute(Callable<Void> stopChecker, ZonedDateTime dateTime, double value, String unit);

	private Callable<Instant> makeInstantCallable(Callable<Void> stopChecker, ExpressionEvaluator instant,
												  ExpressionEvaluator value, ExpressionEvaluator unit,
												  ExpressionEvaluator timeZone) throws Exception {
		if (instant.isConstant() && timeZone.isConstant()) {
			ZonedDateTime constantZonedDateTime = ExpressionParserUtils.getZonedDateTime(instant, timeZone);
			if (value.isConstant() && unit.isConstant()) {
				double constantValue = value.getDoubleFunction().call();
				String constantUnit = unit.getStringFunction().call();
				Instant constantResult = compute(stopChecker, constantZonedDateTime, constantValue, constantUnit);
				return () -> constantResult;
			} else {
				return () -> compute(stopChecker, constantZonedDateTime,
						value.getDoubleFunction().call(), unit.getStringFunction().call());
			}
		} else {
			return () -> {
				ZonedDateTime zonedDateTime = ExpressionParserUtils.getZonedDateTime(instant, timeZone);
				return compute(stopChecker, zonedDateTime, value.getDoubleFunction().call()
						, unit.getStringFunction().call());
			};
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes.length != 4) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), "4",
					inputTypes.length);
		}
		if (inputTypes[0] != ExpressionType.INSTANT) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "date-time", "1.");
		}
		if (inputTypes[1] != ExpressionType.DOUBLE && inputTypes[1] != ExpressionType.INTEGER) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(),
					"double or integer", "2.");
		}
		if (inputTypes[2] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "string", "3.");
		}
		if (inputTypes[3] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "string", "4.");
		}
		return ExpressionType.INSTANT;
	}
}
