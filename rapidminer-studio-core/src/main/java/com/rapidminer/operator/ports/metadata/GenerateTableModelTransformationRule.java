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

import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;


/**
 * A transformation rule that constructs model meta data for the output port from the table meta data at the input port
 * and additional information.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public class GenerateTableModelTransformationRule implements MDTransformationRule {

	private final OutputPort outputPort;
	private final InputPort tableInput;
	private final Class<? extends IOTableModel> modelClass;
	private final GeneralModel.ModelKind[] modelKinds;

	/**
	 * Generates a transformation rule that constructs model meta data for the output port from the table meta data at
	 * the input port and the additional parameters.
	 *
	 * @param tableInput
	 * 		the input port for the data table
	 * @param outputPort
	 * 		the output port for the model
	 * @param modelClass
	 * 		the model class
	 * @param modelKinds
	 * 		the model kinds which are not obvious from the model class, will be ignored for {@link
	 *        IOTablePredictionModel}s
	 */
	public GenerateTableModelTransformationRule(InputPort tableInput, OutputPort outputPort,
												Class<? extends IOTableModel> modelClass,
												GeneralModel.ModelKind... modelKinds) {
		this.outputPort = outputPort;
		this.tableInput = tableInput;
		this.modelClass = modelClass;
		this.modelKinds = modelKinds;
	}

	@Override
	public void transformMD() {
		TableMetaData input = tableInput.getMetaDataAsOrNull(TableMetaData.class);
		if (input != null) {
			TableModelMetaData mmd;
			if (IOTablePredictionModel.class.isAssignableFrom(modelClass)) {
				mmd = new TablePredictionModelMetaData((Class<? extends IOTablePredictionModel>) modelClass, input);
			} else {
				mmd = new TableModelMetaData(modelClass, input, modelKinds);
			}
			mmd.addToHistory(outputPort);
			outputPort.deliverMD(mmd);
			return;
		}
		outputPort.deliverMD(null);
	}

	/**
	 * @return the {@link OutputPort} the MD rule is for
	 */
	public OutputPort getOutputPort() {
		return outputPort;
	}
}