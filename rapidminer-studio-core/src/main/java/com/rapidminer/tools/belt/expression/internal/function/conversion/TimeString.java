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
package com.rapidminer.tools.belt.expression.internal.function.conversion;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FatalExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.FunctionDescription;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionEvaluatorFactory;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;
import com.rapidminer.tools.belt.expression.internal.function.AbstractFunction;


/**
 * Parses a time to a String using the optional pattern and locale.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TimeString extends AbstractFunction {

	private static final String DEFAULT_LOCALE = "us";

	/**
	 * The default format is ISO_LOCAL_TIME.
	 */
	private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

	public TimeString() {
		super("conversion.time_str", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, ExpressionType.STRING);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length < 1 || inputEvaluators.length > 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input",
					getFunctionName(), "1 - 3", inputEvaluators.length);
		}
		getResultType(inputEvaluators);
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be null, therefore, early exit
				return ExpressionEvaluatorFactory.ofString(null);
			}
			Callable<String> stringCallable;
			if (inputEvaluators.length == 1) {
				stringCallable = makeStringCallable(inputEvaluators[0]);
			} else if (inputEvaluators.length == 2) {
				stringCallable = makeStringCallable(inputEvaluators[0], inputEvaluators[1],
						ExpressionEvaluatorFactory.ofString(DEFAULT_LOCALE));
			} else {
				stringCallable = makeStringCallable(inputEvaluators[0], inputEvaluators[1], inputEvaluators[2]);
			}
			return ExpressionEvaluatorFactory.ofString(stringCallable, isResultConstant(inputEvaluators));
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (DateTimeException e) {
			throw new FunctionInputExceptionWrapper(e.getMessage());
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	/**
	 * @return string-callable built from the given inputs
	 * @throws Exception
	 * 		if the evaluation of the inputs fails or if the inputs are of illegal format
	 */
	private Callable<String> makeStringCallable(ExpressionEvaluator localTime) throws Exception {
		if (localTime.isConstant()) {
			String constantResult = compute(localTime.getLocalTimeFunction().call());
			return () -> constantResult;
		}
		return () -> compute(localTime.getLocalTimeFunction().call());
	}

	/**
	 * @return string-callable built from the given inputs
	 * @throws Exception
	 * 		if the evaluation of the inputs fails or if the inputs are of illegal format
	 */
	private Callable<String> makeStringCallable(ExpressionEvaluator time, ExpressionEvaluator pattern,
												ExpressionEvaluator locale) throws Exception {
		if (time.isConstant() && pattern.isConstant() && locale.isConstant()) {
			String constantResult = compute(time.getLocalTimeFunction().call(),
					getDateTimeFormatter(pattern, locale));
			return () -> constantResult;
		} else if (time.isConstant()) {
			LocalTime constantTime = time.getLocalTimeFunction().call();
			return () -> compute(constantTime, getDateTimeFormatter(pattern, locale));
		} else if (pattern.isConstant() && locale.isConstant()) {
			DateTimeFormatter constantFormatter = getDateTimeFormatter(pattern, locale);
			return () -> compute(time.getLocalTimeFunction().call(), constantFormatter);
		}

		return () -> compute(time.getLocalTimeFunction().call(),
				getDateTimeFormatter(pattern, locale));
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes[0] != ExpressionType.LOCAL_TIME) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type.argument",
					1, getFunctionName(), "time");
		}
		for (int i = 1; i < inputTypes.length; i++) {
			if (inputTypes[i] != ExpressionType.STRING) {
				throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type.argument",
						i + 1, getFunctionName(), "string");
			}
		}
		return ExpressionType.STRING;
	}

	/**
	 * @return {@code null} if time or formatter are {@code null}. Otherwise, returns formatter#format(time).
	 */
	private String compute(LocalTime time, DateTimeFormatter formatter) {
		if (time == null || formatter == null) {
			return null;
		}
		try {
			return formatter.format(time);
		} catch(DateTimeException e){
			throw new FunctionInputExceptionWrapper("expression_parser.invalid_argument.time_pattern",
					getFunctionName(), "2.");
		}
	}

	/**
	 * @return {@code null} if the given local time is {@code null}. Otherwise, return DEFAULT_FORMATTER#format(time).
	 */
	private static String compute(LocalTime time) {
		if (time == null) {
			return null;
		}
		return DEFAULT_FORMATTER.format(time);
	}

	/**
	 * @return a DateTimeFormatter built from the given inputs or {@code null} if any of the inputs evaluates to {@code
	 * null}.
	 * @throws Exception
	 * 		if the evaluation of the input fails or if the input is of illegal format
	 */
	private static DateTimeFormatter getDateTimeFormatter(ExpressionEvaluator pattern,
														  ExpressionEvaluator locale) throws Exception {
		String patternString = pattern.getStringFunction().call();
		String localeString = locale.getStringFunction().call();
		if (localeString == null || patternString == null) {
			return null;
		}
		try {
			return DateTimeFormatter.ofPattern(patternString, new Locale(localeString));
		} catch (IllegalArgumentException e){
			throw new FunctionInputExceptionWrapper(e.getMessage());
		}
	}

}
