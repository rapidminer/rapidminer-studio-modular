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
package com.rapidminer.operator.ports.metadata.table;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.rapidminer.operator.ports.metadata.MetaDataInfo;


/**
 * Meta data for {@link com.rapidminer.belt.column.Dictionary} used by {@link ColumnInfo}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class DictionaryInfo {

	private boolean shrunkValueSet = false;

	private Set<String> valueSet = null;

	protected DictionaryInfo(Set<String> valueSet, boolean shrunkValueSet) {
		this.valueSet = valueSet;
		this.shrunkValueSet = shrunkValueSet;
	}

	/**
	 * Whether the dictionary is boolean.
	 *
	 * @return {@code true} if the dictionary is boolean
	 */
	public boolean isBoolean() {
		return false;
	}

	/**
	 * Returns the value set of the dictionary info.
	 *
	 * @return the inner value set as an unmodifiable set
	 */
	public Set<String> getValueSet() {
		return Collections.unmodifiableSet(valueSet);
	}

	/**
	 * Returns whether the dictionary info has a positive value (in case it is boolean).
	 *
	 * @return whether there is a positive value
	 */
	public MetaDataInfo hasPositive() {
			return MetaDataInfo.NO;
	}

	/**
	 * Returns whether the dictionary info has a negative value (in case it is boolean).
	 *
	 * @return whether there is a negative value
	 */
	public MetaDataInfo hasNegative() {
			return MetaDataInfo.NO;
	}

	/**
	 * The optional positive value. Will not be present except {@link #hasPositive()} returns {@link MetaDataInfo#YES}.
	 *
	 * @return an optional for the positive value
	 */
	public Optional<String> getPositiveValue() {
		return Optional.empty();
	}

	/**
	 * The optional negative value. Will not be present except {@link #hasNegative()} returns {@link MetaDataInfo#YES}.
	 *
	 * @return an optional for the negative value
	 */
	public Optional<String> getNegativeValue() {
		return Optional.empty();
	}

	/**
	 * Returns whether the nominal value set inside the {@link DictionaryInfo} has been shrunk because there were more
	 * nominal values than a threshold (defined by the settings property
	 * {@link com.rapidminer.RapidMiner#PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES}).
	 *
	 * @return {@code true} if the value set of this nominal column info was shrunk
	 */
	public boolean valueSetWasShrunk() {
		return shrunkValueSet;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DictionaryInfo that = (DictionaryInfo) o;
		return shrunkValueSet == that.shrunkValueSet &&
				Objects.equals(valueSet, that.valueSet);
	}

	@Override
	public int hashCode() {
		return Objects.hash(shrunkValueSet, valueSet);
	}

	/**
	 * Returns the underlying value set which must not be changed wrapped into a {@link CopyOnWriteValueSet}.
	 *
	 * @return a wrapper for the underlying valueset which ensures immutability
	 */
	CopyOnWriteValueSet getAsCopyOnWrite() {
		return new CopyOnWriteValueSet(valueSet);
	}
}
