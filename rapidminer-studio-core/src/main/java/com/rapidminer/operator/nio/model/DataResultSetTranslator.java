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
package com.rapidminer.operator.nio.model;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Columns;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.LegacyRole;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.table.MixedRowWriter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.table.Writers;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeTypeException;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.operator.nio.model.DataResultSet.ValueType;
import com.rapidminer.operator.nio.model.ParsingError.ErrorCode;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.parameter.internal.DataManagementParameterHelper;


/**
 * This class encapsulates the translation step from a {@link DataResultSetTranslator} to an
 * {@link ExampleSet} or {@link IOTable} which is controlled by the {@link DataResultSetTranslationConfiguration}.
 *
 * @author Sebastian Land, Gisa Meier
 */
public class DataResultSetTranslator {

	private static class NominalValueSet {

		private String first = null;
		private String second = null;
		private boolean moreThanTwo = false;

		private boolean register(String value) {
			if (moreThanTwo) {
				return true;
			} else if (value == null) {
				return false;
			} else if (first == null) {
				first = value;
				return false;
			} else if (first.equals(value)) {
				return false;
			} else if (second == null) {
				second = value;
				return false;
			} else if (second.equals(value)) {
				return false;
			} else {
				moreThanTwo = true;
				return true;
			}
		}
	}

	private volatile boolean shouldStop = false;
	private volatile boolean isReading = false;

	private boolean cancelGuessingRequested = false;
	private boolean cancelLoadingRequested = false;

	private final Map<Pair<Integer, Integer>, ParsingError> errors = new LinkedHashMap<>();

	/**
	 * From this version, the binominal data type never will be chosen, because it fails too often.
	 */
	public static final OperatorVersion VERSION_6_0_3 = new OperatorVersion(6, 0, 3);

	/**
	 * From this version, attribute names will be trimmed on read/import
	 */
	public static final OperatorVersion BEFORE_ATTRIBUTE_TRIMMING = new OperatorVersion(8, 1, 0);

	private final Calendar preferredCalendar = Tools.getPreferredCalendar();

	private Operator operator;

	public DataResultSetTranslator(Operator operator) {
		this.operator = operator;
	}

	/**
	 * This method will start the translation of the actual ResultDataSet to an ExampleSet.
	 */
	public ExampleSet read(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration,
						   boolean previewOnly, ProgressListener listener) throws OperatorException {
		shouldStop = false;
		cancelLoadingRequested = false;
		try {
			isReading = true;
			return readInternal(dataResultSet, configuration, previewOnly, listener);
		} finally {
			isReading = false;
			if (listener != null) {
				listener.complete();
			}
		}
	}

	/**
	 * Translates the ResultDataSet to an IOTable in a way that converting it to an {@link ExampleSet} afterwards
	 * yields the same result as using {@link #read}. For that reason some strange behaviors like assigning the positive
	 * value of a boolean column by which values is encountered first are kept.
	 *
	 * @param dataResultSet
	 * 		the data to translate into a table
	 * @param configuration
	 * 		the configuration to use for the translation
	 * @param previewOnly
	 * 		whether to read only the first few lines
	 * @param listener
	 * 		the progress listener to use
	 * @return the data result set as an {@link IOTable}
	 * @throws OperatorException
	 * 		if the translation fails
	 *
	 * @since 9.9.0
	 */
	public IOTable readTable(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration,
							 boolean previewOnly, ProgressListener listener) throws OperatorException {
		shouldStop = false;
		cancelLoadingRequested = false;
		try {
			isReading = true;
			return readInternalTable(dataResultSet, configuration, previewOnly, listener);
		} finally {
			isReading = false;
			if (listener != null) {
				listener.complete();
			}
		}
	}

	private ExampleSet readInternal(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration,
									boolean previewOnly, ProgressListener listener) throws OperatorException {
		int maxRows = previewOnly ? ImportWizardUtils.getPreviewLength() : -1;
		boolean isFaultTolerant = configuration.isFaultTolerant();
		int[] attributeColumns = configuration.getSelectedIndices();
		int numberOfAttributes = attributeColumns.length;

		Attribute[] attributes = new Attribute[numberOfAttributes];
		for (int i = 0; i < attributes.length; i++) {
			int attributeValueType = configuration.getColumnMetaData(attributeColumns[i]).getAttributeValueType();
			if (attributeValueType == Ontology.ATTRIBUTE_VALUE) {
				attributeValueType = Ontology.POLYNOMINAL;
			}
			attributes[i] = AttributeFactory.createAttribute(
					configuration.getColumnMetaData(attributeColumns[i]).getOriginalAttributeName(),
					attributeValueType);
		}

		// check whether all columns are accessible
		checkAccessibility(dataResultSet, configuration, attributeColumns);

		// building example set
		ExampleSetBuilder builder = ExampleSets.from(attributes);

		// now iterate over complete dataResultSet and copy data
		int currentRow = 0;        // The row in the underlying DataResultSet
		int exampleIndex = 0;        // The row in the example set
		dataResultSet.reset(listener);

		int datamanagement = configuration.getDataManagementType();
		if (!Boolean.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			datamanagement = DataRowFactory.TYPE_DOUBLE_ARRAY;
			// TODO: move to DataResultSetTranslationConfiguration if beta mode becomes standard
			if (operator != null) {
				try {
					builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(operator));
				} catch (UndefinedParameterError e) {
					// use auto mode
				}
			}
		}

