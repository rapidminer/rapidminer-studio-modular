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

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;


/**
 * Interface for an expression generated by an {@link ExpressionParser}. Please note that the default implementation is
 * not thread safe.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public interface Expression {

	/**
	 * Returns the {@link ExpressionType} of the expression. The expression type indicates the result type when
	 * evaluating the expression.
	 *
	 * @return the expression type of the expression
	 */
	ExpressionType getExpressionType();

	/**
	 * If the type of the expression is {@link ExpressionType#STRING} or {@link ExpressionType#BOOLEAN} returns the
	 * String result of the evaluation. The return value can be {@code null} indicating a missing nominal value. Check
	 * the expression type using {@link #getExpressionType()} before calling this method.
	 *
	 * @return the nominal result of the evaluation if the expression is nominal. Can return {@code null}.
	 * @throws ExpressionException
	 * 		if the evaluation failed. The ExpressionException can contain a {@link ExpressionExceptionWrapper} as cause.
	 * 		see the java doc of {@link ExpressionExceptionWrapper} for different marker subclasses of special error cases.
	 */
	String evaluateNominal() throws ExpressionException;

	/**
	 * If the type of the expression is {@link ExpressionType#DOUBLE} or {@link ExpressionType#INTEGER} returns the
	 * double result of the evaluation. Check the expression type using {@link #getExpressionType()} before calling this
	 * method.
	 *
	 * @return the numerical result of the evaluation if the expression is numerical
	 * @throws ExpressionException
	 * 		if the evaluation failed. The ExpressionException can contain a {@link ExpressionExceptionWrapper} as cause.
	 * 		see the java doc of {@link ExpressionExceptionWrapper} for different marker subclasses of special error cases.
	 */
	double evaluateNumerical() throws ExpressionException;

	/**
	 * If the type of the expression is {@link ExpressionType#INSTANT} returns the Instant result of the evaluation. The
	 * return value can be {@code null} indicating a missing instant value. Check the expression type using {@link
	 * #getExpressionType()} before calling this method.
	 *
	 * @return the instant result of the evaluation if the expression is an instant expression, can return {@code null}
	 * @throws ExpressionException
	 * 		if the evaluation failed. The ExpressionException can contain a {@link ExpressionExceptionWrapper} as cause.
	 * 		see the java doc of {@link ExpressionExceptionWrapper} for different marker subclasses of special error cases.
	 */
	Instant evaluateInstant() throws ExpressionException;

	/**
	 * If the type of the expression is {@link ExpressionType#LOCAL_TIME} returns the LocalTime result of the evaluation.
	 * The return value can be {@code null} indicating a missing time value. Check the expression type using {@link
	 * #getExpressionType()} before calling this method.
	 *
	 * @return the instant result of the evaluation if the expression is a local time expression, can return {@code
	 * null}
	 * @throws ExpressionException
	 * 		if the evaluation failed. The ExpressionException can contain a {@link ExpressionExceptionWrapper} as cause.
	 * 		see the java doc of {@link ExpressionExceptionWrapper} for different marker subclasses of special error cases.
	 */
	LocalTime evaluateLocalTime() throws ExpressionException;

	/**
	 * If the type of the expression is {@link ExpressionType#STRING_SET} returns the StringSet result of the
	 * evaluation. The return value can be {@code null} indicating a missing string set value. Check the expression type
	 * using {@link #getExpressionType()} before calling this method.
	 *
	 * @return the string set result of the evaluation if the expression is a string set expression, can return {@code
	 * null}
	 * @throws ExpressionException
	 * 		if the evaluation failed. The ExpressionException can contain a {@link ExpressionExceptionWrapper} as cause.
	 * 		see the java doc of {@link ExpressionExceptionWrapper} for different marker subclasses of special error cases.
	 */
	StringSet evaluateStringSet() throws ExpressionException;

	/**
	 * If the type of the expression is {@link ExpressionType#STRING_LIST} returns the StringList result of the
	 * evaluation. The return value can be {@code null} indicating a missing string list value. Check the expression
	 * type using {@link #getExpressionType()} before calling this method.
	 *
	 * @return the string list result of the evaluation if the expression is a string list expression, can return {@code
	 * null}
	 * @throws ExpressionException
	 * 		if the evaluation failed. The ExpressionException can contain a {@link ExpressionExceptionWrapper} as cause.
	 * 		see the java doc of {@link ExpressionExceptionWrapper} for different marker subclasses of special error cases.
	 */
	StringList evaluateStringList() throws ExpressionException;

	/**
	 * If the type of the expression is {@link ExpressionType#BOOLEAN} returns the Boolean result of the evaluation. The
	 * return value can be {@code null} indicating a missing value. Check the expression type using {@link
	 * #getExpressionType()} before calling this method.
	 *
	 * @return the Boolean result of the evaluation if the expression is nominal, can return {@code null}
	 * @throws ExpressionException
	 * 		if the evaluation failed. The ExpressionException can contain a {@link ExpressionExceptionWrapper} as cause.
	 * 		see the java doc of {@link ExpressionExceptionWrapper} for different marker subclasses of special error cases.
	 */
	Boolean evaluateBoolean() throws ExpressionException;
}