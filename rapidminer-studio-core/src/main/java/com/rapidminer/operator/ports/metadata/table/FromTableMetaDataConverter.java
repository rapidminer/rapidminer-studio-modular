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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.LegacyRole;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.ToTableMetaDataConverter;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.belt.BeltTools;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Converts {@link TableMetaData} to {@link ExampleSetMetaData}. Is used to convert the metadata at the operator ports.
 *
 * @author Kevin Majchrzak
 * @see ToTableMetaDataConverter
 * @since 9.9.0
 */
public enum FromTableMetaDataConverter {
	;//no instance enum

	/**
	 * Key for storing column meta data in additional data of ExampleSetMetaData.
	 */
	static final String ADDITIONAL_DATA_COLUMN_META_DATA_KEY = FromTableMetaDataConverter.class.getName()
			+ ".column_meta_data";

	/**
	 * Key for storing boolean dictionaries (positive / negative values of boolean columns) in additional data of
	 * ExampleSetMetaData.
	 */
	private static final String ADDITIONAL_DATA_POS_NEG_VALUE_KEY = FromTableMetaDataConverter.class.getName()
			+ ".pos_neg_value";

	/**
	 * String into which {@link ColumnRole#METADATA} is converted.
	 */
	private static final String META_DATA_NAME = "metadata";

	/**
	 * String into which {@link ColumnRole#INTERPRETATION} is converted.
	 */
	private static final String INTERPRETATION_NAME = "interpretation";

	/**
	 * String into which {@link ColumnRole#ENCODING} is converted.
	 */
	private static final String ENCODING_NAME = "encoding";

	/**
	 * String into which {@link ColumnRole#SOURCE} is converted.
	 */
	private static final String SOURCE_NAME = "source";

	/**
	 * Prefix of the role names of confidence attributes.
	 */
	private static final String CONFIDENCE_PREFIX = Attributes.CONFIDENCE_NAME + "_";

	/**
	 * Converts the given {@link TableMetaData} to {@link ExampleSetMetaData}.
	 *
	 * @param tmd
	 * 		the table metadata to convert.
	 * @return the resulting {@link ExampleSetMetaData}
	 */
	public static ExampleSetMetaData convert(TableMetaData tmd) {

		ValidationUtilV2.requireNonNull(tmd, "TableMetaData");

		// this set is used to identify duplicate roles
		Set<String> existingRoles = new HashSet<>();
		// this map is used to generate indices for duplicate roles
		Map<String, Integer> indexMap = new HashMap<>();

		// used to collect the boolean dictionary infos and to store
		// them later via #storePosNegValue
		Map<String, DictionaryInfo> posNegValues = new HashMap<>();

		// convert column infos to attribute metadata
		ExampleSetMetaData emd = new ExampleSetMetaData();
		for (String label : tmd.labels()) {
			if (!isAdvanced(label, tmd)) {
				emd.addAttribute(convertColumn(label, tmd, existingRoles, indexMap, posNegValues));
			}
		}

		// copy other metadata
		emd.setNumberOfExamples(tmd.height());
		emd.mergeSetRelation(tmd.getColumnSetRelation());

		// copy additional data
		for (Map.Entry<String, Object> data : tmd.getKeyValueMap().entrySet()) {
			emd.addAdditionalData(data.getKey(), data.getValue());
		}

		// preserve belt-only column metadata
		storeColumnMetaData(tmd, emd);

		// preserve pos / neg values
		storePosNegValue(posNegValues, emd);

		// copy table annotations
		emd.setAnnotations(new Annotations(tmd.getAnnotations()));

		// copy generation history
		List<OutputPort> generationHistory = tmd.getGenerationHistory();
		ListIterator<OutputPort> it = generationHistory.listIterator(generationHistory.size());
		while (it.hasPrevious()) {
			emd.addToHistory(it.previous());
		}

		return emd;
	}

	/**
	 * Returns {@code true} if the column with the given label is an advanced column (see {@link
	 * BeltTools#isAdvanced(ColumnType)}).
	 *
	 * @param label
	 * 		the label of the column to check
	 * @param metaData
	 * 		the metadata holding the column metadata
	 * @return {@code true} iff it is an advanced column
	 */
	private static boolean isAdvanced(String label, TableMetaData metaData) {
		Optional<ColumnType<?>> type = metaData.column(label).getType();
		return type.isPresent() && BeltTools.isAdvanced(type.get());
	}

