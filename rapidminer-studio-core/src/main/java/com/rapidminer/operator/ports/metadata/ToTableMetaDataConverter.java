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
package com.rapidminer.operator.ports.metadata;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.LegacyRole;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.DictionaryInfo;
import com.rapidminer.operator.ports.metadata.table.FromTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Converts from {@link ExampleSetMetaData} to {@link TableMetaData}. Is used to convert the metadata at the operator
 * ports.
 *
 * @author Kevin Majchrzak
 * @see FromTableMetaDataConverter
 * @since 9.9.0
 */
public enum ToTableMetaDataConverter {
	;//no instance enum

	/**
	 * Key for storing boolean dictionaries (positive / negative values of boolean columns) in additional data of
	 * ExampleSetMetaData.
	 */
	static final String ADDITIONAL_DATA_POS_NEG_VALUE_KEY = FromTableMetaDataConverter.class.getName()
			+ ".pos_neg_value";

	/**
	 * Key for storing column meta data in additional data of ExampleSetMetaData.
	 */
	private static final String ADDITIONAL_DATA_COLUMN_META_DATA_KEY = FromTableMetaDataConverter.class.getName()
			+ ".column_meta_data";

	/**
	 * Key for storing the original Belt type of a column in additional data of ExampleSetMetaData.
	 */
	private static final String ADDITIONAL_DATA_TYPE_KEY = FromTableMetaDataConverter.class.getName()
			+ ".original_type";

	/**
	 * Prefix of the role names of confidence attributes.
	 */
	private static final String CONFIDENCE_PREFIX = Attributes.CONFIDENCE_NAME + "_";

	/**
	 * The length of the {@link #CONFIDENCE_PREFIX}.
	 */
	private static final int CONFIDENCE_PREFIX_LENGTH = CONFIDENCE_PREFIX.length();

	/**
	 * Converts the given {@link ExampleSetMetaData} to {@link TableMetaData}.
	 *
	 * @param emd
	 * 		the example set metadata
	 * @return the table metadata
	 */
	public static TableMetaData convert(ExampleSetMetaData emd) {

		ValidationUtilV2.requireNonNull(emd, "ExampleSetMetaData");

		MDInteger rows = emd.getNumberOfExamples();
		TableMetaDataBuilder builder = new TableMetaDataBuilder(rows != null ? rows : MDInteger.newUnknown());

		for (AttributeMetaData amd : emd.getAllAttributes()) {
			ColumnInfo info = convertAttribute(amd, emd);
			builder.add(amd.getName(), info);

			if (amd.isSpecial()) {
				calculateAndSetRole(amd, builder, emd);
			}

			setColumnAnnotation(amd, builder);
			setLegacyType(amd, builder);
		}

		builder.mergeColumnSetRelation(emd.getAttributeSetRelation());

		restoreColumnMetaData(emd, builder);

		TableMetaData tmd = builder.build();

		// copy generation history
		List<OutputPort> generationHistory = emd.getGenerationHistory();
		ListIterator<OutputPort> it = generationHistory.listIterator(generationHistory.size());
		while (it.hasPrevious()) {
			tmd.addToHistory(it.previous());
		}

		// copy annotations
		tmd.setAnnotations(new Annotations(emd.getAnnotations()));

		// copy additional data
		copyAdditionalData(emd, tmd);

		return tmd;
	}

	/**
	 * Stores a {@link LegacyType} for certain types so that these legacy types can be restored if we want to convert
	 * back later.
	 *
	 * @param amd
	 * 		the attribute meta data holding the type
	 * @param builder
	 * 		the table meta data builder
	 */
	private static void setLegacyType(AttributeMetaData amd, TableMetaDataBuilder builder) {
		String name = amd.getName();
		int originalOntology = amd.getValueType();
		if (!LegacyType.DIRECTLY_MAPPED_ONTOLOGIES.contains(originalOntology)
				&& originalOntology != Ontology.ATTRIBUTE_VALUE) {
			builder.addColumnMetaData(name, LegacyType.forOntology(originalOntology));
		}
	}

