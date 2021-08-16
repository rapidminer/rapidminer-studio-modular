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
package com.rapidminer.operator.learner.functions;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.tools.belt.expression.DynamicResolver;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInput;


/**
 * Resolver used to extract numeric function parameters from a given (expression parser) expression. Add this resolver
 * last to the expression parser. Then, after parsing the expression with this resolver the method {@link
 * #getParameterNames()} returns the names of all numeric variables that could not be resolved by other resolvers.
 * <p>
 * Please note: This class is in a beta state and may change in future releases.
 *
 * @author Kevin Majchrzak
 * @since 9.10
 */
class NumericParameterExtractor implements DynamicResolver {

	/**
	 * All parameter names that could not be resolved otherwise are collected here.
	 */
	private final Set<String> parameterNames;

	/**
	 * Creates a new instance with and empty list of parameter names. Parse an expression using this resolver before
	 * calling {@link #getParameterNames()}.
	 */
	public NumericParameterExtractor() {
		parameterNames = new HashSet<>();
	}

	/**
	 * @return an immutable empty set.
	 */
	@Override
	public Collection<FunctionInput> getAllVariables() {
		return Collections.emptySet();
	}

	/**
	 * Always returns {@link ExpressionType#DOUBLE}.
	 */
	@Override
	public ExpressionType getVariableType(String variableName) {
		parameterNames.add(variableName);
		return ExpressionType.DOUBLE;
	}

	/**
	 * Always returns {@code 1} to fake a numeric value.
	 */
	@Override
	public double getDoubleValue(String variableName, int index) {
		return 1;
	}

	/**
	 * Always throws an IllegalStateException since this this is not a real resolver. It only fakes numeric values to
	 * extract parameter names.
	 */
	@Override
	public String getStringValue(String variableName, int index) {
		throw wrongTypeException(variableName, "string");
	}

	/**
	 * Always throws an IllegalStateException since this this is not a real resolver. It only fakes numeric values to
	 * extract parameter names.
	 */
	@Override
	public Instant getInstantValue(String variableName, int index) {
		throw wrongTypeException(variableName, "date-time");
	}

	/**
	 * Always throws an IllegalStateException since this this is not a real resolver. It only fakes numeric values to
	 * extract parameter names.
	 */
	@Override
	public LocalTime getLocalTimeValue(String variableName, int index) {
		throw wrongTypeException(variableName, "time");
	}

	/**
	 * Always throws an IllegalStateException since this this is not a real resolver. It only fakes numeric values to
	 * extract parameter names.
	 */
	@Override
	public StringSet getStringSetValue(String variableName, int index) {
		throw wrongTypeException(variableName, "text-set");
	}

	/**
	 * Always throws an IllegalStateException since this this is not a real resolver. It only fakes numeric values to
	 * extract parameter names.
	 */
	@Override
	public StringList getStringListValue(String variableName, int index) {
		throw wrongTypeException(variableName, "text-list");
	}

	/**
	 * @return the function parameter names
	 */
	public String[] getParameterNames() {
		return parameterNames.toArray(new String[0]);
	}

	/**
	 * @param variableName
	 * 		the variable name
	 * @param type
	 * 		the variable type
	 * @return an IllegalStateException (used when the variable type does not match the requested type)
	 */
	private IllegalStateException wrongTypeException(String variableName, String type) {
		return new IllegalStateException("the variable " + variableName + " does not have a " + type + " value");
	}

}
