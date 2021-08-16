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
package com.rapidminer.tools.belt.expression.internal;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.tools.belt.expression.Constant;
import com.rapidminer.tools.belt.expression.ExpressionParserModule;
import com.rapidminer.tools.belt.expression.Function;
import com.rapidminer.tools.belt.expression.internal.function.basic.Divide;
import com.rapidminer.tools.belt.expression.internal.function.basic.Minus;
import com.rapidminer.tools.belt.expression.internal.function.basic.Modulus;
import com.rapidminer.tools.belt.expression.internal.function.basic.Multiply;
import com.rapidminer.tools.belt.expression.internal.function.basic.Plus;
import com.rapidminer.tools.belt.expression.internal.function.basic.Power;
import com.rapidminer.tools.belt.expression.internal.function.comparison.Equals;
import com.rapidminer.tools.belt.expression.internal.function.comparison.GreaterEqualThan;
import com.rapidminer.tools.belt.expression.internal.function.comparison.GreaterThan;
import com.rapidminer.tools.belt.expression.internal.function.comparison.LessEqualThan;
import com.rapidminer.tools.belt.expression.internal.function.comparison.LessThan;
import com.rapidminer.tools.belt.expression.internal.function.comparison.NotEquals;
import com.rapidminer.tools.belt.expression.internal.function.logical.And;
import com.rapidminer.tools.belt.expression.internal.function.logical.Not;
import com.rapidminer.tools.belt.expression.internal.function.logical.Or;


/**
 * Singleton that holds the standard operations (+,-,*,...).
 *
 * @author Gisa Meier
 * @since 9.11
 */
public enum StandardOperations implements ExpressionParserModule {

	INSTANCE;

	private List<Function> operations = new LinkedList<>();

	StandardOperations() {

		// logical operations
		operations.add(new Not());
		operations.add(new And());
		operations.add(new Or());

		// comparison operations
		operations.add(new Equals());
		operations.add(new NotEquals());
		operations.add(new LessThan());
		operations.add(new GreaterThan());
		operations.add(new LessEqualThan());
		operations.add(new GreaterEqualThan());

		// basic operations
		operations.add(new Plus());
		operations.add(new Minus());
		operations.add(new Multiply());
		operations.add(new Divide());
		operations.add(new Power());
		operations.add(new Modulus());
	}

	@Override
	public String getKey() {
		return "";
	}

	@Override
	public List<Constant> getConstants() {
		return null;
	}

	@Override
	public List<Function> getFunctions() {
		return operations;
	}

}
