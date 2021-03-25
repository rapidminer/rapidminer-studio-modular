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

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDNumber;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Builder for {@link TableMetaData}. Note that this implementation is not synchronized. In case the builder is used
 * concurrently by multiple threads, external synchronization needs to be added.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class TableMetaDataBuilder {

	private static final String MESSAGE_METADATA_CLASS_NULL = "Column metadata type must not be null.";
	private static final String LABEL_STRING = "label";
	private Annotations annotations;
	private SetRelation columnsRelation = SetRelation.EQUAL;

	private MDInteger height = MDInteger.newPossible();

	private Map<String, ColumnInfo> columns = new LinkedHashMap<>();
	private Map<String, List<ColumnMetaData>> metaDataMap = new HashMap<>();

	private boolean ownsColumnMetaData;

	// additional data copied from the given TableMetaData
	private Map<String, Object> keyValueMap = null;

	// history copied from the given TableMetaData
	private List<OutputPort> history = null;

	/**
	 * Creates a new builder with the given height (number of rows).
	 *
	 * @param height
	 * 		the exactly known height
	 */
	public TableMetaDataBuilder(int height) {
		this(new MDInteger(height));
	}

	/**
	 * Creates a new builder with the given height (number of rows).
	 *
	 * @param height
	 * 		the height
	 */
	public TableMetaDataBuilder(MDInteger height) {
		this.height = height.copy();
		if (this.height.getNumber() < 0) {
			this.height.add(-this.height.getNumber());
		}
		ownsColumnMetaData = true;
	}

	/**
	 * Creates a builder from {@link TableMetaData}.
	 *
	 * @param tableMetaData
	 * 		the meta data to change
	 */
	public TableMetaDataBuilder(TableMetaData tableMetaData) {
		//copy annotations so that they don't get lost
		annotations = new Annotations(tableMetaData.getAnnotations());
		height = tableMetaData.height().copy();
		columnsRelation = tableMetaData.getColumnSetRelation();
		ownsColumnMetaData = false;
		metaDataMap = tableMetaData.getMetaDataMap();
		columns = new LinkedHashMap<>(tableMetaData.getFullColumns());
		keyValueMap = new HashMap<>(tableMetaData.getKeyValueMap());
		history = new LinkedList<>(tableMetaData.getGenerationHistory());
	}

	/**
	 * Updates the height (number of rows) and adjusts the {@link ColumnInfo}s: If the height decreases, the number of
	 * missing values decreases and accordingly for increases. If the height decreases the value set relations are
	 * merged with subset.
	 *
	 * @param height
	 * 		the new exact height
	 * @return the builder
	 */
	public TableMetaDataBuilder updateHeight(int height) {
		MDInteger previous = this.height;
		this.height = new MDInteger(height);
		recheckAllColumnInfo(previous);
		return this;
	}

	/**
	 * Updates the height (number of rows) and adjusts the {@link ColumnInfo}s: If the height decreases, the number of
	 * missing values decreases and accordingly for increases. If the height decreases the value set relations are
	 * merged with subset.
	 *
	 * @param height
	 * 		the new height
	 * @return the builder
	 */
	public TableMetaDataBuilder updateHeight(MDInteger height) {
		MDInteger previous = this.height;
		this.height = height.copy();
		recheckAllColumnInfo(previous);
		return this;
	}

	/**
	 * Updates the height (number of rows) without adjusting the {@link ColumnInfo}s. Only use this if you ensure
	 * manually that all {@link ColumnInfo}s are consistent with the new height. Prefer {@link #updateHeight(MDInteger)}.
	 *
	 * @param height
	 * 		the new height
	 * @return the builder
	 */
	public TableMetaDataBuilder updateHeightWithoutSideEffects(MDInteger height) {
		this.height = height.copy();
		return this;
	}

	/**
	 * Reduces the height (number of rows) by an unknown amount and adjusts the {@link ColumnInfo}s: The number of
	 * missing values is reduced and the value set relation is merged with subset.
	 *
	 * @return the builder
	 */
	public TableMetaDataBuilder reduceHeightByUnknownAmount() {
		this.height.reduceByUnknownAmount();
		reduceColumnInfos();
		return this;
	}

	/**
	 * Increases the height (number of rows) by an unknown amount and adjusts the {@link ColumnInfo}s: The number of
	 * missing values is increased.
	 *
	 * @return the builder
	 */
	public TableMetaDataBuilder increaseHeightByUnknownAmount() {
		this.height.increaseByUnknownAmount();
		increaseColumnInfos();
		return this;
	}

	/**
	 * Merges the column set relation with the given relation.
	 *
	 * @param relation
	 * 		the relation to merge with
	 * @return the builder
	 * @see SetRelation#merge(SetRelation)
	 */
	public TableMetaDataBuilder mergeColumnSetRelation(SetRelation relation) {
		columnsRelation = columnsRelation.merge(relation);
		return this;
	}

	/**
	 * Sets the columns set relation to {@link SetRelation#EQUAL}.
	 *
	 * @return the builder
	 */
	public TableMetaDataBuilder columnsAreKnown() {
		columnsRelation = SetRelation.EQUAL;
		return this;
	}

	/**
	 * Declares that the columns are a superset of the ones in this meta data.
	 *
	 * @return the builder
	 */
	public TableMetaDataBuilder columnsAreSuperset() {
		columnsRelation = SetRelation.SUPERSET;
		return this;
	}

	/**
	 * Declares that the columns are a subset of the ones in this meta data.
	 *
	 * @return the builder
	 */
	public TableMetaDataBuilder columnsAreSubset() {
		columnsRelation = SetRelation.SUBSET;
		return this;
	}

	/**
	 * Removes the meta data for the column with the given label.
	 *
	 * @param label
	 * 		the column name
	 * @return the builder
	 */
	public TableMetaDataBuilder remove(String label) {
		columns.remove(label);
		ensureMetaDataOwnership();
		metaDataMap.remove(label);
		return this;
	}

	/**
	 * Changes the name of the column from old to new. Also updates ColumnReferences.
	 *
	 * @param oldLabel
	 * 		the old column name
	 * @param newLabel
	 * 		the new column name
	 * @return the builder
	 * @throws NullPointerException
	 * 		if the new label is {@code null}
	 */
	public TableMetaDataBuilder rename(String oldLabel, String newLabel) {
		ValidationUtilV2.requireNonNull(newLabel, "new label");
		// remove and add, do not want to rebuild linked hash map
		final ColumnInfo remove = columns.remove(oldLabel);
		if (remove != null) {
			columns.put(newLabel, remove);
			ensureMetaDataOwnership();
			final List<ColumnMetaData> oldValue = metaDataMap.remove(oldLabel);
			if (oldValue != null) {
				metaDataMap.put(newLabel, oldValue);
			} else {
				// in case the new label overrides an existing label
				metaDataMap.remove(newLabel);
			}
			updateReferences(oldLabel, newLabel);
		}
		return this;
	}

	/**
	 * Adds a new column info for the given label with the given type and missing value information.
	 *
	 * @param label
	 * 		the column name
	 * @param type
	 * 		the column type, can be {@code null} if not known
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder add(String label, ColumnType<?> type, MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		addColumnInfo(label, new ColumnInfo(type, missings));
		return this;
	}

	/**
	 * Adds a new column info for the given label.
	 *
	 * @param label
	 * 		the column name
	 * @param columnInfo
	 * 		the column info to add, must not be {@code null}. In case of unknown use
	 * 		{@link #add(String, ColumnType, MDInteger)}
	 * @return the builder
	 */
	public TableMetaDataBuilder add(String label, ColumnInfo columnInfo) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		ValidationUtilV2.requireNonNull(columnInfo, "column info");
		columnInfo = checkedColumnInfo(columnInfo);
		addColumnInfo(label, columnInfo);
		return this;
	}

	/**
	 * Replaces the column info for the given label. Keeps the column meta data for the given label since it is
	 * considered being the same column.
	 *
	 * @param label
	 * 		the column name
	 * @param columnInfo
	 * 		the column info to add, must not be {@code null}. In case of unknown use {@link #add(String, ColumnType,
	 *        MDInteger)}
	 * @return the builder
	 */
	public TableMetaDataBuilder replace(String label, ColumnInfo columnInfo) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		ValidationUtilV2.requireNonNull(columnInfo, "column info");
		columnInfo = checkedColumnInfo(columnInfo);
		replaceColumnInfo(label, columnInfo);
		return this;
	}


	/**
	 * Replaces the column info for the given label with the given type and missing value information. Keeps the column
	 * meta data for the given label since it is considered being the same column.
	 *
	 * @param label
	 * 		the column name
	 * @param type
	 * 		the column type, can be {@code null} if not known
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder replace(String label, ColumnType<?> type, MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		replaceColumnInfo(label, new ColumnInfo(type, missings));
		return this;
	}

	/**
	 * Adds a nominal column info with the given dictionary values and missing value information for the given label.
	 * The dictionary values are automatically reduced if they are more than {@link
	 * AttributeMetaData#getMaximumNumberOfNominalValues()}.
	 *
	 * @param label
	 * 		the column name
	 * @param dictionaryValues
	 * 		the dictionary values, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the dictionary values
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder addNominal(String label, Collection<String> dictionaryValues, SetRelation setRelation,
										   MDInteger missings) {
		replaceNominal(label, dictionaryValues, setRelation, missings);
		ensureMetaDataOwnership();
		metaDataMap.remove(label);
		return this;
	}


	/**
	 * Adds a new nominal column info with a boolean dictionary with the given positive and negative value for the
	 * given label.
	 *
	 * @param label
	 * 		the column name
	 * @param positiveValue
	 * 		the positive value for the boolean dictionary, can be {@code null} if no positive exists
	 * @param negativeValue
	 * 		the negative value for the boolean dictionary, can be {@code null} if no negative exists
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder addBoolean(String label, String positiveValue, String negativeValue,
										   MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		addColumnInfo(label, new ColumnInfo(ColumnType.NOMINAL, positiveValue, negativeValue, missings));
		return this;
	}

	/**
	 * Adds a new nominal column with a boolean dictionary where the positive/negative value are unknown for the label.
	 * If the positive or negative value is known, use {@link #addBoolean(String, String, String, MDInteger)} instead.
	 *
	 * @param label
	 * 		the column name
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder addBoolean(String label, MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		addColumnInfo(label, new ColumnInfo(ColumnType.NOMINAL, SetRelation.UNKNOWN, new BooleanDictionaryInfo(),
				null, null, missings));
		return this;
	}

	/**
	 * Adds the column info for an integer column (of {@link ColumnType#INTEGER_53_BIT}) with the given range and
	 * missing values to the given label.
	 *
	 * @param label
	 * 		the column name
	 * @param range
	 * 		the value range, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the range
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder addInteger(String label, Range range, SetRelation setRelation, MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		addColumnInfo(label, new ColumnInfo(ColumnType.INTEGER_53_BIT, range, setRelation, missings));
		return this;
	}

	/**
	 * Adds the column info for a real column with the given range and missing values to the given label.
	 *
	 * @param label
	 * 		the column name
	 * @param range
	 * 		the value range, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the range
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder addReal(String label, Range range, SetRelation setRelation, MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		addColumnInfo(label, new ColumnInfo(ColumnType.REAL, range, setRelation, missings));
		return this;
	}

	/**
	 * Adds the column info for a date-time column with the given range and missing values to the given label.
	 *
	 * @param label
	 * 		the column name
	 * @param range
	 * 		the value range, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the range
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder addDateTime(String label, ObjectRange<Instant> range, SetRelation setRelation,
											MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		addColumnInfo(label, new ColumnInfo(new ColumnInfo.MatchingTypeRange<>(ColumnType.DATETIME, range), setRelation,
				missings));
		return this;
	}

	/**
	 * Adds the column info for a time column with the given range and missing values to the given label.
	 *
	 * @param label
	 * 		the column name
	 * @param range
	 * 		the value range, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the range
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder addTime(String label, ObjectRange<LocalTime> range, SetRelation setRelation,
										MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		addColumnInfo(label, new ColumnInfo(new ColumnInfo.MatchingTypeRange<>(ColumnType.TIME, range), setRelation,
				missings));
		return this;
	}

	/**
	 * Replaces the column info for the given label with a new nominal column info. Keeps the column meta data for the
	 * given label since it is considered being the same column. The dictionary values are automatically reduced if they
	 * are more than {@link AttributeMetaData#getMaximumNumberOfNominalValues()}.
	 *
	 * @param label
	 * 		the column name
	 * @param dictionaryValues
	 * 		the dictionary values, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the dictionary values
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder replaceNominal(String label, Collection<String> dictionaryValues, SetRelation setRelation,
											   MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		ColumnInfo columnInfo;
		if (dictionaryValues != null) {
			columnInfo = new ColumnInfo(ColumnType.NOMINAL, dictionaryValues,
					dictionaryBiggerThanHeight(dictionaryValues) ? setRelation.merge(SetRelation.SUBSET) :
							setRelation.merge(SetRelation.EQUAL), missings);
		} else {
			columnInfo =
					new ColumnInfo(ColumnType.NOMINAL, Collections.emptySet(), setRelation.merge(SetRelation.SUPERSET)
							, missings);
		}
		replaceColumnInfo(label, columnInfo);
		return this;
	}


	/**
	 * Replaces the column info for the given label with a new boolean column info. Keeps the column meta data for the
	 * given label since it is considered being the same column.
	 *
	 * @param label
	 * 		the column name
	 * @param positiveValue
	 * 		the positive value for the boolean dictionary, can be {@code null} if no positive exists
	 * @param negativeValue
	 * 		the negative value for the boolean dictionary, can be {@code null} if no negative exists
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder replaceBoolean(String label, String positiveValue, String negativeValue,
											   MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		replaceColumnInfo(label, new ColumnInfo(ColumnType.NOMINAL, positiveValue, negativeValue, missings));
		return this;
	}

	/**
	 * Replaces the column info for the given label with a new boolean column info. Keeps the column meta data for the
	 * given label since it is considered being the same column. If the positive or negative value is known, use {@link
	 * #replaceBoolean(String, String, String, MDInteger)} instead.
	 *
	 * @param label
	 * 		the column name
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder replaceBoolean(String label, MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		replaceColumnInfo(label, new ColumnInfo(ColumnType.NOMINAL, SetRelation.UNKNOWN, new BooleanDictionaryInfo(),
				null, null, missings));
		return this;
	}

	/**
	 * Replaces the column info for the given label with a new integer column info. Keeps the column meta data for the
	 * given label since it is considered being the same column.
	 *
	 * @param label
	 * 		the column name
	 * @param range
	 * 		the value range, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the range
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder replaceInteger(String label, Range range, SetRelation setRelation, MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		replaceColumnInfo(label, new ColumnInfo(ColumnType.INTEGER_53_BIT, range, setRelation, missings));
		return this;
	}

	/**
	 * Replaces the column info for the given label with a new real info. Keeps the column meta data for the given label
	 * since it is considered being the same column.
	 *
	 * @param label
	 * 		the column name
	 * @param range
	 * 		the value range, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the range
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder replaceReal(String label, Range range, SetRelation setRelation, MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		replaceColumnInfo(label, new ColumnInfo(ColumnType.REAL, range, setRelation, missings));
		return this;
	}

	/**
	 * Replaces the column info for the given label with a new date-time column info. Keeps the column meta data for the
	 * given label since it is considered being the same column.
	 *
	 * @param label
	 * 		the column name
	 * @param range
	 * 		the value range, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the range
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder replaceDateTime(String label, ObjectRange<Instant> range, SetRelation setRelation,
												MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		replaceColumnInfo(label, new ColumnInfo(new ColumnInfo.MatchingTypeRange<>(ColumnType.DATETIME, range), setRelation,
				missings));
		return this;
	}

	/**
	 * Replaces the column info for the given label with a new time column info. Keeps the column meta data for the
	 * given label since it is considered being the same column.
	 *
	 * @param label
	 * 		the column name
	 * @param range
	 * 		the value range, can be {@code null} if unknown
	 * @param setRelation
	 * 		the set relation of the range
	 * @param missings
	 * 		the number of missing values in the column, can be {@code null} if unknown
	 * @return the builder
	 */
	public TableMetaDataBuilder replaceTime(String label, ObjectRange<LocalTime> range, SetRelation setRelation,
											MDInteger missings) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		missings = checkedMissings(missings);
		replaceColumnInfo(label, new ColumnInfo(new ColumnInfo.MatchingTypeRange<>(ColumnType.TIME, range), setRelation,
				missings));
		return this;
	}

	/**
	 * Adds the given meta data to the column with the given label (if existent). If the meta data is column unique and
	 * there is already meta data of this type, it is replaced.
	 *
	 * @param label
	 * 		the column name
	 * @param metaData
	 * 		the meta data
	 * @return the builder
	 */
	public TableMetaDataBuilder addColumnMetaData(String label, ColumnMetaData metaData) {
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		ValidationUtilV2.requireNonNull(metaData, "column meta data");
		if (columns.containsKey(label)) {
			ensureMetaDataOwnership();
			addColumnMD(label, metaData);
		}
		return this;
	}

	/**
	 * Adds the given meta data to the column with the given label (if existent). If some of the meta data is column
	 * unique and there is already meta data of this type, it is replaced.
	 *
	 * @param label
	 * 		the column name
	 * @param metaData
	 * 		the meta data
	 * @return the builder
	 */
	public TableMetaDataBuilder addColumnMetaData(String label, List<ColumnMetaData> metaData) {
		//overwrite existing column-unique meta data
		ValidationUtilV2.requireNonNull(label, LABEL_STRING);
		ValidationUtilV2.requireNonNull(metaData, "column meta data");
		if (columns.containsKey(label)) {
			ensureMetaDataOwnership();
			for (ColumnMetaData item : metaData) {
				ValidationUtilV2.requireNonNull(item, "meta data entry");
				addColumnMD(label, item);
			}
		}
		return this;
	}

	/**
	 * Removes all meta data of the given type from the column with the given label. Has no effect if no such meta data
	 * is attached to the column.
	 *
	 * @param label
	 * 		the column name
	 * @param type
	 * 		the meta data type
	 * @return this builder
	 */
	public <T extends ColumnMetaData> TableMetaDataBuilder removeColumnMetaData(String label, Class<T> type) {
		ValidationUtilV2.requireNonNull(type, "type");
		ensureMetaDataOwnership();
		List<ColumnMetaData> list = metaDataMap.get(label);
		if (list != null) {
			list.removeIf(type::isInstance);
			if (list.isEmpty()) {
				metaDataMap.remove(label);
			}
		}
		return this;
	}

	/**
	 * Removes the given meta data from the column with the given label. Has no effect if no such meta data is attached
	 * to the column.
	 *
	 * @param label
	 * 		the column name
	 * @param metaData
	 * 		the meta data
	 * @return this builder
	 */
	public TableMetaDataBuilder removeColumnMetaData(String label, ColumnMetaData metaData) {
		ValidationUtilV2.requireNonNull(metaData, "meta data");
		ensureMetaDataOwnership();
		List<ColumnMetaData> list = metaDataMap.get(label);
		if (list != null) {
			list.remove(metaData);
			if (list.isEmpty()) {
				metaDataMap.remove(label);
			}
		}
		return this;
	}

	/**
	 * Removes all meta data from the column with the given label. Has no effect if no meta data is attached.
	 *
	 * @param label
	 * 		the column label
	 * @return this builder
	 */
	public TableMetaDataBuilder clearColumnMetadata(String label) {
		ensureMetaDataOwnership();
		metaDataMap.remove(label);
		return this;
	}

	/**
	 * Get the column information for the given label.
	 *
	 * @param label
	 * 		the column name
	 * @return the column information
	 */
	public ColumnInfo column(String label) {
		return columns.get(label);
	}

	/**
	 * Returns all column information as unmodifiable collection.
	 *
	 * @return all the column information
	 */
	public Collection<ColumnInfo> getColumns() {
		return Collections.unmodifiableCollection(columns.values());
	}

	/**
	 * Returns the column labels as unmodifiable set.
	 *
	 * @return all the column names
	 */
	public Set<String> labels() {
		return Collections.unmodifiableSet(columns.keySet());
	}

	/**
	 * Returns whether the table meta data contains a column of the given type.
	 *
	 * @param type
	 * 		the type to look for, can be {@code null} for checking for unknown type
	 * @param includeSpecials
	 * 		whether to include special columns (columns with column meta data of class {@link ColumnRole})
	 * @return whether the table contains the type
	 */
	public MetaDataInfo containsType(ColumnType<?> type, boolean includeSpecials) {
		return TableMetaData.containsType(type, includeSpecials, columns, columnsRelation, metaDataMap);
	}

	/**
	 * Select all column labels associated with columns of the given type.
	 *
	 * @param type
	 * 		the column type to look for, can be {@code null} for selecting all with unknown type
	 * @return a set of labels with columns of the given type
	 */
	public Set<String> selectByType(ColumnType<?> type) {
		return TableMetaData.selectByType(type, columns);
	}

	/**
	 * Select all column labels associated with columns of the given category.
	 *
	 * @param category
	 * 		the column category to look for
	 * @return a list of labels with columns of the given type
	 */
	public Set<String> selectByCategory(Column.Category category) {
		return TableMetaData.selectByCategory(category, columns);
	}

	/**
	 * Returns whether there is a column with the given label.
	 *
	 * @param label
	 * 		the column name
	 * @return whether there is a column with the given name
	 */
	public MetaDataInfo contains(String label) {
		return TableMetaData.containsColumnLabel(label, columns, columnsRelation);
	}

	/**
	 * Selects all column labels associated with column with the given column meta data. In particular, this method is
	 * useful to obtain all label with a certain column role, e.g. {@code selectByColumnMetaData(ColumnRole.LABEL)}.
	 *
	 * @param cmd
	 * 		the column meta data to look for
	 * @return all labels of columns with the given column meta data
	 */
	public Set<String> selectByColumnMetaData(ColumnMetaData cmd) {
		return metaDataMap.entrySet().stream().filter(e -> e.getValue().contains(cmd)).map(Map.Entry::getKey)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Selects all column labels associated with column metadata of the given type. In particular, this method is
	 * useful
	 * to obtain the labels of all special columns via {@code selectByColumnMetaData(ColumnRole.class)}.
	 *
	 * @param type
	 * 		the type of column meta data to look for
	 * @return all labels of columns with the given column meta data class
	 */
	public <T extends ColumnMetaData> Set<String> selectByColumnMetaData(Class<T> type) {
		Objects.requireNonNull(type, MESSAGE_METADATA_CLASS_NULL);
		return metaDataMap.entrySet().stream().filter(e ->
				TableMetaData.withMetaData(e.getValue(), type)).map(Map.Entry::getKey)
				.collect(Collectors.toCollection(LinkedHashSet::new ));
	}

	/**
	 * Return whether there is a column with the given column meta data in this table.
	 *
	 * @param cmd
	 * 		the column meta data to look for
	 * @return whether there is a column with the meta data
	 */
	public MetaDataInfo hasColumnMetaData(ColumnMetaData cmd) {
		return TableMetaData.hasColumnMetaData(cmd, columnsRelation, metaDataMap);
	}

	/**
	 * Return whether there is exactly one column with the given column meta data in this table.
	 *
	 * @param cmd
	 * 		the column meta data to look for
	 * @return whether there is a unique column with the meta data
	 */
	public MetaDataInfo hasUniqueColumnMetaData(ColumnMetaData cmd) {
		return TableMetaData.hasUniqueColumnMetaData(cmd, columnsRelation, metaDataMap);
	}

	/**
	 * Returns whether the actual columns are a sub-/super-set of the meta data columns, equal or unknown.
	 *
	 * @return the column set relation
	 */
	public SetRelation getColumnSetRelation() {
		return columnsRelation;
	}

	/**
	 * Returns the table's number of rows.
	 *
	 * @return a copy of the MDInteger representing the table's height
	 */
	public MDInteger height() {
		return height.copy();
	}

	/**
	 * Returns the meta data attached to the column with the given label (if any).
	 *
	 * @param label
	 * 		the column label
	 * @return unmodifiable list of meta data
	 */
	public List<ColumnMetaData> getColumnMetaData(String label) {
		return TableMetaData.getColumnMetaData(label, metaDataMap);
	}

	/**
	 * Returns the first meta datum with the given type attached to the column with the given label (if any). Use of
	 * this method instead of {@link #getColumnMetaData(String, Class)}  is recommended if it is known that there
	 * can be
	 * at most one match, e.g., if the meta data type has uniqueness level
	 * {@link com.rapidminer.belt.util.ColumnMetaData.Uniqueness#COLUMN}.
	 *
	 * @param label
	 * 		the column label
	 * @param type
	 * 		the meta data type
	 * @return the first attached meta datum or {@code null}
	 */
	public <T extends ColumnMetaData> T getFirstColumnMetaData(String label, Class<T> type) {
		return TableMetaData.getFirstColumnMetaData(label, type, metaDataMap);
	}

	/**
	 * Returns the meta data with the given type attached to the column with the given label (if any).
	 *
	 * @param label
	 * 		the column label
	 * @param type
	 * 		the meta data type
	 * @return the attached meta data
	 * @see #getFirstColumnMetaData(String, Class)
	 */
	public <T extends ColumnMetaData> List<T> getColumnMetaData(String label, Class<T> type) {
		return TableMetaData.getColumnMetaData(label, type, metaDataMap);
	}

	/**
	 * Builds a {@link TableMetaData} from the given information.
	 *
	 * @return the {@link TableMetaData} build from the specified data
	 */
	public TableMetaData build() {
		//construct table meta data
		TableMetaData metaData = new TableMetaData(height, columns, columnsRelation, metaDataMap);

		//set annotations
		metaData.getAnnotations().addAll(annotations);

		// copy additional data
		if (keyValueMap != null) {
			metaData.getKeyValueMap().putAll(keyValueMap);
		}

		// copy history
		if (history != null) {
			ListIterator<OutputPort> it = history.listIterator(history.size());
			while (it.hasPrevious()) {
				metaData.addToHistory(it.previous());
			}
		}

		return metaData;
	}

	/**
	 * Updates all references that point to the old label to the new label. Assumes the builder owns the metadata.
	 */
	private void updateReferences(String oldLabel, String newLabel) {
		for (Map.Entry<String, List<ColumnMetaData>> entry : metaDataMap.entrySet()) {
			int index = 0;
			for (ColumnMetaData metaData : entry.getValue()) {
				if (metaData instanceof ColumnReference) {
					if (oldLabel.equals(((ColumnReference) metaData).getColumn())) {
						metaDataMap.get(entry.getKey()).set(index, new ColumnReference(newLabel,
								((ColumnReference) metaData).getValue()));
					}
					break; //only one reference per label
				}
				index++;
			}
		}
	}

	/**
	 * Adds the column info for the given label. Removes column meta data for the given label since it is considered
	 * being overridden.
	 *
	 * @param label
	 * 		the label
	 * @param columnInfo
	 * 		the column info
	 */
	private void addColumnInfo(String label, ColumnInfo columnInfo) {
		replaceColumnInfo(label, columnInfo);
		ensureMetaDataOwnership();
		metaDataMap.remove(label);
	}

	/**
	 * Replaces the column info for the given label. Keeps column meta data for the given label since it is considered
	 * being the same column.
	 *
	 * @param label
	 * 		the label
	 * @param columnInfo
	 * 		the column info
	 */
	private void replaceColumnInfo(String label, ColumnInfo columnInfo) {
		if (columns.size() < TableMetaData.getMaximumNumberOfMdColumns() || columns.containsKey(label)) {
			columns.put(label, columnInfo);
		} else {
			columnsRelation = columnsRelation.merge(SetRelation.SUPERSET);
			LogService.getRoot().log(Level.INFO, "com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder.column_limit",
					TableMetaData.getMaximumNumberOfMdColumns());
		}
	}

	/**
	 * Rechecks all column infos when the height was changed away from the previous.
	 */
	private void recheckAllColumnInfo(MDInteger previous) {
		if (height.getRelation() == MDNumber.Relation.UNKNOWN) {
			return;
		}
		switch (previous.getRelation()) {
			case UNKNOWN:
				if (height.isKnown() || height.getRelation() == MDNumber.Relation.AT_MOST) {
					checkColumnInfos();
				}
				break;
			case EQUAL:
				handleEqualAdjustment(previous);
				break;
			case AT_LEAST:
				handleAtLeastAdjustment(previous);
				break;
			case AT_MOST:
				handleAtMostAdjustment(previous);
				break;
		}

	}

	/**
	 * Adjustments when the previous relation is at most.
	 */
	private void handleAtMostAdjustment(MDInteger previous) {
		if (previous.getNumber() > height.getNumber() &&
				(height.isKnown() || height.getRelation() == MDNumber.Relation.AT_MOST)) {
			checkColumnInfos();
		} else if (previous.getNumber() < height.getNumber() &&
				(height.isKnown() || height.getRelation() == MDNumber.Relation.AT_LEAST)) {
			increaseColumnInfos();
		}
	}

	/**
	 * Adjustments when the previous relation is at least.
	 */
	private void handleAtLeastAdjustment(MDInteger previous) {
		if (previous.getNumber() > height.getNumber() &&
				(height.isKnown() || height.getRelation() == MDNumber.Relation.AT_MOST)) {
			reduceColumnInfos();
		} else if (previous.getNumber() < height.getNumber() &&
				(height.isKnown() || height.getRelation() == MDNumber.Relation.AT_MOST)) {
			checkColumnInfos();
		}
	}

	/**
	 * Adjustments when the previous relation is equal.
	 */
	private void handleEqualAdjustment(MDInteger previous) {
		if (previous.getNumber() > height.getNumber() &&
				(height.isKnown() || height.getRelation() == MDNumber.Relation.AT_MOST)) {
			reduceColumnInfos();
		} else if (previous.getNumber() < height.getNumber() &&
				(height.isKnown() || height.getRelation() == MDNumber.Relation.AT_LEAST)) {
			increaseColumnInfos();
		}
	}

	/**
	 * Sanity check for all column infos.
	 */
	private void checkColumnInfos() {
		for (Map.Entry<String, ColumnInfo> entry : columns.entrySet()) {
			final ColumnInfo columnInfo = entry.getValue();
			final ColumnInfo checkedColumnInfo = checkedColumnInfo(columnInfo);
			if (columnInfo != checkedColumnInfo) {
				entry.setValue(checkedColumnInfo);
			}
		}
	}

	/**
	 * Reducing for all column infos.
	 */
	private void reduceColumnInfos() {
		for (Map.Entry<String, ColumnInfo> entry : columns.entrySet()) {
			final ColumnInfo columnInfo = entry.getValue();
			final ColumnInfo reducedColumnInfo = reducedColumnInfo(columnInfo);
			if (columnInfo != reducedColumnInfo) {
				entry.setValue(reducedColumnInfo);
			}
		}
	}

	/**
	 * Increasing for all column infos.
	 */
	private void increaseColumnInfos() {
		for (Map.Entry<String, ColumnInfo> entry : columns.entrySet()) {
			final ColumnInfo columnInfo = entry.getValue();
			final ColumnInfo increasedColumnInfo = increasedColumnInfo(columnInfo);
			if (columnInfo != increasedColumnInfo) {
				entry.setValue(increasedColumnInfo);
			}
		}
	}

	/**
	 * Increases the number of missing values.
	 */
	private ColumnInfo increasedColumnInfo(ColumnInfo columnInfo) {
		MDInteger storedMissingValues = columnInfo.getMissingValues();
		final boolean needIncrease =
				storedMissingValues.isKnown() || storedMissingValues.getRelation() == MDNumber.Relation.AT_MOST;
		if (needIncrease) {
			storedMissingValues = storedMissingValues.copy();
			storedMissingValues.increaseByUnknownAmount();
			return new ColumnInfo(columnInfo.getType().orElse(null), columnInfo.getValueSetRelation(),
					columnInfo.getDictionary(), columnInfo.getUncheckedObjectRange(),
					columnInfo.getNumericRange().orElse(null), storedMissingValues);
		}
		return columnInfo;
	}

	/**
	 * Reduces the number of missing values and makes nominal dictionaries a subset.
	 */
	private ColumnInfo reducedColumnInfo(ColumnInfo columnInfo) {
		MDInteger storedMissingValues = columnInfo.getMissingValues();
		final boolean needDecrease =
				storedMissingValues.isKnown() || storedMissingValues.getRelation() == MDNumber.Relation.AT_LEAST;
		if (needDecrease) {
			storedMissingValues = storedMissingValues.copy();
			storedMissingValues.reduceByUnknownAmount();
		}
		// check if merging with subset would change current relation
		boolean valueSetShouldBeSubsetMerged = columnInfo.getValueSetRelation() == SetRelation.SUPERSET ||
						columnInfo.getValueSetRelation() == SetRelation.EQUAL;
		if (needDecrease || valueSetShouldBeSubsetMerged) {
			SetRelation valueSetRelation = columnInfo.getValueSetRelation();
			if (valueSetShouldBeSubsetMerged) {
				valueSetRelation = valueSetRelation.merge(SetRelation.SUBSET);
			}
			columnInfo = new ColumnInfo(columnInfo.getType().orElse(null), valueSetRelation,
					columnInfo.getDictionary(), columnInfo.getUncheckedObjectRange(),
					columnInfo.getNumericRange().orElse(null), storedMissingValues);
		}
		return columnInfo;
	}

	/**
	 * Makes sure the number of missing values is not a contradiction to the number of rows.
	 */
	private ColumnInfo checkedColumnInfo(ColumnInfo columnInfo) {
		MDInteger storedMissingValues = columnInfo.getMissingValues();
		final boolean notMatchingRows = isNotMatchingHeight(storedMissingValues);
		if (notMatchingRows) {
			storedMissingValues = new MDInteger(height.getNumber());
			storedMissingValues.reduceByUnknownAmount();
		}
		boolean dictShouldBeSubset = columnInfo.isNominal() == MetaDataInfo.YES &&
				dictionaryBiggerThanHeight(columnInfo.getDictionary().getValueSet());
		if (notMatchingRows || dictShouldBeSubset) {
			SetRelation valueSetRelation = columnInfo.getValueSetRelation();
			if (dictShouldBeSubset) {
				valueSetRelation = valueSetRelation.merge(SetRelation.SUBSET);
			}
			columnInfo = new ColumnInfo(columnInfo.getType().orElse(null), valueSetRelation,
					columnInfo.getDictionary(), columnInfo.getUncheckedObjectRange(),
					columnInfo.getNumericRange().orElse(null), storedMissingValues);
		}
		return columnInfo;
	}

	/**
	 * Checks if the dictionary is supposed to be bigger than the height.
	 */
	private boolean dictionaryBiggerThanHeight(Collection<String> dictionaryValues) {
		return (height.isKnown() || height.getRelation() == MDNumber.Relation.AT_MOST) &&
				(dictionaryValues.size() > height.getNumber());
	}

	/**
	 * Adjusts the missing values to fit the number of rows and creates a copy.
	 */
	private MDInteger checkedMissings(MDInteger missings) {
		if (missings == null) {
			return MDInteger.newUnknown();
		} else if (isNotMatchingHeight(missings)) {
			//adjust missings to number of rows
			MDInteger adjustedMissings = new MDInteger(height.getNumber());
			adjustedMissings.reduceByUnknownAmount();
			return adjustedMissings;
		} else if (missings.getNumber() < 0) {
			if (missings.getRelation() == MDNumber.Relation.AT_MOST) {
				//return =0 instead of <=0
				return new MDInteger();
			} else {
				missings = missings.copy();
				missings.add(-missings.getNumber());
				return missings;
			}
		}
		//copy so that checked value cannot be changed by api user
		return missings.copy();
	}

	/**
	 * Checks if the number of missing values contradicts the number of rows.
	 */
	private boolean isNotMatchingHeight(MDInteger missings) {
		return (missings.isKnown() || missings.getRelation() == MDNumber.Relation.AT_LEAST) &&
				(height.isKnown() || height.getRelation() == MDNumber.Relation.AT_MOST) &&
				(missings.getNumber() > height.getNumber());
	}

	/**
	 * Ensures a deep copy of the meta data.
	 */
	private void ensureMetaDataOwnership() {
		if (!ownsColumnMetaData) {
			Map<String, List<ColumnMetaData>> copy = new HashMap<>(metaDataMap.size());
			for (Map.Entry<String, List<ColumnMetaData>> entry : metaDataMap.entrySet()) {
				copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
			}
			metaDataMap = copy;
			ownsColumnMetaData = true;
		}
	}

	/**
	 * Checks if the meta data for the label contains the one given metaData.
	 */
	private boolean containsMetaData(String label, ColumnMetaData metaData) {
		if (metaDataMap.containsKey(label)) {
			List<ColumnMetaData> list = metaDataMap.get(label);
			return list.contains(metaData);
		}
		return false;
	}

	/**
	 * Adds column meta data for the label.
	 */
	private void addColumnMD(String label, ColumnMetaData metaData) {
		// overwrite existing column-unique meta data
		if (metaData.uniqueness() == ColumnMetaData.Uniqueness.COLUMN) {
			removeColumnMetaData(label, metaData.getClass());
		} else if (containsMetaData(label, metaData)) {
			// already contained
			return;
		}
		// add, ignoring table uniqueness
		List<ColumnMetaData> list = metaDataMap.computeIfAbsent(label, s -> new ArrayList<>(1));
		list.add(metaData);
	}

}
