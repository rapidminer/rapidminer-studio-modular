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
package com.rapidminer.storage.hdf5;

import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.ATTRIBUTE_LEGACY_ROLE;
import static com.rapidminer.storage.hdf5.ExampleSetHdf5Writer.ATTRIBUTE_LEGACY_TYPE;
import static com.rapidminer.storage.hdf5.IOTableHdf5Writer.ATTRIBUTE_COLUMN_REFERENCE_COLUMN;
import static com.rapidminer.storage.hdf5.IOTableHdf5Writer.ATTRIBUTE_COLUMN_REFERENCE_VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.DateTimeColumn;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.hdf5.file.ColumnDescriptor;
import com.rapidminer.hdf5.file.ColumnDescriptor.Hdf5ColumnRole;
import com.rapidminer.hdf5.file.NumericColumnDescriptor;
import com.rapidminer.hdf5.file.StringColumnDescriptor;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.storage.hdf5.ColumnMetaDataStorageRegistry.Hdf5Serializer;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Triple;


/**
 * Utility class for creating {@link ColumnDescriptor} from an {@link AttributeRole}, {@link AttributeMetaData}, {@link
 * Column} or {@link ColumnInfo}.
 *
 * Stores the name, type, role and the {@link ColumnMetaData}. In case of writing metadata, also the set relation is
 * stored and whether nominal data was shrunk. If requested, statistics is stored additionally, see {@link
 * TableStatisticsHandler} and {@link ExampleSetStatisticsHandler}.
 *
 * @author Jan Czogalla, Gisa Meier
 * @since 9.7.0
 */
enum ColumnDescriptionCreator {

	;//No-instance enum, only static methods

	/**
	 * Set of ontologies that have an associated belt type
	 */
	private static final Set<Integer> UNNECESSARY_ONTOLOGIES = LegacyType.DIRECTLY_MAPPED_ONTOLOGIES;

	private static final Map<Integer, ColumnDescriptor.Hdf5ColumnType> RM_TO_COLUMN_TYPE;

	static {
		Map<Integer, ColumnDescriptor.Hdf5ColumnType> map = new HashMap<>();
		map.put(Ontology.NUMERICAL, ColumnDescriptor.Hdf5ColumnType.REAL);
		map.put(Ontology.REAL, ColumnDescriptor.Hdf5ColumnType.REAL);
		map.put(Ontology.INTEGER, ColumnDescriptor.Hdf5ColumnType.INTEGER);
		map.put(Ontology.NOMINAL, ColumnDescriptor.Hdf5ColumnType.NOMINAL);
		map.put(Ontology.BINOMINAL, ColumnDescriptor.Hdf5ColumnType.NOMINAL);
		map.put(Ontology.POLYNOMINAL, ColumnDescriptor.Hdf5ColumnType.NOMINAL);
		map.put(Ontology.STRING, ColumnDescriptor.Hdf5ColumnType.NOMINAL);
		map.put(Ontology.FILE_PATH, ColumnDescriptor.Hdf5ColumnType.NOMINAL);
		map.put(Ontology.TIME, ColumnDescriptor.Hdf5ColumnType.TIME);
		RM_TO_COLUMN_TYPE = Collections.unmodifiableMap(map);
	}

