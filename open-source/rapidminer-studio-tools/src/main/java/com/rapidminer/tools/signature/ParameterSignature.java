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
package com.rapidminer.tools.signature;

import java.util.Objects;

/**
 * Represents the signature of a parameter.
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public class ParameterSignature {

	private String description;
	private String className;
	private boolean isOptional;

	private ParameterSignature() {}

	public ParameterSignature(String description, String className, boolean isOptional) {
		this.description = description;
		this.className = className;
		this.isOptional = isOptional;
	}

	/**
	 * Returns the description of this signature
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the class of this signature
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns whether this parameter is optional
	 */
	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ParameterSignature that = (ParameterSignature) o;
		return isOptional == that.isOptional && Objects.equals(description, that.description) && Objects.equals(className, that.className);
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, className, isOptional);
	}
}
