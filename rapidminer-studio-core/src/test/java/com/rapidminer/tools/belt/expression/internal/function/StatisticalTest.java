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
 * Tests the results of {@link AntlrParser#parse(String)} from the statistical function block.
 *
 * @author David Arnu, Kevin Majchrzak
 * @since 9.11
 */
public class StatisticalTest {

	// average
	@Test
	public void averageAllEqual() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("avg(2,2,2,2,2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void averageSingle() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("avg(2)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void averageInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("avg(1,2,3,4,5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(3, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void averageDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("avg(1.5, 2.5, 2.5, 2.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2.25, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void averageAllWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("avg(\"aa\", \"bb\")");
	}

	@Test(expected = FunctionInputException.class)
	public void averageMixedWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("avg(\"aa\", 1)");
	}

	@Test
	public void averageInfiniteValue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("avg(1, 1/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void averageNAValue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("avg(1, 0/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void averageEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("avg()");
	}

	// minimum
	@Test
	public void minSingleInteger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("min(1)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minTwoInteger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("min(1,2)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minTwoDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("min(2.5,1.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minTwoMixed() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("min(2,1.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minNA() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("min(2,0/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void minMixedWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("min(\"aa\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void minWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("min(\"aa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void minAllWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("min(\"aa\", \"bb\", \"cc\")");
	}

	@Test
	public void minInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("min(1.5, 2.5, 3.5, 4.5, 5.5, 1/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void minNegInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("min(1.5, 2.5, 3.5, 4.5, 5.5, -1/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	// maximum
	@Test
	public void maxSingleInteger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("max(1)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void maxTwoInteger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("max(1,2)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void maxTwoDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("max(2.5,1.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void maxTwoMixed() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("max(2,1.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void maxNA() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("max(2,0/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void maxInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("max(1.5, 2.5, 3.5, 4.5, 5.5, 1/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void maxNegInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("max(1.5, 2.5, 3.5, 4.5, 5.5, -1/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(5.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void maxMixedWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("max(\"aa\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void maxWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("max(\"aa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void maxAllWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("max(\"aa\", \"bb\", \"cc\")");
	}

	// binominal
	@Test
	public void binomSimpleNSmaller() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("binom(1,2)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void binomRealValues() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("binom(1.9,2.9)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);

		expression = AntlrParserTestUtils.getExpression("binom(2.0,2.9)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void binomSimpleKSmaller() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("binom(5,2)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(10, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void binomAllWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("binom(\"aa\", \"bb\")");
	}

	@Test(expected = FunctionInputException.class)
	public void binomFirstWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("binom(\"aa\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void binomSecondWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("binom(10, \"aa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void binomEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("binom()");
	}

	@Test
	public void binomLargeNumbers() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("binom(20000,4)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(6.664666849995E15, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void binomNAFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getMissingIntegerTable());
		Expression expression = parser.parse("binom(5,[integer])");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void binomNASecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getMissingIntegerTable());
		Expression expression = parser.parse("binom([integer],5)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	// sum
	@Test
	public void sumSingleInteger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sum(1)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sumTwoInteger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sum(1,2)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(3, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sumTwoDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sum(2.5,1.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(4, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sumNA() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sum(2,0/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sumTwoMixed() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sum(2,1.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(3.5, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void sumMixedWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sum(\"aa\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void sumWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sum(\"aa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void sumAllWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("sum(\"aa\", \"bb\", \"cc\")");
	}

	@Test
	public void sumInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sum(1.5, 2.5, 3.5, 4.5, 5.5, 1/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void sumNegInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("sum(1.5, 2.5, 3.5, 4.5, 5.5, -1/0)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

}