		DataRowFactory factory = new DataRowFactory(datamanagement, '.');
		int maxAnnotatedRow = configuration.getLastAnnotatedRowIndex();

		// detect if this is executed in a process
		boolean isRunningInProcess = isRunningInProcess();

		while (dataResultSet.hasNext() && !shouldStop && (currentRow < maxRows || maxRows < 0)) {
			if (isRunningInProcess) {
				operator.checkForStop();
			}
			if (cancelLoadingRequested) {
				break;
			}
			dataResultSet.next(listener);
			// checking for annotation
			String currentAnnotation = getCurrentAnnotation(configuration, currentRow, maxAnnotatedRow);
			if (currentAnnotation != null) {
				// registering annotation on all attributes
				int attributeIndex = 0;
				List<String> attributeNames = new ArrayList<>();
				for (Attribute attribute : attributes) {
					if (AbstractDataResultSetReader.ANNOTATION_NAME.equals(currentAnnotation)) {
						// resetting name

						// going into here, setting the names, maybe add checks here

						String newAttributeName = getString(dataResultSet, exampleIndex, attributeColumns[attributeIndex],
								isFaultTolerant);
						if (newAttributeName != null && !newAttributeName.isEmpty()) {

							// going into here, setting the names, maybe add checks here
							String uniqueAttributeName = newAttributeName;
							int uniqueNameNumber = 1;
							while (attributeNames.contains(uniqueAttributeName)) {
								uniqueAttributeName = newAttributeName + "(" + uniqueNameNumber + ")";
								uniqueNameNumber++;
							}

							attribute.setName(uniqueAttributeName);
							attribute.setConstruction(uniqueAttributeName);
							// We also remember the name in the CMD since we otherwise would
							// override the attribute name later in this method
							ColumnMetaData cmd = configuration.getColumnMetaData(attributeColumns[attributeIndex]);
							if (cmd != null) {
								if (!cmd.isAttributeNameSpecified()) {
									cmd.setUserDefinedAttributeName(uniqueAttributeName);
								}
							}

						}
					} else {
						// setting annotation
						String annotationValue = getString(dataResultSet, exampleIndex, attributeColumns[attributeIndex],
								isFaultTolerant);
						if (annotationValue != null && !annotationValue.isEmpty()) {
							attribute.getAnnotations().put(currentAnnotation, annotationValue);
						}
					}
					attributeNames.add(attribute.getName());
					attributeIndex++;
				}
			} else {
				// creating data row
				DataRow row = factory.create(attributes.length);
				int attributeIndex = 0;
				for (Attribute attribute : attributes) {
					// check for missing
					if (dataResultSet.isMissing(attributeColumns[attributeIndex])) {
						row.set(attribute, Double.NaN);
					} else {
						switch (attribute.getValueType()) {
							case Ontology.INTEGER:
								row.set(attribute, getOrParseNumber(configuration, dataResultSet, exampleIndex,
										attributeColumns[attributeIndex], isFaultTolerant));
								break;
							case Ontology.NUMERICAL:
							case Ontology.REAL:
								row.set(attribute, getOrParseNumber(configuration, dataResultSet, exampleIndex,
										attributeColumns[attributeIndex], isFaultTolerant));
								break;
							case Ontology.DATE_TIME:
							case Ontology.TIME:
							case Ontology.DATE:
								row.set(attribute, getOrParseDate(configuration, dataResultSet, exampleIndex,
										attributeColumns[attributeIndex], isFaultTolerant));
								break;
							default:
								row.set(attribute, getStringIndex(attribute, dataResultSet, exampleIndex,
										attributeColumns[attributeIndex], isFaultTolerant));
						}
					}
					attributeIndex++;
				}
				builder.addDataRow(row);
				exampleIndex++;
			}
			currentRow++;
		}

		// derive ExampleSet from builder and assigning roles
		ExampleSet exampleSet = builder.build();
		// Copy attribute list to avoid concurrent modification when setting to special
		List<Attribute> allAttributes = new LinkedList<>();
		for (Attribute att : exampleSet.getAttributes()) {
			allAttributes.add(att);
		}

		int attributeIndex = 0;
		List<String> attributeNames = new ArrayList<>();
		for (Attribute attribute : allAttributes) {
			// if user defined names have been found, rename accordingly
			final ColumnMetaData cmd = configuration.getColumnMetaData(attributeColumns[attributeIndex]);
			if (!cmd.isSelected()) {
				attributeIndex++;
				continue;
			}

			String userDefinedName = cmd.getUserDefinedAttributeName();
			String uniqueUserDefinedName = userDefinedName;
			int uniqueNameNumber = 1;
			while (attributeNames.contains(uniqueUserDefinedName)) {
				uniqueUserDefinedName = userDefinedName + "(" + uniqueNameNumber + ")";
				uniqueNameNumber++;
			}

			if (uniqueUserDefinedName != null && !uniqueUserDefinedName.isEmpty()) {
				attribute.setName(uniqueUserDefinedName);
			}
			attribute.setConstruction(uniqueUserDefinedName);

			String roleId = cmd.getRole();
			if (!Attributes.ATTRIBUTE_NAME.equals(roleId)) {
				exampleSet.getAttributes().setSpecialAttribute(attribute, roleId);
			}
			attributeIndex++;
			attributeNames.add(attribute.getName());
		}

