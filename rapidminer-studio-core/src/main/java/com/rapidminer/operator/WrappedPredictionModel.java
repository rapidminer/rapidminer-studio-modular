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
package com.rapidminer.operator;

import static com.rapidminer.operator.WrappedModel.getHeaderTable;

import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.Icon;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.ProcessingStep;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * Wraps a {@link PredictionModel} into an {@link IOTablePredictionModel}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class WrappedPredictionModel extends IOTablePredictionModel implements WrappedGeneralModel {

	private final PredictionModel model;

	public WrappedPredictionModel(PredictionModel model) {
		super(getHeaderTable(model.getTrainingHeader()), null);
		this.model = ValidationUtilV2.requireNonNull(model, "table model");
	}

	/**
	 * Returns the wrapped {@link PredictionModel}.
	 *
	 * @return the wrapped table model
	 */
	@Override
	public PredictionModel getDefiningModel() {
		return model;
	}

	@Override
	public void setSource(String sourceName) {
		model.setSource(sourceName);
	}

	@Override
	public String getSource() {
		return model.getSource();
	}

	@Override
	public void appendOperatorToHistory(Operator operator, OutputPort port) {
		model.appendOperatorToHistory(operator, port);
	}

	@Override
	public List<ProcessingStep> getProcessingHistory() {
		return model.getProcessingHistory();
	}

	@Override
	public IOObject copy() {
		return new WrappedModel((Model) model.copy());
	}

	@Override
	public LoggingHandler getLog() {
		return model.getLog();
	}

	@Override
	public void setLoggingHandler(LoggingHandler loggingHandler) {
		model.setLoggingHandler(loggingHandler);
	}

	@Override
	public Annotations getAnnotations() {
		return model.getAnnotations();
	}

	@Override
	public Object getUserData(String key) {
		return model.getUserData(key);
	}

	@Override
	public Object setUserData(String key, Object value) {
		return model.setUserData(key, value);
	}

	@Override
	public String getName() {
		return model.getName();
	}

	@Override
	public String toResultString() {
		return model.toResultString();
	}

	@Override
	public Icon getResultIcon() {
		return model.getResultIcon();
	}

	@Override
	public List<Action> getActions() {
		return model.getActions();
	}

	@Override
	public IOTable getTrainingHeader() {
		return new IOTable(BeltConverter.convert(model.getTrainingHeader(), new SequentialConcurrencyContext())
				.getTable().stripData());
	}

	@Override
	protected Column performPrediction(Table adapted, Map<String, Column> confidences, Operator operator) throws OperatorException {
		throw new UnsupportedOperationException("direct call not supported");
	}

	@Override
	public IOTable apply(IOTable testObject, Operator operator) throws OperatorException {
		ExampleSet exampleSet = BeltConversionTools.asExampleSetOrNull(testObject);
		ExampleSet apply = model.apply(exampleSet, operator);
		return BeltConversionTools.asIOTableOrNull(apply, Resources.getConcurrencyContext(operator));
	}

	@Override
	public void setParameter(String key, Object value) throws OperatorException {
		model.setParameter(key, value);
	}

	@Override
	public boolean isUpdatable() {
		return model.isUpdatable();
	}

	@Override
	public void updateModel(IOTable updateObject, Operator operator) throws OperatorException {
		ExampleSet exampleSet = BeltConversionTools.asExampleSetOrNull(updateObject);
		model.updateModel(exampleSet, operator);
	}

	@Override
	public boolean isModelKind(ModelKind modelKind) {
		return model.isModelKind(modelKind);
	}

	@Override
	public String toString() {
		return model.toString();
	}
}
