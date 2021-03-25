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
package com.rapidminer.tools.math.container;

import java.util.Comparator;
import java.util.Objects;

import com.rapidminer.tools.ValidationUtilV2;


/**
 * A range of object types with respect to a given comparator. Cannot contain {@code null} as lower or upper bounds. If
 * a bound is unknown, use the min/max with respect to the comparator.
 *
 * @param <T>
 * 		the element type
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ObjectRange<T> {

	private final T lower;
	private final T upper;
	private final Comparator<T> comparator;

	/**
	 * Creates a new object range.
	 *
	 * @param lowerBound
	 * 		the lower bound of the range, must not be {@code null}
	 * @param upperBound
	 * 		the upper bound of the range, must not be {@code null}
	 * @param comparator
	 * 		the comparator to check if something is in the range, must not be {@code null}
	 */
	public ObjectRange(T lowerBound, T upperBound, Comparator<T> comparator) {

		//upper and lower cannot be null, but you could use e.g. Instant.MIN, Instant.MAX
		ValidationUtilV2.requireNonNull(lowerBound, "lowerBound");
		ValidationUtilV2.requireNonNull(upperBound, "upperBound");
		ValidationUtilV2.requireNonNull(comparator, "comparator");

		// best effort fix if range is broken to avoid exceptions in metadata transformation
		if (comparator.compare(lowerBound, upperBound) > 0) {
			lowerBound = upperBound;
		}

		this.lower = lowerBound;
		this.upper = upperBound;
		this.comparator = comparator;
	}


	@Override
	public String toString() {
		return "[" + lower + " \u2013 " + upper + "]";
	}

	/**
	 * Checks if the value is contained in the range.
	 *
	 * @param value
	 * 		the value to check
	 * @return {@code true} if the value is in the range
	 */
	public boolean contains(T value) {
		return comparator.compare(value, lower) >= 0 && comparator.compare(value, upper) <= 0;
	}

	/**
	 * Returns the upper bound of the range.
	 *
	 * @return the upper bound
	 */
	public T getUpper() {
		return upper;
	}

	/**
	 * Returns the lower bound of the range.
	 *
	 * @return the lower bound
	 */
	public T getLower() {
		return lower;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ObjectRange<?> that = (ObjectRange<?>) o;
		return Objects.equals(lower, that.lower) &&
				Objects.equals(upper, that.upper);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lower, upper);
	}

	/**
	 * Checks whether the other range is contained in this range.
	 *
	 * @param range
	 * 		the range to check
	 * @return {@code true} it the given range is contained in this range
	 */
	public boolean contains(ObjectRange<T> range) {
		return (comparator.compare(this.lower, range.lower) <= 0 && comparator.compare(this.upper,range.upper) >= 0);
	}

}
