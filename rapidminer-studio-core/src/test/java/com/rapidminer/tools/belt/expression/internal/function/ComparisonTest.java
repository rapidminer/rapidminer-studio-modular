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

import java.time.Instant;

import org.junit.Test;

import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;


/**
 * JUnit Tests for the comparison functions of the expression parser.
 *
 * @author Thilo Kamradt, Kevin Majchrzak
 * @since 9.11
 */
public class ComparisonTest {

	// missing

	@Test
	public void missingTrueNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("missing(MISSING_NOMINAL)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void missingTrueNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("missing(MISSING_NUMERIC)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void missingTrueDateTime() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("missing(MISSING_DATE_TIME)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void missingTrueBinominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("missing(contains(MISSING_NOMINAL,\"test\"))");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void missingNoArg() throws ExpressionException {
		AntlrParserTestUtils.getExpression("missing()");
	}

	@Test(expected = FunctionInputException.class)
	public void missingTooManyArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("missing(1,2,3,4)");
	}

	@Test
	public void missingFalseNumber() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("missing(1)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void missingFalseNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("missing(\"HandsomeJack\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void missingFalseNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("missing(pi)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void missingFalseBoolean() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("missing(TRUE)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void missingFalseDateTime() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("missing([date-time])");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	// <
	@Test
	public void smallerTrueNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1 < 1.2");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void smallerFalseNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1.2 < 1.2");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void smallerTrueNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"bo\" < \"ca\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void smallerFalseNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"boom\" < \"baboom\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void smallerNominalMissingFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"boom\" < MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void smallerNominalMissingError() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" MISSING_NOMINAL < 8");
	}

	@Test
	public void smallerNumericMissingFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5 < MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void smallerNumericMissingError() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" MISSING_NUMERIC < \"boom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerDateTimeMissing() throws ExpressionException {
		AntlrParserTestUtils.getExpression("\"boom\" < MISSING_DATE_TIME");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerDifferentTypes() throws ExpressionException {
		AntlrParserTestUtils.getExpression("8 < \"baboom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("FALSE < TRUE");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerNumericBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("0 < TRUE");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerDateTime() throws ExpressionException {
		Table table = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable())
				.addDateTime("other", i -> Instant.EPOCH).build(Belt.defaultContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		parser.parse("[other] < [date-time]");
	}

	// <=

	@Test
	public void smallerEqualTrueNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1.2 <= 1.2");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void smallerEqualFalseNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("2 <= 1.2");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void smallerEqualTrueNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"bo\" <= \"bo\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void smallerEqualFalseNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"boom\" <= \"baboom\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void smallerEqualNominalMissingFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"boom\" <= MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void smallerEqualNominalMissingError() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" MISSING_NOMINAL <= 8");
	}

	@Test
	public void smallerEqualNumericMissingFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5 <= MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void smallerEqualNumericMissingError() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" MISSING_NUMERIC <= \"boom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerEqualDateTimeMissing() throws ExpressionException {
		AntlrParserTestUtils.getExpression("\"boom\" <= MISSING_DATE_TIME");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerEqualDifferentTypes() throws ExpressionException {
		AntlrParserTestUtils.getExpression("8 <= \"baboom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerEqualBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("FALSE <= TRUE");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerEqualNumericBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("0 <= TRUE");
	}

	@Test(expected = FunctionInputException.class)
	public void smallerEqualDateTime() throws ExpressionException {
		Table table = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable())
				.addDateTime("other", i -> Instant.EPOCH).build(Belt.defaultContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		parser.parse("[other] <= [date-time]");
	}

	// >

	@Test
	public void greaterTrueNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1.2 > 1");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void greaterFalseNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1.2 > 1.2");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void greaterTrueNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"ca\" > \"bc\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void greaterFalseNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"baboom\" > \"boom\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void greaterNominalMissingFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"boom\" > MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void greaterNominalMissingError() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" MISSING_NOMINAL > 8");
	}

	@Test
	public void greaterNumericMissingFalse() throws ExpressionException {
		assertFalse(AntlrParserTestUtils.getExpression("5 > MISSING_NUMERIC").evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void greaterNumericMissingError() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" MISSING_NUMERIC > \"boom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterDateTimeMissing() throws ExpressionException {
		AntlrParserTestUtils.getExpression("\"boom\" > MISSING_DATE_TIME");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterDifferentTypes() throws ExpressionException {
		AntlrParserTestUtils.getExpression("8 > \"baboom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("FALSE > TRUE");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterNumericBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("0 > TRUE");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterDateTime() throws ExpressionException {
		Table table = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable())
				.addDateTime("other", i -> Instant.EPOCH).build(Belt.defaultContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		parser.parse("[other] > [date-time]");
	}

	// >=

	@Test
	public void greaterEqualTrueNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1.2 >= 1.2");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void greaterEqualFalseNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1.2 >= 2");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void greaterEqualTrueNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"ca\" >= \"bc\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void greaterEqualFalseNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"baboom\" >= \"boom\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void greaterEqualNominalMissingFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"boom\" >= MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void greaterEqualNominalMissingError() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" MISSING_NOMINAL >= 8");
	}

	@Test
	public void greaterEqualNumericMissingFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5 >= MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void greaterEqualNumericMissingError() throws ExpressionException {
		AntlrParserTestUtils.getExpression(" MISSING_NUMERIC >= \"boom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterEqualDateTimeMissing() throws ExpressionException {
		AntlrParserTestUtils.getExpression("\"boom\" >= MISSING_DATE_TIME");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterEqualDifferentTypes() throws ExpressionException {
		AntlrParserTestUtils.getExpression("8 >= \"baboom\"");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterEqualBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("FALSE >= TRUE");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterEqualNumericBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("0 >= TRUE");
	}

	@Test(expected = FunctionInputException.class)
	public void greaterEqualDateTime() throws ExpressionException {
		Table table = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable())
				.addDateTime("other", i -> Instant.EPOCH).build(Belt.defaultContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		parser.parse("[other] >= [date-time]");
	}

	// isFinite() tests

	@Test
	public void isFiniteTrueInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("isFinite(234)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void isFiniteTrueDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("isFinite(234.567)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void isFiniteFalseInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("isFinite(INFINITY)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void isFiniteFalseNegInf() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("isFinite(-INFINITY)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void isFiniteMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("isFinite(MISSING_NUMERIC)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void isFiniteErrorNoArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression("isFinite()");
	}

	@Test(expected = FunctionInputException.class)
	public void isFiniteErrorTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("isFinite(23,\"blob\")");
	}

	@Test(expected = FunctionInputException.class)
	public void isFiniteErrorWrongTypeDateTime() throws ExpressionException {
		AntlrParserTestUtils.getExpression("isFinite(date_now())");
	}

	@Test(expected = FunctionInputException.class)
	public void isFiniteErrorWrongTypeNominal() throws ExpressionException {
		AntlrParserTestUtils.getExpression("isFinite(\"Menschenmaterial\")");
	}

	@Test(expected = FunctionInputException.class)
	public void isFiniteErrorWrongTypeBool() throws ExpressionException {
		AntlrParserTestUtils.getExpression("isFinite(TRUE)");
	}
}