	/**
	 * Sets the AttributeMetaData's comment annotation as the TableMetaData's {@link ColumnAnnotation}.
	 *
	 * @param amd
	 * 		the attribute meta data potentially holding a comment
	 * @param builder
	 * 		the builder
	 */
	private static void setColumnAnnotation(AttributeMetaData amd, TableMetaDataBuilder builder) {
		String comment = amd.getAnnotations().get(Annotations.KEY_COMMENT);
		if (comment != null) {
			builder.addColumnMetaData(amd.getName(), new ColumnAnnotation(comment));
		}
	}

	/**
	 * Sets the associated belt role and, if not all the info can be captured by the belt role, stores the original role
	 * name.
	 */
	private static void calculateAndSetRole(AttributeMetaData amd, TableMetaDataBuilder builder, ExampleSetMetaData emd) {
		String label = amd.getName();
		String amdRole = amd.getRole();
		ColumnRole columnRole = BeltConverter.convertRole(amdRole);
		builder.addColumnMetaData(label, columnRole);

		if (columnRole == ColumnRole.METADATA) {
			builder.addColumnMetaData(label, new LegacyRole(amdRole));
		} else if (columnRole == ColumnRole.SCORE) {
			AttributeMetaData prediction = emd.getSpecial(Attributes.PREDICTION_NAME);
			String predictionName = prediction == null ? null : prediction.getName();
			if (amdRole.startsWith(CONFIDENCE_PREFIX)) {
				builder.addColumnMetaData(label, new ColumnReference(predictionName,
						amdRole.substring(CONFIDENCE_PREFIX_LENGTH)));
			} else {
				builder.addColumnMetaData(label, new ColumnReference(predictionName));
				if (!Attributes.CONFIDENCE_NAME.equals(amdRole)) {
					builder.addColumnMetaData(label, new LegacyRole(amdRole));
				}
			}
		}
	}

	/**
	 * Converts the given {@link AttributeMetaData} to {@link ColumnInfo}.
	 *
	 * @param amd
	 * 		the attribute metadata
	 * @param emd
	 * 		the example set meta data holding the attribute meta data
	 * @return the column info
	 */
	private static ColumnInfo convertAttribute(AttributeMetaData amd, ExampleSetMetaData emd) {
		Column.TypeId type = convertType(amd, emd);
		ColumnInfoBuilder infoBuilder = new ColumnInfoBuilder(type == null ? null : ColumnType.forId(type));

		MDInteger missings = amd.getNumberOfMissingValues();
		infoBuilder.setMissings(missings != null ? missings : MDInteger.newUnknown());

		if (type != null) {
			if (type == Column.TypeId.NOMINAL) {
				setDictionary(amd, emd, infoBuilder);
			} else if (type == Column.TypeId.REAL || type == Column.TypeId.INTEGER_53_BIT) {
				infoBuilder.setNumericRange(amd.getValueRange(), amd.getValueSetRelation());
			} else if (type == Column.TypeId.DATE_TIME) {
				setDateTimeRange(amd, infoBuilder);
			} else if (type == Column.TypeId.TIME) {
				setTimeRange(amd, infoBuilder);
			}
		}

		return infoBuilder.build();
	}

	/**
	 * Sets the object range of the given column info according to the given attribute meta data.
	 *
	 * @param amd
	 * 		the {@link AttributeMetaData}
	 * @param infoBuilder
	 * 		the {@link ColumnInfoBuilder}
	 */
	private static void setDateTimeRange(AttributeMetaData amd, ColumnInfoBuilder infoBuilder) {
		Range valueRange = amd.getValueRange();
		if (valueRange != null) {
			double lower = valueRange.getLower();
			double upper = valueRange.getUpper();
			// if both bounds are unknown, full object range is unknown
			if (!(Double.isNaN(lower) && Double.isNaN(upper))) {
				infoBuilder.setObjectRange(new ObjectRange<>(Double.isNaN(lower) ? Instant.MIN :
								Instant.ofEpochMilli((long) lower), Double.isNaN(upper) ? Instant.MAX :
								Instant.ofEpochMilli((long) upper), ColumnType.DATETIME.comparator()),
						amd.getValueSetRelation());
			}
		}
	}

