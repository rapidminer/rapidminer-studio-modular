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
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * A builder for {@link ColumnInfo}. Note that this implementation is not synchronized. In case the builder is used
 * concurrently by multiple threads, external synchronization needs to be added.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ColumnInfoBuilder {

	private static final DictionaryInfo UNKNOWN_DICTIONARY = new DictionaryInfo(Collections.emptySet(), false);
	private static final DictionaryInfo UNKNOWN_BOOLEAN_DICTIONARY = new BooleanDictionaryInfo();

	private final ColumnType<?> type;

	private SetRelation valueSetRelation = SetRelation.UNKNOWN;
	private DictionaryInfo dictionaryInfo = UNKNOWN_DICTIONARY;

	private Range range = null;
	private ObjectRange<?> objectRange = null;

	private MDInteger missings = MDInteger.newPossible();

	/**
	 * Creates a builder for a {@link ColumnInfo} of the given type.
	 *
	 * @param type
	 * 		the type of the column, can be {@code null}
	 */
	public ColumnInfoBuilder(ColumnType<?> type) {
		this.type = type;
	}

	/**
	 * Copies everything from the info into a new builder.
	 *
	 * @param info
	 * 		the column info to adjust, must not be {@code null}
	 */
	public ColumnInfoBuilder(ColumnInfo info){
		ValidationUtilV2.requireNonNull(info, "column info");
		Range range1 = info.getNumericRange().orElse(null);
		this.type = info.getType().orElse(null);
		this.valueSetRelation = info.getValueSetRelation();
		this.dictionaryInfo = info.getDictionary();
		this.objectRange = info.getUncheckedObjectRange();
		this.range = range1 == null ? null : new Range(range1);
		this.missings = info.getMissingValues().copy();
	}

	/**
	 * Sets the number of missing values.
	 *
	 * @param missings
	 * 		the number of missing values
	 * @return the builder
	 */
	public ColumnInfoBuilder setMissings(MDInteger missings) {
		if (missings == null) {
			this.missings = MDInteger.newUnknown();
		} else {
			this.missings = missings.copy();
		}
		return this;
	}

	/**
	 * Sets the number of missing values equal to number.
	 *
	 * @param number
	 * 		the number of missing values
	 * @return the builder
	 */
	public ColumnInfoBuilder setMissings(int number) {
		missings = new MDInteger(number);
		return this;
	}

	/**
	 * Adds to the missing values.
	 *
	 * @param toAdd
	 * 		the number to add to the missing values
	 * @return the builder
	 */
	public ColumnInfoBuilder addMissings(MDInteger toAdd) {
		this.missings.add(toAdd);
		return this;
	}

	/**
	 * Sets the number of missing values to unknown.
	 *
	 * @return the builder
	 */
	public ColumnInfoBuilder unknownMissings() {
		missings = MDInteger.newUnknown();
		return this;
	}

	/**
	 * Increases the number of missing values by an unknown amount.
	 *
	 * @return the builder
	 * @see MDInteger#increaseByUnknownAmount()
	 */
	public ColumnInfoBuilder increaseMissings() {
		missings.increaseByUnknownAmount();
		return this;
	}

	/**
	 * Reduces the number of missing values by an unknown amount.
	 *
	 * @return the builder
	 * @see MDInteger#reduceByUnknownAmount()
	 */
	public ColumnInfoBuilder reduceMissings() {
		missings.reduceByUnknownAmount();
		return this;
	}

	/**
	 * Sets the numeric range together with a set relation for a numeric column.
	 *
	 * @param range
	 * 		the numeric range
	 * @param relation
	 * 		the set relation for the range
	 * @return the builder
	 * @see #isNumeric()
	 */
	public ColumnInfoBuilder setNumericRange(Range range, SetRelation relation) {
		setValueSetRelation(relation);
		this.range = new Range(range);
		return this;
	}

	/**
	 * Sets the object range together with a set relation for an object column.
	 *
	 * @param range
	 * 		the object range
	 * @param relation
	 * 		the set relation for the range
	 * @return the builder
	 * @throws IllegalArgumentException
	 * 		if the objects in the range do not match the element type of the column type
	 * @see #isObject()
	 */
	public ColumnInfoBuilder setObjectRange(ObjectRange<?> range, SetRelation relation) {
		setValueSetRelation(relation);
		if (type != null) {
			//ensure that range is compatible with type
			Object min = range.getLower();
			Object max = range.getUpper();
			Class<?> typeClass = type.elementType();
			if ((typeClass.isInstance(min)) && (typeClass.isInstance(max))) {
				this.objectRange = range;
				return this;
			}
		}
		throw new IllegalArgumentException("Object range does not match type");
	}

	/**
	 * Sets the dictionary values together with the given relation for a nominal column. If the values are more than a
	 * threshold (defined by the settings property
	 * {@link com.rapidminer.RapidMiner#PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES})
	 * the value set is shrunk and the set relation merged with {@link SetRelation#SUPERSET}.
	 *
	 * @param values
	 * 		the nominal values
	 * @param relation
	 * 		how the actual values relate to the given values
	 * @return the builder
	 * @see #isNominal()
	 * @see DictionaryInfo#valueSetWasShrunk()
	 */
	public ColumnInfoBuilder setDictionaryValues(Collection<String> values, SetRelation relation) {
		setValueSetRelation(relation);
		int max = AttributeMetaData.getMaximumNumberOfNominalValues() - 1;
		if (values.size() > max) {
			Set<String> valueSet = new TreeSet<>();
			final Iterator<String> iterator = values.iterator();
			for (int i = 0; i < max; i++) {
				valueSet.add(iterator.next());
			}
			dictionaryInfo = new DictionaryInfo(valueSet, true);
			valueSetRelation = valueSetRelation.merge(SetRelation.SUPERSET);
		} else {
			if (values instanceof CopyOnWriteValueSet && ((CopyOnWriteValueSet) values).isUnchanged()) {
				// can reuse value set that came from conversion of dictionary info
				dictionaryInfo = new DictionaryInfo(((CopyOnWriteValueSet) values).getValueSet(), false);
			} else {
				dictionaryInfo = new DictionaryInfo(new TreeSet<>(values), false);
			}
		}
		return this;
	}

	/**
	 * Sets the value set relation which describes how the actual values are related to the given nominal values, range
	 * or object range.
	 *
	 * @param valueSetRelation
	 * 		the value set realation to set
	 * @return the builder
	 */
	public ColumnInfoBuilder setValueSetRelation(SetRelation valueSetRelation) {
		this.valueSetRelation = valueSetRelation == null ? SetRelation.UNKNOWN : valueSetRelation;
		return this;
	}

	/**
	 * Sets the dictionary information and the associated value set relation.
	 *
	 * @param dictionary
	 * 		the dictionary information
	 * @param relation
	 * 		the relation that describes how the actual dictionary is related to the given dictionary information
	 * @return the builder
	 */
	public ColumnInfoBuilder setDictionary(DictionaryInfo dictionary, SetRelation relation) {
		setValueSetRelation(relation);
		this.dictionaryInfo = dictionary;
		return this;
	}

	/**
	 * Adds the given value set to existing value set of the {@link DictionaryInfo} of a nominal column. If there are
	 * more nominal values than a certain threshold, the value set is shrunk.
	 *
	 * @param valueSet
	 * 		the value set to add
	 * @return the builder
	 */
	public ColumnInfoBuilder addDictionaryValues(Collection<String> valueSet) {
		final TreeSet<String> fullValueSet = new TreeSet<>(dictionaryInfo.getValueSet());
		final Iterator<String> iterator = valueSet.iterator();
		int max = Math.min(
				fullValueSet.size() + valueSet.size(), AttributeMetaData.getMaximumNumberOfNominalValues() - 1) -
				fullValueSet.size();
		for (int i = 0; i < max; i++) {
			fullValueSet.add(iterator.next());
		}
		boolean shrunkValueSet = dictionaryInfo.valueSetWasShrunk();
		if (max < valueSet.size()) {
			valueSetRelation = valueSetRelation.merge(SetRelation.SUPERSET);
			shrunkValueSet = true;
		}
		dictionaryInfo = new DictionaryInfo(fullValueSet, shrunkValueSet);
		return this;
	}

	/**
	 * Unions the current numeric range with the given range.
	 *
	 * @param range
	 * 		the range to union with
	 * @return the builder
	 */
	public ColumnInfoBuilder rangeUnion(Range range) {
		if (range != null) {
			this.range.union(range);
		} else {
			this.range = null;
		}
		return this;
	}

	/**
	 * Merges the current value set relation with the given one.
	 *
	 * @param toMerge
	 * 		the value set relation to merge with
	 * @return the builder
	 * @see SetRelation#merge
	 */
	public ColumnInfoBuilder mergeValueSetRelation(SetRelation toMerge) {
		this.valueSetRelation = this.valueSetRelation.merge(toMerge);
		return this;
	}

	/**
	 * Sets the dictionary to boolean with the given positive and negative values for a nominal column. If they are not
	 * known, use {@link #setUnknownBooleanDictionary()} instead.
	 *
	 * @param positiveValue
	 * 		the positive value
	 * @param negativeValue
	 * 		the negative value
	 * @return the builder
	 */
	public ColumnInfoBuilder setBooleanDictionaryValues(String positiveValue, String negativeValue) {
		dictionaryInfo = new BooleanDictionaryInfo(positiveValue, negativeValue);
		valueSetRelation = SetRelation.EQUAL;
		return this;
	}

	/**
	 * Sets the dictionary to boolean with unknown positive/negative value for a nominal column. This overwrites
	 * already set dictionary values. If known use {@link #setBooleanDictionaryValues(String, String)} instead.
	 */
	public ColumnInfoBuilder setUnknownBooleanDictionary() {
		dictionaryInfo = UNKNOWN_BOOLEAN_DICTIONARY;
		valueSetRelation = SetRelation.SUPERSET;
		return this;
	}

	/**
	 * Can be used for non-boolean dictionaries to manually specify if the value set has been shrunk. (Will do nothing
	 * for boolean dictionaries since they never hold more than two values.)
	 *
	 * @param valueSetIsShrunk
	 * 		the {@code valueSetWasShrunk} property of the dictionary will be set to this value
	 * @see DictionaryInfo#valueSetWasShrunk()
	 */
	public void setValueSetWasShrunk(boolean valueSetIsShrunk) {
		if (!(dictionaryInfo instanceof BooleanDictionaryInfo)) {
			dictionaryInfo = new DictionaryInfo(dictionaryInfo.getValueSet(), valueSetIsShrunk);
		}
	}

	/**
	 * Builds the {@link ColumnInfo} from the given information.
	 *
	 * @return the built column info
	 */
	public ColumnInfo build() {
		//sanity cleanup
		if (type == null) {
			range = null;
			objectRange = null;
			dictionaryInfo = UNKNOWN_DICTIONARY;
			valueSetRelation = SetRelation.UNKNOWN;
		} else if (type.id() == Column.TypeId.NOMINAL) {
			range = null;
			objectRange = null;
		} else if (type.category() == Column.Category.NUMERIC) {
			dictionaryInfo = UNKNOWN_DICTIONARY;
			objectRange = null;
			//get rid of wrong ranges that are allowed for legacy reasons
			if (range != null && range.getLower() > range.getUpper()) {
				range = null;
			}
			if (range == null) {
				valueSetRelation = SetRelation.UNKNOWN;
			} else {
				//copy to ensure immutability
				range = new Range(range);
			}
		} else if (type.category() == Column.Category.OBJECT) {
			dictionaryInfo = UNKNOWN_DICTIONARY;
			range = null;
			if (objectRange == null) {
				valueSetRelation = SetRelation.UNKNOWN;
			}
		}
		if (missings.getNumber() < 0) {
			missings.add(-missings.getNumber());
		}

		return new ColumnInfo(type, valueSetRelation, dictionaryInfo,  objectRange, range, missings.copy());
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


}
