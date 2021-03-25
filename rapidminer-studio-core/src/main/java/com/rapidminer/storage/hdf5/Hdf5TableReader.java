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

import static com.rapidminer.storage.hdf5.Hdf5ExampleSetReader.addAnnotations;
import static com.rapidminer.storage.hdf5.Hdf5ExampleSetReader.getDatasetOrException;
import static com.rapidminer.storage.hdf5.Hdf5ExampleSetReader.getNonnegativeIntAttribute;
import static com.rapidminer.storage.hdf5.Hdf5ExampleSetReader.getSetRelation;
import static com.rapidminer.storage.hdf5.Hdf5ExampleSetReader.getSingleAttributeValueOrNull;
import static com.rapidminer.storage.hdf5.Hdf5ExampleSetReader.isMetadata;
import static com.rapidminer.storage.hdf5.Hdf5ExampleSetReader.readNumberOfRows;
import static com.rapidminer.storage.hdf5.IOTableHdf5Writer.ATTRIBUTE_LEGACY_TYPE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.hdf5.BufferedInChannel;
import com.rapidminer.hdf5.file.ColumnDescriptor;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.storage.hdf5.ColumnMetaDataStorageRegistry.Hdf5Deserializer;
import com.rapidminer.storage.hdf5.ColumnMetaDataStorageRegistry.IllegalHdf5FormatException;
import com.rapidminer.storage.hdf5.HdfReaderException.Reason;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Pair;

import io.jhdf.GlobalHeap;
import io.jhdf.HdfFile;
import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;
import io.jhdf.exceptions.HdfException;


