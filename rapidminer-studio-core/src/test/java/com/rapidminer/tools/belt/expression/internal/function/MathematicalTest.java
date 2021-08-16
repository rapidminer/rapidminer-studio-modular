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


/**
 * Tests the results of {@link AntlrParser#parse(String)} for mathematical functions.
 *
 * @author David Arnu, Marcel Seifert, Kevin Majchrzak
 * @since 9.11
 */
public class MathematicalTest {

	private static final double DELTA = 0.0000001;

	/**
	 * Sqrt tests
	 */
	@Test
	public void sqrtInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sqrt(16)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(4, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sqrtDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sqrt(12.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.sqrt(12.5), expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sqrtNeg() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sqrt(-4)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sqrtNaN() throws ExpressionException {
		assertEquals(Double.NaN, AntlrParserTestUtils.getExpression("sqrt(MISSING_NUMERIC)").evaluateNumerical(), DELTA);
	}

	@Test(expected = FunctionInputException.class)
	public void sqrtEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sqrt()");
	}

	@Test(expected = FunctionInputException.class)
	public void sqrtString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sqrt( \"blup\")");
	}

	/**
	 * exp tests
	 */
	@Test
	public void expInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("exp(1)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.E, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void expDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("exp(1.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.E, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void expNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("exp(-INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void expPosInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("exp(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void expZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("exp(0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void expNaN() throws ExpressionException {
		assertEquals(Double.NaN, AntlrParserTestUtils.getExpression("exp(MISSING_NUMERIC)").evaluateNumerical(), DELTA);
	}

	@Test(expected = FunctionInputException.class)
	public void expEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("exp()");
	}

	@Test(expected = FunctionInputException.class)
	public void expString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("exp( \"blup\")");
	}

	/**
	 * ln tests
	 */
	@Test
	public void lnInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ln(1)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void lnDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ln(1.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void lnNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ln(-1.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void lnNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ln(-INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void lnPosInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ln(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void lnZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ln(0.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void lnNaN() throws ExpressionException {
		assertEquals(Double.NaN, AntlrParserTestUtils.getExpression("ln(MISSING_NUMERIC)").evaluateNumerical(), DELTA);
	}

	@Test(expected = FunctionInputException.class)
	public void lnEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ln()");
	}

	@Test(expected = FunctionInputException.class)
	public void lnString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ln( \"blup\")");
	}

	/**
	 * log tests
	 */
	@Test
	public void logInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("log(1)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void logDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("log(1.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void logNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("log(-1.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void logNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("log(-INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void logPosInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("log(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void logZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("log(0.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void logNaN() throws ExpressionException {
		assertEquals(Double.NaN, AntlrParserTestUtils.getExpression("log(MISSING_NUMERIC)").evaluateNumerical(), DELTA);
	}

	@Test(expected = FunctionInputException.class)
	public void logEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("log()");
	}

	@Test(expected = FunctionInputException.class)
	public void logString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("log( \"blup\")");
	}

	/**
	 * ld tests
	 */
	@Test
	public void ldInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ld(1)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ldDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ld(1.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(0.0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ldNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ld(-1.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ldNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ld(-INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ldPosInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ld(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ldZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ld(0.0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void ldNaN() throws ExpressionException {
		assertEquals(Double.NaN, AntlrParserTestUtils.getExpression("ld(MISSING_NUMERIC)").evaluateNumerical(), DELTA);
	}

	@Test(expected = FunctionInputException.class)
	public void ldEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ld()");
	}

	@Test(expected = FunctionInputException.class)
	public void ldString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ld( \"blup\")");
	}

	/**
	 * sgn tests
	 */
	@Test
	public void sgnInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sgn(1)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sgnDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sgn(5.4)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sgnNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sgn(-8.7)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sgnNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sgn(-INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sgnPosInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sgn(INFINITY)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sgnZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sgn(0.0)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sgnNegZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sgn(-0.0)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sgnNaN() throws ExpressionException {
		assertEquals(Double.NaN, AntlrParserTestUtils.getExpression("sgn(MISSING_NUMERIC)").evaluateNumerical(), DELTA);
	}

	@Test
	public void sgnMissingInt() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getMissingIntegerTable());
		Expression expression = parser.parse("sgn([integer])");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void sgnEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sgn()");
	}

	@Test(expected = FunctionInputException.class)
	public void sgnString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sgn( \"blup\")");
	}

	/**
	 * abs tests
	 */
	@Test
	public void absInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("abs(1)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void absDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("abs(5.4)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(5.4, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void absNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("abs(-8.7)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(8.7, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void absNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("abs(-INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void absPosInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("abs(INFINITY)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void absZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("abs(0)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void absNaN() throws ExpressionException {
		assertEquals(Double.NaN, AntlrParserTestUtils.getExpression("sgn(MISSING_NUMERIC)").evaluateNumerical(), DELTA);
	}

	@Test
	public void absMissingInt() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getMissingIntegerTable());
		Expression expression = parser.parse("abs([integer])");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void absEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("abs()");
	}

	@Test(expected = FunctionInputException.class)
	public void absString() throws ExpressionException {
			AntlrParserTestUtils.getExpression("abs( \"blup\")");
	}

}
