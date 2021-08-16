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
package com.rapidminer.operator;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;


/**
 * This operator updates a {@link GeneralModel} with an {@link IOObject}, usually a
 * {@link com.rapidminer.example.ExampleSet} or {@link com.rapidminer.adaption.belt.IOTable}. Please note that the
 * model must return true for {@link Model#isUpdatable()} in order to be usable with this operator.
 * 
 * @author Ingo Mierswa
 */
public class ModelUpdater extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort modelInput = getInputPorts().createPort("model", Model.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort modelOutput = getOutputPorts().createPort("model");

	public ModelUpdater(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(exampleSetInput, exampleSetOutput);
		getTransformer().addPassThroughRule(modelInput, modelOutput);
	}

	/**
	 * Applies the operator and labels the {@link ExampleSet}. The example set in the input is not
	 * consumed.
	 */
	@Override
	public void doWork() throws OperatorException {
		GeneralModel<?, ?> model = modelInput.getData(GeneralModel.class);
		if (!model.isUpdatable()) {
			throw new UserError(this, 135, model.getClass());
		}

		IOObject updateObject = updateModel(model);

		exampleSetOutput.deliver(updateObject);
		modelOutput.deliver(model);
	}

	/**
	 * Updates the model in a generic way and returns the update data.
	 *
	 * @since 9.10
	 */
	private <T extends IOObject, S extends IOObject> T updateModel(GeneralModel<T, S> model) throws OperatorException {
		T data = exampleSetInput.getData(model.getInputType());
		try {
			model.updateModel(data, this);
		} catch (UserError e) {
			if (e.getOperator() == null) {
				e.setOperator(this);
			}
			throw e;
		}
		return data;
	}
}
