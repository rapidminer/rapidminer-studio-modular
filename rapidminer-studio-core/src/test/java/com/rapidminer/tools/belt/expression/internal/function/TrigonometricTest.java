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

import org.apache.commons.math3.util.FastMath;
import org.junit.Test;

import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for trigonometric functions.
 *
 * @author Denis Schernov, Kevin Majchrzak
 * @since 9.11
 */
public class TrigonometricTest {
	

	@Test
	public void sinInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sin(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sin(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sin(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sin(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sin(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sin(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sin(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void sinEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sin()");
	}

	@Test(expected = FunctionInputException.class)
	public void sinString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sin( \"blup\")");
	}

	@Test
	public void sinNull() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sin(0)");
		Expression expression = AntlrParserTestUtils.getExpression("sin(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sin(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sin(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sin(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sin(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sin(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sin(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sin(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cos(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cos(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cos(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cos(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cos(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cos(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosINFINITY() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cos(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void cosEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cos()");
	}

	@Test(expected = FunctionInputException.class)
	public void cosString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cos( \"blup\")");
	}

	@Test
	public void cosNull() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cos(0)");
		Expression expression = AntlrParserTestUtils.getExpression("cos(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cos(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cos(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cos(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cos(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cos(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cos(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cos(Math.PI / 2), expression.evaluateNumerical(), 1e-15);

	}

	@Test
	public void tanInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tan(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tan(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tan(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tan(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tan(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tan(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tan(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void tanEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("tan()");
	}

	@Test(expected = FunctionInputException.class)
	public void tanString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("tan( \"blup\")");
	}

	@Test
	public void tanNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tan(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tan(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tan(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tan(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tan(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tan(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tan(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tan(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cotInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cot(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.tan(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cotDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cot(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.tan(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cotNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cot(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.tan(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cotInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cot(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void cotEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cot()");
	}

	@Test(expected = FunctionInputException.class)
	public void cotString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cot( \"blup\")");
	}

	@Test
	public void cotNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cot(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cotPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cot(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cotPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cot(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void secInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sec(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.cos(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void secDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sec(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.cos(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void secNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sec(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.cos(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void secInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sec(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void secEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sec()");
	}

	@Test(expected = FunctionInputException.class)
	public void secString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sec( \"blup\")");
	}

	@Test
	public void secNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sec(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.cos(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void secPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sec(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.cos(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void secPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sec(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosecInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosec(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.sin(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosecDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosec(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.sin(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosecNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosec(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.sin(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosecInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosec(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void cosecEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cosec()");
	}

	@Test(expected = FunctionInputException.class)
	public void cosecString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cosec( \"blup\")");
	}

	@Test
	public void cosecNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosec(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosecNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosec(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.sin(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosecPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosec(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void cosecPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosec(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.0 / Math.sin(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asin(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.asin(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asin(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.asin(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asin(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.asin(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asin(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void asinEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("asin()");
	}

	@Test(expected = FunctionInputException.class)
	public void asinString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("asin( \"blup\")");
	}

	@Test
	public void asinNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asin(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.asin(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asin(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.asin(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asin(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.asin(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asin(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.asin(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acosInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acos(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.acos(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acosDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acos(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.acos(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acosNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acos(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.acos(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acosInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acos(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void acosEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("acos()");
	}

	@Test(expected = FunctionInputException.class)
	public void acosString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("acos( \"blup\")");
	}

	@Test
	public void acosNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acos(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.acos(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acosNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acos(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.acos(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acosPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acos(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.acos(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acosPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acos(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.acos(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void atanEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("atan()");
	}

	@Test(expected = FunctionInputException.class)
	public void atanString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("atan( \"blup\")");
	}

	@Test
	public void atanNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.asin(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2IntInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(16,16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(16, 16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2DoubleInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(16.3,16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(16.3, 16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2IntDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(16,16.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(16, 16.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2DoubleDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(33.3,33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(33.3, 33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NegativeNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(-10,-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(-10, -10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NegativePositive() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(-10,10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(-10, 10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PositiveNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(10,-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(10, -10), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NullNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(0,0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(0, 0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NullNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(0,90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(0, 90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NinetyNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(90,0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(90, 0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NinetyNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(90,90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(90, 90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NullPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(0,pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(0, Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NinetyPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(90,pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(90, Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PiNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(pi,0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(Math.PI, 0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PiNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(pi,90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(Math.PI, 90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NullPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(0,pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(0, Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2NinetyPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(90,pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(90, Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PiHalfNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(pi/2,0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(Math.PI / 2, 0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PiHalfNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(pi/2,90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(Math.PI / 2, 90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PiPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(pi,pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(Math.PI, Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PiPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(pi,pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(Math.PI, Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PiHalfPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(pi/2,pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(Math.PI / 2, Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atan2PiHalfPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atan2(pi/2,pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.atan2(Math.PI / 2, Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void atan2Empty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("atan2()");
	}

	@Test(expected = FunctionInputException.class)
	public void atan2String() throws ExpressionException {
		AntlrParserTestUtils.getExpression("atan2( \"blup\")");
	}

	@Test(expected = FunctionInputException.class)
	public void atan2StringInt() throws ExpressionException {
		AntlrParserTestUtils.getExpression("atan2( \"blup\",1)");
	}

	@Test
	public void sinhInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sinh(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sinh(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinhDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sinh(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sinh(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinhNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sinh(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sinh(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void sinhEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sinh()");
	}

	@Test(expected = FunctionInputException.class)
	public void sinhString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sinh( \"blup\")");
	}

	@Test
	public void sinhNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sinh(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sinh(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinhNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sinh(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sinh(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinhPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sinh(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sinh(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sinhPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sinh(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sinh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void coshInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosh(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cosh(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void coshDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosh(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cosh(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void coshNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosh(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cosh(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void coshEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cosh()");
	}

	@Test(expected = FunctionInputException.class)
	public void coshString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cosh( \"blup\")");
	}

	@Test
	public void coshNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosh(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cosh(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void coshNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosh(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cosh(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void coshPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosh(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cosh(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void coshPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cosh(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.cosh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanhInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tanh(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tanh(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanhDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tanh(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tanh(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanhNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tanh(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tanh(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void tanhEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("tanh()");
	}

	@Test(expected = FunctionInputException.class)
	public void tanhString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("tanh( \"blup\")");
	}

	@Test
	public void tanhNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tanh(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tanh(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanhNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tanh(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tanh(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanhPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tanh(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tanh(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void tanhPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("tanh(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.tanh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinhInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asinh(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.asinh(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinhDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asinh(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.asinh(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinhNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asinh(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.asinh(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void asinhEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("asinh()");
	}

	@Test(expected = FunctionInputException.class)
	public void asinhString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("asinh( \"blup\")");
	}

	@Test
	public void asinhNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asinh(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.asinh(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinhNinety() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asinh(90)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.asinh(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinhPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asinh(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.asinh(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void asinhPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("asinh(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.asinh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acoshInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acosh(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.acosh(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acoshDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acosh(33.3)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.acosh(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acoshNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acosh(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.acosh(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void acoshEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("acosh()");
	}

	@Test(expected = FunctionInputException.class)
	public void acoshString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("acosh( \"blup\")");
	}

	@Test
	public void acoshNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acosh(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.acosh(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acoshNinety() throws ExpressionException {
			Expression expression = AntlrParserTestUtils.getExpression("acosh(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acoshPi() throws ExpressionException {
			Expression expression = AntlrParserTestUtils.getExpression("acosh(pi)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.acosh(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void acoshPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("acosh(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.acosh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanhInt() throws ExpressionException {
			Expression expression = AntlrParserTestUtils.getExpression("atanh(16)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(16), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanhDouble() throws ExpressionException {
			Expression expression = AntlrParserTestUtils.getExpression("atanh(33.3)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.atanh(33.3), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanhNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atanh(-10)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.atanh(-10), expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void atanhEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("atanh()");
	}

	@Test(expected = FunctionInputException.class)
	public void atanhString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("atanh( \"blup\")");
	}

	@Test
	public void atanhNull() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atanh(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.atanh(0), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanhNinety() throws ExpressionException {
			Expression expression = AntlrParserTestUtils.getExpression("atanh(90)");
			assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
			assertEquals(FastMath.atanh(90), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanhPi() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atanh(pi)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.atanh(Math.PI), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void atanhPiHalf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("atanh(pi/2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(FastMath.atanh(Math.PI / 2), expression.evaluateNumerical(), 1e-15);
	}
}
