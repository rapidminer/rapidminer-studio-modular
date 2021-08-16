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
package com.rapidminer.operator.learner;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.TableCapabilityCheck;
import com.rapidminer.operator.TableCapabilityProvider;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.GenerateTableModelTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.TableLearnerPrecondition;
import com.rapidminer.tools.BiasExplanation;
import com.rapidminer.tools.BiasTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.belt.BeltErrorTools;

import java.util.Map;


/**
 * Abstract super class for learner operators that work on {@link IOTable} creating {@link IOTablePredictionModel}s.
 * Analog to {@link AbstractLearner} but for tables.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public abstract class AbstractIOTableLearner extends Operator implements TableCapabilityProvider, GeneralLearner<IOTable, IOTable> {

	private final InputPort tableInput = getInputPorts().createPort("training set");
	private final OutputPort modelOutput = getOutputPorts().createPort("model");
	private final OutputPort performanceOutput = getOutputPorts().createPort("estimated performance",
			canEstimatePerformance());
	private final OutputPort weightsOutput = getOutputPorts().createPort("weights", canCalculateWeights());
	private final OutputPort tableOutput = getOutputPorts().createPort("exampleSet");

	/** Creates a new abstract */
	public AbstractIOTableLearner(OperatorDescription description) {
		super(description);
		tableInput.addPrecondition(new TableLearnerPrecondition(this, tableInput));
		getTransformer().addRule(
				new GenerateTableModelTransformationRule(tableInput, modelOutput, getModelClass(),
						GeneralModel.ModelKind.SUPERVISED));
		getTransformer().addRule(new GenerateNewMDRule(performanceOutput, new MetaData(PerformanceVector.class)) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				if (canEstimatePerformance()) {
					return unmodifiedMetaData;
				} else {
					return null;
				}
			}
		});
		getTransformer().addRule(new GenerateNewMDRule(weightsOutput, new MetaData(AttributeWeights.class)) {

			@Override
			public MetaData modifyMetaData(MetaData unmodifiedMetaData) {
				if (canCalculateWeights()) {
					return unmodifiedMetaData;
				} else if (weightsOutput.isConnected()) {
					weightsOutput.addError(getWeightCalculationError(weightsOutput));
				}
				return null;
			}

		});
		getTransformer().addRule(new PassThroughRule(tableInput, tableOutput, false));
	}

	@Override
	public boolean shouldAutoConnect(OutputPort outputPort) {
		if (outputPort == performanceOutput) {
			return canEstimatePerformance();
		} else if (outputPort == weightsOutput) {
			return canCalculateWeights();
		} else if (outputPort == tableOutput) {
			return false;
		} else {
			return super.shouldAutoConnect(outputPort);
		}
	}

	/**
	 * Helper method in case this operator is constructed anonymously. Assigns the table to the input port and returns
	 * the model.
	 *
	 * @param table
	 * 		the table for learning
	 * @return the learned model
	 * @throws OperatorException
	 * 		if learning fails
	 */
	public IOTableModel doWork(IOTable table) throws OperatorException {
		tableInput.receive(table);
		doWork();
		return modelOutput.getData(IOTableModel.class);
	}

	/**
	 * Returns the weights (if computed, after one of the doWork()} methods has been called.
	 *
	 * @throws OperatorException
	 * 		if the weights computation fails
	 */
	public AttributeWeights getWeights() throws OperatorException {
		return weightsOutput.getData(AttributeWeights.class);
	}

	/**
	 * This method might be overridden from subclasses in order to specify exactly which model class they use. This is
	 * to ensure the proper postprocessing of some models like KernelModels (SupportVectorCounter) or TreeModels (Rule
	 * generation)
	 */
	public Class<? extends IOTablePredictionModel> getModelClass() {
		return IOTablePredictionModel.class;
	}

	/**
	 * Trains a model using an {@link IOTable} from the input. Uses the method {@link #learn(IOTable)}.
	 */
	@Override
	public void doWork() throws OperatorException {
		IOTable table = tableInput.getData(IOTable.class);

		BeltErrorTools.nonEmpty(table.getTable(), this);
		BeltErrorTools.hasRegularColumns(table.getTable(), this);

		// check capabilities and produce errors if they are not fulfilled
		TableCapabilityCheck check = new TableCapabilityCheck(this);
		check.checkCapabilities(table.getTable(), this);

		// bias checks -> only create warning logs for now
		Map<String, BiasExplanation> potentialBiasMap = BiasTools.checkForBias(table.getTable());
		if (!potentialBiasMap.isEmpty()) {
			for (Map.Entry<String, BiasExplanation> potentialBias : potentialBiasMap.entrySet()) {
				logWarning(BiasTools.getWarningMessage(potentialBias.getKey(), potentialBias.getValue()));
			}
		}

		IOTableModel model = learn(table);

		// add bias information as annotations
		if (!potentialBiasMap.isEmpty()) {
			for (Map.Entry<String, BiasExplanation> potentialBias : potentialBiasMap.entrySet()) {
				model.getAnnotations().setAnnotation(potentialBias.getKey() + " (" + I18N.getGUILabel("bias.potential") + ")", BiasTools.getShortWarningMessage(potentialBias.getValue()));
			}
		}

		modelOutput.deliver(model);

		// weights must be calculated _after_ learning
		if (canCalculateWeights() && weightsOutput.isConnected()) {
			AttributeWeights weights = getWeights(table);
			if (weights != null) {
				weightsOutput.deliver(weights);
			}
		}

		if (canEstimatePerformance() && performanceOutput.isConnected()) {
			PerformanceVector perfVector = null;
			if (shouldDeliverOptimizationPerformance()) {
				perfVector = getOptimizationPerformance();
			} else {
				perfVector = getEstimatedPerformance();
			}
			performanceOutput.deliver(perfVector);
		}

		tableOutput.deliver(table);
	}

	@Override
	public abstract IOTableModel learn(IOTable trainingTable) throws OperatorException;

	/**
	 * Returns {@code true} if this learner is capable of estimating its performance. If this returns {@code true}, a
	 * port will be created and {@link #getEstimatedPerformance()} will be called if this port is connected.
	 *
	 * @return {@code true} if this learner can estimate its performance
	 */
	public boolean canEstimatePerformance() {
		return false;
	}

	/**
	 * Returns {@code true} if this learner is capable of computing attribute weights. If this method
	 * returns {@code true}, also override {@link #getWeights(IOTable)}
	 *
	 * @return {@code true} if this learner can calculate weights
	 */
	public boolean canCalculateWeights() {
		return false;
	}

	/**
	 * Returns {@code true} if the user wants to deliver the performance of the original optimization
	 * problem. Since many learners are basically optimization procedures for a certain type of
	 * objective function the result of this procedure might also be of interest in some cases.
	 *
	 * @return whether the optimization performance should be delivered
	 */
	public boolean shouldDeliverOptimizationPerformance() {
		return false;
	}

	/**
	 * Returns the estimated performance. Subclasses which supports the capability to estimate the
	 * learning performance must override this method. The default implementation throws an
	 * exception.
	 *
	 * @return the estimated performance
	 */
	public PerformanceVector getEstimatedPerformance() throws OperatorException {
		throw new UserError(this, 912, getName(), "estimation of performance not supported.");
	}

	/**
	 * Returns the resulting performance of the original optimization problem. Subclasses which
	 * supports the capability to deliver this performance must override this method. The default
	 * implementation throws an exception.
	 *
	 * @return the optimization performance
	 */
	public PerformanceVector getOptimizationPerformance() throws OperatorException {
		throw new UserError(this, 912, getName(), "delivering the original optimization" +
				" performance is not supported.");
	}

	/**
	 * Returns the calculated weight vectors. Subclasses which supports the capability to calculate feature weights
	 * must
	 * override this method. The default implementation throws an exception.
	 *
	 * @param table
	 * 		the table for which to calculate the weights
	 * @return the calculated weights
	 */
	public AttributeWeights getWeights(IOTable table) throws OperatorException {
		throw new UserError(this, 916, getName(), "calculation of weights not supported.");
	}

	/**
	 * Returns the error for the weight port.
	 *
	 * @param weightPort
	 * 		the weight port
	 * @return the metadata error
	 * @see #canCalculateWeights()
	 */
	protected MetaDataError getWeightCalculationError(OutputPort weightPort) {
		return new SimpleMetaDataError(Severity.ERROR, weightPort, "parameters.incompatible_for_delivering",
				"AttributeWeights");
	}

	/**
	 * Returns the table input port.
	 *
	 * @return the table input port
	 */
	protected InputPort getTableInputPort() {
		return this.tableInput;
	}

	@Override
	public boolean isLearner() {
		return true;
	}
}
