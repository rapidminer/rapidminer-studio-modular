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

import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.Function;


/**
 * A {@link Function} that evaluates subexpressions as attributes.
 * <p>
 * If the first input of the attribute eval function is constant, the attribute is fixed and a fixed {@link
 * ExpressionEvaluator} for that attribute is constructed. This means that the evaluation of the {@link Expression}s
 * generated from {@code attribute("att"+(4+3))}, {@code attribute("att" + 7)} and {@code attribute("att7")} have the
 * same complexity at evaluation time.
 * <p>
 * If the first argument is not constant, then a second argument is needed to determine the result type and the parser
 * is called every time the resulting {@link Expression} is evaluated. In particular, evaluating {@link Expression}s
 * such as {@code attribute("att"+[att1], REAL)} is slower than the examples above.
 *
 * @author Gisa Meier
 * @since 9.11
 */
public class AttributeEvaluation extends AbstractEvaluation {

	public AttributeEvaluation() {
		super("process.attribute");
	}

	@Override
	protected ExpressionEvaluator compute(ExpressionContext context, String expressionString) {
		ExpressionEvaluator evaluator = context.getDynamicVariable(expressionString);
		if (evaluator == null) {
			throw new AttributeEvaluationExceptionWrapper(getFunctionName(), expressionString);
		}
		return evaluator;
	}
}
