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
package com.rapidminer.operator.preprocessing.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.Appender;
import com.rapidminer.belt.table.Table;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.generator.ExampleSetGenerator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BelowOrEqualOperatorVersionCondition;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.belt.BeltTools;
import com.rapidminer.tools.math.container.Range;
import com.rapidminer.tools.parameter.internal.DataManagementParameterHelper;


/**
 * <p>
 * This operator merges two or more given example sets by adding all examples in one example table
 * containing all data rows. Please note that the new example table is built in memory and this
 * operator might therefore not be applicable for merging huge data set tables from a database. In
 * that case other preprocessing tools should be used which aggregates, joins, and merges tables
 * into one table which is then used by RapidMiner.
 * </p>
 *
 * <p>
 * All input example sets must provide the same attribute signature. That means that all examples
 * sets must have the same number of (special) attributes and attribute names. If this is true this
 * operator simply merges all example sets by adding all examples of all table into a new set which
 * is then returned.
 * </p>
 *
 * @author Ingo Mierswa, Gisa Meier
 */
public class ExampleSetMerge extends Operator {

	/**
	 * Versions > this version make full use of the belt append. This changes the behavior in some edge cases.
	 */
	private static final OperatorVersion VERSION_BELT = new OperatorVersion(9, 9, 2);

