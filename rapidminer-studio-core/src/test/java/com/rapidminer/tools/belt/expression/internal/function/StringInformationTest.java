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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for String information functions.
 *
 * @author David Arnu, Thilo Kamradt, Kevin Majchrzak
 * @since 9.11
 */
public class StringInformationTest {

	// compare
	@Test
	public void compareEqual() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("compare(\"abc\", \"abc\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void compareOneSmaller() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("compare(\"abc\", \"abcd\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void compareTwoSmaller() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("compare(\"abc\", \"abcde\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void compareLarger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("compare(\"babc\", \"abc\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void compareMissing1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("compare( MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void compareMissing2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("compare(\"abc\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void compareWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" compare(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void compareWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" compare(\"abc\",5)");
	}

	@Test(expected = FunctionInputException.class)
	public void compareWrongArgumentNumber() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" compare(\"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void compareEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" compare()");
	}

	// contains
	@Test
	public void containsTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("contains(\"abcd\", \"abc\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void containsFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("contains(\"aaa\", \"bbb\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void containsMissing1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("contains( MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void containsMissing2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("contains(\"abc\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void containsEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" contains()");
	}

	@Test(expected = FunctionInputException.class)
	public void containsWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" contains(\"aaa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void containsWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" contains(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void containsWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" contains(\"abc\",5)");
	}

	// Matches

	@Test
	public void matchesTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("matches(\"abcd\", \"a[bxyz]c[abcdxyz]\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void matchesFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("matches(\"abcd\", \"a[xyz]c[abcxyz]\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void matchesIllegalExpression() throws ExpressionException {
		AntlrParserTestUtils.getExpression("matches(\"abcd\", \"a[xyz]c[abcxyz]{1,3\")");
	}

	@Test
	public void matchesMissing1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("matches( MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void matchesMissing2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("matches(\"abc\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void matchesEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" matches()");
	}

	@Test(expected = FunctionInputException.class)
	public void matchesWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("matches(\"aaa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void matchesWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("matches(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void matchesWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("matches(\"abc\",5)");
	}

	// equals

	@Test
	public void equalsTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("equals(\"abcd\", \"abcd\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("equals(\"abcd\", \"Zer0 als Nummer\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsMissing1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("equals( MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void equalsMissing2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("equals(\"abc\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void equalsEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("equals()");
	}

	@Test(expected = FunctionInputException.class)
	public void equalsWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("equals(\"aaa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void equalsWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("equals(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void equalsWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("equals(\"abc\",5)");
	}

	// starts

	@Test
	public void startsTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("starts(\"abcd\", \"ab\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void startsFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("starts(\"abcd\", \"bi\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void startsMissing1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("starts( MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void startsMissing2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("starts(\"abc\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void statsEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("starts()");
	}

	@Test(expected = FunctionInputException.class)
	public void startsWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("starts(\"aaa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void startsWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("starts(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void startsWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("starts(\"abc\",5)");
	}

	// ends

	@Test
	public void endsTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ends(\"abcd\", \"cd\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void endsFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ends(\"abcd\", \"bi\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void endsMissing1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ends( MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void endsMissing2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("ends(\"abc\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void endsEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ends()");
	}

	@Test(expected = FunctionInputException.class)
	public void endsWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ends(\"aaa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void endsWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ends(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void endsWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("ends(\"abc\",5)");
	}

	// finds

	@Test
	public void findsTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("finds(\"abcd\", \"cd\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void findsFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("finds(\"abcd\", \"bi\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void findsEmptyInString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("finds(\"abcd\", \"\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void findsStringInEmpty() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("finds(\"\", \"bi\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void findsIllegalExpression() throws ExpressionException {
		AntlrParserTestUtils.getExpression("finds(\"abcd\", \"[xyz]c[abcxyz]{1,3\")");
	}

	@Test
	public void findsMissing1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("finds( MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void findsMissing2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("finds(\"abc\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void findsEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("finds()");
	}

	@Test(expected = FunctionInputException.class)
	public void findsWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("finds(\"aaa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void findsWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("finds(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void findsWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("finds(\"abc\",5)");
	}

	// index

	@Test
	public void indexMatchingString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("index(\"abcd\",\"c\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(2, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void indexNotMatchingString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("index(\"abcd\",\"t\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void indexEmptySubstring() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("index(\"abcd\",\"\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void indexEmptyMainString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("index(\"\",\"c\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(-1, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void indexMissing1() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("index( MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void indexMissing2() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("index(\"abc\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void indexEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("index()");
	}

	@Test(expected = FunctionInputException.class)
	public void indexWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("index(\"aaa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void indexWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("index(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void indexWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("index(\"abc\",5)");
	}

	// length

	@Test
	public void lengthSimpleUse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("length(\"abcd\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(4, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void lengthEmptyString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("length(\"\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(0, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void lengthMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("length(MISSING_NOMINAL)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void lengthsEmpty() throws ExpressionException {
		AntlrParserTestUtils.getExpression("length()");
	}

	@Test(expected = FunctionInputException.class)
	public void lengthWrongNumberOfArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("length(\"aaa\",\"aaa\")");
	}

	@Test(expected = FunctionInputException.class)
	public void lengthWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("length(5)");
	}

}
