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
package com.rapidminer.tools.belt.expression.internal.function.rounding;

import java.util.concurrent.Callable;

import com.rapidminer.tools.Ontology;
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
 * Abstract class for a {@link com.rapidminer.tools.belt.expression.Function} that has either one or two double
 * arguments.
 *
 * @author David Arnu
 * @since 9.11
 */
public abstract class Abstract1or2DoubleInputFunction extends Abstract2DoubleInputFunction {

	/**
	 * Constructs an AbstractFunction with {@link FunctionDescription} generated from the arguments
	 * and the function name generated from the description.
	 *
	 * @param i18n
	 *            the key for the {@link FunctionDescription}. The functionName is read from
	 *            "gui.dialog.function.i18nKey.name", the helpTextName from ".help", the groupName
	 *            from ".group", the description from ".description" and the function with
	 *            parameters from ".parameters". If ".parameters" is not present, the ".name" is
	 *            taken for the function with parameters.
	 * @param returnType
	 *            the {@link Ontology#ATTRIBUTE_VALUE_TYPE}
	 */
	public Abstract1or2DoubleInputFunction(String i18n, ExpressionType returnType) {
		super(i18n, FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, returnType);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length == 2) {
			ExpressionType type = getResultType(inputEvaluators);

			ExpressionEvaluator left = inputEvaluators[0];
			ExpressionEvaluator right = inputEvaluators[1];

			return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(left, right), isResultConstant(inputEvaluators), type);
		} else if (inputEvaluators.length == 1) {
			ExpressionType type = getResultType(inputEvaluators);
			ExpressionEvaluator input = inputEvaluators[0];
			return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(input), isResultConstant(inputEvaluators), type);

		}
		throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input_two", getFunctionName(), 1, 2,
				inputEvaluators.length);
	}

	/**
	 * Builds a double callable from a single input {@link #compute(double)}, where constant child
	 * results are evaluated.
	 *
	 * @param input
	 *            the input
	 * @return the resulting double callable
	 */
	private DoubleCallable makeDoubleCallable(ExpressionEvaluator input) {
		final DoubleCallable func = input.getDoubleFunction();

		try {
			final double value = input.isConstant() ? func.call() : Double.NaN;
			if (input.isConstant()) {
				final double result = compute(value);
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
	 * Computes the value a single input argument
	 *
	 * @param value
	 * @return the result of the computation
	 */
	protected abstract double compute(double value);

}
