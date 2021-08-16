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
package com.rapidminer.tools.belt.expression.internal.function.comparison;

import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FatalExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.FunctionDescription;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionEvaluatorFactory;
import com.rapidminer.tools.belt.expression.internal.function.AbstractFunction;


/**
 * A {@link @link com.rapidminer.tools.belt.expression.Function} to check whether the numerical argument is finite or
 * not.
 *
 * @author Thilo Kamradt
 * @since 9.11
 */
public class Finite extends AbstractFunction {

	/**
	 * Constructs the isFinite() function with {@link FunctionDescription}.
	 */
	public Finite() {
		super("comparison.finite", 1, ExpressionType.BOOLEAN);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 1) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 1,
					inputEvaluators.length);
		}
		getResultType(inputEvaluators);

		ExpressionEvaluator argument = inputEvaluators[0];

		return ExpressionEvaluatorFactory.ofBoolean(makeBoolCallable(argument), isResultConstant(inputEvaluators));
	}

	/**
	 * Builds a boolean callable from a single input {@link #compute(double)}, where constant child
	 * results are evaluated.
	 *
	 * @param input
	 *            the input
	 * @return the resulting boolean callable
	 */
	private Callable<Boolean> makeBoolCallable(ExpressionEvaluator input) {
		final DoubleCallable func = input.getDoubleFunction();

		try {
			if (input.isConstant()) {
				final Boolean result = compute(func.call());
				return () -> result;
			} else {
				return () -> compute(func.call());
			}
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	/**
	 * Checks whether the parameter is finite or not.
	 *
	 * @param value
	 *            the variable to check
	 * @return false if value is infinite else true.
	 */
	protected Boolean compute(double value) {
		if (Double.isNaN(value)) {
			return null;
		}
		return !Double.isInfinite(value);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes[0] == ExpressionType.INTEGER || inputTypes[0] == ExpressionType.DOUBLE) {
			return ExpressionType.BOOLEAN;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type", getFunctionName(), "numerical");
		}
	}

}