	/**
	 * Converts the {@link ColumnInfo} of the column with the given label to {@link AttributeMetaData}.
	 *
	 * @param label
	 * 		the label of the column
	 * @param metaData
	 * 		the table metadata holding the column info
	 * @param existingRoles
	 * 		used to track the already used roles (roles need to be unique for example sets)
	 * @param indexMap
	 * 		used to generate indices for duplicate roles
	 * @param posNegValues
	 * 		used to store the pos / neg values via the corresponding boolean dictionary infos
	 * @return the generated attribute metadata
	 */
	private static AttributeMetaData convertColumn(String label, TableMetaData metaData, Set<String> existingRoles,
												   Map<String, Integer> indexMap,
												   Map<String, DictionaryInfo> posNegValues) {
		ColumnInfo columnInfo = metaData.column(label);

		int derivedOntology = convertToOntology(columnInfo);
		LegacyType legacyType = metaData.getFirstColumnMetaData(label, LegacyType.class);
		if (legacyType != null) {
			int legacyOntology = legacyType.ontology();
			if (useLegacyOntology(legacyOntology, derivedOntology, columnInfo)) {
				derivedOntology = legacyOntology;
			}
		}

		String role = convertRole(metaData, label, existingRoles, indexMap);
		AttributeMetaData amd = new AttributeMetaData(label, derivedOntology, role);

		if (columnInfo.getType().isPresent()) {
			ColumnType<?> beltType = columnInfo.getType().get();
			if (beltType.id() == Column.TypeId.NOMINAL) {
				setValueSet(label, posNegValues, columnInfo, amd);
			} else if (beltType.category() == Column.Category.NUMERIC) {
				setNumericRange(columnInfo, amd);
			} else if (beltType.id() == Column.TypeId.TIME) {
				setTimeRange(columnInfo, amd);
			} else if (beltType.id() == Column.TypeId.DATE_TIME) {
				setDateTimeRange(columnInfo, amd);
			}
		}

		amd.setNumberOfMissingValues(columnInfo.getMissingValues());

		ColumnAnnotation annotation = metaData.getFirstColumnMetaData(label, ColumnAnnotation.class);
		if (annotation != null) {
			amd.getAnnotations().put(Annotations.KEY_COMMENT, annotation.annotation());
		}

		return amd;
	}

	/**
	 * Sets the {@link AttributeMetaData}'s value range based on the given {@link ColumnInfo} of a numeric column.
	 *
	 * @param columnInfo
	 * 		the column info holding the range information.
	 * @param amd
	 * 		the range will be stored here
	 */
	private static void setNumericRange(ColumnInfo columnInfo, AttributeMetaData amd) {
		if (columnInfo.getNumericRange().isPresent()) {
			amd.setValueRange(new Range(columnInfo.getNumericRange().get()), columnInfo.getValueSetRelation());
		}
	}

	/**
	 * Sets the {@link AttributeMetaData}'s value range based on the given {@link ColumnInfo} of a date-time column.
	 *
	 * @param columnInfo
	 * 		the column info holding the range information.
	 * @param amd
	 * 		the range will be stored here
	 */
	private static void setDateTimeRange(ColumnInfo columnInfo, AttributeMetaData amd) {
		Optional<ObjectRange<Instant>> time = columnInfo.getObjectRange(Instant.class);
		time.ifPresent(range -> amd.setValueRange(new Range(BeltConverter.toEpochMilli(range.getLower()),
				BeltConverter.toEpochMilli(range.getUpper())), columnInfo.getValueSetRelation()));
	}

	/**
	 * Sets the {@link AttributeMetaData}'s value range based on the given {@link ColumnInfo} of a time column.
	 *
	 * @param columnInfo
	 * 		the column info holding the range information.
	 * @param amd
	 * 		the range will be stored here
	 */
	private static void setTimeRange(ColumnInfo columnInfo, AttributeMetaData amd) {
		Optional<ObjectRange<LocalTime>> timeRange = columnInfo.getObjectRange(LocalTime.class);
		timeRange.ifPresent(range -> amd.setValueRange(new Range(BeltConverter.nanoOfDayToLegacyTime(
				range.getLower().toNanoOfDay()), BeltConverter.nanoOfDayToLegacyTime(range.getUpper().toNanoOfDay())),
				columnInfo.getValueSetRelation()));
	}

