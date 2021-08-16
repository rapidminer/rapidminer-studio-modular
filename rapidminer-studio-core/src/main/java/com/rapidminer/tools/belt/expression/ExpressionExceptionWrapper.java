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


import com.rapidminer.tools.ValidationUtilV2;


/**
 * Wrapper for {@link ExpressionException}s. The expression parser will auto-unwrap and throw the underlying expression.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 * @see ExpressionException
 */
public class ExpressionExceptionWrapper extends RuntimeException {

	private final ExpressionException e;

	/**
	 * Creates a new ExpressionExceptionWrapper wrapping the given ExpressionException.
	 *
	 * @param e
	 * 		the given ExpressionException (not {@code null})
	 */
	public ExpressionExceptionWrapper(ExpressionException e){
		super(e);
		ValidationUtilV2.requireNonNull(e, "ExpressionException");
		this.e = e;
	}

	/**
	 * @return the wrapped ExpressionException.
	 */
	public ExpressionException unwrap(){
		return e;
	}

}
