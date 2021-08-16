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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the signature of a general node
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public abstract class NodeSignature {

	/**
	 * Base builder for {@link NodeSignature}; subclasses of {@link NodeSignature} should also sub class this builder.
	 *
	 * @param <T> the {@link NodeSignature} subtype
	 * @author Jan Czogalla
	 * @since 9.10
	 */
	abstract static class NodeBuilder<T extends NodeSignature> {

		T node;

		NodeBuilder(T node) {
			this.node = node;
		}

		public NodeBuilder<T> inputs(Map<String, IOType> inputs) {
			node.inputs = inputs;
			return this;
		}

		public NodeBuilder<T> outputs(Map<String, IOType> outputs) {
			node.outputs = outputs;
			return this;
		}

		public abstract T build();
	}

	Map<String, IOType> inputs = new LinkedHashMap<>();
	Map<String, IOType> outputs = new LinkedHashMap<>();

	NodeSignature() {}

	NodeSignature(NodeSignature other) {
		this.inputs = other.inputs;
		this.outputs = other.outputs;
	}

	/**
	 * Returns the map of input keys to {@link IOType} for this node
	 */
	public Map<String, IOType> getInputs() {
		return inputs;
	}

	/**
	 * Returns the map of output keys to {@link IOType} for this node
	 */
	public Map<String, IOType> getOutputs() {
		return outputs;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NodeSignature that = (NodeSignature) o;
		return Objects.equals(inputs, that.inputs) && Objects.equals(outputs, that.outputs);
	}

	@Override
	public int hashCode() {
		return Objects.hash(inputs, outputs);
	}
}
