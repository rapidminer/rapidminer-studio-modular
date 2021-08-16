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
package com.rapidminer.tools.belt.expression.internal.function.basic;

import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.function.Abstract2DoubleInputFunction;


/**
 * A {@link @link com.rapidminer.tools.belt.expression.Function} for division.
 *
 * @author Gisa Meier
 * @since 9.11
 */
public class Divide extends Abstract2DoubleInputFunction {

	/**
	 * Constructs a division function.
	 */
	public Divide() {
		super("basic.division", 2, ExpressionType.DOUBLE);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType left = inputTypes[0];
		ExpressionType right = inputTypes[1];
		if ((left == ExpressionType.INTEGER || left == ExpressionType.DOUBLE)
				&& (right == ExpressionType.INTEGER || right == ExpressionType.DOUBLE)) {
			// division has return type double even if both inputs are integers
			return ExpressionType.DOUBLE;
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type", getFunctionName(), "numerical");
		}
	}

	@Override
	protected double compute(double value1, double value2) {
		return value1 / value2;
	}

}
