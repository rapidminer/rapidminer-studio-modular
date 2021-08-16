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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Represents the signature of a top level node, specified by a key.
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public class IOSignature extends NodeSignature {

	/**
	 * Builder for {@link IOSignature IOSignatures}.
	 *
	 * @author Jan Czogalla
	 * @since 9.10
	 */
	public static final class IOBuilder extends NodeBuilder<IOSignature> {

		private IOBuilder() {
			super(new IOSignature());
		}

		public IOBuilder ioHolderKey(String ioHolderKey) {
			node.ioHolderKey = ioHolderKey;
			return this;
		}

		public IOBuilder ioHolderClass(String ioHolderClass) {
			node.ioHolderClass = ioHolderClass;
			return this;
		}

		public IOBuilder parameters(Map<String, ParameterSignature> parameters) {
			node.parameters = parameters;
			return this;
		}

		public IOBuilder capabilities(List<String> capabilities) {
			node.capabilities = capabilities;
			return this;
		}

		public IOBuilder subNodes(List<SubNodeSignature> subNodes) {
			node.subNodes = subNodes;
			return this;
		}

		@Override
		public IOSignature build() {
			return new IOSignature(node);
		}
	}

	private String ioHolderKey;
	private String ioHolderClass;
	private Map<String, ParameterSignature> parameters = new LinkedHashMap<>();
	private List<String> capabilities = new ArrayList<>();
	private List<SubNodeSignature> subNodes = new ArrayList<>();

	private IOSignature() {}

	private IOSignature(IOSignature other) {
		super(other);
		this.ioHolderKey = other.ioHolderKey;
		this.ioHolderClass = other.ioHolderClass;
		this.parameters.putAll(other.parameters);
		this.capabilities.addAll(other.capabilities);
		this.subNodes.addAll(other.subNodes);
	}

	/**
	 * Returns the key of this signature
	 */
	public String getIoHolderKey() {
		return ioHolderKey;
	}

	/**
	 * Returns the class of this signature
	 */
	public String getIoHolderClass() {
		return ioHolderClass;
	}

	/**
	 * Returns the map of key to {@link ParameterSignature} of this signature
	 */
	public Map<String, ParameterSignature> getParameters() {
		return parameters;
	}

	/**
	 * Returns the list of capabilities of this signature
	 */
	public List<String> getCapabilities() {
		return capabilities;
	}

	/**
	 * Returns a copy of the list of subnodes of this signature; might be empty
	 */
	public List<SubNodeSignature> getSubNodes() {
		return subNodes;
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
		IOSignature signature = (IOSignature) o;
		return Objects.equals(ioHolderKey, signature.ioHolderKey) && Objects.equals(ioHolderClass, signature.ioHolderClass) && Objects.equals(parameters, signature.parameters) && Objects.equals(capabilities, signature.capabilities) && Objects.equals(subNodes, signature.subNodes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), ioHolderKey, ioHolderClass, parameters, capabilities, subNodes);
	}

	/**
	 * Creates a builder for a new {@link IOSignature}.
	 */
	public static IOBuilder builder() {
		return new IOBuilder();
	}
}
