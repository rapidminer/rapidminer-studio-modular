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
package com.rapidminer.tools.belt.expression.internal;

import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.Callable;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionType;


/**
 * Abstract {@link ExpressionEvaluator} implementation returning {@code null} for all callables. Used by {@link
 * ExpressionEvaluatorFactory}.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
abstract class AbstractExpressionEvaluator implements ExpressionEvaluator {

	private ExpressionType type;
	private boolean isConstant;

	AbstractExpressionEvaluator(ExpressionType type, boolean isConstant){
		this.type = type;
		this.isConstant = isConstant;
	}

	@Override
	public ExpressionType getType() {
		return type;
	}

	@Override
	public boolean isConstant() {
		return isConstant;
	}

	@Override
	public Callable<String> getStringFunction() {
		return null;
	}

	@Override
	public DoubleCallable getDoubleFunction() {
		return null;
	}

	@Override
	public Callable<Boolean> getBooleanFunction() {
		return null;
	}

	@Override
	public Callable<Instant> getInstantFunction() {
		return null;
	}

	@Override
	public Callable<LocalTime> getLocalTimeFunction() {
		return null;
	}

	@Override
	public Callable<StringSet> getStringSetFunction() {
		return null;
	}

	@Override
	public Callable<StringList> getStringListFunction() {
		return null;
	}
}
