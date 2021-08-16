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

import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for String transformation functions.
 *
 * @author David Arnu, Kevin Majchrzak
 * @since 9.11
 */
public class StringTransformationTest {

	// concat
	@Test
	public void concatOne() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("concat(\"abc\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abc", expression.evaluateNominal());
	}

	@Test
	public void concatTwo() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("concat(\"abc\", \"def\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcdef", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void concatWrongFirstType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" concat(5, \"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void concatWrongSecondType() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" concat(\"abc\",5)");
	}

	@Test
	public void concatEmptyArguments() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("concat()");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test
	public void concatEmptyString() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("concat(\"\", \"abc\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abc", expression.evaluateNominal());
	}

	@Test
	public void concatMissingValue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("concat(MISSING_NOMINAL, \"abc\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abc", expression.evaluateNominal());
	}

	// replaceAll
	@Test
	public void replaceAllBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replaceAll(\"abcd\", \"[ac]\", \"X\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("XbXd", expression.evaluateNominal());
	}

	@Test
	public void replaceAllMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replaceAll(MISSING_NOMINAL, \"[ac]\", \"X\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void replaceAllMissingRegEx() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replaceAll(\"abcd\", MISSING_NOMINAL, \"X\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void replaceAllMissingReplace() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replaceAll(\"abcd\", \"[ac]\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void replaceAllEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replaceAll(\"\", \"[ac]\", \"X\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test
	public void replaceAllemptyReplacement() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replaceAll(\"abcd\", \"[ab]\", \"\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("cd", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void replaceAllMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" replaceAll( \"[ac]\", \"X\")");
	}

	@Test(expected = FunctionInputException.class)
	public void replaceAllTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" replaceAll(\"abcd\", \".*\", \"X\", \"a\")");
	}

	@Test(expected = FunctionInputException.class)
	public void replaceAllEmptyRegEx() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" replaceAll(\"abcd\", \"\", \"X\")");
	}

	// cut

	@Test
	public void cutBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cut(\"abcd\", 1, 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("bc", expression.evaluateNominal());
	}

	@Test
	public void cutBasicDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cut(\"abcdefg\", 1.345, 2.873)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("bc", expression.evaluateNominal());
	}

	@Test
	public void cutMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cut(MISSING_NOMINAL, 1, 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void cutMissingIndex() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cut(\"abcd\", MISSING_NUMERIC, 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("ab", expression.evaluateNominal());
	}

	@Test
	public void cutMissingLength() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("cut(\"abcd\", 2, MISSING_NUMERIC)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void cutEmptyText() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cut(\"\", 2, 2)");
	}

	@Test(expected = FunctionInputException.class)
	public void cutNegIndex() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cut(\"abcde\", -2, 2)");
	}

	@Test(expected = FunctionInputException.class)
	public void cutNegLength() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cut(\"abcde\", 2, -2)");
	}

	@Test(expected = FunctionInputException.class)
	public void cutIndexInf() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cut(\"abcde\", INFINITY, 2)");
	}

	@Test(expected = FunctionInputException.class)
	public void cutIndexNegInf() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cut(\"abcde\", -INFINITY, 2)");
	}

	@Test(expected = FunctionInputException.class)
	public void cutLengthInf() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cut(\"abcde\", 2, INFINITY)");
	}

	@Test(expected = FunctionInputException.class)
	public void cutLengthNegnf() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cut(\"abcde\", 2, -INFINITY)");
	}

	@Test
	public void cutLengthZero() throws ExpressionException {
		AntlrParserTestUtils.getExpression("cut(\"abcde\", 2, 0)");
	}

	@Test(expected = FunctionInputException.class)
	public void cutMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" cut( \"[ac]\", 5)");
	}

	@Test(expected = FunctionInputException.class)
	public void cutTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" cut(\"abcd\",4, 4, 4)");
	}

	@Test
	public void cutText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression(" cut(\"text\",1, 3)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("ext", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void cutTextTooLong() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" cut(\"text\",1, 4)");
	}

	// replace

	@Test
	public void replaceBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replace(\"abcd\", \"a\", \"X\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("Xbcd", expression.evaluateNominal());
	}

	@Test
	public void replaceMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replace(MISSING_NOMINAL, \"ac\", \"X\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void replaceMissingSearch() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replace(\"abcd\", MISSING_NOMINAL, \"X\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void replaceMissingReplace() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replace(\"abcd\", \"a\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void replaceEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replace(\"\", \"ac\", \"X\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test
	public void replaceemptyReplacement() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("replace(\"abcd\", \"a\", \"\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("bcd", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void replaceMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" replace( \"ac\", \"X\")");
	}

	@Test(expected = FunctionInputException.class)
	public void replaceTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" replace(\"abcd\", \".*\", \"X\", \"a\")");
	}

	@Test(expected = FunctionInputException.class)
	public void replaceEmptySearch() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" replace(\"abcd\", \"\", \"X\")");
	}

	// lower

	@Test
	public void lowerBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("lower(\"AbCd\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcd", expression.evaluateNominal());
	}

	@Test
	public void lowerMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("lower(MISSING_NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void lowerEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("lower(\"\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void lowerMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" lower()");
	}

	@Test(expected = FunctionInputException.class)
	public void lowerTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" lower(\"abcd\", \".*\", \"X\", \"a\")");
	}

	// upper

	@Test
	public void upperBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("upper(\"AbCd\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("ABCD", expression.evaluateNominal());
	}

	@Test
	public void upperMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("upper(MISSING_NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void upperEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("upper(\"\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void upperMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" upper()");
	}

	@Test(expected = FunctionInputException.class)
	public void upperTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" upper(\"abcd\", \".*\", \"X\", \"a\")");
	}

	// trim

	@Test
	public void trimBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("trim(\" abcd \")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcd", expression.evaluateNominal());
	}

	@Test
	public void trimTab() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("trim(\" abcd \t\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcd", expression.evaluateNominal());
	}

	@Test
	public void trimNewLine() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("trim(\" abcd \n\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcd", expression.evaluateNominal());
	}

	@Test
	public void trimMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("trim(MISSING_NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void trimEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("trim(\"\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void trimrMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression("trim()");
	}

	@Test(expected = FunctionInputException.class)
	public void trimTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("trim(\"abcd\", \".*\", \"X\", \"a\")");
	}

	// escapeHTML

	@Test
	public void escapeBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("escape_html(\"<div>abcd</div>\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("&lt;div&gt;abcd&lt;/div&gt;", expression.evaluateNominal());
	}

	@Test
	public void escapeMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("escape_html(MISSING_NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void escapeEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("escape_html(\"\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void excapeMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression("escape_html()");
	}

	@Test(expected = FunctionInputException.class)
	public void escapeTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("escape_html(\"abcd\", \".*\", \"X\", \"a\")");
	}

	// suffix

	@Test
	public void suffixBasicInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"abcd\", 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("cd", expression.evaluateNominal());
	}

	@Test
	public void suffixBasicDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"abcd\", 2.54)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("cd", expression.evaluateNominal());
	}

	@Test
	public void suffixTooBigLength() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"abcd\", 10)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcd", expression.evaluateNominal());
	}

	@Test
	public void suffixMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(MISSING_NOMINAL, 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void suffixMissingLength() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"abcd\", MISSING_NUMERIC)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test
	public void suffixEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"\", 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test
	public void suffixNegLength() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"hallo\", -3)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("hallo", expression.evaluateNominal());
	}

	@Test
	public void suffixLengthInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"abcde\", INFINITY)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcde", expression.evaluateNominal());
	}

	@Test
	public void suffixLengthNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"abcde\", -INFINITY)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcde", expression.evaluateNominal());
	}

	@Test
	public void suffixLengthZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("suffix(\"abcde\", 0)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void suffixMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression("suffix( \"[ac]\")");
	}

	@Test(expected = FunctionInputException.class)
	public void suffixTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("suffix(\"abcd\",4, 4, 4)");
	}

	@Test(expected = FunctionInputException.class)
	public void suffixNoArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression("suffix()");
	}

	// prefix

	@Test
	public void prefixBasicInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"abcd\", 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("ab", expression.evaluateNominal());
	}

	@Test
	public void prefixBasicDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"abcd\", 2.654)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("ab", expression.evaluateNominal());
	}

	@Test
	public void prefixTooBigLength() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"abcd\", 10)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcd", expression.evaluateNominal());
	}

	@Test
	public void prefixMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(MISSING_NOMINAL, 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void prefixMissingLength() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"abcd\", MISSING_NUMERIC)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test
	public void prefixEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"\", 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test
	public void prefixNegLength() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"hallo\", -3)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("hallo", expression.evaluateNominal());
	}

	@Test
	public void prefixLengthInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"abcde\", INFINITY)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcde", expression.evaluateNominal());
	}

	@Test
	public void prefixLengthNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"abcde\", -INFINITY)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("abcde", expression.evaluateNominal());
	}

	@Test
	public void prefixLengthZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("prefix(\"abcde\", 0)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void prefixMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" prefix( \"[ac]\")");
	}

	@Test(expected = FunctionInputException.class)
	public void prefixTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" prefix(\"abcd\",4, 4, 4)");
	}

	@Test(expected = FunctionInputException.class)
	public void prefixNoArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" prefix()");
	}

	// char

	@Test
	public void charBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(\"abcd\", 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("c", expression.evaluateNominal());
	}

	@Test
	public void charTooBigLength() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(\"abcd\", 10)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void charMissingText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(MISSING_NOMINAL, 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void charMissingIndex() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(\"abcd\", MISSING_NUMERIC)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("a", expression.evaluateNominal());
	}

	@Test
	public void charEmptyText() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(\"\", 2)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void CharNegIndex() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(\"hallo\", -3)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void charIndexInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(\"abcde\", INFINITY)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void charIndexNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(\"abcde\", -INFINITY)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void charIndexZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("char(\"abcde\", 0)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("a", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void charMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" char( \"[ac]\")");
	}

	@Test(expected = FunctionInputException.class)
	public void charTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" char(\"abcd\",4, 4, 4)");
	}

	@Test(expected = FunctionInputException.class)
	public void charNoArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" char()");
	}

	@Test
	public void charDoubleIndex() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression(" char(\"FireHawk\", 1.974)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("i", expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void charBoolInput() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" char(\"FireHawk\", TRUE)");
	}

	@Test(expected = FunctionInputException.class)
	public void charBoolInput2() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" char(TRUE, 1.974)");
	}

	@Test(expected = FunctionInputException.class)
	public void charWrongOrder() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" char(1.974, \"FireHawk\")");
	}
}
