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
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Information about the data in a {@link Column}. Used by {@link TableMetaData}.
 * Use a {@link ColumnInfoBuilder} to create or adjust a {@link ColumnInfo}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ColumnInfo {

	/**
	 * Empty dictionary in case of unknown.
	 */
	private static final DictionaryInfo UNKNOWN_DICTIONARY = new DictionaryInfo(Collections.emptySet(), false);

	/**
	 * Helper class to ensure that the column type is matching the object range.
	 *
	 * @param <T>
	 * 		the element type of the column
	 */
	static class MatchingTypeRange<T> {
		private final ColumnType<T> type;
		private final ObjectRange<T> range;

		MatchingTypeRange(ColumnType<T> type, ObjectRange<T> range) {
			this.type = type;
			this.range = range;
		}
	}

	private ColumnType<?> type;

	private SetRelation valueSetRelation;

	private DictionaryInfo dictionaryInfo;

	private ObjectRange<?> objectRange;

	private MDInteger missings;

	private Range range;

	/**
	 * Copy constructor. Use the more specialized constructors if possible when constructing for the first time as they
	 * have additional checks. In particular, the nominal constructor needs to be used to ensure that the value set is
	 * shrunk.
	 */
	ColumnInfo(ColumnType<?> type, SetRelation valueSetRelation, DictionaryInfo dictionaryInfo,
			   ObjectRange<?> objectRange, Range range, MDInteger missings) {
		this.type = type;
		this.valueSetRelation = valueSetRelation;
		this.dictionaryInfo = dictionaryInfo;
		this.objectRange = objectRange;
		this.range = range;
		if (missings == null) {
			this.missings = MDInteger.newUnknown();
		} else {
			this.missings = missings;
		}
	}

	/**
	 * Constructor when only the type and the missings are known.
	 */
	ColumnInfo(ColumnType<?> type, MDInteger missings) {
		this(type, SetRelation.UNKNOWN, UNKNOWN_DICTIONARY, null, null, missings);
	}

	/**
	 * Nominal constructor.
	 */
	ColumnInfo(ColumnType<String> type, Collection<String> values, SetRelation valueSetRelation, MDInteger missings) {
		this.type = type;
		int maxValues = AttributeMetaData.getMaximumNumberOfNominalValues() - 1;
		//shorten value set if necessary
		if (values.size() > maxValues) {
			final Iterator<String> iterator = values.iterator();
			Set<String> shortenedValueSet = new TreeSet<>();
			for (int i = 0; i < maxValues; i++) {
				shortenedValueSet.add(iterator.next());
			}
			this.dictionaryInfo = new DictionaryInfo(shortenedValueSet, true);
			this.valueSetRelation = valueSetRelation.merge(SetRelation.SUPERSET);
		} else {
			this.valueSetRelation = valueSetRelation;
			this.dictionaryInfo = new DictionaryInfo(new TreeSet<>(values), false);
		}
		if (missings == null) {
			this.missings = MDInteger.newUnknown();
		} else {
			this.missings = missings;
		}
		this.range = null;
		this.objectRange = null;
	}

	/**
	 * Boolean constructor.
	 */
	ColumnInfo(ColumnType<String> type, String positive, String negative, MDInteger missings) {
		this(type, SetRelation.EQUAL, new BooleanDictionaryInfo(positive, negative), null, null, missings);
	}

	/**
	 * Object constructor which ensures that the object ranges matches the type.
	 */
	ColumnInfo(MatchingTypeRange<?> matchingType, SetRelation valueSetRelation, MDInteger missings) {
		this(matchingType.type, matchingType.range == null ? SetRelation.UNKNOWN :
				valueSetRelation, UNKNOWN_DICTIONARY, matchingType.range, null, missings);
	}

	/**
	 * Numeric constructor.
	 */
	ColumnInfo(ColumnType<Void> type, Range range, SetRelation valueSetRelation, MDInteger missings) {
		this(type, isUnknown(range) ? SetRelation.UNKNOWN : valueSetRelation, UNKNOWN_DICTIONARY,
				null, cleanedUpRange(range), missings);
	}

	/**
	 * Returns an optional column type. Will not be present if the type is not known.
	 *
	 * @return the column type
	 */
	public Optional<ColumnType<?>> getType() {
		return Optional.ofNullable(type);
	}

	/**
	 * Returns an optional object range. Will not be present if the column is not an object column ({@link
	 * Column.Category#OBJECT}) or the range is not known.
	 *
	 * @param objectType
	 * 		the element type of the object column
	 * @param <T>
	 * 		the class of the element type
	 * @return the range for an object column
	 */
	public <T> Optional<ObjectRange<T>> getObjectRange(Class<T> objectType) {
		if (type != null && type.elementType().equals(objectType)) {
			//checked that type matches
			@SuppressWarnings("unchecked") final ObjectRange<T> typedRange = (ObjectRange<T>) this.objectRange;
			return Optional.ofNullable(typedRange);
		}
		return Optional.empty();
	}

	/**
	 * Returns an optional numeric range. Will not be present if the column is not numeric or the range is not known.
	 *
	 * @return the range for a numeric column
	 */
	public Optional<Range> getNumericRange() {
		return Optional.ofNullable(range);
	}

	/**
	 * Returns the dictionary info for this column info. Will be empty if the column is not nominal.
	 *
	 * @return the dictionary info for a nominal column
	 */
	public DictionaryInfo getDictionary() {
		return dictionaryInfo;
	}

	/**
	 * Returns the number of missing values.
	 *
	 * @return a copy of the missing values to ensure immutability
	 */
	public MDInteger getMissingValues() {
		return missings.copy();
	}

	/**
	 * Returns the value set relation for the {@link DictionaryInfo}, {@link Range} or {@link ObjectRange}.
	 *
	 * @return whether the actual value set is a sub/superset or equals to the given dictionary or range or if the
	 * relation is unknown
	 */
	public SetRelation getValueSetRelation() {
		return valueSetRelation;
	}

	/**
	 * Whether there is at least one missing value.
	 *
	 * @return the info about a missing value
	 */
	public MetaDataInfo hasMissingValues() {
		return missings.isAtLeast(1);
	}

	/**
	 * Whether the associated column is {@link ColumnType#NOMINAL}.
	 *
	 * @return the info about if the column is nominal
	 */
	public MetaDataInfo isNominal() {
		if (type == null) {
			return MetaDataInfo.UNKNOWN;
		}
		return type.id() == Column.TypeId.NOMINAL ? MetaDataInfo.YES : MetaDataInfo.NO;
	}

	/**
	 * Whether the associated column is {@link Column.Category#NUMERIC}.
	 *
	 * @return the info about if the column is numeric
	 */
	public MetaDataInfo isNumeric() {
		if (type == null) {
			return MetaDataInfo.UNKNOWN;
		}
		return type.category() == Column.Category.NUMERIC ? MetaDataInfo.YES : MetaDataInfo.NO;
	}

	/**
	 * Whether the associated column is {@link Column.Category#OBJECT}.
	 *
	 * @return the info about if the column is object valued
	 */
	public MetaDataInfo isObject() {
		if (type == null) {
			return MetaDataInfo.UNKNOWN;
		}
		return type.category() == Column.Category.OBJECT ? MetaDataInfo.YES : MetaDataInfo.NO;
	}

	/**
	 * Whether the associated column has at most two categorical values.
	 *
	 * @return the info about the column being at most bicategorical
	 */
	public MetaDataInfo isAtMostBicategorical() {
		if (type == null) {
			return MetaDataInfo.UNKNOWN;
		}
		DictionaryInfo dictionary = getDictionary();
		return isNominal() == MetaDataInfo.YES && (dictionary.isBoolean() || (dictionary.getValueSet().size() <= 2 &&
				(valueSetRelation == SetRelation.EQUAL ||
						valueSetRelation == SetRelation.SUBSET))) ? MetaDataInfo.YES :
				MetaDataInfo.NO;
	}

	@Override
	public String toString() {
		return TableMDDisplayUtils.getDescription(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ColumnInfo that = (ColumnInfo) o;
		return 	Objects.equals(type, that.type) &&
				valueSetRelation == that.valueSetRelation &&
				Objects.equals(dictionaryInfo, that.dictionaryInfo) &&
				Objects.equals(objectRange, that.objectRange) &&
				Objects.equals(range, that.range) &&
				Objects.equals(missings.getRelation(), that.missings.getRelation()) &&
				Objects.equals(missings.getNumber(), that.missings.getNumber());
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, valueSetRelation, dictionaryInfo, objectRange,
				missings.getRelation(), missings.getNumber(), range);
	}

	/**
	 * Gets the object range without type check for internal use.
	 *
	 * @return the raw object range
	 */
	ObjectRange<?> getUncheckedObjectRange() {
		return objectRange;
	}

	/**
	 * Get rid of wrong ranges that are allowed for legacy reasons. Copy for immutabilty.
	 */
	private static Range cleanedUpRange(Range range) {
		if (range == null) {
			return null;
		}
		if (range.getLower() > range.getUpper()) {
			return null;
		}
		return new Range(range);
	}

	/**
	 * Checks whether the set relation should be unknown.
	 */
	private static boolean isUnknown(Range range){
		return range==null || range.getLower() > range.getUpper();
	}
}
