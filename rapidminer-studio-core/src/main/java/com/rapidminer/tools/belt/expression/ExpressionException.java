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

import com.rapidminer.tools.I18N;


/**
 * Parent class for all exceptions thrown by the {@link ExpressionParser} during expression parsing and evaluation.
 * These exceptions need to be wrapped by {@link ExpressionExceptionWrapper}s because the expression parser architecture
 * can only handle runtime exceptions. The original exception will automatically be unwrapped and thrown by the
 * expression parser.
 *
 * @author Kevin Majchzak
 * @since 9.11
 */
public class ExpressionException extends Exception {

	private static final long serialVersionUID = 1566969757994992388L;
	private final int errorLine;

	/**
	 * Creates an expression exception with message associated to the i18n and the arguments and stores the error
	 * line taken from the given error context.
	 *
	 * @param ctx
	 * 		the error context
	 * @param i18n
	 * 		the i18n error key
	 * @param arguments
	 * 		i18n arguments
	 */
	protected ExpressionException(ParserRuleContext ctx, String i18n, Object... arguments) {
		super(I18N.getErrorMessage(i18n, arguments));
		this.errorLine = ctx == null ? -1 : ctx.getStart().getLine();
	}

	/**
	 * Creates an expression exception with message associated to the i18n and the arguments. Does not store the error
	 * line.
	 *
	 * @param i18n
	 * 		the i18n error key
	 * @param arguments
	 * 		i18n arguments
	 */
	protected ExpressionException(String i18n, Object... arguments) {
		super(I18N.getErrorMessage(i18n, arguments));
		this.errorLine = -1;
	}

	/**
	 * Creates an expression exception with message associated to the i18n and the arguments and stores the error line.
	 *
	 * @param errorLine
	 * 		the line where the error occurred
	 * @param i18n
	 * 		the i18n error key
	 * @param arguments
	 * 		i18n arguments
	 */
	protected ExpressionException(int errorLine, String i18n, Object... arguments) {
		super(I18N.getErrorMessage(i18n, arguments));
		this.errorLine = errorLine;
	}

	/**
	 * @return the line of the error or -1 if the error line is unknown
	 */
	public int getErrorLine() {
		return errorLine;

	}

	/**
	 * Returns only the first sentence of the error message. Does not return where exactly the error lies as {{@link
	 * #getMessage()} may.
	 *
	 * @return the first sentence of the error message
	 */
	public String getShortMessage() {
		String message = super.getMessage();
		return message == null ? null : message.split("\n")[0];
	}

}
