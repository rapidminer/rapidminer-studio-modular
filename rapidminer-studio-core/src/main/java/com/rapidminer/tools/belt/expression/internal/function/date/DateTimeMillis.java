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

import java.time.Instant;
import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FatalExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionEvaluatorFactory;
import com.rapidminer.tools.belt.expression.internal.function.AbstractFunction;


/**
 * Returns the time in milliseconds passed since {@link Instant#EPOCH} for a given instant.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateTimeMillis extends AbstractFunction {

	public DateTimeMillis() {
		super("date.date_time_millis", 1, ExpressionType.INTEGER);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 1) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 1,
					inputEvaluators.length);
		}
		ExpressionType resultType = getResultType(inputEvaluators);
		return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(inputEvaluators[0]), isResultConstant(inputEvaluators),
				resultType);
	}

	private DoubleCallable makeDoubleCallable(ExpressionEvaluator instant) {
		try {
			if (instant.isConstant()) {
				final double constantResult = compute(instant.getInstantFunction().call());
				return () -> constantResult;
			} else {
				return () -> compute(instant.getInstantFunction().call());
			}
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	private double compute(Instant valueInstant) {
		if (valueInstant == null) {
			return Double.NaN;
		} else {
			return valueInstant.toEpochMilli();
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes[0] != ExpressionType.INSTANT) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type",
					getFunctionName(), "date-time");
		}
		return ExpressionType.INTEGER;
	}

}
