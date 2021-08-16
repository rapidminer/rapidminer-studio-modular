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
package com.rapidminer.tools.belt.expression.internal.function.basic;

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
import com.rapidminer.tools.belt.expression.internal.function.Abstract2DoubleInputFunction;


/**
 * A {@link @link com.rapidminer.tools.belt.expression.Function} for subtraction.
 *
 * @author Gisa Meier
 * @since 9.11
 */
public class Minus extends Abstract2DoubleInputFunction {

	/**
	 * Constructs a subtraction function.
	 */
	public Minus() {
		super("basic.subtraction", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, ExpressionType.DOUBLE);

	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 2 && inputEvaluators.length != 1) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input_two", getFunctionName(), 1, 2,
					inputEvaluators.length);
		}

		ExpressionType type = getResultType(inputEvaluators);
		DoubleCallable func;

		if (inputEvaluators.length == 1) {
			func = makeDoubleCallable(inputEvaluators[0]);
		} else {
			func = makeDoubleCallable(inputEvaluators[0], inputEvaluators[1]);
		}

		return ExpressionEvaluatorFactory.ofDouble(func, isResultConstant(inputEvaluators), type);
	}

	/**
	 * Creates the callable for one double callable input.
	 *
	 * @param input the input
	 * @return a double callable
	 */
	private DoubleCallable makeDoubleCallable(ExpressionEvaluator input) {
		final DoubleCallable inputFunction = input.getDoubleFunction();
		try {
			if (input.isConstant()) {
				final double inputValue = inputFunction.call();
				final double returnValue = -inputValue;
				return () -> returnValue;
			} else {
				return () -> -inputFunction.call();
			}
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType firstType = inputTypes[0];
		if (firstType == ExpressionType.INTEGER && (inputTypes.length == 1 || inputTypes[1] == ExpressionType.INTEGER)) {
			return ExpressionType.INTEGER;
		} else if ((firstType == ExpressionType.INTEGER || firstType == ExpressionType.DOUBLE)
				&& (inputTypes.length == 1 || inputTypes[1] == ExpressionType.INTEGER || inputTypes[1] == ExpressionType.DOUBLE)) {
			return ExpressionType.DOUBLE;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type", getFunctionName(), "numerical");
		}
	}

	@Override
	protected double compute(double value1, double value2) {
		return value1 - value2;
	}

}
