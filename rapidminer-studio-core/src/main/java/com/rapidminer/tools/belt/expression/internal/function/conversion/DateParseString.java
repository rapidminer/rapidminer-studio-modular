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

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
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
 * Parses a string to an Instant with respect to an (optional) pattern string and an (optional) locale.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateParseString extends AbstractFunction {

	private static final String WRONG_TYPE_ERROR = "expression_parser.function_wrong_type.argument";

	private static final String DEFAULT_LOCALE = "us";

	/**
	 * The default format is {@link DateTimeFormatter#ISO_ZONED_DATE_TIME} but with optional offset and timezone (one of
	 * them being mandatory). Everything is parsed case insensitive. Also we allow leaving out the 'T' or replacing it
	 * with a whitespace.
	 */
	private static final DateTimeFormatter DEFAULT_FORMATTER;

	static {
		DEFAULT_FORMATTER = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.parseStrict()

				.append(ISO_LOCAL_DATE)

				.optionalStart()
				.appendLiteral('T')
				.optionalEnd()
				.optionalStart()
				.appendLiteral(' ')
				.optionalEnd()

				.append(ISO_LOCAL_TIME)

				.optionalStart()
				.appendOffsetId()
				.optionalEnd()

				.optionalStart()
				.appendLiteral('[')
				.appendZoneRegionId()
				.appendLiteral(']')
				.optionalEnd()

				.toFormatter(new Locale(DEFAULT_LOCALE))
				.withChronology(IsoChronology.INSTANCE);
	}

	public DateParseString() {
		super("conversion.date_time_parse_str", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS,
				ExpressionType.INSTANT);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		getResultType(inputEvaluators);
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be null, therefore, early exit
				return ExpressionEvaluatorFactory.ofInstant(null);
			}
			if (inputEvaluators.length == 1) {
				return ExpressionEvaluatorFactory.ofInstant(makeInstantCallable(inputEvaluators[0]),
						isResultConstant(inputEvaluators));
			} else if (inputEvaluators.length == 2) {
				return ExpressionEvaluatorFactory.ofInstant(makeInstantCallable(inputEvaluators[0],
						inputEvaluators[1]), isResultConstant(inputEvaluators));
			} else {
				return ExpressionEvaluatorFactory.ofInstant(makeInstantCallable(inputEvaluators[0],
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
		return ExpressionType.INSTANT;
	}

	/**
	 * @return an Instant Callable build from the given instant string, default locale and default pattern.
	 */
	private Callable<Instant> makeInstantCallable(ExpressionEvaluator instant) throws Exception {
		if (instant.isConstant()) {
			Instant result = compute(instant.getStringFunction().call(), DEFAULT_FORMATTER);
			return () -> result;
		} else {
			return () -> compute(instant.getStringFunction().call(), DEFAULT_FORMATTER);
		}
	}

	/**
	 * @return an Instant Callable build from the given instant string, locale and default pattern.
	 */
	private Callable<Instant> makeInstantCallable(ExpressionEvaluator instant, ExpressionEvaluator pattern) throws Exception {
		if (instant.isConstant() && pattern.isConstant()) {
			Instant result = compute(instant.getStringFunction().call(), getDateTimeFormatter(
					pattern.getStringFunction().call(), DEFAULT_LOCALE));
			return () -> result;
		} else if (pattern.isConstant()) {
			DateTimeFormatter constantFormatter = getDateTimeFormatter(pattern.getStringFunction().call(),
					DEFAULT_LOCALE);
			return () -> compute(instant.getStringFunction().call(), constantFormatter);
		} else {
			return () -> compute(instant.getStringFunction().call(), getDateTimeFormatter(
					pattern.getStringFunction().call(), DEFAULT_LOCALE));
		}
	}

	/**
	 * @return an Instant Callable build from the given instant string, locale and pattern.
	 */
	private Callable<Instant> makeInstantCallable(ExpressionEvaluator instant, ExpressionEvaluator pattern,
												  ExpressionEvaluator locale) throws Exception {
		if (instant.isConstant() && locale.isConstant() && pattern.isConstant()) {
			Instant result = compute(instant.getStringFunction().call(),
					getDateTimeFormatter(pattern.getStringFunction().call(), locale.getStringFunction().call()));
			return () -> result;
		} else if (locale.isConstant() && pattern.isConstant()) {
			DateTimeFormatter constantFormatter = getDateTimeFormatter(pattern.getStringFunction().call(),
					locale.getStringFunction().call());
			return () -> compute(instant.getStringFunction().call(), constantFormatter);
		} else {
			return () -> compute(instant.getStringFunction().call(),
					getDateTimeFormatter(pattern.getStringFunction().call(), locale.getStringFunction().call()));
		}
	}

	/**
	 * Computes the result for three string input values.
	 *
	 * @param dateString
	 * 		the input date-time string
	 * @param formatter
	 * 		the formatter used to parse the string
	 * @return the result of the computation.
	 */
	private Instant compute(String dateString, DateTimeFormatter formatter) {
		if (dateString == null || formatter == null) {
			return null;
		}
		try {
			return ZonedDateTime.parse(dateString, formatter).toInstant();
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
					getFunctionName(), e.getMessage());
		}
	}

}