/**
 * A reader for hdf5 files containing a data table written in the format defined by the {@link TableWriter}. The file
 * must contain the attributes {@link TableWriter#ATTRIBUTE_COLUMNS} and {@link TableWriter#ATTRIBUTE_ROWS}. It must
 * have datasets {@code a0}, ..., {@code an} where n is specified by {@link TableWriter#ATTRIBUTE_COLUMNS}-1 and every
 * dataset must have the attribute {@link TableWriter#ATTRIBUTE_TYPE} and {@link TableWriter#ATTRIBUTE_NAME}.
 * <p>
 * Numeric data must be of type {@link ColumnDescriptor.Hdf5ColumnType#REAL} or
 * {@link ColumnDescriptor.Hdf5ColumnType#INTEGER} and can be {@code double}, {@code float}, {@code int} or {@code long}
 * values, where for long values, {@link Long#MAX_VALUE} is transformed to {@link Double#NaN} and other values might
 * loose precision.
 * <p>
 * String data must be of type {@link ColumnDescriptor.Hdf5ColumnType#NOMINAL}. If the string dataset has the attribute
 * {@link TableWriter#ATTRIBUTE_DICTIONARY}, the attribute must either contain a String array with the dictionary or a
 * reference to a String array dataset with the dictionary. The first dictionary entry stands for the missing value. The
 * data consists of a {@code byte}, {@code short} or {@code int} array containing the category indices fitting to the
 * dictionary. If the string dataset does not have the attribute {@link TableWriter#ATTRIBUTE_DICTIONARY}, it must have
 * the attribute {@link TableWriter#ATTRIBUTE_MISSING} which specifies a String representing the missing value. The
 * dataset is then an array of String values. All String arrays can be either variable length or fixed length Strings.
 * If the nominal column should be boolean, it must have the attribute {@link TableWriter#ATTRIBUTE_POSITVE_INDEX}
 * describing the positive index which is either the dictionary index, or in case of String values, the position in the
 * column.
 * <p>
 * Date-time data must be of type {@link ColumnDescriptor.Hdf5ColumnType#DATE_TIME} and the dataset must be an array of
 * {@code long} values specifying seconds since 1970. Additionally, the dataset can have the attribute {@link
 * TableWriter#ATTRIBUTE_ADDITIONAL} which contains a reference to a dataset containing additional nanoseconds.
 * <p>
 * Statistics can be included for every column, see {@link TableStatisticsHandler}. The statistics is only read if the
 * attribute {@link IOTableHdf5Writer#ATTRIBUTE_HAS_STATISTICS} exists and is {@code 1}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public enum Hdf5TableReader {

	;//No-instance enum, only static methods

	private static final ColumnInfo NOMINAL_PLACEHOLDER = new ColumnInfoBuilder(ColumnType.NOMINAL).build();

	/**
	 * Reads an {@link IOTable} from the hdf5 file at the given path. See the class javadoc for the admissible formats.
	 *
	 * @param path
	 * 		the path to read from
	 * @param context
	 * 		the belt context to use for the {@link TableBuilder#build(Context)}. Since all columns are calculated
	 * 		beforehand, this can be a sequential context without performance loss
	 * @return the table read from the path
	 * @throws IOException
	 * 		if reading fails
	 * @throws HdfReaderException
	 * 		in case the content of the hdf5 file does not match the admissible format
	 */
	public static IOTable read(Path path, Context context) throws IOException {
		try (HdfFile hdfFile = new HdfFile(path)) {
			if (isMetadata(hdfFile)) {
				throw new HdfReaderException(Reason.IS_META_DATA, "File only contains meta data");
			}

			int numberOfRows = getNonnegativeIntAttribute(hdfFile, TableWriter.ATTRIBUTE_ROWS);
			int numberOfColumns = getNonnegativeIntAttribute(hdfFile, TableWriter.ATTRIBUTE_COLUMNS);

			List<String> names = new ArrayList<>(numberOfColumns);
			List<ColumnType<?>> types = new ArrayList<>(numberOfColumns);
			Map<String, LinkedHashSet<String>> dictionaries = new LinkedHashMap<>();
			List<Dataset> sets = new ArrayList<>(numberOfColumns);
			try (BufferedInChannel inChannel =
						 new BufferedInChannel(hdfFile.getHdfChannel().getFileChannel(), 1 << 16)) {
				Map<Long, GlobalHeap> heaps = new HashMap<>();
				Hdf5MappingReader mappingReader = new Hdf5MappingReader(hdfFile, inChannel, heaps);
				for (int i = 0; i < numberOfColumns; i++) {
					Dataset set = getDatasetOrException(hdfFile, i);
					sets.add(set);
					final String name = getName(set);
					names.add(name);
					final ColumnType<?> type = getType(set);
					types.add(type);
					if (type.id() == Column.TypeId.NOMINAL) {
						final LinkedHashSet<String> dictionary = mappingReader.getDictionary(set);
						dictionaries.put(name, dictionary);
					}
				}
				TableBuilder builder = Builders.newTableBuilder(numberOfRows);
				final Annotations annotations = new Annotations();
				addAnnotations(hdfFile, annotations);
				final Hdf5ColumnReader hdf5ColumnReader = new Hdf5ColumnReader(numberOfRows, hdfFile, inChannel,
						heaps);
				for (int i = 0; i < numberOfColumns; i++) {
					final String label = names.get(i);
					builder.add(label, hdf5ColumnReader.read(types.get(i), sets.get(i), dictionaries.get(label)));
					List<ColumnMetaData> columnMetaData = getColumnMetaData(sets.get(i));
					if (!columnMetaData.isEmpty()) {
						builder.addMetaData(label, columnMetaData);
					}
				}
				final IOTable ioTable = new IOTable(builder.build(context));
				ioTable.getAnnotations().addAll(annotations);
				return ioTable;
			}
		} catch (HdfException e) {
			throw new HdfReaderException(Reason.INCONSISTENT_FILE, e.getMessage());
		}
	}

	/**
	 * Reads an {@link TableMetaData} from the hdf5 file at the given path. See the class javadoc for the
	 * admissible formats. Ignores missing hdf5 statistics attributes or their wrong formats but fails on violations
	 * of other parts of the admissible formats.
	 *
	 * @param path
	 * 		the path to read from
	 * @return the example set metadata read from the path, or {@code null} if the {@link
	 *            ExampleSetHdf5Writer#ATTRIBUTE_HAS_STATISTICS} attribute is not 1
	 * @throws IOException
	 * 		if reading fails
	 * @throws HdfReaderException
	 * 		in case the content of the hdf5 file does not match the admissible format, this means it will also not be
	 * 		possible to read the {@link IOTable} and create the meta data from it
	 */
	public static TableMetaData readMetaData(Path path) throws IOException {
		return readMetaData(path, true);
	}

	/**
	 * Reads a {@link TableMetaData} from the hdf5 file at the given path. See the class javadoc for the
	 * admissible formats. Ignores missing hdf5 statistics attributes or their wrong formats but fails on violations
	 * of other parts of the admissible formats.
	 *
	 * @param path
	 * 		the path to read from
	 * @param statsMandatory
	 * 		whether the statistics must be part of the file to be read
	 * @return the example set metadata read from the path, or {@code null} if the {@link
	 *            ExampleSetHdf5Writer#ATTRIBUTE_HAS_STATISTICS} attribute is not 1
	 * @throws IOException
	 * 		if reading fails
	 * @throws HdfReaderException
	 * 		in case the content of the hdf5 file does not match the admissible format, this means it will also not be
	 * 		possible to read the {@link IOTable} and create the meta data from it
	 */
	public static TableMetaData readMetaData(Path path, boolean statsMandatory) throws IOException {
		try (HdfFile hdfFile = new HdfFile(path)) {
			Number statsValue =
					getSingleAttributeValueOrNull(hdfFile, ExampleSetHdf5Writer.ATTRIBUTE_HAS_STATISTICS,
							Number.class);
			if (statsMandatory && (statsValue == null || statsValue.byteValue() != 1)) {
				return null;
			}
			boolean isMetaData = isMetadata(hdfFile);

			List<Pair<String, ColumnInfoBuilder>> nominalBuilders = new ArrayList<>();
			List<Dataset> nominalDatasets = new ArrayList<>();

			final MDInteger rows = readNumberOfRows(hdfFile, isMetaData);
			TableMetaDataBuilder builder = new TableMetaDataBuilder(rows);
			readColumns(hdfFile, builder, nominalBuilders, nominalDatasets, isMetaData);

			Annotations annotations = new Annotations();
			addAnnotations(hdfFile, annotations);
			//this closes the file channel if there are dictionaries to read, so it must be after annotation reading
			readDictionaries(hdfFile, nominalBuilders, nominalDatasets, builder, isMetaData);

			final TableMetaData tmd = builder.build();
			tmd.getAnnotations().addAll(annotations);
			return tmd;

		} catch (HdfException e) {
			throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE, e.getMessage());
		}
	}


	/**
	 * Reads the columns from the {@link HdfFile} and adds the information as a {@link ColumnInfo} to the
	 * {@code metaData}. Also collects information about additional nominal information in {@code nominalBuilders} and
	 * {@code nominalDatasets}. If {@code isMetaData} is {@code true}, also reads the {@link SetRelation} for each column.
	 */
	private static void readColumns(HdfFile hdfFile, TableMetaDataBuilder builder,
									List<Pair<String, ColumnInfoBuilder>> nominalBuilders,
									List<Dataset> nominalDatasets,
									boolean isMetaData) {
		int numberOfColumns = getNonnegativeIntAttribute(hdfFile, TableWriter.ATTRIBUTE_COLUMNS);
		int maximumNumberOfMdColumns = TableMetaData.getMaximumNumberOfMdColumns();
		if (numberOfColumns > maximumNumberOfMdColumns) {
			numberOfColumns = maximumNumberOfMdColumns;
			builder.columnsAreSuperset();
		}
		for (int i = 0; i < numberOfColumns; i++) {
			Dataset set = getDatasetOrException(hdfFile, i);
			final String name = getName(set);
			final ColumnType<?> type = getType(set);
			ColumnInfoBuilder info = new ColumnInfoBuilder(type);
			if (isMetaData) {
				info.setValueSetRelation(getSetRelation(set));
			}

			TableStatisticsHandler.readStatistics(set, info);

			if (info.isNominal() == MetaDataInfo.YES) {
				nominalBuilders.add(new Pair<>(name, info));
				nominalDatasets.add(set);
				builder.add(name, NOMINAL_PLACEHOLDER);
			} else {
				builder.add(name, info.build());
			}
			List<ColumnMetaData> columnMetaData = getColumnMetaData(set);
			if (!columnMetaData.isEmpty()) {
				builder.addColumnMetaData(name, columnMetaData);
			}
		}
	}

	/**
	 * Reads the nominal dictionaries of the given nominal columns and adds the mapping to the corresponding {@link
	 * ColumnInfoBuilder} and then adds it to the table builder.
	 */
	private static void readDictionaries(HdfFile hdfFile, List<Pair<String, ColumnInfoBuilder>> nominalBuilders,
										 List<Dataset> nominalDatasets, TableMetaDataBuilder builder, boolean isMetaData) throws IOException {
		if (nominalBuilders.isEmpty()) {
			return;
		}

		int limit = AttributeMetaData.getMaximumNumberOfNominalValues();
		try (BufferedInChannel inChannel
					 = new BufferedInChannel(hdfFile.getHdfChannel().getFileChannel(), 1 << 16)) {
			Hdf5MappingReader mappingReader = new Hdf5MappingReader(hdfFile, inChannel, new HashMap<>());
			int index = 0;
			for (Pair<String, ColumnInfoBuilder> builderPair : nominalBuilders) {
				Dataset set = nominalDatasets.get(index++);
				SetRelation oldRelation =
						isMetaData ? builderPair.getSecond().getValueSetRelation() : SetRelation.EQUAL;
				mappingReader.addDictionary(set, builderPair.getSecond(), limit);
				builderPair.getSecond().setValueSetRelation(oldRelation.merge(builderPair.getSecond().getValueSetRelation()));

				if (isMetaData) {
					checkShrunk(builderPair, set);
				}
				builder.replace(builderPair.getFirst(), builderPair.getSecond().build());
			}
		}
	}

	/**
	 * Checks if the shrunk property needs to be changed.
	 */
	private static void checkShrunk(Pair<String, ColumnInfoBuilder> builderPair, Dataset set) {
		Number wasShrunk =
				getSingleAttributeValueOrNull(set, ExampleSetHdf5Writer.ATTRIBUTE_NOMINAL_SHRUNK,
						Number.class);
		if (wasShrunk != null && wasShrunk.byteValue() == 1 &&
				!builderPair.getSecond().getDictionary().valueSetWasShrunk()) {
			builderPair.getSecond().setValueSetWasShrunk(true);
		}
	}

	/**
	 * Retrieves column role, legacy type and legacy role for the dataset if present.
	 */
	private static List<ColumnMetaData> getColumnMetaData(Dataset set) {
		List<ColumnMetaData> metaData = new ArrayList<>(1);
		readRole(set, metaData);
		readColumnReference(set, metaData);

		final Set<String> identifiers = ColumnMetaDataStorageRegistry.getIdentifiers();
		//read all other ColumnMetaData
		for (String identifier : identifiers) {
			Attribute att = set.getAttribute(identifier);
			if (att != null) {
				final Pair<Class<?>, Hdf5Deserializer> deserializer =
						ColumnMetaDataStorageRegistry.getDeserializer(identifier);
				if (deserializer != null) {
					final ColumnMetaData deserialize = deserialize(att, identifier, deserializer, set);
					if (deserialize != null) {
						metaData.add(deserialize);
					}
				} else {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.storage.hdf5.Hdf5TableReader" +
							".no_deserializer", identifier);
				}
			}
		}
		// add warning for identifiers from extensions that cannot be read
		for (String key : set.getAttributes().keySet()) {
			if (key.startsWith("rmx_") && !identifiers.contains(key)) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.storage.hdf5.Hdf5TableReader" +
						".no_deserializer_plugin", new Object[]{key, key.replace("rmx_", "").split(":")[0]});
			}
		}
		return metaData;
	}

	/**
	 * Reads the {@link ColumnReference} from two separate attributes.
	 */
	private static List<ColumnMetaData> readColumnReference(Dataset set, List<ColumnMetaData> metaData) {
		final Attribute referenceAttribute = set.getAttribute(IOTableHdf5Writer.ATTRIBUTE_COLUMN_REFERENCE_COLUMN);
		String referenceColumn = null;
		if (referenceAttribute != null) {
			referenceColumn =
					getStringData(set, referenceAttribute, IOTableHdf5Writer.ATTRIBUTE_COLUMN_REFERENCE_COLUMN);
		}
		final Attribute referenceValueAttribute = set.getAttribute(IOTableHdf5Writer.ATTRIBUTE_COLUMN_REFERENCE_VALUE);
		String referenceColumnValue = null;
		if (referenceValueAttribute != null) {
			referenceColumnValue =
					getStringData(set, referenceValueAttribute, IOTableHdf5Writer.ATTRIBUTE_COLUMN_REFERENCE_VALUE);
		}
		if (referenceColumn != null || referenceColumnValue != null) {
			metaData.add(new ColumnReference(referenceColumn, referenceColumnValue));
		}
		return metaData;
	}

	/**
	 * Reads the column role.
	 */
	private static List<ColumnMetaData> readRole(Dataset set, List<ColumnMetaData> metaData) {
		Attribute role = set.getAttribute(TableWriter.ATTRIBUTE_ROLE);
		if (role != null) {
			String value = getStringData(set, role, TableWriter.ATTRIBUTE_ROLE).trim();
			try {
				final ColumnRole columnRole = ColumnRole.valueOf(value);
				metaData.add(columnRole);
			} catch (IllegalArgumentException e) {
				//wrong role
				throw new HdfReaderException(Reason.INCONSISTENT_FILE, "Illegal column role " + value);
			}
		}
		return metaData;
	}

	/**
	 * Gets string data from the attribute of the set or throws exception.
	 */
	private static String getStringData(Dataset set, Attribute attribute, String name) {
		if (!String.class.equals(attribute.getJavaType()) || attribute.getDimensions().length > 0) {
			throw new HdfReaderException(Reason.ILLEGAL_TYPE, "attribute " + name + " must have a" +
					" String value for " + set.getPath());
		}
		return ((String) attribute.getData());
	}

	/**
	 * Deserializes the data of the att with the deserializer.
	 */
	private static ColumnMetaData deserialize(Attribute att, String identifier,
											  Pair<Class<?>, Hdf5Deserializer> deserializer, Dataset set) {
		final Class<?> desiredClass = deserializer.getFirst();
		if (att.getDimensions().length > 0) {
			throw new HdfReaderException(Reason.ILLEGAL_TYPE, "attribute " + identifier + " must" +
					" not be an array object for " + set.getPath());
		}
		if (isNumber(desiredClass)) {
			Object data = att.getData();
			if (!(data instanceof Number)) {
				throw new HdfReaderException(Reason.ILLEGAL_TYPE,
						identifier + " attribute must have a number value");
			}
			return deserializeNumber(deserializer, desiredClass, (Number) data, identifier);
		} else {
			if (!desiredClass.equals(att.getJavaType())) {
				throw new HdfReaderException(Reason.ILLEGAL_TYPE, "attribute " + identifier + " must" +
						" have a " + desiredClass.getSimpleName() + " value for " + set.getPath());
			}
			final Object data = att.getData();
			try {
				return deserializer.getSecond().applyWithException(data);
			} catch (IllegalHdf5FormatException e) {
				throw new HdfReaderException(Reason.ILLEGAL_TYPE, e.getMessage());
			} catch (RuntimeException e) {
				//prevent faulty custom ColumnMetaData serialization to prevent whole read
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.storage.hdf5.Hdf5TableReader.error_deserializer", identifier), e);
				return null;
			}
		}
	}

	/**
	 * Deserializes number data with the deserializer function.
	 */
	private static ColumnMetaData deserializeNumber(Pair<Class<?>, Hdf5Deserializer> deserializer,
													Class<?> desiredClass, Number data, String identifier) {
		try {
			final Hdf5Deserializer deserializationFunction = deserializer.getSecond();
			if (int.class.equals(desiredClass)) {
				return deserializationFunction.applyWithException(data.intValue());
			} else if (byte.class.equals(desiredClass)) {
				return deserializationFunction.applyWithException(data.byteValue());
			} else if (short.class.equals(desiredClass)) {
				return deserializationFunction.applyWithException(data.shortValue());
			} else if (double.class.equals(desiredClass)) {
				return deserializationFunction.applyWithException(data.doubleValue());
			} else if (long.class.equals(desiredClass)) {
				return deserializationFunction.applyWithException(data.longValue());
			} else {
				throw new AssertionError("Unexpected number class for deserialization");
			}
		} catch (IllegalHdf5FormatException e) {
			throw new HdfReaderException(Reason.ILLEGAL_TYPE, e.getMessage());
		} catch (RuntimeException e) {
			//prevent faulty custom ColumnMetaData serialization to prevent whole read
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.storage.hdf5.Hdf5TableReader.error_deserializer", identifier), e);
			return null;
		}
	}

	/**
	 * Checks if the clazz is int, long, double, short or byte.
	 */
	private static boolean isNumber(Class<?> clazz) {
		return int.class.equals(clazz) || byte.class.equals(clazz) || double.class.equals(clazz) ||
				long.class.equals(clazz) || short.class.equals(clazz);
	}

	/**
	 * Retrieves the column type from the type attribute of the dataset.
	 */
	private static ColumnType<?> getType(Dataset set) {
		Attribute type = set.getAttribute(TableWriter.ATTRIBUTE_TYPE);
		if (type == null) {
			throw new HdfReaderException(Reason.MISSING_ATTRIBUTE,
					TableWriter.ATTRIBUTE_TYPE + " attribute missing for " + set.getPath());
		}
		Object typeData = type.getData();
		if (!(typeData instanceof String)) {
			throw new HdfReaderException(Reason.ILLEGAL_TYPE, TableWriter.ATTRIBUTE_TYPE +
					" attribute must have String value");
		}
		ColumnDescriptor.Hdf5ColumnType cType = ColumnDescriptor.Hdf5ColumnType.fromString((String) typeData);
		if (cType == null) {
			throw new HdfReaderException(Reason.ILLEGAL_TYPE, type + " not allowed as type for " +
					set.getPath());
		}
		switch (cType) {
			case NOMINAL:
				return ColumnType.NOMINAL;
			case REAL:
				return ColumnType.REAL;
			case INTEGER:
				return ColumnType.INTEGER_53_BIT;
			case TIME:
				return ColumnType.TIME;
			case DATE_TIME:
				//handle the case that it is time stored as date-time in pre 9.9
				Attribute legacyType = set.getAttribute(ATTRIBUTE_LEGACY_TYPE);
				if (legacyType != null) {
					Object ontologyData = legacyType.getData();
					if (ontologyData instanceof Number && ((Number) ontologyData).intValue() == Ontology.TIME) {
						return ColumnType.TIME;
					}
				}
				return ColumnType.DATETIME;
			default:
				throw new HdfReaderException(Reason.ILLEGAL_TYPE, cType + " not allowed as type for " +
						set.getPath());
		}
	}

	/**
	 * Retrieves the column name from the name attribute of the dataset.
	 */
	private static String getName(Dataset set) {
		Attribute name = set.getAttribute(TableWriter.ATTRIBUTE_NAME);
		if (name == null) {
			throw new HdfReaderException(Reason.MISSING_ATTRIBUTE,
					TableWriter.ATTRIBUTE_NAME + " attribute missing for " + set.getPath());
		}
		Object nameData = name.getData();
		if (!(nameData instanceof String)) {
			throw new HdfReaderException(Reason.ILLEGAL_TYPE, TableWriter.ATTRIBUTE_NAME +
					" attribute must have String value");
		}
		return (String) nameData;
	}

}