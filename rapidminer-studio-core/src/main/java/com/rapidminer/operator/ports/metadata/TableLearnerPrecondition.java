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
package com.rapidminer.operator.ports.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.TableCapabilityProvider;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.operator.learner.meta.AbstractMetaLearner;
import com.rapidminer.operator.learner.meta.Binary2MultiClassLearner;
import com.rapidminer.operator.learner.meta.ClassificationByRegression;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.quickfix.OperatorInsertionQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.tools.OperatorService;


/**
 * Precondition for the table input port of {@link com.rapidminer.operator.learner.AbstractIOTableLearner}s.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public class TableLearnerPrecondition extends TableCapabilityPrecondition {

	public TableLearnerPrecondition(TableCapabilityProvider capabilityProvider, InputPort inputPort) {
		super(capabilityProvider, inputPort);
	}

	/**
	 * This method has to return a collection of quick fixes which are appropriate when regression
	 * is supported and the data needs classification.
	 */
	@Override
	protected Collection<QuickFix> getFixesForClassificationWhenRegressionSupported() {
		Operator learner = getInputPort().getPorts().getOwner().getOperator();
		final Class<ClassificationByRegression> quickFixOpClass = ClassificationByRegression.class;
		OperatorDescription[] ods = OperatorService.getOperatorDescriptions(quickFixOpClass);
		String name = null;
		if (ods.length > 0) {
			name = ods[0].getName();
		}

		QuickFix fix = new OperatorInsertionQuickFix("insert_classification_by_regression_learner",
				new Object[]{name, learner.getOperatorDescription().getName()}, 3, getInputPort()) {

			@Override
			public void apply() {
				applyQuickFix(quickFixOpClass);
			}

			@Override
			public Operator createOperator() throws OperatorCreationException {
				// not needed
				return null;
			}
		};

		return Collections.singletonList(fix);
	}

	/**
	 * This has to return a list of appropriate quick fixes in the case, that only binominal labels
	 * are supported but the data contains polynomials.
	 */
	@Override
	protected Collection<QuickFix> getFixesForPolynomialClassificationWhen2ClassSupported() {
		Operator learner = getInputPort().getPorts().getOwner().getOperator();
		final Class<Binary2MultiClassLearner> quickFixOpClass = Binary2MultiClassLearner.class;
		OperatorDescription[] ods = OperatorService.getOperatorDescriptions(quickFixOpClass);
		String name = null;
		if (ods.length > 0) {
			name = ods[0].getName();
		}
		QuickFix fix = new OperatorInsertionQuickFix("insert_binominal_to_multiclass_learner",
				new Object[]{name, learner.getOperatorDescription().getName()}, 8, getInputPort()) {

			@Override
			public void apply() {
				applyQuickFix(quickFixOpClass);
			}

			@Override
			public Operator createOperator() throws OperatorCreationException {
				// not needed
				return null;
			}
		};
		return Collections.singletonList(fix);
	}

	/**
	 * Applies the {@link OperatorInsertionQuickFix} for the given {@link AbstractMetaLearner} class.
	 *
	 * @param quickFixOpClass
	 * 		the class of the new operator to be inserted
	 */
	private void applyQuickFix(Class<? extends AbstractMetaLearner> quickFixOpClass) {
		List<Port<?, ?>> toUnlock = new LinkedList<>();
		try {
			Operator learner = getInputPort().getPorts().getOwner().getOperator();
			ExecutionUnit learnerUnit = learner.getExecutionUnit();

			// searching for model outport
			OutputPort modelOutput = null;
			MetaData modelMetaData =
					new TablePredictionModelMetaData(IOTablePredictionModel.class, new TableMetaData());
			for (OutputPort port : learner.getOutputPorts().getAllPorts()) {
				MetaData data = port.getRawMetaData();
				if (modelMetaData.isCompatible(data, CompatibilityLevel.VERSION_5)) {
					modelOutput = port;
					toUnlock.add(modelOutput);
					modelOutput.lock();
					break;
				}
			}

			AbstractMetaLearner metaLearner = OperatorService.createOperator(quickFixOpClass);
			learnerUnit.addOperator(metaLearner, learnerUnit.getIndexOfOperator(learner));

			// connecting meta learner input port
			OutputPort output = getInputPort().getSource();
			toUnlock.add(output);
			output.lock();
			output.disconnect();
			output.connectTo(metaLearner.getTrainingSetInputPort());

			// connecting meta learner output port
			if (modelOutput != null) {
				InputPort inputPort = modelOutput.getDestination();
				// connecting meta learner
				if (inputPort != null) {
					toUnlock.add(inputPort);
					inputPort.lock();
					modelOutput.disconnect();
					metaLearner.getModelOutputPort().connectTo(inputPort);
				}
			}

			// moving learner inside meta learner
			learner.remove();
			metaLearner.getSubprocess(0).addOperator(learner);

			// connecting learner input port to meta learner
			metaLearner.getSubprocess(0).getInnerSources().getPortByIndex(0).connectTo(getInputPort());

			// connecting learner output port to meta learner
			if (modelOutput != null) {
				modelOutput.connectTo(metaLearner.getInnerModelSink());
			}

		} catch (OperatorCreationException ex) {
			//ignore
		} finally {
			for (Port<?, ?> port : toUnlock) {
				port.unlock();
			}
		}
	}
}