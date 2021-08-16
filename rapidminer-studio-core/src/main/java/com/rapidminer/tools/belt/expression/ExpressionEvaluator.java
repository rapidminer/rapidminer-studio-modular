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
import java.util.concurrent.Callable;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;


/**
 * Interface for a container that provides intermediate results when building an {@link Expression} via an {@link
 * ExpressionParser}. Please note that the default implementation is not thread safe.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public interface ExpressionEvaluator {

	/**
	 * Returns the {@link ExpressionType}.
	 *
	 * @return the type of the expression
	 */
	ExpressionType getType();

	/**
	 * Returns whether the callable this container returns is constant (returns always the same value).
	 *
	 * @return {@code true} if the callable is constant
	 */
	boolean isConstant();

	/**
	 * Returns the stored String callable, can be {@code null} if the expression type is not compatible with a String
	 * callable.
	 *
	 * @return a String callable, can be {@code null}
	 */
	Callable<String> getStringFunction();

	/**
	 * Returns the stored double callable, can be {@code null} if the expression type is not compatible with a double
	 * callable.
	 *
	 * @return a double callable, can be {@code null}
	 */
	DoubleCallable getDoubleFunction();

	/**
	 * Returns the stored Boolean callable, can be {@code null} if the expression type is not compatible with a Boolean
	 * callable.
	 *
	 * @return a Boolean callable, can be {@code null}
	 */
	Callable<Boolean> getBooleanFunction();

	/**
	 * Returns the stored Instant callable. Can be {@code null} if the type is not compatible with an Instant callable.
	 *
	 * @return an Instant callable or {@code null}
	 */
	Callable<Instant> getInstantFunction();

	/**
	 * Returns the stored LocalTime callable. Can be {@code null} if the type is not compatible with a LocalTime
	 * callable.
	 *
	 * @return a LocalTime callable or {@code null}
	 */
	Callable<LocalTime> getLocalTimeFunction();

	/**
	 * Returns the stored StringSet callable. Can be {@code null} if the type is not compatible with a StringSet
	 * callable.
	 *
	 * @return a StringSet callable or {@code null}
	 */
	Callable<StringSet> getStringSetFunction();

	/**
	 * Returns the stored StringList callable. Can be {@code null} if the type is not compatible with a StringList
	 * callable.
	 *
	 * @return a StringList callable or {@code null}
	 */
	Callable<StringList> getStringListFunction();

}
