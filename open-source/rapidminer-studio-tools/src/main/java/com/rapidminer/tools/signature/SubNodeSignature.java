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
 * Represents the signature of a low level node, indicating its position.
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public class SubNodeSignature extends NodeSignature {

	/**
	 * Builder for {@link SubNodeSignature SubNodeSignatures}.
	 *
	 * @author Jan Czogalla
	 * @since 9.10
	 */
	public static final class SubNodeBuilder extends NodeBuilder<SubNodeSignature> {

		private SubNodeBuilder() {
			super(new SubNodeSignature());
		}

		public SubNodeBuilder index(int index) {
			node.index = index;
			return this;
		}

		@Override
		public SubNodeSignature build() {
			return new SubNodeSignature(node);
		}
	}

	private int index;

	private SubNodeSignature() {}

	private SubNodeSignature(SubNodeSignature other) {
		super(other);
		this.index = other.index;
	}

	/**
	 * Returns the index of this subnode
	 */
	public int getIndex() {
		return index;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		SubNodeSignature that = (SubNodeSignature) o;
		return index == that.index;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), index);
	}

	/**
	 * Creates a builder for a new {@link SubNodeSignature}
	 */
	public static SubNodeBuilder builder() {
		return new SubNodeBuilder();
	}
}
