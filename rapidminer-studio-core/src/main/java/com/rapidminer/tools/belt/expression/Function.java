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
package com.rapidminer.tools.belt.expression;

import java.util.concurrent.Callable;

/**
 * Class for functions that can be used inside expressions. Functions are registered in {@link ExpressionParserModule}s
 * which can be used in the {@link ExpressionParserBuilder} via {@link ExpressionParserBuilder#withModule(ExpressionParserModule)}
 * or {@link ExpressionParserBuilder#withModules(java.util.List)}. The module containing the {@link Function} can be
 * registered via the {@link ExpressionRegistry} such that the functions are used in the standard core operators that
 * use an {@link ExpressionParser}.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public interface Function {

	/**
	 * @return the {@link FunctionDescription} of the function
	 */
	FunctionDescription getFunctionDescription();

	/**
	 * @return the function name of the function
	 */
	String getFunctionName();

	/**
	 * Creates an {@link ExpressionEvaluator} for this function with the given inputEvaluators as
	 * arguments.
	 *
	 * @param stopChecker
	 * 		checks for stop and may throw an exception to indicate to stop the compuatation
	 * @param inputEvaluators
	 * 		the {@link ExpressionEvaluator ExpressionEvaluators} containing the input arguments
	 * @return an expression evaluator for this function applied to the inputEvaluators
	 * @throws ExpressionExceptionWrapper
	 * 		if the creation of the ExpressionEvaluator fails, FunctionInputException if the
	 * 		cause for the failure is a wrong argument
	 * @since 9.6.0
	 */
	ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context, ExpressionEvaluator... inputEvaluators);

}
