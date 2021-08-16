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
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.function.logical.And;
import com.rapidminer.tools.belt.expression.internal.function.logical.Not;
import com.rapidminer.tools.belt.expression.internal.function.logical.Or;


/**
 * JUnit test for the Logical Functions ({@link Or}, {@link And} and {@link Not}) of the antlr ExpressionParser.
 *
 * @author Thilo Kamradt, Sabrina Kirstein, Kevin Majchrzak
 * @since 9.11
 */
public class LogicalTest {

	// and
	@Test
	public void andBooleanFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("TRUE && FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void andBooleanTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("TRUE && TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void andNumericFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1 && 0");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void andNumericTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("45.654321 && -45");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void andMixedFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5456 && FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void andMixedTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5456 && TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void andBooleanMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("contains(MISSING_NOMINAL,\"Luke\") && FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void andDoubleMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("MISSING_NUMERIC && FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void andWrongTypeNominal() throws ExpressionException {
		AntlrParserTestUtils.getExpression("TRUE && \"baboom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void andWrongTypeInstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE && [date-time]");
	}

	@Test(expected = FunctionInputException.class)
	public void andWrongTypeTime() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE && [time]");
	}

	@Test(expected = FunctionInputException.class)
	public void andWrongTypeStringSet() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE && [text-set]");
	}

	@Test(expected = FunctionInputException.class)
	public void andWrongTypeStringList() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE && [text-list]");
	}

	@Test(expected = FunctionInputException.class)
	public void andWrongTypeText() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE && [text]");
	}

	// or

	@Test
	public void orBooleanFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("FALSE || FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void orBooleanTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("FALSE || TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void orNumericFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("0 || 0");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void orNumericTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("0 || 45");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void orMixedFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("0 || FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void orMixedTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5456 || TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void orBooleanMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("contains(MISSING_NOMINAL,\"Luke\") || FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void orDoubleMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("MISSING_NUMERIC || TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void orWrongTypeNominal() throws ExpressionException {
		AntlrParserTestUtils.getExpression("TRUE || \"baboom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void orWrongTypeInstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE || [date-time]");
	}

	@Test(expected = FunctionInputException.class)
	public void orWrongTypeTime() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE || [time]");
	}

	@Test(expected = FunctionInputException.class)
	public void orWrongTypeStringSet() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE || [text-set]");
	}

	@Test(expected = FunctionInputException.class)
	public void orWrongTypeStringList() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE || [text-list]");
	}

	@Test(expected = FunctionInputException.class)
	public void orWrongTypeText() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.parse("TRUE || [text]");
	}

	// not

	@Test
	public void notBooleanTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("!FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notBooleanFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("!TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notNumericTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("!0");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notNumericFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("!45");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notBooleanMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("!contains(MISSING_NOMINAL,\"Luke\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void notNumericMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("!MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void notWrongTypeNominal() throws ExpressionException {
		AntlrParserTestUtils.getExpression("!\"baboom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void notWrongTypeInstant() throws ExpressionException {
		AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable()).parse("![date-time]");
	}

	@Test(expected = FunctionInputException.class)
	public void notWrongTypeLocalTime() throws ExpressionException {
		AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable()).parse("![time]");
	}

	@Test(expected = FunctionInputException.class)
	public void notWrongTypeStringSet() throws ExpressionException {
		AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable()).parse("![text-set]");
	}

	@Test(expected = FunctionInputException.class)
	public void notWrongTypeStringList() throws ExpressionException {
		AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable()).parse("![text-list]");
	}

	@Test(expected = FunctionInputException.class)
	public void notWrongTypeText() throws ExpressionException {
		AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable()).parse("![text]");
	}

}
