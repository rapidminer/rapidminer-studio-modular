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
package com.rapidminer.tools.belt.expression.internal.function;

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


/**
 * Abstract class for a {@link com.rapidminer.tools.belt.expression.Function} that has two double arguments.
 *
 * @author Gisa Schaefer
 * @since 9.11
 */
public abstract class Abstract2DoubleInputFunction extends AbstractFunction {

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
	 * @param numberOfArgumentsToCheck
	 *            the fixed number of parameters this functions expects or -1
	 * @param returnType
	 *            the {@link Ontology#ATTRIBUTE_VALUE_TYPE}
	 */
	public Abstract2DoubleInputFunction(String i18n, int numberOfArgumentsToCheck, ExpressionType returnType) {
		super(i18n, numberOfArgumentsToCheck, returnType);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 2) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 2,
					inputEvaluators.length);
		}
		ExpressionType type = getResultType(inputEvaluators);

		ExpressionEvaluator left = inputEvaluators[0];
		ExpressionEvaluator right = inputEvaluators[1];
		return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(left, right), isResultConstant(inputEvaluators), type);
	}

	/**
	 * Builds a double callable from left and right using {@link #compute(double, double)}, where
	 * constant child results are evaluated.
	 *
	 * @param left
	 *            the left input
	 * @param right
	 *            the right input
	 * @return the resulting double callable
	 */
	protected DoubleCallable makeDoubleCallable(ExpressionEvaluator left, ExpressionEvaluator right) {
		final DoubleCallable funcLeft = left.getDoubleFunction();
		final DoubleCallable funcRight = right.getDoubleFunction();
		try {

			final double valueLeft = left.isConstant() ? funcLeft.call() : Double.NaN;
			final double valueRight = right.isConstant() ? funcRight.call() : Double.NaN;

			if (left.isConstant() && right.isConstant()) {
				final double result = compute(valueLeft, valueRight);

				return () -> result;
			} else if (left.isConstant()) {
				return () -> compute(valueLeft, funcRight.call());

			} else if (right.isConstant()) {
				return () -> compute(funcLeft.call(), valueRight);

			} else {
				return () -> compute(funcLeft.call(), funcRight.call());
			}
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	/**
	 * Computes the result for two input double values.
	 *
	 * @param value1
	 * @param value2
	 * @return the result of the computation.
	 */
	protected abstract double compute(double value1, double value2);

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType left = inputTypes[0];
		ExpressionType right = inputTypes[1];
		if (left == ExpressionType.INTEGER && right == ExpressionType.INTEGER) {
			return ExpressionType.INTEGER;
		} else if ((left == ExpressionType.INTEGER || left == ExpressionType.DOUBLE)
				&& (right == ExpressionType.INTEGER || right == ExpressionType.DOUBLE)) {
			return ExpressionType.DOUBLE;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type", getFunctionName(), "numerical");
		}
	}

}
