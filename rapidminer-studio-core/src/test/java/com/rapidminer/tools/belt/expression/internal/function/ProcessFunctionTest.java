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
package com.rapidminer.tools.belt.expression.internal.function;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.IntSupplier;

import org.junit.Test;

import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.Function;
import com.rapidminer.tools.belt.expression.FunctionDescription;
import com.rapidminer.tools.belt.expression.FunctionInput;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;
import com.rapidminer.tools.belt.expression.internal.function.process.ParameterValue;
import com.rapidminer.tools.belt.expression.internal.function.statistical.Random;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for functions that need a process.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class ProcessFunctionTest {

	protected static final Map<String, Function> FUNCTION_MAP;

	static {
		FUNCTION_MAP = new HashMap<>();
		addFunction(new Random(null));
		addFunction(new ParameterValue(null));
	}

	protected static void addFunction(Function function) {
		FUNCTION_MAP.put(function.getFunctionName(), function);
	}

	protected static final ExpressionContext FUNCTION_CONTEXT = new ExpressionContext() {

		@Override
		public Function getFunction(String functionName) {
			return FUNCTION_MAP.get(functionName);
		}

		@Override
		public ExpressionEvaluator getVariable(String variableName) {
			return null;
		}

		@Override
		public ExpressionEvaluator getDynamicVariable(String variableName) {
			return null;
		}

		@Override
		public ExpressionEvaluator getDynamicVariable(String variableName, IntSupplier indexSupplier) {
			return null;
		}

		@Override
		public void setIndex(int index) {
			// nothing to do
		}

		@Override
		public int getIndex() {
			return -1;
		}

		@Override
		public ExpressionType getDynamicVariableType(String variableName) {
			return null;
		}

		@Override
		public ExpressionEvaluator getScopeConstant(String scopeName) {
			return null;
		}

		@Override
		public String getScopeString(String scopeName) {
			return null;
		}

		@Override
		public List<FunctionDescription> getFunctionDescriptions() {
			return null;
		}

		@Override
		public List<FunctionInput> getFunctionInputs() {
			return null;
		}

		@Override
		public ExpressionEvaluator getConstant(String constantName) {
			return null;
		}

		@Override
		public Callable<Void> getStopChecker() {
			return null;
		}
	};

	private Expression getExpression(String expression) throws ExpressionException {
		AntlrParser parser = new AntlrParser(FUNCTION_CONTEXT);
		return parser.parse(expression);
	}

	@Test
	public void randWithArgument() throws ExpressionException {
		Expression expression = getExpression("rand(2015)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.6224847827770777, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void randWithArgumentDouble() throws ExpressionException {
		Expression expression = getExpression("rand(2015.9)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.6224847827770777, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void randWithArgumentWrongType() throws ExpressionException {
		getExpression("rand(\"bla\")");
	}

	@Test(expected = FunctionInputException.class)
	public void randWithWrongNumberOfArguments() throws ExpressionException {
		getExpression("rand(2,3)");
	}

	@Test(expected = FunctionInputException.class)
	public void paramWitArgumentWrongType() throws ExpressionException {
		getExpression("param(5,\"bla\")");
	}

	@Test(expected = FunctionInputException.class)
	public void randWithNoArgument() throws ExpressionException {
		getExpression("param()");
	}

	@Test(expected = FunctionInputException.class)
	public void paramWithWrongNumberOfArguments() throws ExpressionException {
		getExpression("param(\"operator\",\"parameter\",\"blup\")");
	}

}
