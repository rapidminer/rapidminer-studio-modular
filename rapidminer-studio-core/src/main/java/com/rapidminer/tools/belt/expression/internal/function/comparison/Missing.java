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

import java.time.Instant;
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
 * Class for the MISSING function that has 1 arbitrary input
 *
 * @author Sabrina Kirstein
 * @since 9.11
 */
public class Missing extends AbstractFunction {

	/**
	 * Constructs a MISSING Function with 1 parameter with {@link FunctionDescription}
	 */
	public Missing() {
		super("comparison.missing", 1, ExpressionType.BOOLEAN);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {

		if (inputEvaluators.length != 1) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), "1",
					inputEvaluators.length);
		}
		getResultType(inputEvaluators);
		ExpressionEvaluator evaluator = inputEvaluators[0];

		return ExpressionEvaluatorFactory.ofBoolean(makeBooleanCallable(evaluator), isResultConstant(inputEvaluators));
	}

	/**
	 * Builds a boolean callable from a {@link ExpressionEvaluator}, where constant results are
	 * evaluated.
	 *
	 * @param evaluator
	 * @return the resulting boolean callable
	 */
	protected Callable<Boolean> makeBooleanCallable(ExpressionEvaluator evaluator) {
		try {
			// act depending on the type of the given evaluator
			switch (evaluator.getType()) {
				case INTEGER:
				case DOUBLE:
					final DoubleCallable funcDouble = evaluator.getDoubleFunction();
					final double valueDouble = evaluator.isConstant() ? funcDouble.call() : Double.NaN;
					if (evaluator.isConstant()) {
						final Boolean result = compute(valueDouble);
						return () -> result;
					} else {
						return () -> compute(funcDouble.call());
					}
				case STRING:
					final Callable<String> funcString = evaluator.getStringFunction();
					final String valueString = evaluator.isConstant() ? funcString.call() : null;
					if (evaluator.isConstant()) {
						final Boolean result = compute(valueString);
						return () -> result;
					} else {
						return () -> compute(funcString.call());
					}
				case INSTANT:
					final Callable<Instant> funcInstant = evaluator.getInstantFunction();
					final Instant instantValue = evaluator.isConstant() ? funcInstant.call() : null;
					if (evaluator.isConstant()) {
						final Boolean result = compute(instantValue);
						return new Callable<Boolean>() {

							@Override
							public Boolean call() throws Exception {
								return result;
							}
						};
					} else {
						return () -> compute(funcInstant.call());
					}
				case BOOLEAN:
					final Callable<Boolean> funcBoolean = evaluator.getBooleanFunction();
					final Boolean valueBoolean = evaluator.isConstant() ? funcBoolean.call() : null;
					if (evaluator.isConstant()) {
						final Boolean result = compute(valueBoolean);
						return () -> result;
					} else {
						return () -> compute(funcBoolean.call());
					}
				default:
					return null;
			}

		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		// has to be one argument
		if (inputTypes.length != 1) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), "1",
					inputTypes.length);
		}
		// result is always boolean
		return ExpressionType.BOOLEAN;
	}

	/**
	 * Computes the result for a double value.
	 *
	 * @param value
	 * @return the result of the computation.
	 */
	protected Boolean compute(double value) {
		return Double.isNaN(value);
	}

	/**
	 * Computes the result for a boolean value.
	 *
	 * @param value
	 * @return the result of the computation.
	 */
	protected Boolean compute(Boolean value) {
		return value == null;
	}

	/**
	 * Computes the result for a String value.
	 *
	 * @param value
	 * @return the result of the computation.
	 */
	protected Boolean compute(String value) {
		return value == null;
	}

	/**
	 * Computes the result for a Date value.
	 *
	 * @param value
	 * @return the result of the computation.
	 */
	protected Boolean compute(Instant value) {
		return value == null;
	}

	// TODO add new types
}
