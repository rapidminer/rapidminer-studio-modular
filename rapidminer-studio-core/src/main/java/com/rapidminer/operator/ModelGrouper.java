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

import java.util.Arrays;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;


/**
 * <p>
 * This operator groups all input models together into a grouped (combined) model. This model can be
 * completely applied on new data or written into a file as once. This might become useful in cases
 * where preprocessing and prediction models should be applied together on new and unseen data.
 * </p>
 *
 * <p>
 * This operator replaces the automatic model grouping known from previous versions of RapidMiner.
 * The explicit usage of this grouping operator gives the user more control about the grouping
 * procedure. A grouped model can be ungrouped with the {@link ModelUngrouper} operator.
 * </p>
 *
 * <p>
 * Please note that the input models will be added in reverse order, i.e. the last created model,
 * which is usually the first one at the start of the io object, queue will be added as the last
 * model to the combined group model.
 * </p>
 *
 * @author Ingo Mierswa, Sebastian Land
 */
public class ModelGrouper extends Operator {

	private final InputPortExtender modelInputExtender = new InputPortExtender("models in", getInputPorts()) {

		@Override
		protected Precondition makePrecondition(InputPort port) {
			int index = modelInputExtender.getManagedPorts().size();
			return new SimplePrecondition(port, new MetaData(Model.class), index < 2);
		};
	};
	private final OutputPort modelOutput = getOutputPorts().createPort("model out");

	public ModelGrouper(OperatorDescription description) {
		super(description);

		modelInputExtender.ensureMinimumNumberOfPorts(2);

		getTransformer().addRule(new MDTransformationRule() {

			@Override
			public void transformMD() {
				List<ModelMetaData> metaDatas = modelInputExtender.getMetaDataAsOrNull(ModelMetaData.class, true);
				if (!metaDatas.isEmpty()) {
					ModelMetaData input = metaDatas.iterator().next();
					if (input != null) {
						ExampleSetMetaData trainMD = input.getTrainingSetMetaData();
						if (trainMD != null) {
							GeneralModel.ModelKind[] modelKinds = calculateModelKinds(metaDatas);
							ModelMetaData mmd = new ModelMetaData(GroupedModel.class, trainMD, modelKinds);
							mmd.addToHistory(modelOutput);
							modelOutput.deliverMD(mmd);
							return;
						}
						modelOutput.deliverMD(null);
						return;
					}
				}
			}
		});
		modelInputExtender.start();
	}

	@Override
	public void doWork() throws OperatorException {
		List<Model> modelList = modelInputExtender.getData(Model.class, true);

		GroupedModel groupedModel;
		if (modelList.size() < 1) {
			groupedModel = new GroupedModel(null);
		} else {
			ExampleSet trainingHeader = modelList.get(modelList.size() - 1).getTrainingHeader();
			groupedModel = new GroupedModel(trainingHeader);
		}

		for (Model model : modelList) {
			groupedModel.addModel(model);
		}

		modelOutput.deliver(groupedModel);
	}

	/**
	 * Calculates the model kinds based of those from the model meta data list.
	 */
	private GeneralModel.ModelKind[] calculateModelKinds(List<ModelMetaData> metaDatas) {
		return Arrays.stream(GeneralModel.ModelKind.values()).filter(k -> {
			if (k == GeneralModel.ModelKind.POSTPROCESSING || k ==
					GeneralModel.ModelKind.PREPROCESSING) {
				return allAreModelKind(k, metaDatas);
			} else {
				return oneIsModelKind(k, metaDatas);
			}
		}).toArray(GeneralModel.ModelKind[]::new);
	}

	/**
	 * Returns {@code true} iff all of the model meta datas if of the given kind.
	 */
	private boolean allAreModelKind(GeneralModel.ModelKind modelKind, List<ModelMetaData> metaDatas) {
		for (ModelMetaData model : metaDatas) {
			if (model != null && model.isModelKind(modelKind)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns {@code true} iff one of the model meta datas if of the given kind.
	 */
	private boolean oneIsModelKind(GeneralModel.ModelKind modelKind, List<ModelMetaData> metaDatas) {
		for (ModelMetaData model : metaDatas) {
			if (model != null && !model.isModelKind(modelKind)) {
				return false;
			}
		}
		return true;
	}
}
