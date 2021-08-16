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

import com.rapidminer.tools.belt.expression.ExpressionException;


/**
 * A {@link ExpressionException} that is thrown when the function {@link Evaluation} fails to evaluate a
 * subexpression.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class SubexpressionEvaluationException extends ExpressionException {

	private static final long serialVersionUID = -3989637172794153508L;

	/**
	 * Creates a {@link SubexpressionEvaluationException} with the given cause and a message generated from
	 * functionName, subExpression and the message of the cause.
	 *
	 * @param functionName
	 * 		the name of the {@link Evaluation} function
	 * @param subExpression
	 * 		the subexpression for which the {@link Evaluation} function failed
	 * @param cause
	 * 		the cause of the failure
	 */
	SubexpressionEvaluationException(String functionName, String subExpression, ExpressionException cause) {
		super("expression_parser.eval_failed", functionName, subExpression, cause.getMessage());
	}

}
