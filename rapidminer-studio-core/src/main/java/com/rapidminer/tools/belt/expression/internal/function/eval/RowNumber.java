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
package com.rapidminer.tools.belt.expression.internal.function.eval;

import java.util.concurrent.Callable;

import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionEvaluatorFactory;
import com.rapidminer.tools.belt.expression.internal.function.AbstractFunction;


/**
 * Function that returns the current row number (the range is {@code [1,table_length]}).
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class RowNumber extends AbstractFunction {

	/**
	 * Creates a new instance of the function.
	 */
	public RowNumber() {
		super("process.row_number", 0, ExpressionType.INTEGER);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		if (inputTypes != null && inputTypes.length > 0) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 0, inputTypes.length);
		}
		return ExpressionType.INTEGER;
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {
		DoubleCallable callable = () -> context.getIndex() + 1;
		return ExpressionEvaluatorFactory.ofDouble(callable, false, ExpressionType.INTEGER);
	}

	@Override
	protected boolean isConstantOnConstantInput() {
		return false;
	}
}