	private final InputPortExtender inputExtender = new InputPortExtender("example set", getInputPorts()) {

		@Override
		protected Precondition makePrecondition(InputPort port) {
			return new ExampleSetPrecondition(port) {

				{
					setOptional(true);
				}

				@Override
				public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
					for (ExampleSetMetaData metaData : inputExtender.getMetaDataAsOrNull(ExampleSetMetaData.class, true)) {
						if (metaData != null) {
							MetaDataInfo result = emd.equalHeader(metaData);
							if (result != MetaDataInfo.YES) {
								addError(new SimpleProcessSetupError(result == MetaDataInfo.NO ? Severity.ERROR : Severity.WARNING, getPortOwner(),
										"exampleset.sets_incompatible"));
								break;
							}
						}
					}
				}
			};
		}
	};
	private final OutputPort mergedOutput = getOutputPorts().createPort("merged set");

	/** The parameter name for &quot;Determines, how the data is represented internally.&quot; */
	public static final String PARAMETER_DATAMANAGEMENT = ExampleSetGenerator.PARAMETER_DATAMANAGEMENT;

	public ExampleSetMerge(OperatorDescription description) {
		super(description);

		inputExtender.start();

		getTransformer().addRule(inputExtender.makeFlatteningPassThroughRule(mergedOutput));
		getTransformer().addRule(() -> {
			if (getCompatibilityLevel().isAtMost(VERSION_BELT)) {
				transformExampleSetMetaData();
			} else {
				transformTableMetaData();
			}
		});
	}

	/**
	 * Transforms the meta data as {@link ExampleSet}s.
	 */
	private void transformExampleSetMetaData() {
		List<MetaData> metaDatas = inputExtender.getMetaData(true);
		List<ExampleSetMetaData> emds = new ArrayList<>(metaDatas.size());
		for (MetaData metaData : metaDatas) {
			if (metaData instanceof ExampleSetMetaData) {
				emds.add((ExampleSetMetaData) metaData);
			}
		}

		// now unify all single attributes meta data
		if (!emds.isEmpty()) {
			ExampleSetMetaData resultEMD = emds.get(0).clone();
			for (int i = 1; i < emds.size(); i++) {
				ExampleSetMetaData mergerEMD = emds.get(i);
				resultEMD.getNumberOfExamples().add(mergerEMD.getNumberOfExamples());

				// now iterating over all single attributes in order to merge their meta
				// data
				for (AttributeMetaData amd : resultEMD.getAllAttributes()) {
					adjustAttributeMetaData(mergerEMD, amd);
				}
			}
			mergedOutput.deliverMD(resultEMD);
		}
	}

	/**
	 * Transforms a single attribute meta data.
	 */
	private void adjustAttributeMetaData(ExampleSetMetaData mergerEMD, AttributeMetaData amd) {
		String name = amd.getName();
		AttributeMetaData mergingAMD = mergerEMD.getAttributeByName(name);
		if (mergingAMD != null) {
			// values
			if (amd.isNominal()) {
				amd.getValueSet().addAll(mergingAMD.getValueSet());
			} else {
				amd.getValueRange().union(mergingAMD.getValueRange());
			}
			amd.getValueSetRelation().merge(mergingAMD.getValueSetRelation());
			// missing values
			amd.getNumberOfMissingValues().add(mergingAMD.getNumberOfMissingValues());
		}
	}

	/**
	 * Transforms the meta data as {@link TableMetaData}.
	 */
	private void transformTableMetaData() {
		List<TableMetaData> metaDatas = inputExtender.getMetaDataAsOrNull(TableMetaData.class, true);
		List<TableMetaData> tmds = metaDatas.stream().filter(Objects::nonNull).collect(Collectors.toList());

		if (!tmds.isEmpty()) {
			TableMetaData firstTMD = tmds.get(0);
			TableMetaDataBuilder builder = new TableMetaDataBuilder(firstTMD);
			MDInteger height = builder.height();
			for (int i = 1; i < tmds.size(); i++) {
				TableMetaData mergerTMD = tmds.get(i);
				height.add(mergerTMD.height());
			}
			builder.updateHeight(height);

			// now iterating over all single columns in order to merge their meta
			// data
			for (String name : firstTMD.labels()) {
				ColumnInfo firstColumn = builder.column(name);
				ColumnInfoBuilder columnInfoBuilder = new ColumnInfoBuilder(firstColumn);
				for (int i = 1; i < tmds.size(); i++) {
					TableMetaData mergerTMD = tmds.get(i);
					adjustColumnInfo(name, firstColumn, columnInfoBuilder, mergerTMD);
				}
				builder.replace(name, columnInfoBuilder.build());
			}

			mergedOutput.deliverMD(builder.build());
		}
	}

	/**
	 * Transforms a single column info.
	 */
	private void adjustColumnInfo(String name, ColumnInfo firstColumn, ColumnInfoBuilder columnInfoBuilder,
						   TableMetaData mergerTMD) {
		if (mergerTMD.contains(name) == MetaDataInfo.YES) {
			ColumnInfo column = mergerTMD.column(name);
			columnInfoBuilder.mergeValueSetRelation(column.getValueSetRelation());
			columnInfoBuilder.addMissings(column.getMissingValues());
			if (firstColumn.isNominal() == MetaDataInfo.YES) {
				columnInfoBuilder.addDictionaryValues(column.getDictionary().getValueSet());
			} else if (columnInfoBuilder.getNumericRange().isPresent() && column.getNumericRange().isPresent()) {
				Range range = columnInfoBuilder.getNumericRange().get();
				range.union(column.getNumericRange().get());
				columnInfoBuilder.setNumericRange(range, columnInfoBuilder.getValueSetRelation());
			}
		}
	}

	@Override
	public void doWork() throws OperatorException {
		if (getCompatibilityLevel().isAtMost(VERSION_BELT)) {
			List<ExampleSet> allExampleSets = inputExtender.getData(ExampleSet.class, true);
			mergedOutput.deliver(merge(allExampleSets));
		} else {
			List<IOTable> data = inputExtender.getData(IOTable.class, true);
			mergedOutput.deliver(append(data));
		}
	}

	/**
	 * Appends the given list of {@link IOTable}s into one, if possible.
	 *
	 * @param ioTables
	 * 		the tables to append
	 * @return the resulting table
	 * @throws UserError
	 * 		if appending is not possible
	 * @since 9.10
	 */
	public IOTable append(List<IOTable> ioTables) throws UserError {
		// throw error if no tables are available
		if (ioTables.isEmpty()) {
			throw new MissingIOObjectException(IOTable.class);
		}
		List<Table> tables = new ArrayList<>(ioTables.size());
		for (IOTable ioTable : ioTables) {
			tables.add(ioTable.getTable());
		}
		getProgress().setCheckForStop(false);
		getProgress().setTotal(100);
		try {
			Table appended = Appender.append(tables, this::silentProgress, BeltTools.getContext(this));
			IOTable result = new IOTable(appended);
			result.getAnnotations().addAll(ioTables.get(0).getAnnotations());
			return result;
		} catch (Appender.IncompatibleTableWidthException e) {
			throw new UserError(this, "append_compatibility.incompatible_width", e.getTableIndex() + 1);
		} catch (Appender.IncompatibleColumnsException e) {
			throw new UserError(this, "append_compatibility.incompatible_column",
					e.getTableIndex() + 1, e.getColumnName());
		} catch (Appender.IncompatibleTypesException e) {
			throw new UserError(this, "append_compatibility.incompatible_type", e.getColumnName(),
					e.getDesiredType().replace("Column type ", ""),
					e.getActualType().replace("Column type ", ""));
		} catch (Appender.TableTooLongException e) {
			throw new UserError(this, "append.result_too_big");
		}
	}

	private void silentProgress(double progress) {
		try {
			getProgress().setCompleted((int) (progress * 100));
		} catch (ProcessStoppedException e) {
			// cannot happen, check for stop is disabled
		}
	}

	/**
	 * Appends the {@link ExampleSet}s into one, if possible.
	 *
	 * @param allExampleSets
	 * 		the example sets to append
	 * @return the resulting example set
	 * @throws OperatorException
	 * 		if appending fails
	 * @deprecated since 9.10, use {@link #append(List)} instead
	 */
	@Deprecated
	public ExampleSet merge(List<ExampleSet> allExampleSets) throws OperatorException {
		// throw error if no example sets were available
		if (allExampleSets.size() == 0) {
			throw new MissingIOObjectException(ExampleSet.class);
		}

		// checks if all example sets have the same signature
		checkForCompatibility(allExampleSets);

		// create new example table
		ExampleSet firstSet = allExampleSets.get(0);
		List<Attribute> newAttributeList = new ArrayList<Attribute>();
		HashMap<String, Attribute> newAttributeNameMap = new HashMap<String, Attribute>();
		Map<Attribute, String> specialAttributesMap = new LinkedHashMap<Attribute, String>();
		Iterator<AttributeRole> a = firstSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute oldAttribute = role.getAttribute();

			int newType;
			if (oldAttribute.isNominal()) {
				// collect values to see if we have at least two
				Set<String> values = new HashSet<String>();
				values.addAll(oldAttribute.getMapping().getValues());
				boolean hasNominal = false;
				boolean hasPolynominal = false;
				boolean hasSameValueType = true;
				for (ExampleSet otherExampleSet : allExampleSets) {
					Attribute otherAttribute = otherExampleSet.getAttributes().get(oldAttribute.getName());
					// At least one non-nominal -> throw
					if (!otherAttribute.isNominal()) {
						throwIncompatible(oldAttribute, otherAttribute);
					}
					values.addAll(otherAttribute.getMapping().getValues());
					hasSameValueType &= (otherAttribute.getValueType() == oldAttribute.getValueType());
					hasNominal |= (otherAttribute.getValueType() == Ontology.NOMINAL);
					hasPolynominal |= Ontology.ATTRIBUTE_VALUE_TYPE.isA(otherAttribute.getValueType(), Ontology.POLYNOMINAL);
				}
				// binominals with more than 2 values cannot keep their value type, else try to
				// preserve value type is all have the same
				if (hasSameValueType
						&&
						(!Ontology.ATTRIBUTE_VALUE_TYPE.isA(oldAttribute.getValueType(), Ontology.BINOMINAL) || values
								.size() <= 2)) {
					newType = oldAttribute.getValueType();
				} else if (hasNominal) {
					newType = Ontology.NOMINAL;
				} else if (hasPolynominal || values.size() > 2) {
					newType = Ontology.POLYNOMINAL;
				} else {
					newType = oldAttribute.getValueType();
				}
			} else if (oldAttribute.isNumerical()) {
				boolean hasReal = false;
				boolean hasNumerical = false;
				boolean hasSameValueType = true;
				for (ExampleSet otherExampleSet : allExampleSets) {
					Attribute otherAttribute = otherExampleSet.getAttributes().get(oldAttribute.getName());
					// At least one non-numerical -> throw
					if (!otherAttribute.isNumerical()) {
						throwIncompatible(oldAttribute, otherAttribute);
					}
					hasSameValueType &= (otherAttribute.getValueType() == oldAttribute.getValueType());
					hasNumerical |= (otherAttribute.getValueType() == Ontology.NUMERICAL);
					hasReal |= Ontology.ATTRIBUTE_VALUE_TYPE.isA(otherAttribute.getValueType(), Ontology.REAL);
				}
				if (hasSameValueType) {
					newType = oldAttribute.getValueType();
				} else if (hasNumerical) {
					newType = Ontology.NUMERICAL;
				} else if (hasReal) {
					newType = Ontology.REAL;
				} else {
					newType = oldAttribute.getValueType();
				}
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(oldAttribute.getValueType(), Ontology.DATE)
					|| (Ontology.ATTRIBUTE_VALUE_TYPE.isA(oldAttribute.getValueType(), Ontology.TIME) ||
					(Ontology.ATTRIBUTE_VALUE_TYPE
							.isA(oldAttribute.getValueType(), Ontology.DATE_TIME)))) {
				// this case covers the date, time, date_time valueType
				// if all attribute valueTypes are the same keep it, otherwise switch to date_time
				// as the parent valueType
				newType = oldAttribute.getValueType();
				for (ExampleSet otherExampleSet : allExampleSets) {
					Attribute otherAttribute = otherExampleSet.getAttributes().get(oldAttribute.getName());
					// not the same type but all
					if (otherAttribute.getValueType() != newType) {
						if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(oldAttribute.getValueType(), Ontology.DATE) ||
								(Ontology.ATTRIBUTE_VALUE_TYPE
										.isA(oldAttribute.getValueType(), Ontology.TIME) ||
										(Ontology.ATTRIBUTE_VALUE_TYPE.isA(
												oldAttribute.getValueType(), Ontology.DATE_TIME)))) {
							newType = Ontology.DATE_TIME;
						} else {
							// totally different valueType, cannot merge -> throw
							throwIncompatible(oldAttribute, otherAttribute);
						}
					}
				}
			} else {
				for (ExampleSet otherExampleSet : allExampleSets) {
					Attribute otherAttribute = otherExampleSet.getAttributes().get(oldAttribute.getName());
					// At least one non-numerical -> throw
					if (otherAttribute.getValueType() != oldAttribute.getValueType()) {
						throwIncompatible(oldAttribute, otherAttribute);
					}
				}
				newType = oldAttribute.getValueType();
			}

			Attribute newAttribute = AttributeFactory.createAttribute(oldAttribute.getName(), newType, // oldAttribute.getValueType(),
					oldAttribute.getBlockType());
			newAttributeNameMap.put(newAttribute.getName(), newAttribute);
			newAttributeList.add(newAttribute);
			if (role.isSpecial()) {
				specialAttributesMap.put(newAttribute, role.getSpecialName());
			}
		}
		int totalSize = 0;
		for (ExampleSet set : allExampleSets) {
			totalSize += set.size();
		}

		ExampleSetBuilder builder = ExampleSets.from(newAttributeList);

		if (Boolean.parseBoolean(
				ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SYSTEM_LEGACY_DATA_MGMT))) {
			// to preserve the (legacy) data management, we must use the data row factory
			builder.withExpectedSize(totalSize);
			int datamanagement = getParameterAsInt(PARAMETER_DATAMANAGEMENT);
			int numberOfAttributes = newAttributeList.size();
			DataRowFactory factory = new DataRowFactory(datamanagement, '.');
			for (ExampleSet exampleSet : allExampleSets) {
				for (Example example : exampleSet) {
					DataRow dataRow = factory.create(numberOfAttributes);
					Iterator<Attribute> iterator = exampleSet.getAttributes().allAttributes();
					while (iterator.hasNext()) {
						Attribute oldAttribute = iterator.next();
						Attribute newAttribute = newAttributeNameMap.get(oldAttribute.getName());
						double oldValue = example.getValue(oldAttribute);
						if (Double.isNaN(oldValue)) {
							dataRow.set(newAttribute, oldValue);
						} else {
							if (oldAttribute.isNominal()) {
								dataRow.set(newAttribute, newAttribute.getMapping()
										.mapString(oldAttribute.getMapping().mapIndex((int) oldValue)));
							} else {
								dataRow.set(newAttribute, oldValue);
							}
						}
					}
					// adding new row to builder
					builder.addDataRow(dataRow);
				}
				checkForStop();
			}
		} else {
			builder.withBlankSize(totalSize);
			builder.withOptimizationHint(DataManagementParameterHelper.getSelectedDataManagement(this));

			int[] sizes = new int[allExampleSets.size()];
			int i = 0;
			for (ExampleSet set : allExampleSets) {
				sizes[i] = set.size();
				i++;
			}
			int[] sizesSums = new int[sizes.length];
			sizesSums[0] = sizes[0];
			for (int j = 1; j < sizes.length; j++) {
				sizesSums[j] = sizesSums[j - 1] + sizes[j];
			}

			for (Attribute newAttribute : newAttributeList) {
				builder.withColumnFiller(newAttribute, new IntToDoubleFunction() {

					private final String attributeName = newAttribute.getName();

					private ExampleSet oldSet = null;
					private Attribute oldAttribute = null;

					private int start = 0;
					private int end = 0;
					private int oldExampleSetIndex = -1;

					@Override
					public synchronized double applyAsDouble(int i) {
						if (i < start || i >= end) {
							try {
								ExampleSetMerge.this.checkForStop();
							} catch (ProcessStoppedException e) {
								throw new ProcessStoppedRuntimeException();
							}
							int startIndex = 0;
							end = 0;
							if (oldExampleSetIndex > -1 && i >= sizesSums[oldExampleSetIndex]) {
								startIndex = oldExampleSetIndex + 1;
								end = sizesSums[oldExampleSetIndex];
							}
							for (int j = startIndex; j < sizesSums.length; j++) {
								start = end;
								end = sizesSums[j];
								if (end > i) {
									oldExampleSetIndex = j;
									oldSet = allExampleSets.get(j);
									oldAttribute = oldSet.getAttributes().get(attributeName);
									break;
								}
							}

						}
						double oldValue = oldSet.getExample(i - start).getValue(oldAttribute);
						if (Double.isNaN(oldValue)) {
							return Double.NaN;
						} else {
							if (oldAttribute.isNominal()) {
								return newAttribute.getMapping()
										.mapString(oldAttribute.getMapping().mapIndex((int) oldValue));
							} else {
								return oldValue;
							}
						}
					}
				});
			}
		}

		// create result example set
		ExampleSet resultSet = builder.withRoles(specialAttributesMap).build();
		resultSet.getAnnotations().addAll(firstSet.getAnnotations());
		resultSet.setAllUserData(firstSet.getAllUserData());
		return resultSet;
	}

	private void throwIncompatible(Attribute oldAttribute, Attribute otherAttribute) throws UserError {
		throw new UserError(this, 925,
				"Attribute '" + oldAttribute.getName() + "' has incompatible types ("
						+ Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(oldAttribute.getValueType()) + " and "
						+ Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(otherAttribute.getValueType()) +
						") in two input sets.");
	}

	/**
	 * Checks whether all attributes in set 1 occur in the others as well. Types are (deliberately)
	 * not checked. Type check happens in {@link #merge(List)} itself.
	 *
	 * @throws UserError
	 *             on failed check
	 */
	private void checkForCompatibility(List<ExampleSet> allExampleSets) throws OperatorException {
		ExampleSet first = allExampleSets.get(0);
		Iterator<ExampleSet> i = allExampleSets.iterator();
		while (i.hasNext()) {
			checkForCompatibility(first, i.next());
		}
	}

	private void checkForCompatibility(ExampleSet first, ExampleSet second) throws OperatorException {
		if (first.getAttributes().allSize() != second.getAttributes().allSize()) {
			throw new UserError(this, 925, "numbers of attributes are different");
		}

		Iterator<Attribute> firstIterator = first.getAttributes().allAttributes();
		while (firstIterator.hasNext()) {
			Attribute firstAttribute = firstIterator.next();
			Attribute secondAttribute = second.getAttributes().get(firstAttribute.getName());
			if (secondAttribute == null) {
				throw new UserError(this, 925, "Attribute with name '" + firstAttribute.getName()
						+ "' is not part of second example set.");
				// No type check necessary. Type check is done in mrege() itself.
				// //if (firstAttribute.getValueType() != secondAttribute.getValueType()) { //
				// ATTENTION: Breaks compatibility for previously running
				// processes
				// // maybe even better: check for subtypes in both directions and use super-type
				// above
				// if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(secondAttribute.getValueType(),
				// firstAttribute.getValueType())) {
				// throw new UserError(this, 925, "Attribute '" + firstAttribute.getName() +
				// "' has incompatible types (" +
				// Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(firstAttribute.getValueType()) + " and " +
				// Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(secondAttribute.getValueType()) +
				// ") in two input sets.");
				// }
			}
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		List<ParameterType> dependentTypes = new ArrayList<>();
		DataManagementParameterHelper.addParameterTypes(dependentTypes, this);
		for (ParameterType dependentType : dependentTypes) {
			dependentType.registerDependencyCondition(new BelowOrEqualOperatorVersionCondition(this, VERSION_BELT));
		}
		types.addAll(dependentTypes);

		// deprecated parameter
		ParameterType type = new ParameterTypeCategory("merge_type",
				"Indicates if all input example sets or only the first two example sets should be merged.",
				new String[]{"all", "first_two"}, 0);
		type.setDeprecated();
		types.add(type);
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPorts().getPortByIndex(0),
				ExampleSetMerge.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.add(super.getIncompatibleVersionChanges(),
				VERSION_BELT);
	}
}
