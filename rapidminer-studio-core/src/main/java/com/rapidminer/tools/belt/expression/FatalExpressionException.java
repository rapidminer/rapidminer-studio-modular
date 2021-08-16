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


import org.antlr.v4.runtime.ParserRuleContext;

import com.rapidminer.tools.ValidationUtilV2;


/**
 * An {@link ExpressionException} that indicates fatal errors like NullPointers (usually a bug).
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class FatalExpressionException extends ExpressionException {

	private static final long serialVersionUID = 5359130447884474741L;

	/**
	 * Creates and a new fatal expression exception that informs the user that a fatal error has occurred while parsing.
	 * Adds the causing exceptions name and message to the error message.
	 *
	 * @param e
	 * 		the causing exception (NullPointer etc.) - not {@code null}
	 */
	public FatalExpressionException(Exception e) {
		this("expression_parser.fatal_error", ValidationUtilV2.requireNonNull(e, "Exception").getClass().getName(),
				ValidationUtilV2.requireNonNull(e, "Exception").getMessage());
	}

	/**
	 * Creates a new fatal expression exception.
	 *
	 * @param i18n
	 * 		the i18n error key
	 * @param arguments
	 * 		the i18n arguments
	 */
	public FatalExpressionException(String i18n, Object... arguments) {
		super(i18n, arguments);
	}

	/**
	 * Creates a new fatal expression exception.
	 *
	 * @param ctx
	 * 		the error context
	 * @param i18n
	 * 		the i18n error key
	 * @param arguments
	 * 		the i18n arguments
	 */
	public FatalExpressionException(ParserRuleContext ctx, String i18n, Object... arguments) {
		super(ctx, i18n, arguments);
	}

}
