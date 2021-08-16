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
 * Tests the results of {@link AntlrParser#parse(String)} for rounding functions.
 *
 * @author David Arnu, Thilo Kamradt, Kevin Majchrzak
 * @since 9.11
 */
public class RoundingTest {

	// round

	@Test
	public void roundDownSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(1.4)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundUpSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(1.7)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void round1ArgumentInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Math.round(Double.POSITIVE_INFINITY), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundDown2Args() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(1.3333,2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.33, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundUp2Args() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(1.666,2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.67, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundUp2ArgsNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(-1.666,2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(-1.67, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundUp2ArgsDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(1.666,2.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.67, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundDown2ArgsDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(1.3333,2.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.33, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void round2ArgumentInfinity1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(INFINITY, 2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void round2ArgumentInfinity2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(2, INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void round2ArgumentNegInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(2, -INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void round2ArgumentNegativePrecission() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(2, -5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundMissing1Argument() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(MISSING_NUMERIC)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundMissing2ArgumentFirst() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(MISSING_NUMERIC, 5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void roundMissing2ArgumentSecond() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(5.55,MISSING_NUMERIC)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(6, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void roundEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("round()");
	}

	@Test(expected = FunctionInputException.class)
	public void roundWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("round(\"aa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void roundWrongTypes() throws ExpressionException {
		AntlrParserTestUtils.getExpression("round(\"aa\", \"bb\")");
	}

	// rint

	@Test
	public void rintDownSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(2.5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintUpSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(1.5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rint1ArgumentInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Math.rint(Double.POSITIVE_INFINITY), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintDown2Args() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(1.3333,2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.33, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintUp2Args() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(1.666,2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.67, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintUp2ArgsNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(-1.666,2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(-1.67, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintUp2ArgsDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(1.666,2.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.67, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintDown2ArgsDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(1.3333,2.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.33, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rint2ArgumentInfinity1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(INFINITY, 2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rint2ArgumentInfinity2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(2, INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rint2ArgumentNegInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(2, -INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rint2ArgumentNegativePrecission() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("round(2562, -3)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(3000, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintMissing1Argument() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(MISSING_NUMERIC)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintMissing2ArgumentFirst() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(MISSING_NUMERIC, 5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void rintMissing2ArgumentSecond() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("rint(5.55,MISSING_NUMERIC)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(6, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void rintEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("rint()");
	}

	@Test(expected = FunctionInputException.class)
	public void rintWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("rint(\"aa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void rintWrongTypes() throws ExpressionException {
		AntlrParserTestUtils.getExpression("rint(\"aa\", \"bb\")");
	}

	// floor

	@Test
	public void floorDownSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("floor(2.5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void floorDownSimpleNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("floor(-2.5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-3, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void floor1ArgumentInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("floor(INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void floor1ArgumentNegInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("floor(-INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void floorMissing1Argument() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("floor(MISSING_NUMERIC)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void floorEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("floor()");
	}

	@Test(expected = FunctionInputException.class)
	public void floorWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("floor(\"aa\")");
	}

	// ceil

	@Test
	public void ceilSimple() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ceil(2.5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(3, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ceilSimpleNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ceil(-2.5)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ceil1ArgumentInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ceil(INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ceil1ArgumentNegInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ceil(-INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ceilMissing1Argument() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ceil(MISSING_NUMERIC)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void ceilEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ceil()");
	}

	@Test(expected = FunctionInputException.class)
	public void ceilWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ceil(\"aa\")");
	}

}