		return exampleSet;
	}

	/**
	 * Reads the dataResultSet into an {@link IOTable} in a way that converting it to an {@link ExampleSet} afterwards
	 * yields the same result as using {@link #readInternal}. For that reason some strange behaviors like assigning the
	 * positive value of a boolean column by which values is encountered first are kept.
	 */
	private IOTable readInternalTable(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration,
									  boolean previewOnly, ProgressListener listener) throws OperatorException {
		int maxRows = previewOnly ? ImportWizardUtils.getPreviewLength() : -1;
		int[] attributeColumns = configuration.getSelectedIndices();
		int numberOfAttributes = attributeColumns.length;

		List<String> names = new ArrayList<>(numberOfAttributes);
		int[] originalOntologies = new int[numberOfAttributes];
		List<Column.TypeId> columnTypes = new ArrayList<>(numberOfAttributes);
		readNamesAndTypes(configuration, attributeColumns, names, originalOntologies, columnTypes);

		// check whether all columns are accessible
		checkAccessibility(dataResultSet, configuration, attributeColumns);

		// building example set
		MixedRowWriter writer;
		if (maxRows < 0) {
			writer = Writers.mixedRowWriter(names, columnTypes, false);
		} else {
			writer = Writers.mixedRowWriter(names, columnTypes, maxRows, false);
		}


		// now iterate over complete dataResultSet and copy data
		int currentRow = 0;        // The row in the underlying DataResultSet
		int exampleIndex = 0;        // The row in the example set
		dataResultSet.reset(listener);

		int maxAnnotatedRow = configuration.getLastAnnotatedRowIndex();

		// detect if this is executed in a process
		boolean isRunningInProcess = isRunningInProcess();

		Map<String, String> renamingMap = new HashMap<>();
		Map<Integer, List<String>> binominalLegacy = new HashMap<>();

		while (dataResultSet.hasNext() && !shouldStop && (currentRow < maxRows || maxRows < 0)) {
			if (isRunningInProcess) {
				operator.checkForStop();
			}
			if (cancelLoadingRequested) {
				break;
			}
			dataResultSet.next(listener);
			// checking for annotation
			String currentAnnotation = getCurrentAnnotation(configuration, currentRow, maxAnnotatedRow);
			if (currentAnnotation != null) {
				// registering annotation on all attributes
				readAnnotationData(dataResultSet, configuration, attributeColumns, names, renamingMap,
						currentAnnotation, exampleIndex);
			} else {
				// creating data row
				writer.move();
				readRow(dataResultSet, configuration, attributeColumns, originalOntologies,
						columnTypes, writer, exampleIndex, binominalLegacy);
				exampleIndex++;
			}
			currentRow++;
		}


		// derive table from builder and assigning roles
		Table table = writer.create();
		TableBuilder builder = Builders.newTableBuilder(table);

		finishTableContent(configuration, attributeColumns, names, originalOntologies, renamingMap, table, builder);
		final ConcurrencyContext concurrencyContext =
				operator == null ? new SequentialConcurrencyContext() : Resources.getConcurrencyContext(operator);
		final Table build = builder.build(ContextAdapter.adapt(concurrencyContext));
		// renaming via builder is slow, so do it afterwards. Also allows swapping of names which is otherwise not
		// supported
		final Table renamed = build.rename(renamingMap);
		return new IOTable(renamed);
	}

	/**
	 * Gets the annotation for the current row.
	 */
	private String getCurrentAnnotation(DataResultSetTranslationConfiguration configuration, int currentRow,
										int maxAnnotatedRow) {
		String currentAnnotation;
		if (currentRow <= maxAnnotatedRow) {
			currentAnnotation = configuration.getAnnotation(currentRow);
		} else {
			currentAnnotation = null;
		}
		return currentAnnotation;
	}

	/**
	 * Reads the annotated data row. If the annotation is the name annotation, the data from the row is taken for the
	 * new column names.
	 */
	private void readAnnotationData(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration,
									int[] attributeColumns, List<String> names, Map<String,	String> renamingMap,
									String currentAnnotation, int exampleIndex) throws UserError {
		boolean isFaultTolerant = configuration.isFaultTolerant();
		int attributeIndex = 0;
		Set<String> attributeNames = new HashSet<>();
		for (String attribute : names) {
			if (AbstractDataResultSetReader.ANNOTATION_NAME.equals(currentAnnotation)) {
				// resetting name

				String newAttributeName =
						getString(dataResultSet, exampleIndex, attributeColumns[attributeIndex],
								isFaultTolerant);
				handleColumnName(configuration, attributeColumns, renamingMap, attributeIndex, attributeNames,
						attribute, newAttributeName);
			} else {
				// setting annotation
				// not supported by belt tables but keeping reading behavior
				getString(dataResultSet, exampleIndex, attributeColumns[attributeIndex], isFaultTolerant);

				attributeNames.add(attribute);
			}

			attributeIndex++;
		}
	}

	/**
	 * Checks if the translation is running in a process.
	 */
	private boolean isRunningInProcess() {
		boolean isRunningInProcess = false;
		if (operator != null) {
			Process process;
			process = operator.getProcess();
			if (process != null && process.getProcessState() == Process.PROCESS_STATE_RUNNING) {
				isRunningInProcess = true;
			}
		}
		return isRunningInProcess;
	}

	/**
	 * Handles a column name read from the annotation and ensures uniqueness.
	 */
	private void handleColumnName(DataResultSetTranslationConfiguration configuration, int[] attributeColumns,
								  Map<String, String> renamingMap, int attributeIndex, Set<String> attributeNames,
								  String attribute, String newAttributeName) {
		if (newAttributeName == null || newAttributeName.isEmpty()) {
			attributeNames.add(attribute);
			return;
		}

		String uniqueAttributeName = newAttributeName;
		int uniqueNameNumber = 1;
		while (attributeNames.contains(uniqueAttributeName)) {
			uniqueAttributeName = newAttributeName + "(" + uniqueNameNumber + ")";
			uniqueNameNumber++;
		}

		renamingMap.put(attribute, uniqueAttributeName);

		//need to keep this legacy stuff, otherwise might change the behaviour:
		// We also remember the name in the CMD since we otherwise would
		// override the attribute name later in this method
		ColumnMetaData cmd = configuration.getColumnMetaData(attributeColumns[attributeIndex]);
		if (cmd != null && !cmd.isAttributeNameSpecified()) {
			cmd.setUserDefinedAttributeName(uniqueAttributeName);
		}
		attributeNames.add(uniqueAttributeName);
	}

	/**
	 * Reads one data row into the row writer.
	 */
	private void readRow(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration,
						 int[] attributeColumns, int[] originalOntologies, List<Column.TypeId> columnTypes,
						 MixedRowWriter writer, int exampleIndex, Map<Integer, List<String>> binominalLegacy) throws OperatorException {
		boolean isFaultTolerant = configuration.isFaultTolerant();
		int attributeIndex = 0;
		for (Column.TypeId type : columnTypes) {
			// check for missing
			if (dataResultSet.isMissing(attributeColumns[attributeIndex])) {
				writeMissing(writer, attributeIndex, type);
			} else {
				switch (type) {
					case INTEGER_53_BIT:
					case REAL:
						writer.set(attributeIndex, getOrParseNumber(configuration, dataResultSet, exampleIndex,
								attributeColumns[attributeIndex], isFaultTolerant));
						break;
					case DATE_TIME:
						writer.set(attributeIndex, getOrParseInstant(configuration, dataResultSet,
								exampleIndex,
								attributeColumns[attributeIndex], isFaultTolerant));
						break;
					case TIME:
						writer.set(attributeIndex, getOrParseLocalTime(configuration, dataResultSet,
								exampleIndex,
								attributeColumns[attributeIndex], isFaultTolerant));
						break;
					default:
						if (originalOntologies[attributeIndex] == Ontology.BINOMINAL) {
							writer.set(attributeIndex,
									getLegacyBinominal(dataResultSet, exampleIndex,
											attributeColumns[attributeIndex], binominalLegacy,
											isFaultTolerant));
						} else {
							writer.set(attributeIndex,
									getUntrimmedString(dataResultSet, exampleIndex,
											attributeColumns[attributeIndex],
											isFaultTolerant));
						}
				}
			}
			attributeIndex++;
		}
	}

	private void writeMissing(MixedRowWriter writer, int attributeIndex, Column.TypeId type) {
		if (type == Column.TypeId.INTEGER_53_BIT || type == Column.TypeId.REAL) {
			writer.set(attributeIndex, Double.NaN);
		} else {
			writer.set(attributeIndex, null);
		}
	}

	/**
	 * Checks the column names from the column meta data for uniqueness. Ensures binominal columns are made boolean for
	 * compatibility reason even if this means blindly picking the second value as positive. Keeps legacy types and
	 * roles for compatiblity.
	 */
	private void finishTableContent(DataResultSetTranslationConfiguration configuration, int[] attributeColumns,
									List<String> names, int[] originalOntologies, Map<String, String> renamingMap,
									Table table, TableBuilder builder) {
		int attributeIndex = 0;
		Set<String> attributeNames = new HashSet<>();
		for (String label : names) {
			// if user defined names have been found, rename accordingly
			final ColumnMetaData cmd = configuration.getColumnMetaData(attributeColumns[attributeIndex]);
			if (!cmd.isSelected()) {
				attributeIndex++;
				continue;
			}

			ensureColumnNameUniqueness(renamingMap, attributeNames, label, cmd);
			final String renamingName = renamingMap.get(label);
			attributeNames.add(renamingName != null ? renamingName : label);

			final int originalOntology = originalOntologies[attributeIndex];
			makeBinominalBoolean(table, builder, label, originalOntology);
			// keep original ontology so that when converting back to ExampleSet the result is the same as readInternal
			if (!LegacyType.DIRECTLY_MAPPED_ONTOLOGIES.contains(originalOntology)) {
				builder.addMetaData(label, LegacyType.forOntology(originalOntology));
			}

			String roleId = cmd.getRole();
			if (!Attributes.ATTRIBUTE_NAME.equals(roleId)) {
				ColumnRole convert = BeltConverter.convertRole(roleId);
				builder.addMetaData(label, convert);
				if (convert == ColumnRole.METADATA || convert == ColumnRole.SCORE) {
					builder.addMetaData(label, new LegacyRole(roleId));
				}
			}
			attributeIndex++;

		}
	}

	private void makeBinominalBoolean(Table table, TableBuilder builder, String label, int originalOntology) {
		if (originalOntology == Ontology.BINOMINAL) {
			// wrote it as nominal in writer, must change to boolean for backwards compatibility
			final Column nominalColumn = table.column(label);
			final Dictionary dictionary = nominalColumn.getDictionary();
			String positiveValue = null;
			if (dictionary.size() == 2) {
				// first is negative, second is positive
				positiveValue = dictionary.get(2);
			}
			builder.replace(label, Columns.toBoolean(nominalColumn, positiveValue));
		}
	}

	private void ensureColumnNameUniqueness(Map<String, String> renamingMap, Set<String> attributeNames, String label, ColumnMetaData cmd) {
		String userDefinedName = cmd.getUserDefinedAttributeName();
		String uniqueUserDefinedName = userDefinedName;
		int uniqueNameNumber = 1;
		while (attributeNames.contains(uniqueUserDefinedName)) {
			uniqueUserDefinedName = userDefinedName + "(" + uniqueNameNumber + ")";
			uniqueNameNumber++;
		}

		if (uniqueUserDefinedName != null && !uniqueUserDefinedName.isEmpty()) {
			renamingMap.put(label, uniqueUserDefinedName);
		}
	}

	/**
	 * Checks if all required columns are accessible and throws an error if not fault tolerant.
	 */
	private void checkAccessibility(DataResultSet dataResultSet, DataResultSetTranslationConfiguration configuration,
									int[] attributeColumns) throws UserError {
		int numberOfAvailableColumns = dataResultSet.getNumberOfColumns();
		for (int attributeColumn : attributeColumns) {
			if (!configuration.isFaultTolerant() && attributeColumn >= numberOfAvailableColumns) {
				throw new UserError(null, "data_import.specified_more_columns_than_exist",
						configuration.getColumnMetaData(attributeColumn).getUserDefinedAttributeName(),
						attributeColumn);
			}
		}
	}

	/**
	 * Reads initial names and types from the configuration.
	 */
	private void readNamesAndTypes(DataResultSetTranslationConfiguration configuration, int[] attributeColumns,
								   List<String> names, int[] originalOntologies, List<Column.TypeId> columnTypes) {
		for (int i = 0; i < originalOntologies.length; i++) {
			int attributeValueType = configuration.getColumnMetaData(attributeColumns[i]).getAttributeValueType();
			if (attributeValueType == Ontology.ATTRIBUTE_VALUE) {
				attributeValueType = Ontology.POLYNOMINAL;
			}
			originalOntologies[i] = attributeValueType;
			String originalAttributeName =
					configuration.getColumnMetaData(attributeColumns[i]).getOriginalAttributeName();
			if (originalAttributeName == null || originalAttributeName.isEmpty()) {
				originalAttributeName = AttributeFactory.GENSYM_PREFIX + i;
			}
			names.add(originalAttributeName);
			columnTypes.add(getTypeForOntology(attributeValueType));
		}
	}

	/**
	 * Translates ontology to belt column type.
	 */
	private Column.TypeId getTypeForOntology(int attributeValueType) {
		switch (attributeValueType) {
			case Ontology.INTEGER:
				return Column.TypeId.INTEGER_53_BIT;
			case Ontology.NUMERICAL:
			case Ontology.REAL:
				return Column.TypeId.REAL;
			case Ontology.TIME:
				return Column.TypeId.TIME;
			case Ontology.DATE:
			case Ontology.DATE_TIME:
				return Column.TypeId.DATE_TIME;
			default:
				return Column.TypeId.NOMINAL;
		}
	}

	/**
	 * If native type is date, returns the date. Otherwise, uses string and parses.
	 */
	private double getOrParseDate(DataResultSetTranslationConfiguration config, DataResultSet dataResultSet, int row,
								  int column, boolean isFaultTolerant) throws OperatorException {
		ValueType nativeValueType;
		try {
			nativeValueType = dataResultSet.getNativeValueType(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e1) {
			addOrThrow(isFaultTolerant, e1.getError(), row);
			return Double.NaN;
		}
		if (nativeValueType == ValueType.DATE) {
			return getDate(dataResultSet, row, column, isFaultTolerant);
		} else {
			String value = getString(dataResultSet, row, column, isFaultTolerant);
			try {
				return config.getDateFormat().parse(value).getTime();
			} catch (ParseException e) {
				ParsingError error = new ParsingError(dataResultSet.getCurrentRow() + 1, column, ErrorCode.UNPARSEABLE_DATE,
						value, e);
				addOrThrow(isFaultTolerant, error, row);
				return Double.NaN;
			}
		}
	}

	private double getDate(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant)
			throws OperatorException {
		try {
			return dataResultSet.getDate(column).getTime();
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return Double.NaN;
		}
	}

	/**
	 * If native type is date, returns the date as instant. Otherwise, uses string and parses. Same as
	 * {@link #getOrParseDate} except that it returns an {@link Instant} instead of a double.
	 */
	private Instant getOrParseInstant(DataResultSetTranslationConfiguration config, DataResultSet dataResultSet,
									  int row,
									  int column, boolean isFaultTolerant) throws OperatorException {
		ValueType nativeValueType;
		try {
			nativeValueType = dataResultSet.getNativeValueType(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e1) {
			addOrThrow(isFaultTolerant, e1.getError(), row);
			return null;
		}
		if (nativeValueType == ValueType.DATE) {
			return getInstant(dataResultSet, row, column, isFaultTolerant);
		} else {
			String value = getString(dataResultSet, row, column, isFaultTolerant);
			try {
				return config.getDateFormat().parse(value).toInstant();
			} catch (ParseException e) {
				ParsingError error =
						new ParsingError(dataResultSet.getCurrentRow() + 1, column, ErrorCode.UNPARSEABLE_DATE,
								value, e);
				addOrThrow(isFaultTolerant, error, row);
				return null;
			}
		}
	}

	private Instant getInstant(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant)
			throws OperatorException {
		try {
			return dataResultSet.getDate(column).toInstant();
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return null;
		}
	}

	/**
	 * If native type is date and it should be time, returns the date as local time. Otherwise, uses string and parses.
	 * Same as {@link #getOrParseDate} except that it returns an {@link LocalTime} converted from the double.
	 */
	private LocalTime getOrParseLocalTime(DataResultSetTranslationConfiguration config, DataResultSet dataResultSet,
										  int row,
										  int column, boolean isFaultTolerant) throws OperatorException {
		ValueType nativeValueType;
		try {
			nativeValueType = dataResultSet.getNativeValueType(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e1) {
			addOrThrow(isFaultTolerant, e1.getError(), row);
			return null;
		}
		if (nativeValueType == ValueType.DATE) {
			return getLocalTime(dataResultSet, row, column, isFaultTolerant);
		} else {
			String value = getString(dataResultSet, row, column, isFaultTolerant);
			try {
				return LocalTime.ofNanoOfDay(BeltConverter.legacyTimeDoubleToNanoOfDay(
						config.getDateFormat().parse(value).getTime(), preferredCalendar));
			} catch (ParseException e) {
				ParsingError error =
						new ParsingError(dataResultSet.getCurrentRow() + 1, column, ErrorCode.UNPARSEABLE_DATE,
								value, e);
				addOrThrow(isFaultTolerant, error, row);
				return null;
			}
		}
	}

	private LocalTime getLocalTime(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant)
			throws OperatorException {
		try {
			return LocalTime.ofNanoOfDay(BeltConverter.legacyTimeDoubleToNanoOfDay(dataResultSet.getDate(column).getTime(), preferredCalendar));
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return null;
		}
	}

	private double getStringIndex(Attribute attribute, DataResultSet dataResultSet, int row, int column,
								  boolean isFaultTolerant) throws UserError {
		String value = null;
		try {
			value = dataResultSet.getString(column);
			return attribute.getMapping().mapString(value);
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return Double.NaN;
		} catch (AttributeTypeException e) {
			ParsingError error =
					new ParsingError(dataResultSet.getCurrentRow() + 1, column, ErrorCode.MORE_THAN_TWO_VALUES,
							value, e);
			addOrThrow(isFaultTolerant, error, row);
			return Double.NaN;
		}
	}

	/**
	 * Tries to reproduce the legacy behaviour for binominal in the method {@link #getStringIndex} while returning a
	 * String
	 */
	private String getLegacyBinominal(DataResultSet dataResultSet, int row, int column, Map<Integer,
			List<String>> binominalLegacy, boolean isFaultTolerant) throws UserError {
		String value = getUntrimmedString(dataResultSet, row, column, isFaultTolerant);
		if (value == null) {
			return null;
		}
		List<String> alreadyContained = binominalLegacy.get(column);
		if (alreadyContained == null) {
			alreadyContained = new ArrayList<>(2);
			binominalLegacy.put(column, alreadyContained);
		}
		if (!alreadyContained.contains(value)) {
			if (alreadyContained.size() == 2) {
				//already two binominal value, do the same as BinominalMapping#mapString
				ParsingError error =
						new ParsingError(dataResultSet.getCurrentRow() + 1, column, ErrorCode.MORE_THAN_TWO_VALUES,
								value, new AttributeTypeException(
								"Cannot map another string for binary attribute: already mapped two strings (" +
										alreadyContained.get(0) + ", "
										+ alreadyContained.get(1) + "). The third string that was tried to add: '" +
										value + "'"));
				addOrThrow(isFaultTolerant, error, row);
				return null;
			} else {
				alreadyContained.add(value);
			}
		}
		return value;
	}

	private String getString(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant) throws UserError {
		try {
			String string = dataResultSet.getString(column);
			if (operator == null || operator.getCompatibilityLevel().isAbove(BEFORE_ATTRIBUTE_TRIMMING)) {
				string = string == null ? null : string.trim();
			}
			return string;
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return null;
		}
	}

	/**
	 * For compatibility reasons does the same as getStringIndex for nominal.
	 */
	private String getUntrimmedString(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant) throws UserError {
		try {
			return dataResultSet.getString(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			addOrThrow(isFaultTolerant, e.getError(), row);
			return null;
		}
	}

	/**
	 * If native type is date, returns the date. Otherwise, uses string and parses.
	 */
	private double getOrParseNumber(DataResultSetTranslationConfiguration config, DataResultSet dataResultSet, int row,
									int column, boolean isFaultTolerant) throws OperatorException {
		ValueType nativeValueType;
		try {
			nativeValueType = dataResultSet.getNativeValueType(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e1) {
			addOrThrow(isFaultTolerant, e1.getError(), row);
			return Double.NaN;
		}
		if (nativeValueType == ValueType.NUMBER) {
			return getNumber(dataResultSet, row, column, isFaultTolerant).doubleValue();
		} else {
			String value = getString(dataResultSet, row, column, isFaultTolerant);
			NumberFormat numberFormat = config.getNumberFormat();
			if (numberFormat != null) {
				try {
					Number parsedValue;
					parsedValue = numberFormat.parse(value);
					if (parsedValue == null) {
						return Double.NaN;
					} else {
						return parsedValue.doubleValue();
					}
				} catch (ParseException e) {
					ParsingError error = new ParsingError(dataResultSet.getCurrentRow() + 1, column,
							ErrorCode.UNPARSEABLE_REAL, value, e);
					addOrThrow(isFaultTolerant, error, row);
					return Double.NaN;
				}
			} else {
				try {
					return Double.parseDouble(value);
				} catch (NumberFormatException e) {
					ParsingError error = new ParsingError(dataResultSet.getCurrentRow(), column, ErrorCode.UNPARSEABLE_REAL,
							value, e);
					addOrThrow(isFaultTolerant, error, row);
					return Double.NaN;
				}
			}
		}
	}

	private Number getNumber(DataResultSet dataResultSet, int row, int column, boolean isFaultTolerant)
			throws OperatorException {
		try {
			return dataResultSet.getNumber(column);
		} catch (com.rapidminer.operator.nio.model.ParseException e) {
			if (isFaultTolerant) {
				addError(e.getError(), row);
				return Double.NaN;
			} else {
				throw new UserError(operator, "data_parsing_error", e.toString());
			}
		}
	}

	public void guessValueTypes(DataResultSetTranslationConfiguration configuration, DataResultSet dataResultSet,
			ProgressListener listener) throws OperatorException {
		int maxProbeRows;
		try {
			maxProbeRows = Integer.parseInt(ParameterService
					.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_TEST_ROWS));
		} catch (NumberFormatException e) {
			maxProbeRows = 100;
		}
		guessValueTypes(configuration, dataResultSet, maxProbeRows, listener);
	}

	public void guessValueTypes(DataResultSetTranslationConfiguration configuration, DataResultSet dataResultSet,
			int maxNumberOfRows, ProgressListener listener) throws OperatorException {
		int[] originalValueTypes = new int[configuration.getNumerOfColumns()];
		for (int i = 0; i < originalValueTypes.length; i++) {
			originalValueTypes[i] = configuration.getColumnMetaData(i).getAttributeValueType();
		}
		final int[] guessedTypes = guessValueTypes(originalValueTypes, configuration, dataResultSet, maxNumberOfRows,
				listener);
		for (int i = 0; i < guessedTypes.length; i++) {
			configuration.getColumnMetaData(i).setAttributeValueType(guessedTypes[i]);
		}
	}

	/**
	 * This method will select the most appropriate value types defined on the first few thousand
	 * rows.
	 *
	 * @throws OperatorException
	 */
	private int[] guessValueTypes(int[] definedTypes, DataResultSetTranslationConfiguration configuration,
			DataResultSet dataResultSet, int maxProbeRows, ProgressListener listener) throws OperatorException {
		cancelGuessingRequested = false;

		if (listener != null) {
			listener.setTotal(1 + maxProbeRows);
		}
		DateFormat dateFormat = configuration.getDateFormat();
		NumberFormat numberFormat = configuration.getNumberFormat();

		if (listener != null) {
			listener.setCompleted(1);
		}

		// TODO: The following could be made more efficient using an indirect indexing to access the
		// columns: would
		dataResultSet.reset(listener);
		// the row in the underlying DataResultSet
		int currentRow = 0;
		// the example row in the ExampleTable
		int exampleIndex = 0;
		NominalValueSet[] nominalValues = new NominalValueSet[dataResultSet.getNumberOfColumns()];
		for (int i = 0; i < nominalValues.length; i++) {
			nominalValues[i] = new NominalValueSet();
		}
		int maxAnnotatedRow = configuration.getLastAnnotatedRowIndex();
		while (dataResultSet.hasNext() && (currentRow < maxProbeRows || maxProbeRows <= 0)) {
			if (cancelGuessingRequested) {
				break;
			}
			dataResultSet.next(listener);
			if (listener != null) {
				listener.setCompleted(1 + currentRow);
			}

			// skip rows with annotations
			if (currentRow > maxAnnotatedRow || configuration.getAnnotation(currentRow) == null) {
				int numCols = dataResultSet.getNumberOfColumns();
				// number of columns can change as we read the data set.
				if (numCols > definedTypes.length) {
					String excessString;
					try {
						excessString = dataResultSet.getString(definedTypes.length);
					} catch (com.rapidminer.operator.nio.model.ParseException e) {
						excessString = null;
					}
					addError(new ParsingError(dataResultSet.getCurrentRow() + 1, 0, ErrorCode.ROW_TOO_LONG, excessString,
							null), exampleIndex);
				}
				for (int column = 0; column < definedTypes.length; column++) {
					// No more guessing necessary if guessed type is polynomial (this is the most
					// general case)
					if (definedTypes[column] == Ontology.POLYNOMINAL || dataResultSet.isMissing(column)) {
						continue;
					}

					ValueType nativeType;
					String stringRepresentation;
					try {
						nativeType = dataResultSet.getNativeValueType(column);
						stringRepresentation = dataResultSet.getString(column);
					} catch (com.rapidminer.operator.nio.model.ParseException e) {
						final ParsingError error = e.getError();
						addError(error, exampleIndex);
						continue;
					}
					nominalValues[column].register(stringRepresentation);

					if (nativeType != ValueType.STRING) {
						// Native representation is not a string, so we trust the data source
						// and adapt the type accordingly.
						int isType = nativeType.getRapidMinerAttributeType();
						if (nativeType == ValueType.NUMBER) {
							Number value = getNumber(dataResultSet, exampleIndex, column, true);
							if (!Double.isNaN(value.doubleValue())) {
								if (value.intValue() == value.doubleValue()) {
									isType = Ontology.INTEGER;
								} else {
									isType = Ontology.REAL;
								}
							}
						}
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(isType, definedTypes[column])) {
							// We're good, nothing to do
							if (definedTypes[column] == Ontology.ATTRIBUTE_VALUE) {
								// First row, just use the one delivered
								definedTypes[column] = isType;
							}
							continue;
						} else {
							// otherwise, generalize until we are good
							while (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(isType, definedTypes[column])) {
								definedTypes[column] = Ontology.ATTRIBUTE_VALUE_TYPE.getParent(definedTypes[column]);
							}
							// in the most general case, we switch to polynomial
							if (definedTypes[column] == Ontology.ATTRIBUTE_VALUE) {
								if (operator != null && operator.getCompatibilityLevel().isAtMost(VERSION_6_0_3)) {
									definedTypes[column] = nominalValues[column].moreThanTwo ? Ontology.POLYNOMINAL
											: Ontology.BINOMINAL;
								} else {
									// Don't set to binominal type, it fails too often.
									definedTypes[column] = Ontology.POLYNOMINAL;
								}
							}
						}
					} else {
						// for strings, we try parsing ourselves
						// fill value buffer for binominal assessment
						// trim value
						if (stringRepresentation != null && configuration.trimForGuessing()) {
							stringRepresentation = stringRepresentation.trim();
						}
						definedTypes[column] = guessValueType(definedTypes[column], stringRepresentation,
								!nominalValues[column].moreThanTwo, dateFormat, numberFormat);
					}
				}
				exampleIndex++;
			}
			currentRow++;
		}
		if (listener != null) {
			listener.complete();
		}
		return definedTypes;
	}


	/**
	 * This method tries to guess the value type by taking into account the current guessed type and
	 * the string value. The type will be transformed to more general ones.
	 */
	private int guessValueType(int currentValueType, String value, boolean onlyTwoValues, DateFormat dateFormat,
			NumberFormat numberFormat) {
		if (onlyTwoValues && operator != null && operator.getCompatibilityLevel().isAtMost(VERSION_6_0_3)
				&& currentValueType == Ontology.BINOMINAL) {
			return Ontology.BINOMINAL;
		} else if (currentValueType == Ontology.BINOMINAL || currentValueType == Ontology.POLYNOMINAL) {
			// Don't set to binominal type, it fails too often.
			return Ontology.POLYNOMINAL;
		}

		if (currentValueType == Ontology.DATE) {
			try {
				dateFormat.parse(value);
				return currentValueType;
			} catch (ParseException e) {
				if (operator != null && operator.getCompatibilityLevel().isAtMost(VERSION_6_0_3)) {
					return guessValueType(Ontology.BINOMINAL, value, onlyTwoValues, dateFormat, numberFormat);
				} else {
					return Ontology.POLYNOMINAL;
				}
			}
		}
		if (currentValueType == Ontology.REAL) {
			if (numberFormat != null) {
				try {
					numberFormat.parse(value);
					return Ontology.REAL;
				} catch (ParseException e) {
					return guessValueType(Ontology.DATE, value, onlyTwoValues, dateFormat, numberFormat);
				}
			} else {
				try {
					Double.parseDouble(value);
					return Ontology.REAL;
				} catch (NumberFormatException e) {
					return guessValueType(Ontology.DATE, value, onlyTwoValues, dateFormat, null);
				}
			}
		}
		try {
			Integer.parseInt(value);
			return Ontology.INTEGER;
		} catch (NumberFormatException e) {
			return guessValueType(Ontology.REAL, value, onlyTwoValues, dateFormat, numberFormat);
		}
	}

	/**
	 * This method will stop any ongoing read action and close the underlying DataResultSet. It will
	 * wait until this has been successfully performed.
	 *
	 * @throws OperatorException
	 */
	public void close() throws OperatorException {
		if (isReading) {
			shouldStop = true;
		}
	}

	public void clearErrors() {
		errors.clear();
	}

	private void addOrThrow(boolean isFaultTolerant, ParsingError error, int row) throws UserError {
		if (isFaultTolerant) {
			addError(error, row);
		} else {
			throw new UserError(operator, 403, error.toString());
		}
	}

	private void addError(final ParsingError error, int exampleIndex) {
		error.setExampleIndex(exampleIndex);
		errors.put(new Pair<>(error.getExampleIndex(), error.getColumn()), error);
	}

	public Collection<ParsingError> getErrors() {
		return errors.values();
	}

	public ParsingError getErrorByExampleIndexAndColumn(int row, int column) {
		return errors.get(new Pair<>(row, column));
	}

	/**
	 * Cancels
	 * {@link #guessValueTypes(int[], DataResultSetTranslationConfiguration, DataResultSet, int, ProgressListener)}
	 * after the next row.
	 */
	public void cancelGuessing() {
		cancelGuessingRequested = true;
	}

	/**
	 * Cancels
	 * {@link #read(DataResultSet, DataResultSetTranslationConfiguration, int, ProgressListener)}
	 * after the next row.
	 */
	public void cancelLoading() {
		cancelLoadingRequested = true;
	}

	public boolean isGuessingCancelled() {
		return cancelGuessingRequested;
	}

	public boolean isLoadingCancelled() {
		return cancelLoadingRequested;
	}
}