	/**
	 * Sets the {@link AttributeMetaData}'s value set based on the given {@link DictionaryInfo} of a nominal column.
	 *
	 * @param columnInfo
	 * 		the column info holding the dictionary information.
	 * @param amd
	 * 		the value set will be stored here
	 */
	private static void setValueSet(String label, Map<String, DictionaryInfo> posNegValues, ColumnInfo columnInfo, AttributeMetaData amd) {
		DictionaryInfo dictionary = columnInfo.getDictionary();
		if (dictionary != null) {
			amd.setValueSet(dictionary.getAsCopyOnWrite(), columnInfo.getValueSetRelation());
			amd.valueSetIsShrunk(dictionary.valueSetWasShrunk());
			if (dictionary.isBoolean()) {
				posNegValues.put(label, dictionary);
			}
		}
	}

	/**
	 * Converts the {@link ColumnRole} of the column with the given label to the appropriate studio role string.
	 *
	 * @param metaData
	 * 		the table metadata holding the column role
	 * @param label
	 * 		the column label
	 * @param existingRoles
	 * 		the roles that have already been used - needed to check for duplicate roles
	 * @param indexMap
	 * 		map used to generate indices for duplicate roles
	 * @return the studio role or {@code null} if the column does not have a role
	 */
	private static String convertRole(TableMetaData metaData, String label, Set<String> existingRoles,
									  Map<String, Integer> indexMap) {
		String studioRole = convertRole(metaData, label);
		if (studioRole != null) {
			// add an index if necessary
			String studioRoleWithIndex = studioRole;
			while (existingRoles.contains(studioRoleWithIndex)) {
				int index = indexMap.getOrDefault(studioRole, 2);
				studioRoleWithIndex = studioRole + "_" + index;
				indexMap.put(studioRole, index + 1);
			}
			existingRoles.add(studioRoleWithIndex);
			return studioRoleWithIndex;
		}
		return null;
	}

	/**
	 * Converts the {@link ColumnRole} of the column with the given label to the appropriate studio role string. Also
	 * considers the legacy roles. Analog to {@link com.rapidminer.belt.table.BeltConverter#convertRole(Table,
	 * String)}.
	 *
	 * @param metadata
	 * 		the metadata to consider
	 * @param label
	 * 		the name of the column
	 * @return the legacy role name
	 */
	public static String convertRole(TableMetaData metadata, String label) {
		ColumnRole role = metadata.getFirstColumnMetaData(label, ColumnRole.class);
		if (role == null) {
			// Nothing to convert, abort...
			return null;
		}
		String convertedRole;
		switch (role) {
			case LABEL:
				convertedRole = Attributes.LABEL_NAME;
				break;
			case ID:
				convertedRole = Attributes.ID_NAME;
				break;
			case PREDICTION:
				convertedRole = Attributes.PREDICTION_NAME;
				break;
			case CLUSTER:
				convertedRole = Attributes.CLUSTER_NAME;
				break;
			case OUTLIER:
				convertedRole = Attributes.OUTLIER_NAME;
				break;
			case WEIGHT:
				convertedRole = Attributes.WEIGHT_NAME;
				break;
			case BATCH:
				convertedRole = Attributes.BATCH_NAME;
				break;
			case SOURCE:
				convertedRole = SOURCE_NAME;
				break;
			case ENCODING:
				convertedRole = ENCODING_NAME;
				break;
			case INTERPRETATION:
				convertedRole = INTERPRETATION_NAME;
				break;
			default:
				convertedRole = null;
				break;
		}

		if (convertedRole == null) {
			// no definite match for role, take legacy role into account
			LegacyRole legacyRole = metadata.getFirstColumnMetaData(label, LegacyRole.class);
			if (legacyRole != null) {
				return legacyRole.role();
			} else if (role == ColumnRole.SCORE) {
				ColumnReference reference = metadata.getFirstColumnMetaData(label, ColumnReference.class);
				if (reference != null && reference.getValue() != null) {
					return CONFIDENCE_PREFIX + reference.getValue();
				} else {
					return Attributes.CONFIDENCE_NAME;
				}
			} else if (role == ColumnRole.METADATA) {
				return META_DATA_NAME;
			}
		}
		return convertedRole;
	}

