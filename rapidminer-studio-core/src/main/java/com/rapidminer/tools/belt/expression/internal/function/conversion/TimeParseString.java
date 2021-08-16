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

import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
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
 * Parses a string to a LocalTime with respect to an (optional) pattern string and an (optional) locale. The default
 * format is ISO_LOCAL_TIME.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TimeParseString extends AbstractFunction {

	private static final String WRONG_TYPE_ERROR = "expression_parser.function_wrong_type.argument";

	private static final String DEFAULT_LOCALE = "us";

	/**
	 * The default format is {@link DateTimeFormatter#ISO_LOCAL_TIME}.
	 */
	private static final DateTimeFormatter DEFAULT_FORMATTER = ISO_LOCAL_TIME;

	public TimeParseString() {
		super("conversion.time_parse_str", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS,
				ExpressionType.LOCAL_TIME);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		getResultType(inputEvaluators);
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be null, therefore, early exit
				return ExpressionEvaluatorFactory.ofLocalTime(null);
			}
			if (inputEvaluators.length == 1) {
				return ExpressionEvaluatorFactory.ofLocalTime(makeLocalTimeCallable(inputEvaluators[0]),
						isResultConstant(inputEvaluators));
			} else if (inputEvaluators.length == 2) {
				return ExpressionEvaluatorFactory.ofLocalTime(makeLocalTimeCallable(inputEvaluators[0],
						inputEvaluators[1]), isResultConstant(inputEvaluators));
			} else {
				return ExpressionEvaluatorFactory.ofLocalTime(makeLocalTimeCallable(inputEvaluators[0],
						inputEvaluators[1], inputEvaluators[2]), isResultConstant(inputEvaluators));
			}
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes.length < 1 || inputTypes.length > 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input",
					getFunctionName(), "1 - 3", inputTypes.length);
		}
		for (int i = 0; i < inputTypes.length; i++) {
			if (inputTypes[i] != ExpressionType.STRING) {
				throw new FunctionInputExceptionWrapper(WRONG_TYPE_ERROR, i + 1, getFunctionName(), "nominal");
			}
		}
		return ExpressionType.LOCAL_TIME;
	}

	/**
	 * @return an LocalTime Callable build from the given local time string, default locale and default pattern.
	 */
	private Callable<LocalTime> makeLocalTimeCallable(ExpressionEvaluator time) throws Exception {
		if (time.isConstant()) {
			LocalTime result = compute(time.getStringFunction().call(), DEFAULT_FORMATTER);
			return () -> result;
		} else {
			return () -> compute(time.getStringFunction().call(), DEFAULT_FORMATTER);
		}
	}

	/**
	 * @return an LocalTime Callable build from the given local time string, locale and default pattern.
	 */
	private Callable<LocalTime> makeLocalTimeCallable(ExpressionEvaluator time, ExpressionEvaluator pattern) throws Exception {
		if (time.isConstant() && pattern.isConstant()) {
			LocalTime result = compute(time.getStringFunction().call(), getDateTimeFormatter(
					pattern.getStringFunction().call(), DEFAULT_LOCALE));
			return () -> result;
		} else if (pattern.isConstant()) {
			DateTimeFormatter constantFormatter = getDateTimeFormatter(pattern.getStringFunction().call(),
					DEFAULT_LOCALE);
			return () -> compute(time.getStringFunction().call(), constantFormatter);
		} else {
			return () -> compute(time.getStringFunction().call(), getDateTimeFormatter(
					pattern.getStringFunction().call(), DEFAULT_LOCALE));
		}
	}

	/**
	 * @return an LocalTime Callable build from the given local time string, locale and pattern.
	 */
	private Callable<LocalTime> makeLocalTimeCallable(ExpressionEvaluator time, ExpressionEvaluator pattern,
													  ExpressionEvaluator locale) throws Exception {
		if (time.isConstant() && locale.isConstant() && pattern.isConstant()) {
			LocalTime result = compute(time.getStringFunction().call(),
					getDateTimeFormatter(pattern.getStringFunction().call(), locale.getStringFunction().call()));
			return () -> result;
		} else if (locale.isConstant() && pattern.isConstant()) {
			DateTimeFormatter constantFormatter = getDateTimeFormatter(pattern.getStringFunction().call(),
					locale.getStringFunction().call());
			return () -> compute(time.getStringFunction().call(), constantFormatter);
		} else {
			return () -> compute(time.getStringFunction().call(),
					getDateTimeFormatter(pattern.getStringFunction().call(), locale.getStringFunction().call()));
		}
	}

	/**
	 * Computes the result for three string input values.
	 *
	 * @param timeString
	 * 		the input time string
	 * @param formatter
	 * 		the formatter used to parse the string
	 * @return the result of the computation.
	 */
	private LocalTime compute(String timeString, DateTimeFormatter formatter) {
		if (timeString == null || formatter == null) {
			return null;
		}
		try {
			return LocalTime.parse(timeString, formatter);
		} catch (DateTimeParseException e) {
			throw new FunctionInputExceptionWrapper(e.getMessage());
		}
	}

	/**
	 * @return a new DateTimeFormatter based on the given locale and pattern or {@code null} if locale or pattern are
	 * {@code null}.
	 */
	private DateTimeFormatter getDateTimeFormatter(String pattern, String locale) {
		if (locale == null || pattern == null) {
			return null;
		}
		try {
			return new DateTimeFormatterBuilder().parseCaseInsensitive().parseStrict()
					.appendPattern(pattern).toFormatter(new Locale(locale));
		} catch (IllegalArgumentException e) {
			throw new FunctionInputExceptionWrapper("expression_parser.invalid_argument.custom_format",
					getFunctionName());
		}
	}

}
