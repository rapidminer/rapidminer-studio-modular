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

import java.time.LocalTime;
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
 * A Function for getting the local time part of a zoned date-time. Builds the zoned date-time of the given instant and
 * timezone.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateGetTime extends AbstractFunction {

	private static final String I18N_WRONG_TYPE_AT = "expression_parser.function_wrong_type_at";

	public DateGetTime() {
		super("conversion.date_get_time", 2, ExpressionType.LOCAL_TIME);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		getResultType(inputEvaluators);
		ExpressionEvaluator date = inputEvaluators[0];
		ExpressionEvaluator timeZone = inputEvaluators[1];
		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be null, therefore, early exit
				return ExpressionEvaluatorFactory.ofLocalTime(null);
			}
			return ExpressionEvaluatorFactory.ofLocalTime(makeLocalTimeCallable(date, timeZone),
					isResultConstant(inputEvaluators));
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
		if (inputTypes[0] != ExpressionType.INSTANT) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "time", "1.");
		}
		if (inputTypes[1] != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(I18N_WRONG_TYPE_AT, getFunctionName(), "string", "2.");
		}
		return ExpressionType.LOCAL_TIME;
	}

	private Callable<LocalTime> makeLocalTimeCallable(ExpressionEvaluator instant, ExpressionEvaluator timeZone) throws Exception {
		if (instant.isConstant() && timeZone.isConstant()) {
			LocalTime constantResult = compute(ExpressionParserUtils.getZonedDateTime(instant, timeZone));
			return () -> constantResult;
		}
		return () -> compute(ExpressionParserUtils.getZonedDateTime(instant, timeZone));
	}

	private LocalTime compute(ZonedDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.toLocalTime();
	}

}
