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


/**
 * A {@link ExpressionExceptionWrapper} that indicates fatal errors like NullPointers (usually a bug).
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class FatalExpressionExceptionWrapper extends ExpressionExceptionWrapper {

	private static final long serialVersionUID = 5359130447884474741L;

	/**
	 * Creates and wraps a new fatal expression exception that informs the user that a fatal error has occurred while
	 * parsing. Adds the causing exceptions name and message to the error message.
	 *
	 * @param e
	 * 		the causing exception (NullPointer etc.) - not {@code null}
	 */
	public FatalExpressionExceptionWrapper(Exception e) {
		super(new FatalExpressionException(e));
	}

	/**
	 * Creates an expression exception with message associated to the i18n and the arguments.
	 *
	 * @param i18n
	 * 		the i18n error key
	 * @param arguments
	 * 		the i18n arguments
	 */
	public FatalExpressionExceptionWrapper(String i18n, Object... arguments) {
		super(new FatalExpressionException(i18n, arguments));
	}

	/**
	 * Creates a parsing exception with message associated to the i18n and the arguments and stores the error line taken
	 * from the context.
	 *
	 * @param ctx
	 * 		the error context
	 * @param i18n
	 * 		the i18n error key
	 * @param arguments
	 * 		the i18n arguments
	 */
	public FatalExpressionExceptionWrapper(ParserRuleContext ctx, String i18n, Object... arguments) {
		super(new FatalExpressionException(ctx, i18n, arguments));
	}

}
