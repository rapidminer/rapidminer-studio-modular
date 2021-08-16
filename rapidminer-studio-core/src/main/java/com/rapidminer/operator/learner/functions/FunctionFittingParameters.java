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
package com.rapidminer.operator.learner.functions;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.tools.belt.expression.Constant;
import com.rapidminer.tools.belt.expression.ExpressionParserModule;
import com.rapidminer.tools.belt.expression.Function;
import com.rapidminer.tools.belt.expression.internal.SimpleConstant;


/**
 * Helper module used by FunctionFitting operator to register function parameters. This class is experimental and may be
 * removed in future releases.
 *
 * @author Kevin Majchrzak
 * @since 9.10
 */
class FunctionFittingParameters implements ExpressionParserModule {

	private List<Constant> constants = new LinkedList<>();

	FunctionFittingParameters(String[] names, double[] values) {
		for (int i = 0; i < names.length; i++) {
			this.constants.add(new SimpleConstant(names[i], values[i]));
		}
	}

	@Override
	public String getKey() {
		return "core.function_fitting_parameters";
	}

	@Override
	public List<Constant> getConstants() {
		return constants;
	}

	@Override
	public List<Function> getFunctions() {
		return Collections.emptyList();
	}

}
