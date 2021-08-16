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

import org.junit.Test;

import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;
import com.rapidminer.tools.belt.expression.internal.antlr.UnknownFunctionException;


/**
 * Tests the results of {@link AntlrParser#parse(String)} from the basic functions block.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class BasicTest {

	@Test
	public void integerInput() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext("23643");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(23643d, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void doubleInput() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext("236.43");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(236.43, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void doubleScientific() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext("2378423e-10");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2378423e-10, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void doubleScientificPositive() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext(".141529e12");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(.141529e12, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void doubleScientificPlus() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext("3.141529E+12");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(3.141529e12, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void stringInput() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext("\"bla blup\"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("bla blup", expression.evaluateNominal());
	}

	@Test
	public void stringWithEscaped() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext("\"bla\\\"\\\\3\\\" blup\"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("bla\"\\3\" blup", expression.evaluateNominal());
	}

	@Test
	public void stringWithUnicode() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext("\"\\u5f3e bla\\u234f blup\\u3333\"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("\u5f3e bla\u234f blup\u3333", expression.evaluateNominal());
	}

	@Test
	public void stringWithTabsAndNewlines() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpressionWithoutContext("\"\\u5f3e bla\nhello\tworld\r\nblup!\"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("\u5f3e bla hello world blup!", expression.evaluateNominal());
	}

	@Test
	public void multiplyInts() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3*5");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(3 * 5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void multiplyDoubles() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3.0*5");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(3.0 * 5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void divideInts() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("4 /2");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(4.0 / 2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void divideDoubles() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5.0 /2");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(5.0 / 2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void divideByZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5.0 /0");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void moduloInts() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5 %2");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(5 % 2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void moduloDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("4.7 %1.5");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void differentPointOperations() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("4%3 *5/2");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(5 / 2.0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void powerInts() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("2^3^2");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Math.pow(2, Math.pow(3, 2)), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void powerDoubles() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("2^3.0^2");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.pow(2, Math.pow(3, 2)), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void stringMultiplication() throws ExpressionException {
		AntlrParserTestUtils.getExpression("3* \"blup\"");
	}

	@Test(expected = FunctionInputException.class)
	public void stringDivision() throws ExpressionException {
		AntlrParserTestUtils.getExpression("\"blup\" /4");
	}

	@Test(expected = UnknownFunctionException.class)
	public void unknownFunction() throws ExpressionException {
		AntlrParserTestUtils.getExpression("unknown(3)");
	}

	@Test
	public void powerAsFunctionDoubles() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("pow(2,0.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.pow(2, 0.5), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void powerAsFunctionInts() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("pow (2,3)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Math.pow(2, 3), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void powerAsFunctionWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("pow(2)");
	}

	@Test
	public void moduloAsFunctionDoubles() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("mod(2 ,1.5 )");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2 % 1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minusOneDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("- 1.5");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(-1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minusDoubles() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("2- 1.5");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2 - 1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minusOneInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("- -11");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(11, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minusInts() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("-3-12 -11");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-3 - 12 - 11, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void minusWrong() throws ExpressionException {
		AntlrParserTestUtils.getExpression("-3-\"blup\"");
	}

	@Test(expected = FunctionInputException.class)
	public void minusWrongLeft() throws ExpressionException {
		AntlrParserTestUtils.getExpression("\"blup\"-5.678");
	}

	@Test(expected = FunctionInputException.class)
	public void minusWrongOne() throws ExpressionException {
		AntlrParserTestUtils.getExpression("-\"blup\"");
	}

	@Test
	public void plusOneInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("++11");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(11, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void plusOneDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("+11.06476");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(11.06476, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void plusOneString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("+\"blup\"");
	}

	@Test
	public void plusInts() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("+12+11");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(12 + 11, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void plusDoubles() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression(".123123+11.06476");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.123123 + 11.06476, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void plusStrings() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"hello \"+\"world\"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("hello world", expression.evaluateNominal());
	}

	@Test
	public void plusStringAndDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"hello \"+3.5");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("hello 3.5", expression.evaluateNominal());
	}

	@Test
	public void plusStringAndMissingDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"hello \"+0/0");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("hello ", expression.evaluateNominal());
	}

	@Test
	public void plusStringAndInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"hello \"+3");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("hello 3", expression.evaluateNominal());
	}

	@Test
	public void plusIntAndString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3+\"hello \"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("3hello ", expression.evaluateNominal());
	}

	@Test
	public void plusDoubleAndString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3.1415+\"hello \"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("3.1415hello ", expression.evaluateNominal());
	}

	@Test
	public void plusMissingDoubleAndString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("0/0+\"hello \"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("hello ", expression.evaluateNominal());
	}

	@Test
	public void morePlusDoubleAndString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3.1+3+\"hello \"");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("6.1hello ", expression.evaluateNominal());
	}

	@Test
	public void morePlusStringAndInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"hello \"+3+4");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("hello 34", expression.evaluateNominal());
	}

	@Test
	public void emptyStringAndInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"\"+3");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("3", expression.evaluateNominal());
	}

	@Test
	public void stringAndInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"\"+INFINITY");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("\u221E", expression.evaluateNominal());
	}

	@Test
	public void stringAndMinusInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"\"+ -INFINITY");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("-\u221E", expression.evaluateNominal());
	}

	@Test
	public void evalAttributeWithSecondNotConstant2() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getMissingIntegerTable());
		Expression expression = parser.parse("[integer]+[integer]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

}
