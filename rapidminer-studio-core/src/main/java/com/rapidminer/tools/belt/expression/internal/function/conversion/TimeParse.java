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
import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FatalExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionEvaluatorFactory;
import com.rapidminer.tools.belt.expression.internal.function.AbstractFunction;


/**
 * Parses nanoseconds since midnight to a LocalTime.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TimeParse extends AbstractFunction {

	public TimeParse() {
		super("conversion.time_parse", 1, ExpressionType.LOCAL_TIME);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 1) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input",
					getFunctionName(), 1, inputEvaluators.length);
		}
		getResultType(inputEvaluators);

		ExpressionEvaluator input = inputEvaluators[0];

		return ExpressionEvaluatorFactory.ofLocalTime(makeLocalTimeCallable(input), isResultConstant(inputEvaluators));
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType input = inputTypes[0];
		if (input == ExpressionType.DOUBLE || input == ExpressionType.INTEGER) {
			return ExpressionType.LOCAL_TIME;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type",
					getFunctionName(), "numerical");
		}
	}

	/**
	 * Builds a LocalTime Callable from the given input evaluator representing nanoseconds since midnight
	 *
	 * @param inputEvaluator
	 * 		the nanoseconds since midnight
	 * @return the resulting Callable<LocalTime>
	 */
	private Callable<LocalTime> makeLocalTimeCallable(final ExpressionEvaluator inputEvaluator) {
		try {
			if (inputEvaluator.isConstant()) {
				final LocalTime constantResult = compute(inputEvaluator.getDoubleFunction().call());
				return () -> constantResult;
			} else {
				return () -> compute(inputEvaluator.getDoubleFunction().call());
			}
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	/**
	 * @return the corresponding LocalTime for the given nanoseconds midnight
	 */
	private LocalTime compute(double nanoOfDay) {
		if (Double.isNaN(nanoOfDay)) {
			return null;
		}
		try {
			return LocalTime.ofNanoOfDay((long) nanoOfDay);
		} catch (DateTimeException e) {
			throw new FunctionInputExceptionWrapper(e.getMessage());
		}
	}

}
