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

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;


/**
 * Resolver for dynamic (non-constant) variables. The most common implementation ({@link TableResolver}) maps dynamic
 * variables to table columns.
 *
 * @author Kevin Majchrzak
 * @see TableResolver
 * @since 9.11
 */
public interface DynamicResolver {

	/**
	 * Returns the {@link FunctionInput}s of all variables known to this resolver.
	 *
	 * @return the {@link FunctionInput}s of all known variables
	 */
	Collection<FunctionInput> getAllVariables();

	/**
	 * Returns the {@link ExpressionType} of the variable with name variableName or {@code null} if this variable does
	 * not exist.
	 *
	 * @param variableName
	 * 		the name of the variable
	 * @return the type of the variable variableName or {@code null}
	 */
	ExpressionType getVariableType(String variableName);

	/**
	 * Returns the String value of the variable variableName, if this variable has a String value. Check the expression
	 * type of the variable using {@link #getVariableType(String)} before calling this method.
	 *
	 * @param variableName
	 * 		the name of the variable
	 * @return the String value of the variable variableName, if this variable has a String value
	 * @throws IllegalStateException
	 * 		if the variable is not of type {@link ExpressionType#STRING}
	 */
	String getStringValue(String variableName, int index);

	/**
	 * Returns the double value of the variable variableName, if this variable has a double value. Check the expression
	 * type of the variable using {@link #getVariableType(String)} before calling this method.
	 *
	 * @param variableName
	 * 		the name of the variable
	 * @return the double value of the variable variableName, if this variable has a double value
	 * @throws IllegalStateException
	 * 		if the variable is not of type {@link ExpressionType#INTEGER} or {@link ExpressionType#DOUBLE}
	 */
	double getDoubleValue(String variableName, int index);

	/**
	 * Returns the Instant value of the variable variableName, if this variable has an instant value. Check the
	 * expression type of the variable using {@link #getVariableType(String)} before calling this method.
	 *
	 * @param variableName
	 * 		the name of the variable
	 * @return the Instant value of the variable variableName, if this variable has an Instant value
	 * @throws IllegalStateException
	 * 		if the variable is not of type {@link ExpressionType#INSTANT}
	 */
	Instant getInstantValue(String variableName, int index);

	/**
	 * Returns the LocalTime value of the variable variableName, if this variable has a LocalTime value. Check the
	 * expression type of the variable using {@link #getVariableType(String)} before calling this method.
	 *
	 * @param variableName
	 * 		the name of the variable
	 * @return the LocalTime value of the variable variableName, if this variable has a LocalTime value
	 * @throws IllegalStateException
	 * 		if the variable is not of type {@link ExpressionType#LOCAL_TIME}
	 */
	LocalTime getLocalTimeValue(String variableName, int index);

	/**
	 * Returns the StringSet value of the variable variableName, if this variable has a StringSet value. Check the
	 * expression type of the variable using {@link #getVariableType(String)} before calling this method.
	 *
	 * @param variableName
	 * 		the name of the variable
	 * @return the StringSet value of the variable variableName, if this variable has a StringSet value
	 * @throws IllegalStateException
	 * 		if the variable is not of type {@link ExpressionType#STRING_SET}
	 */
	StringSet getStringSetValue(String variableName, int index);

	/**
	 * Returns the StringList value of the variable variableName, if this variable has a StringList value. Check the
	 * expression type of the variable using {@link #getVariableType(String)} before calling this method.
	 *
	 * @param variableName
	 * 		the name of the variable
	 * @return the StringList value of the variable variableName, if this variable has a StringList value
	 * @throws IllegalStateException
	 * 		if the variable is not of type {@link ExpressionType#STRING_LIST}
	 */
	StringList getStringListValue(String variableName, int index);

}
