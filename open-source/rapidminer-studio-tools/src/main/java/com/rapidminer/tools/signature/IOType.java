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
 * Represents the signature at an I/O point.
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public class IOType {

	private String className;
	private boolean isCollection;
	private boolean isSpecific;

	private IOType() {}

	public IOType(String className, boolean isCollection, boolean isSpecific) {
		this.className = className;
		this.isCollection = isCollection;
		this.isSpecific = isSpecific;
	}

	/**
	 * Returns the class name of this type
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Returns whether this type represents a collection
	 */
	public boolean isCollection() {
		return isCollection;
	}

	/**
	 * Returns whether this type is specific
	 */
	public boolean isSpecific() {
		return isSpecific;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IOType ioType = (IOType) o;
		return isCollection == ioType.isCollection && isSpecific == ioType.isSpecific && Objects.equals(className, ioType.className);
	}

	@Override
	public int hashCode() {
		return Objects.hash(className, isCollection, isSpecific);
	}
}
