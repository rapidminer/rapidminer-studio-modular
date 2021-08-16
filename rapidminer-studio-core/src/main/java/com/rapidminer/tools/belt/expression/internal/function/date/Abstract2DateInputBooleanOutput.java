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
 * Abstract class for a Function that has two date-time arguments and returns a boolean.
 *
 * @author David Arnu, Kevin Majchrzak
 * @since 9.11
 */
public abstract class Abstract2DateInputBooleanOutput extends AbstractFunction {

	public Abstract2DateInputBooleanOutput(String i18nKey) {
		super(i18nKey, 2, ExpressionType.BOOLEAN);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 2) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 2,
					inputEvaluators.length);
		}
		getResultType(inputEvaluators);

		ExpressionEvaluator left = inputEvaluators[0];
		ExpressionEvaluator right = inputEvaluators[1];

		try {
			if (ExpressionParserUtils.containsConstantMissing(Arrays.asList(inputEvaluators))) {
				// the result will always be null, therefore, early exit
				return ExpressionEvaluatorFactory.ofBoolean(null);
			}
			return ExpressionEvaluatorFactory.ofBoolean(makeBooleanCallable(left, right), isResultConstant(inputEvaluators));
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (DateTimeException e) {
			throw new FunctionInputExceptionWrapper(e.getMessage());
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	/**
	 * Builds a DoubleCallable from left and right using {@link #compute(Instant, Instant)}, where constant child
	 * results are evaluated.
	 *
	 * @param left
	 * 		the left input
	 * @param right
	 * 		the right input
	 * @return the resulting DoubleCallable
	 */
	private Callable<Boolean> makeBooleanCallable(ExpressionEvaluator left,
												  ExpressionEvaluator right) throws Exception {
		if (left.isConstant() && right.isConstant()) {
			Boolean constantResult = compute(left.getInstantFunction().call(),
					right.getInstantFunction().call());
			return () -> constantResult;
		} else if (left.isConstant()) {
			Instant constantLeft = left.getInstantFunction().call();
			return () -> compute(constantLeft, right.getInstantFunction().call());
		} else if (right.isConstant()) {
			Instant constantRight = right.getInstantFunction().call();
			return () -> compute(left.getInstantFunction().call(), constantRight);
		} else {
			return () -> compute(left.getInstantFunction().call(),
					right.getInstantFunction().call());
		}
	}

	/**
	 * Computes the result for two input instant values.
	 */
	protected abstract Boolean compute(Instant left, Instant right);

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType left = inputTypes[0];
		ExpressionType right = inputTypes[1];
		if (left == ExpressionType.INSTANT && right == ExpressionType.INSTANT) {
			return ExpressionType.BOOLEAN;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type", getFunctionName(), "date-time");
		}
	}

}
