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
import static org.junit.Assert.assertNull;

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
import com.rapidminer.tools.belt.expression.internal.function.eval.SubexpressionEvaluationException;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for the eval function.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class EvalTest {

	private static final Table testTable = createTestTable();

	@Test(expected = FunctionInputException.class)
	public void evalWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("eval()");
	}

	@Test(expected = FunctionInputException.class)
	public void evalWrongInputType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("eval(2 ,1.5 )");
	}

	@Test
	public void multiplyIntsViaEval() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(\"3*5\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(3 * 5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void evalMissingString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(MISSING_NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void multiplyIntsViaEvalToDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(\"3*5\",REAL)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(3 * 5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void multiplyDoublesViaEval() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(\"3.0\"+\"*5\")");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(3.0 * 5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void divideIntsViaEvalWithType() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(\"4 /2\",REAL)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(4.0 / 2, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void divideDoublesViaEvalWithWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("eval(\"5.0 /2\",INTEGER)");
	}

	@Test(expected = FunctionInputException.class)
	public void evalWithInvalidType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("eval(\"5.0 /2\",\"blup\")");
	}

	@Test
	public void moduloIntsViaEvalWithStringType() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(\"5 %2\",NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(5 % 2 + "", expression.evaluateNominal());
	}

	@Test
	public void evalWithStringTypeMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(\"MISSING_NOMINAL\",NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void evalWithStringTypeMissingNumerical() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(\"MISSING_NUMERIC\",NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void evalWithStringTypeMissingInstant() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("eval(\"MISSING_DATE_TIME\",NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test(expected = SubexpressionEvaluationException.class)
	public void differentPointOperationsWithNestedEvalFail() throws ExpressionException {
		AntlrParserTestUtils.getExpression("eval(\"4%3 *\"+\"eval(\"+\"5/2\"+\")\")");
	}

	@Test
	public void powerIntsEvalWithMacro() throws ExpressionException {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("my macro", "2^3^2");
		MacroResolver resolver = new MacroResolver(handler);

		Expression expression = AntlrParserTestUtils.getExpression("eval(%{my macro})", resolver);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Math.pow(2, Math.pow(3, 2)), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void differentPointOperationsWithNestedEval() throws ExpressionException {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("my macro", "\"5/2\"");
		MacroResolver resolver = new MacroResolver(handler);

		Expression expression = AntlrParserTestUtils.getExpression("eval(\"4%3 *\"+\"eval(\"+%{my macro}+\")\")",
				resolver);
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void evalAttributeWithoutSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("eval([nominal])");
	}

	@Test
	public void evalAttributeWithSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(\"3*\"+[integer],INTEGER)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(3 * 5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void evalAttributeWithSecondToDouble() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(\"3*\"+[integer],REAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(3.0 * 5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void evalAttributeWithSecondToString() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(\"3*\"+[integer],NOMINAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(3 * 5 + "", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void evalAttributeWithSecondNotConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("eval(\"3*\"+[integer],[nominal])");
	}

	@Test
	public void evalAttributeWithSecondToStringMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(\"MISSING_DATE_TIME\"+[integer],NOMINAL)");
		parser.getExpressionContext().setIndex(1);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void evalAttributeWithSecondToStringMissingBoth() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(MISSING+[integer],NOMINAL)");
		parser.getExpressionContext().setIndex(1);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void evalAttributeWithSecondToDateTime() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(\"3*\"+[integer],DATE_TIME)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		expression.evaluateInstant();
	}

	@Test
	public void evalRealAttributeWithSecondToDouble() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(\"3*\"+[numerical],REAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(3 * 1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void evalRealAttributeWithSecondToInteger() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(\"3*\"+[numerical],INTEGER)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateNumerical();
	}

	@Test
	public void evalStringAttributeWithSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval([nominal],NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals("false", expression.evaluateNominal());
		parser.getExpressionContext().setIndex(1);
		assertEquals("ab", expression.evaluateNominal());
	}

	@Test
	public void evalBooleanAttributeWithSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval([nominal],BINOMINAL)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(false, expression.evaluateBoolean());
	}

	@Test
	public void evalAttributeFromString() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("eval(\"integer\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void attributeFromMacro() throws ExpressionException {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("attribute", "integer");
		MacroResolver macroResolver = new MacroResolver(handler);

		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable, macroResolver);
		Expression expression = parser.parse("eval(%{attribute})");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void attributeFromMacroWithType() throws ExpressionException {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("attribute", "integer");
		MacroResolver macroResolver = new MacroResolver(handler);

		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable, macroResolver);
		Expression expression = parser.parse("eval(%{attribute},NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals("5", expression.evaluateNominal());
	}

	/**
	 * @return a test table used for the tests in this class.
	 */
	private static Table createTestTable() {
		TableBuilder builder = Builders.newTableBuilder(2);
		builder.addNominal("nominal", i -> i == 0 ? "contains(\"a\", \"b\")" : "concat(\"a\", \"b\")");
		builder.addReal("numerical", i -> i == 0 ? 1.5 : 3.0);
		builder.addInt53Bit("integer", i -> i == 0 ? 5 : Double.NaN);
		return builder.build(new SequentialContext());
	}

}
