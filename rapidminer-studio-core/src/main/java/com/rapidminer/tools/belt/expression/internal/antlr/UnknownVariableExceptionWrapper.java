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
package com.rapidminer.tools.belt.expression.internal.antlr;

import org.antlr.v4.runtime.ParserRuleContext;

import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;


/**
 * A {@link ExpressionExceptionWrapper} that is thrown when a variable (used as 'variable' in the expression) is
 * unknown.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class UnknownVariableExceptionWrapper extends ExpressionExceptionWrapper {

	private static final long serialVersionUID = -2213254062598487753L;

	/**
	 * Creates a parsing exception with message associated to the i18n and the arguments.
	 *
	 * @param i18n
	 * 		the i18n error key
	 * @param arguments
	 * 		the i18n arguments
	 */
	UnknownVariableExceptionWrapper(String i18n, Object... arguments) {
		super(new UnknownVariableException(i18n, arguments));
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
	UnknownVariableExceptionWrapper(ParserRuleContext ctx, String i18n, Object... arguments) {
		super(new UnknownVariableException(ctx, i18n, arguments));
	}

}
