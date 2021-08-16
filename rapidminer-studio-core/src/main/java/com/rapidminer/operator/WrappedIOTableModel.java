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

import static com.rapidminer.operator.WrappedIOTablePredictionModel.getHeaderExampleSet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import javax.swing.Action;
import javax.swing.Icon;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.HeaderExampleSet;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.ProcessingStep;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * Wraps an {@link IOTableModel} into a {@link Model}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class WrappedIOTableModel extends AbstractModel implements WrappedGeneralModel {

	private final IOTableModel tableModel;

	public WrappedIOTableModel(IOTableModel tableModel) {
		super(getHeaderExampleSet(ValidationUtilV2.requireNonNull(tableModel, "table model").getTrainingHeader()));
		this.tableModel = tableModel;
	}

	/**
	 * Returns the wrapped {@link IOTableModel}.
	 *
	 * @return the wrapped table model
	 */
	@Override
	public IOTableModel getDefiningModel(){
		return tableModel;
	}

	@Override
	public HeaderExampleSet getTrainingHeader() {
		return getHeaderExampleSet(tableModel.getTrainingHeader());
	}

	@Override
	public ExampleSet apply(ExampleSet testSet) throws OperatorException {
		return apply(testSet, null);
	}

	@Override
	public ExampleSet apply(ExampleSet testObject, Operator operator) throws OperatorException {
		IOTable table =
				BeltConversionTools.asIOTableOrNull(testObject, operator == null ? null :
						Resources.getConcurrencyContext(operator));
		IOTable result = tableModel.apply(table, operator);
		return BeltConversionTools.asExampleSetOrNull(result);
	}

	@Override
	public void setParameter(String key, Object value) throws OperatorException {
		tableModel.setParameter(key, value);
	}

	@Override
	public boolean isUpdatable() {
		return tableModel.isUpdatable();
	}

	@Override
	public void updateModel(ExampleSet updateExampleSet) throws OperatorException {
		updateModel(updateExampleSet, null);
	}

	@Override
	public void updateModel(ExampleSet updateObject, Operator operator) throws OperatorException {
		IOTable table =
				BeltConversionTools.asIOTableOrNull(updateObject, operator == null ? null :
						Resources.getConcurrencyContext(operator));
		tableModel.updateModel(table, operator);
	}

	@Override
	public void setSource(String sourceName) {
		tableModel.setSource(sourceName);
	}

	@Override
	public String getSource() {
		return tableModel.getSource();
	}

	@Override
	public void appendOperatorToHistory(Operator operator, OutputPort port) {
		tableModel.appendOperatorToHistory(operator, port);
	}

	@Override
	public List<ProcessingStep> getProcessingHistory() {
		return tableModel.getProcessingHistory();
	}

	@Override
	public IOObject copy() {
		return new WrappedIOTableModel((IOTableModel) tableModel.copy());
	}

	@Override
	public LoggingHandler getLog() {
		return tableModel.getLog();
	}

	@Override
	public void setLoggingHandler(LoggingHandler loggingHandler) {
		tableModel.setLoggingHandler(loggingHandler);
	}

	@Override
	public Annotations getAnnotations() {
		return tableModel.getAnnotations();
	}

	@Override
	public Object getUserData(String key) {
		return tableModel.getUserData(key);
	}

	@Override
	public Object setUserData(String key, Object value) {
		return tableModel.setUserData(key, value);
	}

	@Override
	public String getName() {
		return tableModel.getName();
	}

	@Override
	public String toResultString() {
		return tableModel.toResultString();
	}

	@Override
	public Icon getResultIcon() {
		return tableModel.getResultIcon();
	}

	@Override
	public List<Action> getActions() {
		return Collections.emptyList();
	}

	@Override
	public boolean isInTargetEncoding() {
		return false;
	}

	@Override
	public boolean isModelKind(ModelKind modelKind) {
		return tableModel.isModelKind(modelKind);
	}

	@Override
	public String toString() {
		return tableModel.toString();
	}
}
