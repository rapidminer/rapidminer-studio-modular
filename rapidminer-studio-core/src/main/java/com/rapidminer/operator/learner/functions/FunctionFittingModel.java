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
package com.rapidminer.operator.learner.functions;

import java.util.Arrays;
import java.util.Map;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.IOTablePredictionModel;


/**
 * Prediction model for function fitting. It uses the expression parser to apply the optimal parameters calculated in
 * {@link FunctionFitting}.
 * <p>
 * Please note: This class is in a beta state and may change in future releases.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class FunctionFittingModel extends IOTablePredictionModel {

	private final String expression;
	private final double[] optimalParameters;
	private final String[] parameterNames;

	private FunctionFittingModel() {
		//for json deserialization
		expression = "";
		optimalParameters = new double[0];
		parameterNames = new String[0];
	}

	public FunctionFittingModel(Table trainingHeader, String expression, double[] optimalParameters,
								String[] parameterNames) {
		super(new IOTable(trainingHeader), Tables.ColumnSetRequirement.SUPERSET,
				Tables.TypeRequirement.REQUIRE_MATCHING_TYPES, Tables.TypeRequirement.ALLOW_INT_FOR_REAL);
		this.expression = expression;
		this.optimalParameters = optimalParameters;
		this.parameterNames = parameterNames;
	}

	@Override
	protected Column performPrediction(Table adapted, Map<String, Column> confidences, Operator operator) throws OperatorException {
		return FunctionFitting.createPredictionColumn(adapted, parameterNames, optimalParameters, expression, operator);
	}

	@Override
	public String toString() {
		return super.toString() + "\n" +
				"expression: " + expression + "\n" +
				"parameters: " + Arrays.toString(parameterNames) + "\n" +
				"optimal values: " + Arrays.toString(optimalParameters);
	}
}