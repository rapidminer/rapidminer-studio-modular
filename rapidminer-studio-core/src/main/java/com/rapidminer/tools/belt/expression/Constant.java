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
 * Interface for a constant used inside an expression that is parsed by an {@link ExpressionParser}.
 *
 * @author Gisa Schaefer, Kevin Majchrzak
 * @since 9.11
 */
public interface Constant {

	/**
	 * @return the {@link ExpressionType}
	 */
	ExpressionType getType();

	/**
	 * @return the name
	 */
	String getName();

	/**
	 * @return the string value if the constant has type {@link ExpressionType#STRING}
	 * @throws IllegalStateException
	 * 		if the type is not {@link ExpressionType#STRING}
	 */
	String getStringValue();

	/**
	 * @return the double value if the constant has type {@link ExpressionType#DOUBLE}
	 * @throws IllegalStateException
	 * 		if the type is not {@link ExpressionType#DOUBLE}
	 */
	double getDoubleValue();

	/**
	 * @return the boolean value if the constant has type {@link ExpressionType#BOOLEAN}
	 * @throws IllegalStateException
	 * 		if the type is not {@link ExpressionType#BOOLEAN}
	 */
	boolean getBooleanValue();

	/**
	 * @return the Instant value if the constant has type {@link ExpressionType#INSTANT}
	 * @throws IllegalStateException
	 * 		if the type is not {@link ExpressionType#INSTANT}
	 */
	Instant getInstantValue();

	/**
	 * @return the LocalTime value if the constant has type {@link ExpressionType#LOCAL_TIME}
	 * @throws IllegalStateException
	 * 		if the type is not {@link ExpressionType#LOCAL_TIME}
	 */
	LocalTime getLocalTimeValue();

	/**
	 * @return the StringSet value if the constant has type {@link ExpressionType#STRING_SET}
	 * @throws IllegalStateException
	 * 		if the type is not {@link ExpressionType#STRING_SET}
	 */
	StringSet getStringSetValue();

	/**
	 * @return the StringList value if the constant has type {@link ExpressionType#STRING_LIST}
	 * @throws IllegalStateException
	 * 		if the type is not {@link ExpressionType#STRING_LIST}
	 */
	StringList getStringListValue();

	/**
	 * Returns the annotation of this constant, for example a description of a constant or where it is used.
	 *
	 * @return the annotation
	 */
	String getAnnotation();

	/**
	 * @return if this {@link Constant} should be visible in the UI
	 */
	boolean isInvisible();
}