	/**
	 * Converts the type of the given {@link ColumnInfo} to the appropriate {@link Ontology}.
	 *
	 * @param columnInfo
	 * 		the column metadata
	 * @return the appropriate ontology (integer representing the studio type)
	 */
	private static int convertToOntology(ColumnInfo columnInfo) {
		if (!columnInfo.getType().isPresent()) {
			return Ontology.ATTRIBUTE_VALUE;
		}
		ColumnType<?> type = columnInfo.getType().get();
		switch (type.id()) {
			case INTEGER_53_BIT:
				return Ontology.INTEGER;
			case REAL:
				return Ontology.REAL;
			case NOMINAL:
				DictionaryInfo dictionary = columnInfo.getDictionary();
				if (dictionary != null && dictionary.isBoolean()
						&& !(dictionary.hasPositive() == MetaDataInfo.YES
						&& dictionary.hasNegative() == MetaDataInfo.NO)) {
					return Ontology.BINOMINAL;
				}
				return Ontology.NOMINAL;
			case DATE_TIME:
				return Ontology.DATE_TIME;
			case TIME:
				return Ontology.TIME;
			default:
				// advanced column types
				return Ontology.ATTRIBUTE_VALUE;
		}
	}

	/**
	 * Checks if the legacy type should be used instead of the given type. Analog to {@link
	 * com.rapidminer.belt.table.BeltConverter#useLegacyOntology(int, int, Column)}.
	 */
	private static boolean useLegacyOntology(int legacyOntology, int derivedOntology, ColumnInfo columnInfo) {
		// we never want to fall back to the legacy ontology for these two
		if (derivedOntology == Ontology.INTEGER || derivedOntology == Ontology.BINOMINAL) {
			return false;
		}
		// legacy ontology is super type or the same
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(derivedOntology, legacyOntology)) {
			return true;
		}
		// if binominal is requested for a nominal derived type, check dictionary size and if only positive
		if (legacyOntology == Ontology.BINOMINAL && derivedOntology == Ontology.NOMINAL) {
			DictionaryInfo dictionary = columnInfo.getDictionary();
			return dictionary == null || dictionary.getValueSet().size() <= 2 &&
					//BinominalMapping can not have a positive value if it has no negative
					!(dictionary.isBoolean() && dictionary.hasPositive() == MetaDataInfo.YES
							&& dictionary.hasNegative() == MetaDataInfo.NO);
		}
		// derived ontology is a nominal subtype and legacy ontology, too
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(derivedOntology, Ontology.NOMINAL) && Ontology.ATTRIBUTE_VALUE_TYPE
				.isA(legacyOntology, Ontology.NOMINAL)) {
			return true;
		}
		// for legacy support we allow conversion from date-time to time
		if (legacyOntology == Ontology.TIME && derivedOntology == Ontology.DATE_TIME) {
			return true;
		}
		// date-time can be shown as date
		return legacyOntology == Ontology.DATE && derivedOntology == Ontology.DATE_TIME;
	}

	/**
	 * ColumnMetaData (except for roles) cannot be stored in {@link ExampleSetMetaData}. Therefore, we store the meta
	 * data in the ExampleSetMetaData's additional data.
	 *
	 * @param metaData
	 * 		the table metadata holding the column meta data that will be stored
	 * @param emd
	 * 		the meta data will be stored to this ExampleSetMetaData's additional data
	 */
	private static void storeColumnMetaData(TableMetaData metaData, ExampleSetMetaData emd) {
		emd.addAdditionalData(ADDITIONAL_DATA_COLUMN_META_DATA_KEY,
				Collections.unmodifiableMap(metaData.getMetaDataMap()));
	}

	/**
	 * Stores the given map from labels to boolean dictionary infos in the ExampleSetMetaData's additional data. This is
	 * necessary because ExampleSetMetaData stores no information on pos / neg values. So we can use this map to restore
	 * the information later.
	 *
	 * @param posNegValues
	 * 		the boolean dictionary infos holding the positive / negative value information
	 * @param emd
	 * 		the ExampleSetMetaData used to store the pos / neg values
	 */
	private static void storePosNegValue(Map<String, DictionaryInfo> posNegValues, ExampleSetMetaData emd) {
		emd.addAdditionalData(ADDITIONAL_DATA_POS_NEG_VALUE_KEY, Collections.unmodifiableMap(posNegValues));
	}

}
