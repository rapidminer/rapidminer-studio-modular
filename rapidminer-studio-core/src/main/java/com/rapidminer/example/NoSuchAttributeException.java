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
package com.rapidminer.example;

import java.util.NoSuchElementException;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorRuntimeException;


/**
 * Exception class that indicates that an {@link Attribute} or {@link AttributeRole} does not exist in a context.
 * Uses an {@link AttributeError} object to store the information and provide an appropriate {@link com.rapidminer.operator.UserError UserError}
 * when caught in an {@link com.rapidminer.operator.Operator Operator}.
 * <p>
 * This extends {@link NoSuchElementException} for legacy reasons and implements {@link OperatorRuntimeException} to
 * counter unwanted side effects.
 *
 * @author Jan Czogalla
 * @since 8.2
 */
public class NoSuchAttributeException extends NoSuchElementException implements OperatorRuntimeException {

	private final AttributeError error;

	/**
	 * Creates an exception to indicate a nonexistent attribute with the given name. Same as
	 * {@link #NoSuchAttributeException(String, boolean) NoSuchAttributeException(name, false)}.
	 */
	public NoSuchAttributeException(String name) {
		this(name, false);
	}

	/**
	 * Creates an exception to indicate a nonexistent attribute or attribute role with the given name.
	 *
	 * @param name
	 * 		the name of the attribute (role)
	 * @param isRole
	 * 		whether the name corresponds to an attribute role or not
	 */
	public NoSuchAttributeException(String name, boolean isRole) {
		this.error = new AttributeError();
		error.baseKey = "no_such_attribute";
		error.name = name;
		error.isRole = isRole;
		error.isUserError = true;
	}

	@Override
	public String getMessage() {
		return toOperatorException().getMessage();
	}

	@Override
	public OperatorException toOperatorException() {
		return error.toOperatorException(getStackTrace());
	}
}
