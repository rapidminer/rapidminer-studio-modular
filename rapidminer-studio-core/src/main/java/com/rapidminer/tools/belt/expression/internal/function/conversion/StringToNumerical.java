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

import java.util.concurrent.Callable;

import com.rapidminer.tools.Tools;
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
 *
 * A Function parsing a string to a number.
 *
 * @author Marcel Seifert
 * @since 9.11
 */
public class StringToNumerical extends AbstractFunction {

	/**
	 * {@link NumericalToString} converts infinitys to a symbol, determined by
	 * {@link Tools#FORMAT_SYMBOLS}. Use those strings to allow to recognize them when converting
	 * back.
	 */
	private static final String POSITIVE_INFINITY_STRING = Tools.FORMAT_SYMBOLS.getInfinity();
	private static final String NEGATIVE_INFINITY_STRING = Tools.FORMAT_SYMBOLS.getMinusSign()
			+ Tools.FORMAT_SYMBOLS.getInfinity();

	/**
	 * Constructs an AbstractFunction with {@link FunctionDescription} generated from the arguments
	 * and the function name generated from the description.
	 */
	public StringToNumerical() {
		super("conversion.parse", 1, ExpressionType.DOUBLE);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 1) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 1,
					inputEvaluators.length);
		}
		ExpressionType type = getResultType(inputEvaluators);

		ExpressionEvaluator input = inputEvaluators[0];

		return ExpressionEvaluatorFactory.ofDouble(makeDoubleCallable(input), isResultConstant(inputEvaluators), type);
	}

	/**
	 * Builds a DoubleCallable from one String input argument
	 *
	 * @param inputEvaluator
	 *            the input
	 * @return the resulting callable<String>
	 */
	protected DoubleCallable makeDoubleCallable(final ExpressionEvaluator inputEvaluator) {
		final Callable<String> func = inputEvaluator.getStringFunction();

		try {
			if (inputEvaluator.isConstant()) {
				final double result = compute(func.call());
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
	 * Computes the result for one input String value.
	 *
	 * @param value
	 *            the string to parse
	 *
	 * @return the result of the computation.
	 */
	protected double compute(String value) {
		if (value != null) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				// check if the value is positive or negative infinity as coming from {@link NumericalToString}
				if (POSITIVE_INFINITY_STRING.equals(value)) {
					return Double.POSITIVE_INFINITY;
				} else if (NEGATIVE_INFINITY_STRING.equals(value)) {
					return Double.NEGATIVE_INFINITY;
				}
				return Double.NaN;
			}
		}
		return Double.NaN;
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType input = inputTypes[0];
		if (input == ExpressionType.STRING) {
			return ExpressionType.DOUBLE;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type", getFunctionName(), "nominal");
		}
	}

}
