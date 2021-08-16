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
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for bitwise functions.
 *
 * @author David Arnu, Kevin Majchrzak
 * @since 9.11
 */
public class BitwiseTest {

	@Test
	public void bitOrSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("bit_or(2,1)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(3, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void bitOrNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("bit_or(-2,1)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-1, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void bitOrDoubleInteger() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_or(2.5,1)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitOrIntegerDouble() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_or(2,1.5)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitOrInfinity() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_or(2,INFINITY)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitOrMissing() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_or(2,MISSING_NUMERIC)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitOrWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_or(\"aa\",1.5)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_or()");
	}

	// bit XOR

	@Test
	public void bitXorSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("bit_xor(6,5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(3, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void bitXorNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("bit_xor(-2,1)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-1, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void bitXorDoubleInteger() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_xor(2.5,1)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitXorIntegerDouble() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_xor(2,1.5)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitXorInfinity() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_xor(2,INFINITY)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitXorMissing() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_xor(2,MISSING_NUMERIC)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitXorWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_xor(\"aa\",1.5)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitXorEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_xor()");
	}

	// bit AND

	@Test
	public void bitAndSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("bit_and(6,5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(4, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void bitAndNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("bit_and(-2,5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(4, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void bitAndDoubleInteger() throws ExpressionException {
		AntlrParserTestUtils.getExpression("bit_and(2.5,1)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitAndIntegerDouble() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_and(2,1.5)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitAndInfinity() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_and(2,INFINITY)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitAndMissing() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_and(2,MISSING_NUMERIC)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitAndWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_and(\"aa\",1.5)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitAndEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_and()");
	}

	// bit NOT

	@Test
	public void bitNotSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("bit_not(2)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-3, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void bitNotNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("bit_not(-2)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void bitNotDouble() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_not(2.5)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitNotInfinity() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_not(INFINITY)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitNotMissing() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_not(MISSING_NUMERIC)");
	}

	@Test(expected = FunctionInputException.class)
	public void bitNotWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_not(\"aa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void bitNotEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" bit_not()");
	}
}