	/**
	 * Sets the object range of the given column info according to the given attribute meta data.
	 *
	 * @param amd
	 * 		the {@link AttributeMetaData}
	 * @param infoBuilder
	 * 		the {@link ColumnInfoBuilder}
	 */
	private static void setTimeRange(AttributeMetaData amd, ColumnInfoBuilder infoBuilder) {
		Range valueRange = amd.getValueRange();
		if (valueRange != null) {
			double lower = valueRange.getLower();
			double upper = valueRange.getUpper();
			infoBuilder.setObjectRange(new ObjectRange<>(Double.isNaN(lower) ? LocalTime.MIN : toLocalTime(lower),
							Double.isNaN(upper) ? LocalTime.MAX : toLocalTime(upper), ColumnType.TIME.comparator()),
					amd.getValueSetRelation());
		}
	}

	/**
	 * Sets the dictionary of the given column info builder according to the given attribute meta data.
	 *
	 * @param amd
	 * 		the {@link AttributeMetaData}
	 * @param emd
	 *        {@link ExampleSetMetaData} potentially holding additional information on the pos / neg value for boolean
	 * 		columns.
	 * @param infoBuilder
	 * 		the {@link ColumnInfoBuilder}
	 */
	private static void setDictionary(AttributeMetaData amd, ExampleSetMetaData emd, ColumnInfoBuilder infoBuilder) {

		DictionaryInfo restoredDictionary = restorePosNegValue(amd, emd);
		if (restoredDictionary != null) {
			infoBuilder.setDictionary(restoredDictionary, amd.getValueSetRelation());
		} else {
			Set<String> valueSet = amd.getValueSet();
			if (amd.isBinominal()) {
				setBooleanDictionary(infoBuilder, valueSet);
			} else {
				infoBuilder.setDictionaryValues(valueSet == null ? Collections.emptySet() : valueSet,
						amd.getValueSetRelation());
			}
		}

		if (infoBuilder.getDictionary().valueSetWasShrunk() != amd.valueSetWasShrunk()) {
			infoBuilder.setValueSetWasShrunk(amd.valueSetWasShrunk());
		}
	}

	/**
	 * Sets a boolean dictionary based on the given value set on a best effort practice. There is no way to know the
	 * true positive and negative value so we guess that the first is negative and the second is positive.
	 *
	 * @param infoBuilder
	 * 		the builder
	 * @param valueSet
	 * 		the value set
	 */
	private static void setBooleanDictionary(ColumnInfoBuilder infoBuilder, Set<String> valueSet) {
		if (valueSet == null || valueSet.isEmpty()) {
			infoBuilder.setUnknownBooleanDictionary();
		} else {
			Iterator<String> it = valueSet.iterator();
			String negativeValue = it.next();
			String positiveValue = it.hasNext() ? it.next() : null;
			infoBuilder.setBooleanDictionaryValues(positiveValue, negativeValue);
		}
	}

