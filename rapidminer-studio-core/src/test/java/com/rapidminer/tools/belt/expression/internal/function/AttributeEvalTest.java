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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.rapidminer.MacroHandler;
import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.MacroResolver;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;
import com.rapidminer.tools.belt.expression.internal.function.eval.AttributeEvaluationException;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for the attribute eval function.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class AttributeEvalTest {

	private static final Table testTable = createTestTable();

	@Test(expected = FunctionInputException.class)
	public void evalWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("attribute()");
	}

	@Test(expected = FunctionInputException.class)
	public void evalWrongInputType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("attribute(2 ,1.5 )");
	}


	@Test
	public void withEvalWithMacro() throws ExpressionException {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("my macro", "\"int\"+\"eger\"");
		MacroResolver macroResolver = new MacroResolver(handler);

		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable, macroResolver);
		Expression expression = parser.parse("attribute(eval(%{my macro}))");
		parser.getExpressionContext().setIndex(1);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void evalWithMacroAndAttribute() throws ExpressionException {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("my macro", "numeric");
		handler.addMacro("al", "al");
		MacroResolver macroResolver = new MacroResolver(handler);

		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable, macroResolver);
		Expression expression = parser.parse("eval(\"attribute(%{my macro}+%{al})*2\")");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2 * 1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = AttributeEvaluationException.class)
	public void evalAttributeConstantNaN() throws ExpressionException {
		AntlrParserTestUtils.getParser(testTable).parse("attribute(MISSING_NOMINAL)");
	}

	@Test
	public void evalAttributeConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("attribute(\"nominal 1\")");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("a", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void evalAttributeConstantWrongType() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("attribute(\"nominal 1\", REAL)");
	}

	@Test
	public void evalAttributeNonConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("attribute(\"nominal \"+integer, NOMINAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("a", expression.evaluateNominal());
	}

	@Test
	public void evalAttributeNonConstantWrongType() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("attribute(\"nominal \"+integer, REAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		try {
			expression.evaluateNumerical();
			fail();
		} catch (FunctionInputException e) {
			assertTrue(e.getMessage().contains("REAL"));
		}
	}

	@Test(expected = AttributeEvaluationException.class)
	public void evalAttributeNonConstantNaN() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("attribute(\"nominal \"+integer, NOMINAL)");
		parser.getExpressionContext().setIndex(1);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		expression.evaluateNominal();
	}

	@Test
	public void evalAttributeWithSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("attribute(\"int\"+ \"eger\",INTEGER)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void evalAttributeWithSecondToDouble() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("attribute(lower(\"NUmerical\"),REAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void evalAttributeWithSecondNotConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("attribute(\"integer\",[nominal 1])");
	}

	@Test(expected = FunctionInputException.class)
	public void evalAttributeWithSecondToStringMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("attribute(\"nominal 1\", MISSING_NOMINAL)");
	}

	@Test(expected = FunctionInputException.class)
	public void evalAttributeWithSecondToDateTime() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("attribute(\"inte\"+\"ger\", DATE_TIME)");
	}

	@Test
	public void attributeFromMacro() throws ExpressionException {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("attribute", "integer");
		MacroResolver macroResolver = new MacroResolver(handler);

		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable, macroResolver);
		Expression expression = parser.parse("attribute(%{attribute})");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void attributeFromMacroWithType() throws ExpressionException {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("attribute", "integer");
		MacroResolver macroResolver = new MacroResolver(handler);

		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable, macroResolver);
		Expression expression = parser.parse("attribute(%{attribute},NOMINAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("1", expression.evaluateNominal());
	}

	private static Table createTestTable() {
		TableBuilder builder = Builders.newTableBuilder(2);
		builder.addNominal("nominal 1", i -> i == 0 ? "a" : "b");
		builder.addReal("numerical", i -> i == 0 ? 1.5 : 3.0);
		builder.addInt53Bit("integer", i -> i == 0 ? 1 : Double.NaN);
		return builder.build(new SequentialContext());
	}

}
