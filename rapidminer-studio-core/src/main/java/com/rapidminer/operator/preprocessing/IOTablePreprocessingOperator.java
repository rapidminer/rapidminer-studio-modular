/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.preprocessing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.AbstractIOTableProcessing;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.GenerateTableModelTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.operator.ports.quickfix.AbstractQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.tools.TableSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.belt.BeltConversionTools;
import com.rapidminer.tools.container.Pair;


/**
 * Superclass for all preprocessing operators. Classes which extend this class must implement the method {@link
 * #createPreprocessingModel(IOTable)}. This method can also be returned by this operator and will be combined with
 * other models. Analog to {@link PreprocessingOperator}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public abstract class IOTablePreprocessingOperator extends AbstractIOTableProcessing {

	/**
	 * the name of the preprocessing model output port
	 */
	public static final String PREPROCESSING_MODEL_OUTPUT_PORT_NAME = "preprocessing model";

	/**
	 * The parameter name for &quot;Indicates if the preprocessing model should also be returned&quot;
	 */
	public static final String PARAMETER_RETURN_PREPROCESSING_MODEL = "return_preprocessing_model";

	private final OutputPort modelOutput = getOutputPorts().createPort(PREPROCESSING_MODEL_OUTPUT_PORT_NAME);

	protected final TableSubsetSelector subsetSelector =
			new TableSubsetSelector(this, getTableInputPort(), getAllowedTypes());


	public IOTablePreprocessingOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(
				new GenerateTableModelTransformationRule(getTableInputPort(), modelOutput, getPreprocessingModelClass(),
						GeneralModel.ModelKind.PREPROCESSING));
		getTableInputPort().addPrecondition(new Precondition() {
			@Override
			public void check(MetaData metaData) {
				TableMetaData tmd = BeltConversionTools.asTableMetaDataOrNull(metaData);
				if (tmd != null) {
					TableMetaData metaDataSubset = subsetSelector.getMetaDataSubset(tmd, false);
					if (metaDataSubset.labels().isEmpty()) {
						QuickFix selectAllQuickFix = new AbstractQuickFix(4, false, "attributefilter_select_all") {

							@Override
							public void apply() {
								IOTablePreprocessingOperator.this.getParameters().setParameter(TableSubsetSelector.PARAMETER_FILTER_NAME,
										TableSubsetSelector.ALL_ATTRIBUTES_FILTER);
							}
						};
						SimpleMetaDataError error =
								new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, getTableInputPort(),
										Collections.singletonList(selectAllQuickFix),
										"attribute_selection_empty");
						getTableInputPort().addError(error);
					}
				}
			}

			@Override
			public String getDescription() {
				return "Example set matching at least one selected attribute.";
			}

			@Override
			public boolean isCompatible(MetaData input, CompatibilityLevel level) {
				return null != input && (IOTable.class.isAssignableFrom(input.getObjectClass()) ||
						AtPortConverter.isConvertible(input.getObjectClass(), IOTable.class));
			}

			@Override
			public void assumeSatisfied() {
				//not needed
			}

			@Override
			public MetaData getExpectedMetaData() {
				return new TableMetaData();
			}
		});
	}

	/**
	 * Subclasses might override this method to define the meta data transformation performed by this operator. The
	 * default implementation takes all columns specified by the {@link TableSubsetSelector} and passes them to {@link
	 * #modifyColumnMetaData(TableMetaData, String)} and replaces them accordingly.
	 */
	@Override
	protected TableMetaData modifyMetaData(TableMetaData tableMetaData) throws UndefinedParameterError {
		TableMetaData subsetMetaData = subsetSelector.getMetaDataSubset(tableMetaData, false);
		checkSelectedSubsetMetaData(subsetMetaData);
		TableMetaDataBuilder builder = new TableMetaDataBuilder(tableMetaData);
		for (String name : subsetMetaData.labels()) {
			Collection<Pair<String, ColumnInfo>> replacement = modifyColumnMetaData(tableMetaData, name);
			if (replacement != null) {
				if (replacement.size() == 1) {
					Pair<String, ColumnInfo> single = replacement.iterator().next();
					builder.replace(name, single.getSecond());
					if (!single.getFirst().equals(name)) {
						builder.rename(name, single.getFirst());
					}
				} else {
					builder.remove(name);
					for (Pair<String, ColumnInfo> next : replacement) {
						builder.add(next.getFirst(), next.getSecond());
					}
				}
			}
		}
		return builder.build();
	}

	/**
	 * Can be overridden to check the selected attributes for compatibility. Does nothing by default.
	 *
	 * @param subsetMetaData
	 * 		the selected subset to check
	 */
	protected void checkSelectedSubsetMetaData(TableMetaData subsetMetaData) {
	}

	/**
	 * If this preprocessing operator generates new columns, the corresponding meta data should be returned by this
	 * method. The column will be replaced by the collection. If this operator modifies a single one, a single one
	 * should be returned. Note: If an empty collection is returned, the column will be removed, but no new column will
	 * be added. If {@code null} is returned, nothing is changed.
	 **/
	protected abstract Collection<Pair<String, ColumnInfo>> modifyColumnMetaData(TableMetaData tmd, String name)
			throws UndefinedParameterError;

	/**
	 * Creates a preprocessing model based on the given ioTable.
	 *
	 * @param ioTable
	 * 		the table for which to create a preprocessing model
	 * @return the created model
	 * @throws OperatorException
	 * 		if the creation fails
	 */
	public abstract IOTablePreprocessingModel createPreprocessingModel(IOTable ioTable) throws OperatorException;

	/**
	 * This method allows subclasses to easily get a collection of the affected columns. Used by
	 * {@link #apply(IOTable)}
	 * and uses the subset selector by default.
	 *
	 * @throws UndefinedParameterError
	 * 		if the parameters for the selector are not defined
	 * @throws UserError
	 * 		if the subset fails
	 */
	protected final Table getSelectedAttributes(Table table) throws UndefinedParameterError, UserError {
		return subsetSelector.getSubset(table, false);
	}

	@Override
	public final IOTable apply(IOTable ioTable) throws OperatorException {
		IOTable workingSet = new IOTable(getSelectedAttributes(ioTable.getTable()));

		IOTableModel model = createPreprocessingModel(workingSet);
		if (getTableOutputPort().isConnected()) {
			ioTable = model.apply(ioTable, this);
		}

		modelOutput.deliver(model);
		return ioTable;
	}

	/**
	 * Helper wrapper that can be called by other operators to apply this operator when it is created anonymously.
	 *
	 * @param ioTable
	 * 		the input table
	 * @return the output table after the model application
	 */
	public IOTable doWork(IOTable ioTable) throws OperatorException {
		IOTable workingSet = new IOTable(getSelectedAttributes(ioTable.getTable()));

		IOTableModel model = createPreprocessingModel(workingSet);

		return model.apply(ioTable, this);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort outputPort) {
		if (outputPort == modelOutput) {
			return getParameterAsBoolean(PARAMETER_RETURN_PREPROCESSING_MODEL);
		} else {
			return super.shouldAutoConnect(outputPort);
		}
	}

	/**
	 * Defines the types of the columns which are processed or affected by this operator. Has to be overridden to
	 * restrict the attributes which can be chosen by a {@link TableSubsetSelector}.
	 *
	 * @return array of allowed types
	 * @see TableSubsetSelector
	 */
	protected abstract String[] getAllowedTypes();

	/**
	 * Returns the preprocessing model class created by this operator.
	 *
	 * @return the preprocessing model class
	 */
	public abstract Class<? extends IOTablePreprocessingModel> getPreprocessingModelClass();

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_RETURN_PREPROCESSING_MODEL,
				"Indicates if the preprocessing model should also be returned", false);
		type.setHidden(true);
		types.add(type);

		types.addAll(subsetSelector.getParameterTypes());
		return types;
	}

	/**
	 * @return the model output port
	 */
	protected OutputPort getPreprocessingModelOutputPort() {
		return modelOutput;
	}
}