	/**
	 * Returns the matching dictionary info for the given attribute meta data or {@code null}, if no matching dictionary
	 * info exists. Please note: We can never be 100% sure that the pos / neg values still match since the attribute
	 * meta data could have been modified. As a heuristic we check that the values sets of the attribute meta data and
	 * the dictionary info are equal.
	 *
	 * @param amd
	 * 		the attribute meta data
	 * @param emd
	 * 		ExampleSetMetaData potentially holding a map from label to dictionary info in its additional data
	 * @return the matching dictionary info or {@code null}
	 */
	private static DictionaryInfo restorePosNegValue(AttributeMetaData amd, ExampleSetMetaData emd) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, DictionaryInfo> posNegValues = (Map<String, DictionaryInfo>)
					emd.getAdditionalData(ADDITIONAL_DATA_POS_NEG_VALUE_KEY);
			if (posNegValues != null) {
				DictionaryInfo dictionaryInfo = posNegValues.get(amd.getName());
				// heuristic: if the value sets are equal we assume it is the same column as before
				// and reuse the pos / neg value stored in the ExampleSetMetaData's additional data
				if (dictionaryInfo != null && Objects.equals(dictionaryInfo.getValueSet(), amd.getValueSet())) {
					return dictionaryInfo;
				}
			}
		} catch (ClassCastException e) {
			// well then there is nothing we can do
		}
		return null;
	}

	/**
	 * Converts the given {@link Ontology} the the corresponding {@link ColumnType}.
	 *
	 * @param amd
	 * 		the attribtue meta data
	 * @param emd
	 * 		the example set meta data
	 * @return the column type
	 */
	private static Column.TypeId convertType(AttributeMetaData amd, ExampleSetMetaData emd) {
		switch (amd.getValueType()) {
			case Ontology.NUMERICAL:
			case Ontology.REAL:
				return Column.TypeId.REAL;
			case Ontology.INTEGER:
				return Column.TypeId.INTEGER_53_BIT;
			case Ontology.BINOMINAL:
			case Ontology.POLYNOMINAL:
			case Ontology.NOMINAL:
			case Ontology.STRING:
			case Ontology.FILE_PATH:
				return Column.TypeId.NOMINAL;
			case Ontology.DATE:
			case Ontology.DATE_TIME:
				return Column.TypeId.DATE_TIME;
			case Ontology.TIME:
				return Column.TypeId.TIME;
			default:
				return null; // unknown type
		}
	}

	/**
	 * {@link ColumnMetaData} (except for roles) cannot be stored in an ExampleSetMetaData. Therefore, we store the
	 * column meta data in the ExampleSetsMetaData's additional data. This is the method that restores this saved meta
	 * data from the additional data. It adds the restored meta data to the given {@link TableMetaDataBuilder}.
	 * <p>
	 * It is important that this method is called after the rest of the metadata has already been built because this
	 * method potentially overrides some of the existing meta data.
	 *
	 * @param emd
	 * 		the example set metadata potentially holding some column meta data in its additional data
	 * @param builder
	 * 		the builder that the meta data will be added to
	 */
	private static void restoreColumnMetaData(ExampleSetMetaData emd, TableMetaDataBuilder builder) {
		try {
			@SuppressWarnings("unchecked")
			Map<String, List<ColumnMetaData>> beltMetaData =
					(Map<String, List<ColumnMetaData>>) emd.getAdditionalData(ADDITIONAL_DATA_COLUMN_META_DATA_KEY);
			if (beltMetaData != null) {
				Set<String> labels = builder.labels();
				beltMetaData.forEach((label, columnMetaDataList) -> {
					if (labels.contains(label)) {
						for (ColumnMetaData md : columnMetaDataList) {
							// column roles, column annotations legacy types and legacy roles are already determined by
							// the example set metadata. for the rest we want to use the belt meta data that has been
							// stored before
							if (!(md instanceof ColumnRole || md instanceof LegacyType || md instanceof LegacyRole
									|| md instanceof ColumnAnnotation)) {
								builder.addColumnMetaData(label, md);
							}
						}
					}
				});
			}
		} catch (ClassCastException e) {
			// well then there is nothing we can do
		}
	}

	/**
	 * Copies the additional data from the ExampleSetMetData to the TableMetaData. Ignores {@link
	 * #ADDITIONAL_DATA_COLUMN_META_DATA_KEY}, {@link #ADDITIONAL_DATA_TYPE_KEY} and {@link
	 * #ADDITIONAL_DATA_POS_NEG_VALUE_KEY} because they are not needed anymore.
	 */
	private static void copyAdditionalData(ExampleSetMetaData emd, TableMetaData tmd) {
		for (Map.Entry<String, Object> data : emd.keyValueMap.entrySet()) {
			if (!ADDITIONAL_DATA_COLUMN_META_DATA_KEY.equals(data.getKey()) &&
					!ADDITIONAL_DATA_TYPE_KEY.equals(data.getKey()) &&
					!ADDITIONAL_DATA_POS_NEG_VALUE_KEY.equals(data.getKey())) {
				tmd.keyValueMap.put(data.getKey(), data.getValue());
			}
		}
	}

	/**
	 * Converts the given non-NaN double value to {@link LocalTime}.
	 *
	 * @param legacyTime
	 * 		the legacy studio (date)-time as double
	 * @return the corresponding local time
	 */
	private static LocalTime toLocalTime(double legacyTime) {
		return LocalTime.ofNanoOfDay(BeltConverter.legacyTimeDoubleToNanoOfDay(legacyTime, Tools.getPreferredCalendar()));
	}

}