	/**
	 * Creates a column descriptor for an attribute, either a {@link StringColumnDescriptor} or a {@link
	 * NumericColumnDescriptor} depending on the value type. If the value type or the role cannot be stored as one of
	 * the fixed allowed value for {@link TableWriter#ATTRIBUTE_TYPE} or {@link TableWriter#ATTRIBUTE_ROLE} they are
	 * stored using {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_TYPE} or
	 * {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_ROLE}
	 * respectively. For binominal attributes, the positive index {@code 2} is stored in the {@link
	 * TableWriter#ATTRIBUTE_POSITVE_INDEX} if a positive value exists, {@code -1} otherwise.
	 *
	 * @param attRole
	 * 		the attribute role containing the attribute
	 * @param statisticsProvider
	 * 		the source for statistics, can be {@code null} if no statistics should be written
	 * @param shortenMD
	 * 		whether or not to shorten the metadata (i.e. store not all attributes and/or shorten the list of nominal
	 * 		values)
	 * @return the associated column descriptor
	 */
	static ColumnDescriptor create(AttributeRole attRole, ExampleSet statisticsProvider, boolean shortenMD) {
		Attribute attribute = attRole.getAttribute();
		boolean isNom = attribute.isNominal();
		NominalMapping mapping = isNom ? attribute.getMapping() : null;
		List<String> values = isNom ? mapping.getValues() : null;
		boolean wasShortened = false;
		double mode = -1;
		if (shortenMD && isNom) {
			int maxNomValues = AttributeMetaData.getMaximumNumberOfNominalValues();
			if (maxNomValues < values.size()) {
				values = values.subList(0, maxNomValues);
				wasShortened = true;
				if (statisticsProvider != null) {
					mode = statisticsProvider.getStatistics(attribute, Statistics.MODE);
					if (mode >= values.size()) {
						String modeValue = mapping.mapIndex((int) mode);
						values = new ArrayList<>(values);
						values.add(modeValue);
					}
				}
			}
		}
		ColumnDescriptor descriptor = create(attribute.getValueType(), attRole.getSpecialName(), attribute.getName(),
				attribute.isNumerical(), isNom, attribute.isDateTime(),
				values, isNom ? v -> mapping.getIndex(v) >= 0 : null);
		if (statisticsProvider != null) {
			ExampleSetStatisticsHandler.addStatistics(descriptor, attribute, statisticsProvider);
		}
		SetRelation relation = isNom && shortenMD && wasShortened ? SetRelation.SUPERSET : SetRelation.EQUAL;
		descriptor.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_SET_RELATION, String.class, relation.toString());
		if (wasShortened) {
			descriptor.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_NOMINAL_SHRUNK, byte.class, (byte) 1);
			if (mode >= values.size()) {
				int mdMode = values.size();
				descriptor.getAdditionalAttributes().computeIfPresent(ExampleSetStatisticsHandler.STATISTICS_MODE,
						(k, v) -> new ImmutablePair<>(int.class, mdMode));
			}
		}
		return descriptor;
	}

	/**
	 * Creates a column descriptor for the column with label in table, either a {@link StringColumnDescriptor} or a {@link
	 * NumericColumnDescriptor} depending on the column type. Adds the {@link ColumnMetaData}. For nominal columns with
	 * boolean dictionaries, the positive index is stored in the {@link TableWriter#ATTRIBUTE_POSITVE_INDEX}.
	 *
	 * @param label
	 * 		the label of the column for which to create the descriptor
	 * @param table
	 * 		the table containing a column with the label
	 * @return the associated column descriptor
	 */
	static ColumnDescriptor create(String label, Table table) {
		Column column = table.column(label);
		boolean isNom = column.type().id() == Column.TypeId.NOMINAL;
		boolean hasNanos = false;
		if (column instanceof DateTimeColumn) {
			hasNanos = ((DateTimeColumn) column).hasSubSecondPrecision();
		}
		ColumnDescriptor descriptor =
				create(column.type(), label, table.getMetaData(label), hasNanos,
						isNom ? new FakeDictionaryCollection(column.getDictionary()) : null);
		if (isNom && column.getDictionary().isBoolean()) {
			descriptor.addAdditionalAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX, byte.class,
					(byte) column.getDictionary().getPositiveIndex());
		}
		TableStatisticsHandler.addStatistics(descriptor, label, table);
		return descriptor;
	}

	/**
	 * Creates a column descriptor for an {@link AttributeMetaData}, either a {@link StringColumnDescriptor} or a {@link
	 * NumericColumnDescriptor} depending on the value type. If the value type or the role cannot be stored as one of
	 * the fixed allowed value for {@link TableWriter#ATTRIBUTE_TYPE} or {@link TableWriter#ATTRIBUTE_ROLE} they are
	 * stored using {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_TYPE} or
	 * {@link ExampleSetHdf5Writer#ATTRIBUTE_LEGACY_ROLE}
	 * respectively. For binominal attributes, the positive index {@code 2} is stored in the {@link
	 * TableWriter#ATTRIBUTE_POSITVE_INDEX} if a positive value exists, {@code -1} otherwise.
	 *
	 * @param amd
	 * 		the attribute meta data
	 * @param writeStatistics
	 * 		whether statistics should be written
	 * @param shortenMD
	 * 		whether or not to shorten the metadata (i.e. store not all attributes and/or shorten the list of nominal
	 * 		values)
	 * @return the associated column descriptor
	 */
	static ColumnDescriptor create(AttributeMetaData amd, boolean writeStatistics, boolean shortenMD) {
		boolean isNom = amd.isNominal();
		List<String> values = null;
		String mode = amd.getMode();
		SetRelation relation = amd.getValueSetRelation();
		if (isNom) {
			if (relation == SetRelation.UNKNOWN) {
				values = new ArrayList<>();
			} else {
				Stream<String> valueStream = amd.getValueSet().stream();
				if (shortenMD) {
					valueStream = valueStream.limit(AttributeMetaData.getMaximumNumberOfNominalValues());
				}
				values = valueStream.collect(Collectors.toList());
				if (values.size() < amd.getValueSet().size()) {
					relation = relation.merge(SetRelation.SUPERSET);
				}
			}
		}
		if (isNom && mode != null && !values.contains(mode)) {
			values.add(mode);
		}
		ColumnDescriptor descriptor = create(amd.getValueType(), amd.getRole(), amd.getName(),
				amd.isNumerical(), isNom, amd.isDateTime(), values, isNom ? values::contains : null);
		if (writeStatistics) {
			ExampleSetStatisticsHandler.addStatistics(descriptor, amd);
		}
		if (relation != SetRelation.UNKNOWN) {
			descriptor.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_SET_RELATION, String.class,
					relation.toString());
		}
		if (isNom && amd.valueSetWasShrunk()) {
			descriptor.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_NOMINAL_SHRUNK, byte.class, (byte) 1);
		}
		return descriptor;
	}

	/**
	 * Creates a column descriptor for the column with label in table, either a {@link StringColumnDescriptor} or a {@link
	 * NumericColumnDescriptor} depending on the column type. Adds the {@link ColumnMetaData}. For nominal columns with
	 * boolean dictionaries, the positive index is stored in the {@link TableWriter#ATTRIBUTE_POSITVE_INDEX}.
	 *
	 * @param label
	 * 		the label of the column for which to create the descriptor
	 * @param tmd
	 * 		the table meta data containing a column with the label
	 * @return the associated column descriptor
	 */
	static ColumnDescriptor create(String label, TableMetaData tmd) {
		ColumnInfo column = tmd.column(label);
		boolean isNom = column.isNominal() == MetaDataInfo.YES;
		List<String> values = null;
		SetRelation relation = column.getValueSetRelation();
		if (isNom) {
			if (relation == SetRelation.UNKNOWN) {
				values = new ArrayList<>();
			} else {
				values = new ArrayList<>(column.getDictionary().getValueSet());
			}
		}
		final ColumnType<?> columnType = column.getType().orElse(ColumnType.NOMINAL); //unknown does not happen
		ColumnDescriptor descriptor = create(columnType, label, tmd.getColumnMetaData(label), false, values);
		TableStatisticsHandler.addStatistics(descriptor, label, tmd);
		if (relation != SetRelation.UNKNOWN) {
			descriptor.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_SET_RELATION, String.class,
					relation.toString());
		}
		if (isNom && column.getDictionary().valueSetWasShrunk()) {
			descriptor.addAdditionalAttribute(ExampleSetHdf5Writer.ATTRIBUTE_NOMINAL_SHRUNK, byte.class, (byte) 1);
		}
		if (isNom && column.getDictionary().isBoolean()) {
			int positiveIndex = -1;
			final String positiveValue = column.getDictionary().getPositiveValue().orElse(null);
			if (!values.isEmpty() && values.get(0).equals(positiveValue)) {
				positiveIndex = 1;
			} else if (values.size() > 1 && values.get(1).equals(positiveValue)) {
				positiveIndex = 2;
			}
			descriptor.addAdditionalAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX, byte.class,
					(byte) positiveIndex);
		}
		return descriptor;
	}

	/**
	 * Creates the actual {@link ColumnDescriptor} from the extracted information from either {@link AttributeRole} or
	 * {@link AttributeMetaData}.
	 *
	 * @param valueType
	 * 		the attributes value type
	 * @param specialName
	 * 		the special role; might be {@code null}
	 * @param attName
	 * 		the attribute name
	 * @param isNum
	 * 		if the attribute is numerical
	 * @param isNom
	 * 		if the attribute is nominal
	 * @param isDateTime
	 * 		if the attribute is a date/time
	 * @param dictionary
	 * 		the nominal values of the attribute if it is nominal; {@code null} otherwise
	 * @param containedInDictionary
	 * 		predicate to decide if a given nominal value is actually in the dictionary; {@code null},
	 * 		if the attribute is not nominal
	 * @return the column descriptor
	 */
	private static ColumnDescriptor create(int valueType, String specialName, String attName,
										   boolean isNum, boolean isNom, boolean isDateTime,
										   List<String> dictionary, Predicate<String> containedInDictionary) {
		ColumnDescriptor descriptor;
		ColumnDescriptor.Hdf5ColumnType columnType = fromOntology(valueType);
		Pair<Hdf5ColumnRole, String> columnRole = toColumnRole(specialName);
		if (isNum) {
			descriptor = new NumericColumnDescriptor(attName, columnType, columnRole.getLeft());
		} else {
			if (isNom) {
				// use -1 as number of rows to prevent nominal columns being stored without category indices
				descriptor = new StringColumnDescriptor(attName, columnType, columnRole.getLeft(),
						dictionary, containedInDictionary, -1);
			} else {
				if (isDateTime) {
					if (valueType == Ontology.TIME) {
						descriptor = new NumericColumnDescriptor(attName, columnType, columnRole.getLeft());
					} else {
						descriptor = NumericColumnDescriptor.createDateTime(attName, columnRole.getLeft(),
								valueType != Ontology.DATE);
					}
				} else {
					throw new AssertionError();
				}
			}
		}

		if (!UNNECESSARY_ONTOLOGIES.contains(valueType)) {
			descriptor.addAdditionalAttribute(ATTRIBUTE_LEGACY_TYPE, byte.class, (byte) valueType);
		}

		String legacyRole = columnRole.getRight();
		if (legacyRole != null) {
			descriptor.addAdditionalAttribute(ATTRIBUTE_LEGACY_ROLE, String.class, legacyRole);
		}

		if (valueType == Ontology.BINOMINAL) {
			int size = dictionary.size();
			if (size == 2) {
				//for binominal, the first value is negative and the second positive, plus shift by 1
				descriptor.addAdditionalAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX, byte.class, (byte) 2);
			} else if (size < 2) {
				//if no second value exists, there is no positive index
				descriptor.addAdditionalAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX, byte.class, (byte) -1);
			}
		}
		return descriptor;
	}

	/**
	 * Creates the column descriptor for the given parameters.
	 */
	private static ColumnDescriptor create(ColumnType<?> type, String attName, List<ColumnMetaData> metaData,
										   boolean hasNanos, Collection<String> dictionaryWithoutNull) {
		if (metaData != null && !metaData.isEmpty()) {
			metaData = new ArrayList<>(metaData);
		}
		ColumnDescriptor.Hdf5ColumnType columnType = fromType(type);
		Hdf5ColumnRole columnRole = removeColumnRole(metaData);
		ColumnDescriptor descriptor =
				createColumnInfo(type, attName, hasNanos, dictionaryWithoutNull, columnType, columnRole);
		//write column reference as two separate values to easily handle null values
		ColumnReference reference = removeColumnMetaData(metaData, ColumnReference.class);
		if (reference != null) {
			String referenceColumn = reference.getColumn();
			if (referenceColumn != null) {
				descriptor.addAdditionalAttribute(ATTRIBUTE_COLUMN_REFERENCE_COLUMN, String.class, referenceColumn);
			}
			String referenceValue = reference.getValue();
			if (referenceValue != null) {
				descriptor.addAdditionalAttribute(ATTRIBUTE_COLUMN_REFERENCE_VALUE, String.class, referenceValue);
			}
		}
		if (metaData != null) {
			addColumnMetaData(metaData, descriptor);
		}
		return descriptor;
	}

	/**
	 * Adds the other column meta data from the list to the descriptor.
	 */
	private static void addColumnMetaData(List<ColumnMetaData> metaData, ColumnDescriptor descriptor) {
		for (ColumnMetaData metaDatum : metaData) {
			final Triple<String, Class<?>, Hdf5Serializer> serializer =
					ColumnMetaDataStorageRegistry.getSerializer(metaDatum.getClass());
			if (serializer != null) {
				try {
					final Object toStore = serializer.getThird().apply(metaDatum);
					final Class<?> storingClass = serializer.getSecond();
					if (storingClass.isInstance(toStore) || (storingClass.isPrimitive() &&
							ClassUtils.primitiveToWrapper(storingClass).isInstance(toStore))) {
						descriptor.addAdditionalAttribute(serializer.getFirst(), storingClass, toStore);
					} else {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.storage.hdf5.ColumnDescriptorCreator" +
								".serializer_class", metaDatum.getClass().getSimpleName());
					}
				} catch (RuntimeException e) {
					//prevent faulty custom ColumnMetaData serialization to prevent storage
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.storage.hdf5.ColumnDescriptorCreator.error_serializer",
							metaDatum.getClass().getSimpleName()), e);
				}
			} else {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.storage.hdf5.ColumnDescriptorCreator.no_serializer",
						metaDatum.getClass().getSimpleName());
			}
		}
	}

	/**
	 * Creates the descriptor from the parameters.
	 */
	private static ColumnDescriptor createColumnInfo(ColumnType<?> type, String attName, boolean hasNanos,
													 Collection<String> dictionaryWithoutNull,
													 ColumnDescriptor.Hdf5ColumnType columnType,
													 Hdf5ColumnRole columnRole) {
		ColumnDescriptor descriptor;
		if (type.category() == Column.Category.NUMERIC || type.id() == Column.TypeId.TIME) {
			descriptor = new NumericColumnDescriptor(attName, columnType, columnRole);
		} else {
			if (type.category() == Column.Category.CATEGORICAL) {
				// use -1 as number of rows to prevent nominal columns being stored without category indices
				descriptor = new StringColumnDescriptor(attName, columnType, columnRole, dictionaryWithoutNull,
						dictionaryWithoutNull::contains, -1);
			} else {
				if (type.id() == Column.TypeId.DATE_TIME) {
					descriptor = NumericColumnDescriptor.createDateTime(attName, columnRole, hasNanos);
				} else {
					throw new AssertionError();
				}
			}
		}
		return descriptor;
	}

	/**
	 * Removes a column reference from the metaData list if it exists.
	 */
	private static <T extends ColumnMetaData> T removeColumnMetaData(List<ColumnMetaData> metaData, Class<T> type) {
		if (metaData != null) {
			for (Iterator<ColumnMetaData> iterator = metaData.iterator(); iterator.hasNext(); ) {
				final ColumnMetaData next = iterator.next();
				if (type.isInstance(next)) {
					iterator.remove();
					return type.cast(next);
				}
			}
		}
		return null;
	}

	/**
	 * Converts on column type to the other.
	 */
	private static ColumnDescriptor.Hdf5ColumnType fromType(ColumnType<?> type) {
		switch (type.id()) {
			case REAL:
				return ColumnDescriptor.Hdf5ColumnType.REAL;
			case INTEGER_53_BIT:
				return ColumnDescriptor.Hdf5ColumnType.INTEGER;
			case NOMINAL:
				return ColumnDescriptor.Hdf5ColumnType.NOMINAL;
			case DATE_TIME:
				return ColumnDescriptor.Hdf5ColumnType.DATE_TIME;
			case TIME:
				return ColumnDescriptor.Hdf5ColumnType.TIME;
			default:
				throw new AssertionError();
		}
	}

	/**
	 * Removes a column role from the metaData list if it exists.
	 */
	private static Hdf5ColumnRole removeColumnRole(List<ColumnMetaData> metaData) {
		ColumnRole role = removeColumnMetaData(metaData, ColumnRole.class);
		return role == null ? null : Hdf5ColumnRole.valueOf(role.name());
	}

	/**
	 * Converts ontology to column type using the {@link #RM_TO_COLUMN_TYPE} map.
	 */
	private static ColumnDescriptor.Hdf5ColumnType fromOntology(int ontology) {
		return RM_TO_COLUMN_TYPE.getOrDefault(ontology, ColumnDescriptor.Hdf5ColumnType.REAL);
	}

	/**
	 * Converts the role to a pair that contains the column role and, if that is not specific enough, the role itself
	 * again.
	 *
	 * @param role
	 * 		the role to convert
	 * @return a pair of a column role and optional the input role again
	 */
	private static Pair<ColumnDescriptor.Hdf5ColumnRole, String> toColumnRole(String role) {
		if (role == null) {
			return new ImmutablePair<>(null, null);
		}
		switch (role) {
			case Attributes.LABEL_NAME:
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.LABEL, null);
			case Attributes.ID_NAME:
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.ID, null);
			case Attributes.PREDICTION_NAME:
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.PREDICTION, null);
			case Attributes.CONFIDENCE_NAME:
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.SCORE, null);
			case Attributes.CLUSTER_NAME:
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.CLUSTER, null);
			case Attributes.OUTLIER_NAME:
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.OUTLIER, null);
			case Attributes.WEIGHT_NAME:
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.WEIGHT, null);
			case Attributes.BATCH_NAME:
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.BATCH, null);
			case "interpretation":
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.INTERPRETATION, null);
			case "source":
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.SOURCE, null);
			case "encoding":
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.ENCODING, null);
			default:
				if (role.startsWith(Attributes.CONFIDENCE_NAME)) {
					return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.SCORE, role);
				}
				return new ImmutablePair<>(ColumnDescriptor.Hdf5ColumnRole.METADATA, role);
		}

	}

	/**
	 * Wraps a {@link Dictionary} into a {@link Collection} that only supports some of the methods. Package private for tests.
	 */
	static class FakeDictionaryCollection implements Collection<String>{

		private final Dictionary dictionary;

		FakeDictionaryCollection(Dictionary dictionary) {
			this.dictionary = dictionary;
		}

		@Override
		public int size() {
			return dictionary.size();
		}

		@Override
		public boolean isEmpty() {
			return dictionary.size() == 0;
		}

		@Override
		public boolean contains(Object o) {
			for (int i = 1; i <= dictionary.maximalIndex(); i++) {
				if (dictionary.get(i).equals(o)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Iterator<String> iterator() {
			return new Iterator<String>() {
				private int index = 0;

				@Override
				public boolean hasNext() {
					return index < dictionary.maximalIndex();
				}

				@Override
				public String next() {
					return dictionary.get(++index);
				}
			};
		}


		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean add(String s) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addAll(Collection<? extends String> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

}
