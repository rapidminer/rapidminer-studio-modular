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

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.gui.processeditor.results.DisplayContext;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.settings.Settings;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Meta data for {@link IOTable}s. Use the {@link TableMetaDataBuilder} to construct table meta data.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public final class TableMetaData extends MetaData {

	/**
	 * The {@link Settings} key for the maximal number of columns in table meta data.
	 */
	public static final String SETTING_METADATA_COLUMN_LIMIT = "rapidminer.metadata.column_limit";

	/**
	 * The cached value for {@link #SETTING_METADATA_COLUMN_LIMIT} parsed from the settings
	 */
	private static int maximumNumberOfMdColumns;

	private static final String MESSAGE_METADATA_CLASS_NULL = "Column metadata type must not be null.";

	static {
		Settings.addSettingsListener((context, key, value) -> {
			if (SETTING_METADATA_COLUMN_LIMIT.equals(key)) {
				maximumNumberOfMdColumns = getMaximumNumberOfColumns();
			}
		});
		maximumNumberOfMdColumns = getMaximumNumberOfColumns();
	}

	//actual columns are sub/super/equals to this column information
	private SetRelation columnsRelation = SetRelation.EQUAL;

	private MDInteger height = MDInteger.newPossible();
	/**
	 * Not serializable, but we convert to {@link com.rapidminer.operator.ports.metadata.ExampleSetMetaData} for
	 * serialization.
	 */
	private transient Map<String, ColumnInfo> columns = new LinkedHashMap<>();
	private transient Map<String, List<ColumnMetaData>> metaDataMap = new HashMap<>();

	/**
	 * Creates an empty table meta data.
	 */
	public TableMetaData() {
		super(IOTable.class);
	}

	/**
	 * Creates a table meta data by setting its fields. All parameter must not be {@code null}.
	 */
	TableMetaData(MDInteger height, Map<String, ColumnInfo> columns, SetRelation columnsRelation,
				  Map<String, List<ColumnMetaData>> metaDataMap) {
		super(IOTable.class);
		this.columnsRelation = columnsRelation;
		this.height = height;
		this.columns = columns;
		this.metaDataMap = metaDataMap;
	}


	/**
	 * Creates a {@link TableMetaData} object for the provided ioTable object. The meta data contains only the first
	 * {@link } columns or all columns if this is negative.
	 *
	 * @param ioTable
	 * 		the table object the meta data should be constructed for
	 * @param ignoreStatistics
	 * 		defines whether the table statistics should be ignored or added
	 */
	public TableMetaData(IOTable ioTable, boolean ignoreStatistics) {
		super(IOTable.class);
		getAnnotations().addAll(ioTable.getAnnotations());
		final Table table = ioTable.getTable();
		height = new MDInteger(table.height());
		List<String> labels = table.labels();
		final int maxColumns = maximumNumberOfMdColumns;
		if (maxColumns > 0 && labels.size() > maxColumns) {
			labels = labels.subList(0, maxColumns);
			columnsRelation = SetRelation.SUPERSET;
		}
		for (String label : labels) {
			final List<ColumnMetaData> metaData = table.getMetaData(label);
			if (metaData != null && !metaData.isEmpty()) {
				metaDataMap.put(label, metaData);
			}
			final Column column = table.column(label);
			if (ignoreStatistics) {
				addInfosWithoutStatistics(label, column);
			} else {
				addInfosWithStatistics(label, column);
			}
		}
	}

	@Override
	public TableMetaData clone() {
		//on clone need to clone annotations, so clone super
		TableMetaData clone = (TableMetaData) super.clone();
		clone.columnsRelation = this.columnsRelation;
		clone.height = this.height;
		clone.columns = this.columns;
		clone.metaDataMap = this.metaDataMap;
		return clone;
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
	 * Select all column labels associated with columns of the given type.
	 *
	 * @param type
	 * 		the column type to look for, can be {@code null} for selecting all with unknown type
	 * @return a list of labels with columns of the given type
	 */
	public Set<String> selectByType(ColumnType<?> type) {
		return selectByType(type, columns);
	}

	/**
	 * Select all column labels associated with columns of the given category.
	 *
	 * @param category
	 * 		the column category to look for
	 * @return a list of labels with columns of the given type
	 */
	public Set<String> selectByCategory(Column.Category category) {
		return selectByCategory(category, columns);
	}

	/**
	 * Returns whether the table meta data contains a column of the given type.
	 *
	 * @param type
	 * 		the type to look for, can be {@code null} for checking for unknown type
	 * @param includeSpecials
	 * 		whether to include special columns, e.g. columns with column meta data of class {@link ColumnRole}
	 * @return whether the table contains the type
	 */
	public MetaDataInfo containsType(ColumnType<?> type, boolean includeSpecials) {
		return containsType(type, includeSpecials, columns, columnsRelation, metaDataMap);
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
		return getColumnMetaData(label, type, metaDataMap);
	}

	/**
	 * Returns the first meta datum with the given type attached to the column with the given label (if any). Use of
	 * this method instead of {@link #getColumnMetaData(String, Class)}  is recommended if it is known that there can be at
	 * most one match, e.g., if the meta data type has uniqueness level
	 * {@link com.rapidminer.belt.util.ColumnMetaData.Uniqueness#COLUMN}.
	 *
	 * @param label
	 * 		the column label
	 * @param type
	 * 		the meta data type
	 * @return the first attached meta datum or {@code null}
	 */
	public <T extends ColumnMetaData> T getFirstColumnMetaData(String label, Class<T> type) {
		return getFirstColumnMetaData(label, type, metaDataMap);
	}

	/**
	 * Returns the meta data attached to the column with the given label (if any).
	 *
	 * @param label
	 * 		the column label
	 * @return unmodifiable list of meta data
	 */
	public List<ColumnMetaData> getColumnMetaData(String label) {
		return getColumnMetaData(label, metaDataMap);
	}

	/**
	 * Selects all column labels associated with columns with the given column meta data. In particular, this method is
	 * useful to obtain all labels with a certain column role, e.g. {@code selectByColumnMetaData(ColumnRole.LABEL)}.
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
	 * useful to obtain the labels of all special columns via {@code selectByColumnMetaData(ColumnRole.class)}.
	 *
	 * @param type
	 * 		the type of column meta data to look for
	 * @return all labels of columns with the given column meta data class
	 */
	public <T extends ColumnMetaData> Set<String> selectByColumnMetaData(Class<T> type) {
		Objects.requireNonNull(type, MESSAGE_METADATA_CLASS_NULL);
		return metaDataMap.entrySet().stream().filter(e -> withMetaData(e.getValue(), type)).map(Map.Entry::getKey)
				.collect(Collectors.toCollection( LinkedHashSet::new ));
	}

	/**
	 * Return whether there is a column with the given column meta data in this table.
	 *
	 * @param cmd
	 * 		the column meta data to look for
	 * @return whether there is a column with the meta data
	 */
	public MetaDataInfo hasColumnMetaData(ColumnMetaData cmd) {
		return hasColumnMetaData(cmd, columnsRelation, metaDataMap);
	}

	/**
	 * Return whether there is exactly one column with the given column meta data in this table.
	 *
	 * @param cmd
	 * 		the column meta data to look for
	 * @return whether there is a unique column with the meta data
	 */
	public MetaDataInfo hasUniqueColumnMetaData(ColumnMetaData cmd) {
		return hasUniqueColumnMetaData(cmd, columnsRelation, metaDataMap);
	}

	/**
	 * Returns whether there is a column with the given label.
	 *
	 * @param label
	 * 		the column name
	 * @return whether there is a column with the given name
	 */
	public MetaDataInfo contains(String label) {
		return containsColumnLabel(label, columns, columnsRelation);
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
	 * Returns the number of rows in the table.
	 *
	 * @return a copy of the MDInteger representing the table's height
	 */
	public MDInteger height() {
		return height.copy();
	}

	/**
	 * Creates a new table with old names replaced by those specified by the renaming map. This does also rename any
	 * {@link ColumnReference}s. Old names that are not in the table are ignored.
	 *
	 * @param renamingMap
	 * 		a map from old name to new name
	 * @return a new table with renamed columns
	 * @throws NullPointerException
	 * 		if the renaming map is {@code null} or contains {@code null}
	 */
	public TableMetaData rename(Map<String, String> renamingMap) {
		ValidationUtilV2.requireNonNull(renamingMap, "renaming map");
		if (columns.size() == 0 || renamingMap.isEmpty()) {
			return this;
		}
		TableMetaDataBuilder builder = new TableMetaDataBuilder(this);
		for (String oldName : renamingMap.keySet()) {
			// remove the old column
			builder.remove(oldName);
		}
		for (Map.Entry<String, String> entry : renamingMap.entrySet()) {
			String oldName = entry.getKey();
			String newName = entry.getValue();
			if (columns.containsKey(oldName)) {
				// add the new column
				builder.add(newName, columns.get(oldName));
				List<ColumnMetaData> columnMetaData = metaDataMap.get(oldName);
				if (columnMetaData != null) {
					builder.addColumnMetaData(newName, columnMetaData);
				}
			} // else: ignore
		}
		renameReferences(builder, renamingMap);
		return builder.build();
	}

	/**
	 * Helper method used in {@link #rename(Map)}. Runs once over all meta data and checks for {@link ColumnReference}s
	 * that refer to a column that is renamed.
	 *
	 * @param renamingMap
	 * 		the map from old to new column name
	 */
	private void renameReferences(TableMetaDataBuilder builder, Map<String, String> renamingMap) {
		for (String label : builder.labels()) {
			ColumnReference reference = builder.getFirstColumnMetaData(label, ColumnReference.class);
			if (reference != null) {
				String newName = renamingMap.get(reference.getColumn());
				if (newName != null) {
					builder.addColumnMetaData(label, new ColumnReference(newName, reference.getValue()));
				}
			}
		}
	}

	/**
	 * Returns {@code null}.
	 *
	 * @deprecated use {@link #getAnnotations()} instead
	 */
	@Override
	@Deprecated
	public Object getAdditionalData(String key) {
		return null;
	}

	/**
	 * Always throws an unsupported exception.
	 *
	 * @deprecated use {@code getAnnotations().put(key,value)} instead
	 */
	@Override
	@Deprecated
	public Object addAdditionalData(String key, Object value) {
		throw new UnsupportedOperationException("Use getAnnotations().put(key,value) instead");
	}

	/**
	 * Returns {@code null}.
	 *
	 * @deprecated use {@link #getAnnotations()} instead
	 */
	@Override
	@Deprecated
	public String getAdditionalDataAsString(String key) {
		return null;
	}

	/**
	 * Always throws an unsupported exception.
	 *
	 * @deprecated use {@code getAnnotations().put(key,value)} instead
	 */
	@Override
	@Deprecated
	public String addAdditionalData(String key, String value) {
		throw new UnsupportedOperationException("Use getAnnotations().put(key,value) instead");
	}

	@Override
	public String toString() {
		return TableMDDisplayUtils.toString(this);
	}

	@Override
	public String getDescription() {
		//use the legacy description here, so that display in Result History is the same as for ExampleSetMetaData
		return TableMDDisplayUtils.getLegacyDescription(super.getDescription(), this);
	}

	public String getShortDescription() {
		//use the legacy description here, so that the tooltip popup looks the same as for ExampleSetMetaData
		return TableMDDisplayUtils.getLegacyShortDescription(super.getDescription(), this);
	}

	/**
	 * Filters this TableMetaData for the columns with the given labels. Labels that do not exist are ignored.
	 *
	 * @param columnLabels
	 * 		the labels of the columns to add to the new TableMetaData
	 * @return new {@link TableMetaData}
	 */
	public TableMetaData columns(Collection<String> columnLabels){
		// filter columns and meta data
		Map<String, ColumnInfo> filteredColums = new HashMap<>();
		Map<String, List<ColumnMetaData>> filteredMD = new HashMap<>();
		for (String label : columnLabels) {
			ColumnInfo info = columns.get(label);
			if (info != null) {
				filteredColums.put(label, info);
				List<ColumnMetaData> md = metaDataMap.get(label);
				if (md != null && !md.isEmpty()) {
					filteredMD.put(label, md);
				}
			}
		}

		SetRelation sr = filteredColums.isEmpty() && columnsRelation == SetRelation.SUBSET ? SetRelation.EQUAL
				: columnsRelation;

		TableMetaData subset = new TableMetaData(height, filteredColums, sr, filteredMD);
		subset.setAnnotations(new Annotations(getAnnotations()));
		subset.keyValueMap.putAll(keyValueMap);

		// copy generation history
		List<OutputPort> generationHistory = getGenerationHistory();
		ListIterator<OutputPort> it = generationHistory.listIterator(generationHistory.size());
		while (it.hasPrevious()) {
			subset.addToHistory(it.previous());
		}

		return subset;
	}

	/**
	 * When serializing (only used for storing in old repo), convert to
	 * {@link com.rapidminer.operator.ports.metadata.ExampleSetMetaData}.
	 */
	private Object writeReplace() throws ObjectStreamException {
		return FromTableMetaDataConverter.convert(this);
	}

	/**
	 * @return the internal map of columns
	 */
	Map<String, ColumnInfo> getFullColumns() {
		return columns;
	}

	/**
	 * @return the internal meta data map
	 */
	Map<String, List<ColumnMetaData>> getMetaDataMap() {
		return metaDataMap;
	}

	/**
	 * @return the internal key value map
	 */
	Map<String, Object> getKeyValueMap() {
		return keyValueMap;
	}

	/**
	 * Returns the maximal number of columns in meta data defined by the setting {@link #SETTING_METADATA_COLUMN_LIMIT}
	 *
	 * @return the limit for column infos in table meta data
	 */
	public static int getMaximumNumberOfMdColumns() {
		return maximumNumberOfMdColumns;
	}

	/**
	 * Returns the meta data attached to the column with the given label (if any).
	 *
	 * @param label
	 * 		the column label
	 * @param metaDataMap
	 * 		the map to check
	 * @return unmodifiable list of meta data
	 */
	static List<ColumnMetaData> getColumnMetaData(String label, Map<String, List<ColumnMetaData>> metaDataMap) {
		List<ColumnMetaData> meta = metaDataMap.get(label);
		if (meta == null) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableList(meta);
		}
	}

	/**
	 * Returns the meta data with the given type attached to the column with the given label (if any).
	 *
	 * @param label
	 * 		the column label
	 * @param type
	 * 		the meta data type
	 * @param metaDataMap
	 * 		the map to check
	 * @return the attached meta data
	 */
	static <T extends ColumnMetaData> List<T> getColumnMetaData(String label, Class<T> type, Map<String,
			List<ColumnMetaData>> metaDataMap) {
		ValidationUtilV2.requireNonNull(type, "type");
		List<ColumnMetaData> meta = metaDataMap.get(label);
		if (meta == null) {
			return Collections.emptyList();
		} else {
			List<T> subset = new ArrayList<>(meta.size());
			for (ColumnMetaData data : meta) {
				if (type.isInstance(data)) {
					subset.add(type.cast(data));
				}
			}
			return subset;
		}
	}

	/**
	 * Returns the first meta datum with the given type attached to the column with the given label (if any).
	 *
	 * @param label
	 * 		the column label
	 * @param type
	 * 		the meta data type
	 * @param metaDataMap
	 * 		the map to check
	 * @return the first attached meta datum or {@code null}
	 */
	static <T extends ColumnMetaData> T getFirstColumnMetaData(String label, Class<T> type, Map<String,
			List<ColumnMetaData>> metaDataMap) {
		ValidationUtilV2.requireNonNull(type, "type");
		List<ColumnMetaData> metaData = metaDataMap.get(label);
		if (metaData != null) {
			for (ColumnMetaData data : metaData) {
				if (type.isInstance(data)) {
					return type.cast(data);
				}
			}
		}
		return null;
	}

	/**
	 * Returns whether the table meta data contains a column of the given type.
	 *
	 * @param type
	 * 		the type to look for
	 * @param includeSpecials
	 * 		whether to include special columns, e.g. columns with column meta data of class {@link ColumnRole}
	 * @param columns
	 * 		the columns to check
	 * @param columnsRelation
	 * 		relation of the columns
	 * @param metaDataMap
	 * 		the map with the column meta data
	 * @return whether the table contains the type
	 */
	static MetaDataInfo containsType(ColumnType<?> type, boolean includeSpecials, Map<String, ColumnInfo> columns,
									 SetRelation columnsRelation, Map<String, List<ColumnMetaData>> metaDataMap) {
		if (columnsRelation == SetRelation.UNKNOWN) {
			return MetaDataInfo.UNKNOWN;
		}

		if (type == null) {
			return containsUnknownType(includeSpecials, columns, columnsRelation, metaDataMap);
		} else {
			return containsKnownType(type, includeSpecials, columns, columnsRelation, metaDataMap);
		}
	}


	/**
	 * Return whether there is a column with the given column meta data in this table.
	 *
	 * @param cmd
	 * 		the column meta data to look for
	 * @param columnsRelation
	 * 		relation of the columns
	 * @param metaDataMap
	 * 		the map with the column meta data
	 * @return whether there is a column with the meta data
	 */
	static MetaDataInfo hasColumnMetaData(ColumnMetaData cmd, SetRelation columnsRelation, Map<String,
			List<ColumnMetaData>> metaDataMap) {
		ValidationUtilV2.requireNonNull(cmd, "column meta data");
		if (columnsRelation == SetRelation.UNKNOWN) {
			return MetaDataInfo.UNKNOWN;
		}
		for (List<ColumnMetaData> value : metaDataMap.values()) {
			if (value.contains(cmd)) {
				//found
				if (columnsRelation == SetRelation.SUBSET) {
					//do not know if found is in the subset
					return MetaDataInfo.UNKNOWN;
				} else {
					return MetaDataInfo.YES;
				}
			}
		}
		//not found
		return handleNotFound(columnsRelation);
	}

	/**
	 * Return whether there is exactly one column with the given column meta data in this table.
	 *
	 * @param cmd
	 * 		the column meta data to look for
	 * @param columnsRelation
	 * 		relation of the columns
	 * @param metaDataMap
	 * 		the map with the column meta data
	 * @return whether there is a unique column with the meta data
	 */
	static MetaDataInfo hasUniqueColumnMetaData(ColumnMetaData cmd, SetRelation columnsRelation, Map<String,
			List<ColumnMetaData>> metaDataMap) {
		ValidationUtilV2.requireNonNull(cmd, "column meta data");
		if (columnsRelation == SetRelation.UNKNOWN) {
			return MetaDataInfo.UNKNOWN;
		}
		boolean alreadyFound = false;
		for (List<ColumnMetaData> value : metaDataMap.values()) {
			if (value.contains(cmd)) {
				if (alreadyFound) {
					//found twice
					if (columnsRelation == SetRelation.SUBSET) {
						//do not know if the found are in the subset
						return MetaDataInfo.UNKNOWN;
					} else {
						return MetaDataInfo.NO;
					}
				} else {
					alreadyFound = true;
				}
			}
		}
		return handleResult(columnsRelation, alreadyFound);
	}

	/**
	 * Returns whether there is a column with the given label.
	 *
	 * @param label
	 * 		the column name
	 * @param columns
	 * 		the columns to check
	 * @param columnsRelation
	 * 		relation of the columns
	 * @return whether there is a column with the given name
	 */
	static MetaDataInfo containsColumnLabel(String label, Map<String, ColumnInfo> columns,
											SetRelation columnsRelation) {
		boolean contains = columns.containsKey(label);
		switch (columnsRelation) {
			case EQUAL:
				return contains ? MetaDataInfo.YES : MetaDataInfo.NO;
			case SUPERSET:
				return contains ? MetaDataInfo.YES : MetaDataInfo.UNKNOWN;
			case SUBSET:
				return contains ? MetaDataInfo.UNKNOWN : MetaDataInfo.NO;
			case UNKNOWN:
			default:
				return MetaDataInfo.UNKNOWN;
		}
	}

	/**
	 * Select all column labels associated with columns of the given type.
	 *
	 * @param type
	 * 		the column type to look for
	 * @param columns
	 * 		the columns to check for the type
	 * @return a set of labels with columns of the given type
	 */
	static Set<String> selectByType(ColumnType<?> type, Map<String, ColumnInfo> columns) {
		return columns.entrySet().stream().filter(e -> (type == null && !e.getValue().getType().isPresent()) ||
				(e.getValue().getType().filter(t -> t.equals(type)).isPresent()))
				.map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Select all column labels associated with columns of the given category.
	 *
	 * @param category
	 * 		the column category to look for
	 * @param columns
	 * 		the columns to check for the type
	 * @return a set of labels with columns of the given type
	 */
	static Set<String> selectByCategory(Column.Category category, Map<String, ColumnInfo> columns) {
		return columns.entrySet().stream()
				.filter(e ->(e.getValue().getType().filter(t -> t.category() == category).isPresent()))
				.map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Adds the column infos for the column without statistics.
	 */
	private void addInfosWithoutStatistics(String label, Column column) {
		if (column.type().id() == Column.TypeId.NOMINAL) {
			final Dictionary dictionary = column.getDictionary();
			if (dictionary.isBoolean()) {
				String posValue = dictionary.get(dictionary.getPositiveIndex());
				String negValue = dictionary.get(dictionary.getNegativeIndex());
				columns.put(label, new ColumnInfo(ColumnType.NOMINAL, posValue, negValue, MDInteger.newPossible()));
			} else {
				List<String> values = getFirstDictionaryValues(dictionary);
				columns.put(label, new ColumnInfo(ColumnType.NOMINAL, values, SetRelation.EQUAL,
						MDInteger.newPossible()));
			}
		} else {
			columns.put(label, new ColumnInfo(column.type(), MDInteger.newPossible()));
		}
	}

	/**
	 * Adds the column infos for the column with statistics.
	 */
	private void addInfosWithStatistics(String label, Column column) {
		if (column.type().id() == Column.TypeId.NOMINAL) {
			addNominalInfoStatistics(label, column);
		} else if (column.type().category() == Column.Category.NUMERIC) {
			addNumericInfoStatistics(label, column);
		} else if (Statistics.supported(column, Statistics.Statistic.MIN) &&
				Statistics.supported(column, Statistics.Statistic.MAX) &&
				Statistics.supported(column, Statistics.Statistic.COUNT)) {
			addMinMaxCountStatistics(label, column);
		} else if (Statistics.supported(column, Statistics.Statistic.COUNT)) {
			final Statistics.Result count =
					Statistics.compute(column, Statistics.Statistic.COUNT, new DisplayContext());
			int missings = height.getNumber() - (int) count.getNumeric();
			columns.put(label, new ColumnInfo(column.type(), new MDInteger(missings)));
		} else {
			columns.put(label, new ColumnInfo(column.type(), MDInteger.newPossible()));
		}
	}

	/**
	 * Adds a column info for object columns that have min, max and count statistics.
	 */
	private void addMinMaxCountStatistics(String label, Column column) {
		final Map<Statistics.Statistic, Statistics.Result> resultMap =
				Statistics.compute(column, EnumSet.of(Statistics.Statistic.COUNT, Statistics.Statistic.MIN
						, Statistics.Statistic.MAX), new DisplayContext());
		int missings =
				height.getNumber() - (int) resultMap.get(Statistics.Statistic.COUNT).getNumeric();
		columns.put(label, new ColumnInfo(extractRange(column.type(), resultMap), SetRelation.EQUAL,
				new MDInteger(missings)));
	}

	/**
	 * Adds a numeric column info with range and missings.
	 */
	private void addNumericInfoStatistics(String label, Column column) {
		final Map<Statistics.Statistic, Statistics.Result> resultMap =
				Statistics.compute(column, EnumSet.of(Statistics.Statistic.COUNT, Statistics.Statistic.MIN,
						Statistics.Statistic.MAX), new DisplayContext());
		int missings =
				height.getNumber() - (int) resultMap.get(Statistics.Statistic.COUNT).getNumeric();
		final double min = resultMap.get(Statistics.Statistic.MIN).getNumeric();
		final double max = resultMap.get(Statistics.Statistic.MAX).getNumeric();
		Range range;
		if (Double.isNaN(min) || Double.isNaN(max)) {
			range = null;
		} else {
			range = new Range(min, max);
		}
		columns.put(label, new ColumnInfo((ColumnType<Void>) column.type(), range,SetRelation.EQUAL, new MDInteger(missings)));
	}

	/**
	 * Adds a numeric column info with dictionary and missings.
	 */
	private void addNominalInfoStatistics(String label, Column column) {
		final Statistics.Result count =
				Statistics.compute(column, Statistics.Statistic.COUNT, new DisplayContext());
		int missings = height.getNumber() - (int) count.getNumeric();
		final Dictionary dictionary = column.getDictionary();
		if (dictionary.isBoolean()) {
			String posValue = dictionary.get(dictionary.getPositiveIndex());
			String negValue = dictionary.get(dictionary.getNegativeIndex());
			if (posValue != null || negValue != null) {
				columns.put(label, new ColumnInfo(ColumnType.NOMINAL, posValue, negValue, new MDInteger(missings)));
			} else {
				columns.put(label, new ColumnInfoBuilder(ColumnType.NOMINAL).setUnknownBooleanDictionary()
						.setValueSetRelation(SetRelation.EQUAL).setMissings(missings).build());
			}
		} else {
			List<String> values = getFirstDictionaryValues(dictionary);
			columns.put(label, new ColumnInfo(ColumnType.NOMINAL, values, SetRelation.EQUAL,
					new MDInteger(missings)));
		}
	}

	/**
	 * Gets the first values from the dictionary.
	 */
	private List<String> getFirstDictionaryValues(Dictionary dictionary) {
		final int maxDictSize = AttributeMetaData.getMaximumNumberOfNominalValues();
		final int size = Math.min(dictionary.size(),
				maxDictSize + 1); //take one more for auto detection that it is more
		List<String> values = new ArrayList<>(size);
		final Iterator<Dictionary.Entry> iterator = dictionary.iterator();
		for (int i = 0; i < size; i++) {
			values.add(iterator.next().getValue());
		}
		return values;
	}

	/**
	 * Handles the found result depending on the columns relation: if it is found and the relation is equals, then the
	 * result is yes, otherwise it is unknown. If it is not found and the relation is superset then the result is
	 * unknown, otherwise it is no.
	 *
	 * @param columnsRelation
	 * 		the relation of the columns for which something was found or not
	 * @param found
	 * 		whether something was found in the meta data column which are related to the actual column with the
	 * 		columnsRelation
	 */
	private static MetaDataInfo handleResult(SetRelation columnsRelation, boolean found) {
		if (found) {
			//found once
			if (columnsRelation == SetRelation.EQUAL) {
				return MetaDataInfo.YES;
			} else {
				//if subset: do not know if the found is in it
				//if superset: there might be more
				return MetaDataInfo.UNKNOWN;
			}
		}
		//not found
		return handleNotFound(columnsRelation);
	}

	/**
	 * Checks for a known type.
	 */
	private static MetaDataInfo containsKnownType(ColumnType<?> type, boolean includeSpecials,
												  Map<String, ColumnInfo> columns, SetRelation columnsRelation,
												  Map<String, List<ColumnMetaData>> metaDataMap) {
		for (Map.Entry<String, ColumnInfo> value : columns.entrySet()) {
			if (!includeSpecials && getFirstColumnMetaData(value.getKey(), ColumnRole.class, metaDataMap) != null) {
				continue;
			}
			final Optional<ColumnType<?>> columnType = value.getValue().getType();
			if (columnType.isPresent() && columnType.get().equals(type)) {
				//found
				if (columnsRelation == SetRelation.SUBSET) {
					//do not know if found is in the subset
					return MetaDataInfo.UNKNOWN;
				} else {
					return MetaDataInfo.YES;
				}
			}
		}
		return handleNotFound(columnsRelation);
	}

	/**
	 * Checks for an unknown type.
	 */
	private static MetaDataInfo containsUnknownType(boolean includeSpecials, Map<String, ColumnInfo> columns,
													SetRelation columnsRelation,
													Map<String, List<ColumnMetaData>> metaDataMap) {
		for (Map.Entry<String, ColumnInfo> value : columns.entrySet()) {
			if (!includeSpecials && getFirstColumnMetaData(value.getKey(), ColumnRole.class, metaDataMap) != null) {
				continue;
			}
			final Optional<ColumnType<?>> columnType = value.getValue().getType();
			if (!columnType.isPresent()) {
				//found
				if (columnsRelation == SetRelation.SUBSET) {
					//do not know if found is in the subset
					return MetaDataInfo.UNKNOWN;
				} else {
					return MetaDataInfo.YES;
				}
			}
		}
		return handleNotFound(columnsRelation);
	}

	/**
	 * Handles the not found with respect to the columns relation: In case of superset the result is unknown, otherwise
	 * it is no.
	 */
	private static MetaDataInfo handleNotFound(SetRelation columnsRelation) {
		if (columnsRelation == SetRelation.SUPERSET) {
			//there can be more
			return MetaDataInfo.UNKNOWN;
		} else {
			return MetaDataInfo.NO;
		}
	}

	/**
	 * Extracts a {@link ColumnInfo.MatchingTypeRange} from the resultMap.
	 */
	private static <T> ColumnInfo.MatchingTypeRange<T> extractRange(ColumnType<T> type, Map<Statistics.Statistic,
			Statistics.Result> resultMap) {
		final Statistics.Result minResult = resultMap.get(Statistics.Statistic.MIN);
		final Statistics.Result maxResult = resultMap.get(Statistics.Statistic.MAX);
		final T min = minResult.getObject(type.elementType());
		final T max = maxResult.getObject(type.elementType());
		if (min == null || max == null) {
			return new ColumnInfo.MatchingTypeRange<>(type, null);
		}
		return new ColumnInfo.MatchingTypeRange<>(type, new ObjectRange<>(min, max, type.comparator()));
	}

	/**
	 * Returns the maximum number of column info in a {@link TableMetaData} specified by the setting {@link
	 * #SETTING_METADATA_COLUMN_LIMIT}.
	 */
	private static int getMaximumNumberOfColumns() {
		int maxSize = 10_000;
		String maxSizeString = Settings.getSetting(SETTING_METADATA_COLUMN_LIMIT);
		if (maxSizeString != null) {
			try {
				maxSize = Integer.parseInt(maxSizeString);
			} catch (NumberFormatException e) {
				//ignore setting
			}
			if (maxSize <= 0) {
				maxSize = Integer.MAX_VALUE;
			}
		}
		return maxSize;
	}

	/**
	 * Checks if the list contains metadata of the given type.
	 */
	static <T extends ColumnMetaData> boolean withMetaData(List<ColumnMetaData> list, Class<T> type) {
		if (list != null) {
			for (ColumnMetaData data : list) {
				if (type.isInstance(data)) {
					return true;
				}
			}
		}
		return false;
	}

}
