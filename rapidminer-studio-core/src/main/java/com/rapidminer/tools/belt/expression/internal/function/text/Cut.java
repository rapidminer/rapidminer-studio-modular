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
package com.rapidminer.tools.belt.expression.internal.function.text;

import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FatalExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.Function;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionEvaluatorFactory;
import com.rapidminer.tools.belt.expression.internal.function.AbstractFunction;


/**
 * A {@link Function} to cut a nominal value.
 *
 * @author Thilo Kamradt
 * @since 9.11
 */
public class Cut extends AbstractFunction {

	/**
	 * Creates a function to cut a nominal value.
	 */
	public Cut() {
		super("text_transformation.cut", 3, ExpressionType.STRING);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		if (inputEvaluators.length != 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 3,
					inputEvaluators.length);
		}
		getResultType(inputEvaluators);

		ExpressionEvaluator text = inputEvaluators[0];
		ExpressionEvaluator startIndex = inputEvaluators[1];
		ExpressionEvaluator length = inputEvaluators[2];

		return ExpressionEvaluatorFactory.ofString(makeStringCallable(text, startIndex, length),
				isResultConstant(inputEvaluators));
	}

	/**
	 * Builds a DoubleCallable from left and right using {@link #compute(String, String)}, where
	 * constant child results are evaluated.
	 *
	 * @param left
	 *            the left input
	 * @param right
	 *            the right input
	 * @return the resulting DoubleCallable
	 */
	protected Callable<String> makeStringCallable(ExpressionEvaluator text, ExpressionEvaluator startIndex,
			ExpressionEvaluator length) {

		final Callable<String> funcText = text.getStringFunction();
		final DoubleCallable funcIndex = startIndex.getDoubleFunction();
		final DoubleCallable funcLength = length.getDoubleFunction();
		try {

			final String valueText = text.isConstant() ? funcText.call() : "";
			final double valueIndex = startIndex.isConstant() ? funcIndex.call() : Double.NaN;
			final double valueLength = length.isConstant() ? funcLength.call() : Double.NaN;

			// all three expressions are const
			if (text.isConstant() && startIndex.isConstant() && length.isConstant()) {
				final String result = compute(valueText, valueIndex, valueLength);

				return () -> result;
				// two const expressions
			} else if (!text.isConstant() && startIndex.isConstant() && length.isConstant()) {
				return () -> compute(funcText.call(), valueIndex, valueLength);
			} else if (text.isConstant() && !startIndex.isConstant() && length.isConstant()) {
				return () -> compute(valueText, funcIndex.call(), valueLength);
			} else if (text.isConstant() && startIndex.isConstant() && !length.isConstant()) {
				return () -> compute(valueText, valueIndex, funcLength.call());
				// only one expression is const
			} else if (text.isConstant() && !startIndex.isConstant() && !length.isConstant()) {
				return () -> compute(valueText, funcIndex.call(), funcLength.call());
			} else if (!text.isConstant() && startIndex.isConstant() && !length.isConstant()) {
				return () -> compute(funcText.call(), valueIndex, funcLength.call());
			} else if (!text.isConstant() && !startIndex.isConstant() && length.isConstant()) {
				return () -> compute(funcText.call(), funcIndex.call(), valueLength);
			} else {
				// no expression is const
				return () -> compute(funcText.call(), funcIndex.call(), funcLength.call());
			}
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	/**
	 * Computes the result.
	 *
	 * @param text
	 * @param index
	 * @param length
	 * @return the result of the computation.
	 */
	protected String compute(String text, double index, double length) {
		if (Double.isNaN(index)) {
			// this must be changed for compatibility with the old parser
			index = 0;
		}
		if (Double.isNaN(length)) {
			// this must be changed for compatibility with the old parser
			length = 0;
		}
		if (text == null) {
			return null;
		} else if (index < 0 || length < 0) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_non_negative", getFunctionName());
		} else if (index + length > text.length()) {
			throw new FunctionInputExceptionWrapper("expression_parser.parameter_value_too_big", "start", "length",
					getFunctionName(), text);
		}
		return text.substring((int) index, (int) index + (int) length);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes[0] == ExpressionType.STRING
				&& (inputTypes[1] == ExpressionType.INTEGER || inputTypes[1] == ExpressionType.DOUBLE)
				&& (inputTypes[2] == ExpressionType.INTEGER || inputTypes[2] == ExpressionType.DOUBLE)) {
			return ExpressionType.STRING;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type", getFunctionName(),
					"(nominal,integer,integer)");
		}
	}
}
