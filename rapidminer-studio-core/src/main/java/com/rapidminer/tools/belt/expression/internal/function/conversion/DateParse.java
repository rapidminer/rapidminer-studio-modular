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

import java.time.Instant;
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
 * Parses (potentially negative) milliseconds since epoch to an Instant.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateParse extends AbstractFunction {

	public DateParse() {
		super("conversion.date_time_parse", 1, ExpressionType.INSTANT);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 1) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 1,
					inputEvaluators.length);
		}
		getResultType(inputEvaluators);

		ExpressionEvaluator input = inputEvaluators[0];

		return ExpressionEvaluatorFactory.ofInstant(makeInstantCallable(input), isResultConstant(inputEvaluators));
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType input = inputTypes[0];
		if (input == ExpressionType.DOUBLE || input == ExpressionType.INTEGER) {
			return ExpressionType.INSTANT;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type",
					getFunctionName(), "numerical");
		}
	}

	/**
	 * Builds an Instance Callable from the given input evaluator representing millis since epoch
	 *
	 * @param inputEvaluator
	 * 		the millis since epoch
	 * @return the resulting Callable<Instant>
	 */
	private Callable<Instant> makeInstantCallable(final ExpressionEvaluator inputEvaluator) {
		try {
			if (inputEvaluator.isConstant()) {
				final Instant constantResult = compute(inputEvaluator.getDoubleFunction().call());
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
	 * @return the corresponding Instant for the given milliseconds since epoch
	 */
	private Instant compute(double epochMillis) {
		if (Double.isNaN(epochMillis)) {
			return null;
		}
		return Instant.ofEpochMilli((long) epochMillis);
	}

}
