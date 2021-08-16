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

import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;


/**
 * A {@link ExpressionExceptionWrapper} that is thrown when the function {@link AttributeEvaluation} fails to
 * find the attribute specified by the inner expression.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class AttributeEvaluationExceptionWrapper extends ExpressionExceptionWrapper {

	private static final long serialVersionUID = -7644715146786931281L;

	/**
	 * Creates a {@link AttributeEvaluationExceptionWrapper} with a message generated from functionName and subExpression.
	 *
	 * @param functionName
	 *            the name of the {@link AttributeEvaluation} function
	 * @param subExpression
	 *            the subexpression for which the {@link AttributeEvaluation} function failed
	 */
	AttributeEvaluationExceptionWrapper(String functionName, String subExpression) {
		super(new AttributeEvaluationException(functionName, subExpression));
	}

}
