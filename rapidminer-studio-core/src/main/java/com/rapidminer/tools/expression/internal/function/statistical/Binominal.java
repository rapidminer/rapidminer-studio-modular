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
package com.rapidminer.tools.expression.internal.function.statistical;

import org.apache.commons.math3.util.CombinatoricsUtils;

import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.FunctionInputException;
import com.rapidminer.tools.expression.internal.function.Abstract2DoubleInputFunction;


/**
 *
 * A {@link Function} for binominal coefficents.
 *
 * @author David Arnu
 *
 * @deprecated since 9.11, see {@link com.rapidminer.tools.belt.expression.ExpressionParser}
 */
@Deprecated
public class Binominal extends Abstract2DoubleInputFunction {

	/**
	 * Constructs a binominal function.
	 */
	public Binominal() {
		super("statistical.binom", 2, Ontology.INTEGER);
	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		ExpressionType left = inputTypes[0];
		ExpressionType right = inputTypes[1];

		if ((left == ExpressionType.INTEGER || left == ExpressionType.DOUBLE) && (right == ExpressionType.INTEGER || right == ExpressionType.DOUBLE)) {
			return ExpressionType.INTEGER;
		} else {
			throw new FunctionInputException("expression_parser.function_wrong_type", getFunctionName(), "double or integer");
		}
	}

	@Override
	protected double compute(double value1, double value2) {

		// special case for handling missing values
		if (Double.isNaN(value1) || Double.isNaN(value2) || Double.isInfinite(value1) || Double.isInfinite(value2)) {
			return Double.NaN;
		}

		int v1 = (int) value1;
		int v2 = (int) value2;

		if (v1 < 0 || v2 < 0) {
			throw new FunctionInputException("expression_parser.function_non_negative", getFunctionName());
		}
		// This is the common definition for the case for k > n.
		if (v2 > v1) {
			return 0;
		} else {
			return CombinatoricsUtils.binomialCoefficientDouble(v1, v2);
		}
	}

}
