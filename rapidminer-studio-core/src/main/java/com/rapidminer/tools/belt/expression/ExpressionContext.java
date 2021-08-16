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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.IntSupplier;


/**
 * Interface for a context to evaluate an expression. Stores all {@link Function}s and has access to all variables,
 * dynamic variables, scope constants and the current index (row number). Please note that the default implementation is
 * not thread safe.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public interface ExpressionContext {

	/**
	 * Returns the function with name functionName if it exists or {@code null}.
	 *
	 * @param functionName
	 * 		the name of the function
	 * @return the function with functionName or {@code null}
	 */
	Function getFunction(String functionName);

	/**
	 * Returns the {@link ExpressionEvaluator} for the variable with name variableName if such a variable exists, or
	 * {@code null}.
	 *
	 * @param variableName
	 * 		the name of the variable
	 * @return the {@link ExpressionEvaluator} for variableName or {@code null}
	 */
	ExpressionEvaluator getVariable(String variableName);

	/**
	 * Returns the {@link ExpressionEvaluator} for the dynamic variable (for example a column) with name variableName if
	 * such a dynamic variable exists, or {@code null}.
	 *
	 * @param variableName
	 * 		the name of the dynamic variable
	 * @return the {@link ExpressionEvaluator} for variableName or {@code null}
	 */
	ExpressionEvaluator getDynamicVariable(String variableName);

	/**
	 * Returns the {@link ExpressionEvaluator} for the dynamic variable with the given name and index if such a dynamic
	 * variable exists, {@code null} otherwise.
	 *
	 * @param variableName
	 * 		the name of the dynamic variable
	 * @param indexSupplier
	 * 		the index (row number) of the dynamic variable
	 * @return the expression evaluator or {@code null}
	 */
	ExpressionEvaluator getDynamicVariable(String variableName, IntSupplier indexSupplier);

	/**
	 * Sets the current index (row number). The index will influence the result of some functions (e.g. {@link
	 * com.rapidminer.tools.belt.expression.internal.function.eval.RowNumber}) and dynamic variables.
	 *
	 * @param index
	 * 		the current index
	 */
	void setIndex(int index);

	/**
	 * Returns the current index (rownumber).
	 *
	 * @return the index
	 */
	int getIndex();

	/**
	 * Returns the type of the dynamic variable with the given name.
	 *
	 * @param variableName
	 * 		the variable name
	 * @return the {@link ExpressionType}
	 */
	ExpressionType getDynamicVariableType(String variableName);

	/**
	 * Returns the {@link ExpressionEvaluator} for the scope constant with name scopeName if such a scope constant
	 * exists, or {@code null}.
	 *
	 * @param scopeName
	 * 		the name of the scope constant
	 * @return the {@link ExpressionEvaluator} for scopeName or {@code null}
	 */
	ExpressionEvaluator getScopeConstant(String scopeName);

	/**
	 * Returns the content of the scope constant with name scopeName as String if such a scope constant exists, or
	 * {@code null}.
	 *
	 * @param scopeName
	 * 		the name of the scope constant
	 * @return the String content of scopeName or {@code null}
	 */
	String getScopeString(String scopeName);

	/**
	 * Returns a the {@link FunctionDescription} for all {@link Function}s known to the context.
	 *
	 * @return the function descriptions for all known functions
	 */
	List<FunctionDescription> getFunctionDescriptions();

	/**
	 * Returns the {@link FunctionInput}s associated to all attributes, variables and macros known by the context.
	 *
	 * @return all known function inputs
	 */
	List<FunctionInput> getFunctionInputs();

	/**
	 * Returns the {@link ExpressionEvaluator} for the constant with name constantName if such a constant exists, or
	 * {@code null}.
	 *
	 * @param constantName
	 * 		the name of the constant
	 * @return the {@link ExpressionEvaluator} for constantName or {@code null}
	 */
	ExpressionEvaluator getConstant(String constantName);

	/**
	 * Returns a stop checker (can be {@code null}).
	 */
	Callable<Void> getStopChecker();

}
