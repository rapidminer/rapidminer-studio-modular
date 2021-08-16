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

import com.rapidminer.gui.tools.VersionNumber;

/**
 * Represents a collection of {@link IOSignature IOSignatures} that are bundled together with an ID and version number.
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public class IOHolderProviderInfo {


	/**
	 * Builder for {@link IOHolderProviderInfo IOHolderProviderInfos}.
	 *
	 * @author Jan Czogalla
	 * @since 9.10
	 */
	public static final class InfoBuilder {

		private IOHolderProviderInfo info = new IOHolderProviderInfo();

		private InfoBuilder() {}

		public InfoBuilder providerID(String providerID) {
			info.providerID = providerID;
			return this;
		}

		public InfoBuilder version(VersionNumber version) {
			info.version = version;
			return this;
		}

		public InfoBuilder isNew(boolean isNew) {
			info.isNew = isNew;
			return this;
		}

		public InfoBuilder signatures(Map<String, IOSignature> signatures) {
			info.signatures = signatures;
			return this;
		}

		public IOHolderProviderInfo build() {
			return new IOHolderProviderInfo(info);
		}
	}

	private String providerID;
	private VersionNumber version;
	private boolean isNew;
	private Map<String, IOSignature> signatures = new LinkedHashMap<>();

	private IOHolderProviderInfo() {}

	IOHolderProviderInfo(IOHolderProviderInfo other) {
		this.providerID = other.providerID;
		this.version = other.version;
		this.isNew = other.isNew;
		this.signatures.putAll(other.signatures);
	}

	/**
	 * Returns the provider ID of this info
	 */
	public String getProviderID() {
		return providerID;
	}

	/**
	 * Returns the version of this info
	 */
	public VersionNumber getVersion() {
		return version;
	}

	/**
	 * Returns whether this is newly created
	 */
	public boolean isNew() {
		return isNew;
	}

	/**
	 * Returns the full map of keys to {@link IOSignature IOSignatures} of this info
	 */
	public Map<String, IOSignature> getSignatures() {
		return signatures;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		IOHolderProviderInfo that = (IOHolderProviderInfo) o;
		return Objects.equals(providerID, that.providerID)
				&& Objects.equals(version, that.version) && isNew == that.isNew
				&&  Objects.equals(signatures, that.signatures);
	}

	@Override
	public int hashCode() {
		return Objects.hash(providerID, version, isNew, signatures);
	}

	/**
	 * Creates a builder for a new {@link IOHolderProviderInfo}.
	 */
	public static InfoBuilder builder() {
		return new InfoBuilder();
	}
}
