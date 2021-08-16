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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
 * Parses a date-time to a String using the optional timezone, pattern and locale.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateString extends AbstractFunction {

	private static final String DEFAULT_LOCALE = "us";

	/**
	 * The default format is ISO_OFFSET_DATE_TIME.
	 */
	private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	/**
	 * The default format (ISO_OFFSET_DATE_TIME) with UTC override time zone. Used to format instants with no time zone
	 * information.
	 */
	private static final DateTimeFormatter DEFAULT_FORMATTER_UTC = DEFAULT_FORMATTER.withZone(ZoneOffset.UTC);

	public DateString() {
		super("conversion.date_time_str", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, ExpressionType.STRING);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length < 1 || inputEvaluators.length > 4) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input",
					getFunctionName(), "1 - 4", inputEvaluators.length);
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
				stringCallable = makeStringCallable(inputEvaluators[0], inputEvaluators[1]);
			} else if (inputEvaluators.length == 3) {
				stringCallable = makeStringCallable(inputEvaluators[0], inputEvaluators[1], inputEvaluators[2],
						ExpressionEvaluatorFactory.ofString(DEFAULT_LOCALE));
			} else {
				stringCallable = makeStringCallable(inputEvaluators[0], inputEvaluators[1], inputEvaluators[2],
						inputEvaluators[3]);
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
	private Callable<String> makeStringCallable(ExpressionEvaluator instant) throws Exception {
		if (instant.isConstant()) {
			String constantResult = compute(instant.getInstantFunction().call());
			return () -> constantResult;
		}
		return () -> compute(instant.getInstantFunction().call());
	}

	/**
	 * @return string-callable built from the given inputs
	 * @throws Exception
	 * 		if the evaluation of the inputs fails or if the inputs are of illegal format
	 */
	private Callable<String> makeStringCallable(ExpressionEvaluator instant, ExpressionEvaluator timeZone) throws Exception {
		if (instant.isConstant() && timeZone.isConstant()) {
			String constantResult = compute(ExpressionParserUtils
					.getZonedDateTime(instant, timeZone), DEFAULT_FORMATTER);
			return () -> constantResult;
		} else {
			return () -> compute(ExpressionParserUtils
					.getZonedDateTime(instant, timeZone), DEFAULT_FORMATTER);
		}
	}

	/**
	 * @return string-callable built from the given inputs
	 * @throws Exception
	 * 		if the evaluation of the inputs fails or if the inputs are of illegal format
	 */
	private Callable<String> makeStringCallable(ExpressionEvaluator instant, ExpressionEvaluator timeZone,
												ExpressionEvaluator pattern, ExpressionEvaluator locale) throws Exception {
		if (instant.isConstant() && timeZone.isConstant() && pattern.isConstant()
				&& locale.isConstant()) {
			String constantResult = compute(ExpressionParserUtils.getZonedDateTime(instant, timeZone),
					getDateTimeFormatter(pattern, locale));
			return () -> constantResult;
		} else if (instant.isConstant() && timeZone.isConstant()) {
			ZonedDateTime constantDate = ExpressionParserUtils.getZonedDateTime(instant, timeZone);
			return () -> compute(constantDate, getDateTimeFormatter(pattern, locale));
		} else if (pattern.isConstant() && locale.isConstant()) {
			DateTimeFormatter constantFormatter = getDateTimeFormatter(pattern, locale);
			return () -> compute(ExpressionParserUtils.getZonedDateTime(instant, timeZone), constantFormatter);
		}

		return () -> compute(ExpressionParserUtils.getZonedDateTime(instant, timeZone),
				getDateTimeFormatter(pattern, locale));
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes[0] != ExpressionType.INSTANT) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type.argument", 1, getFunctionName(), "date-time");
		}
		for (int i = 1; i < inputTypes.length; i++) {
			if (inputTypes[i] != ExpressionType.STRING) {
				throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type.argument", i + 1, getFunctionName(),
						"string");
			}
		}
		return ExpressionType.STRING;
	}

	/**
	 * @return {@code null} if dateTime or formatter are {@code null}. Otherwise, returns formatter#format(dateTime).
	 */
	private String compute(ZonedDateTime dateTime, DateTimeFormatter formatter) {
		if (dateTime == null || formatter == null) {
			return null;
		}
		try {
			return formatter.format(dateTime);
		} catch (DateTimeException e) {
			throw new FunctionInputExceptionWrapper("expression_parser.invalid_argument.date_time_pattern",
					getFunctionName(), "3.");
		}
	}

	/**
	 * @return {@code null} if the given instant is {@code null}. Otherwise, return instant#toString.
	 */
	private static String compute(Instant instant) {
		if (instant == null) {
			return null;
		}
		return DEFAULT_FORMATTER_UTC.format(instant);
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
