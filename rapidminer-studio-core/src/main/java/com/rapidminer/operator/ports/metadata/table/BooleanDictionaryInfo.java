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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.rapidminer.operator.ports.metadata.MetaDataInfo;


/**
 * A {@link DictionaryInfo} which is boolean and has information about positive and negative values.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class BooleanDictionaryInfo extends DictionaryInfo {

	private boolean unknown = false;
	private String positive;
	private String negative;

	/**
	 * Creates a boolean {@link DictionaryInfo} which has unknown positive and negative.
	 */
	protected BooleanDictionaryInfo() {
		super(Collections.emptySet(), false);
		unknown = true;
	}

	/**
	 * Creates a boolean {@link DictionaryInfo} with the given positive and negative.
	 */
	protected BooleanDictionaryInfo(String positive, String negative) {
		super(new TreeSet<>(toSet(positive, negative)), false);
		this.positive = positive;
		this.negative = negative;
	}

	@Override
	public boolean isBoolean() {
		return true;
	}

	@Override
	public MetaDataInfo hasPositive() {
		if (unknown) {
			return MetaDataInfo.UNKNOWN;
		}
		return positive == null ? MetaDataInfo.NO : MetaDataInfo.YES;
	}

	@Override
	public MetaDataInfo hasNegative() {
		if (unknown) {
			return MetaDataInfo.UNKNOWN;
		}
		return negative == null ? MetaDataInfo.NO : MetaDataInfo.YES;
	}

	@Override
	public Optional<String> getPositiveValue() {
		return Optional.ofNullable(positive);
	}

	@Override
	public Optional<String> getNegativeValue() {
		return Optional.ofNullable(negative);
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
		BooleanDictionaryInfo that = (BooleanDictionaryInfo) o;
		return unknown == that.unknown &&
				Objects.equals(positive, that.positive) &&
				Objects.equals(negative, that.negative);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), unknown, positive, negative);
	}

	/**
	 * Creates a set containing the positive and negative string except when they are {@code null}.
	 */
	private static Collection<String> toSet(String positive, String negative) {
		Set<String> set = new HashSet<>();
		set.add(positive);
		set.add(negative);
		set.remove(null);
		return set;
	}
}
