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
import com.rapidminer.tools.belt.expression.internal.function.comparison.Equals;
import com.rapidminer.tools.belt.expression.internal.function.comparison.NotEquals;


/**
 * JUnit Tests for the {@link Equals} and {@link NotEquals} functions of the Antlr ExpressionParser
 *
 * @author Sabrina Kirstein, Kevin Majchrzak
 * @since 9.11
 */
public class EqualsTest {

	@Test
	public void equalsTrueNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"Moe\" == \"Moe\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsTrueBoolean() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("TRUE == TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsTrueDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("[date-time] == [date-time]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsTrueNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1.45 == 1.45");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsFalseNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"Moe\" == \"Mr.Szyslak\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsFalseBoolean() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("TRUE == FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsFalseDate() throws ExpressionException {
		Table table = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable())
				.addDateTime("other", i -> Instant.EPOCH).build(Belt.defaultContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		Expression expression = parser.parse("[date-time] == [other]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsFalseNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3.14 == 1");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsTrueNumericNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3 == \"3\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsFalseNumericNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3 == \"Claptrap\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsFalseNumericDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("3 == [date-time]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsNumericBooleanTrueTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1 == TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsNumericBooleanFalseTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("0 == FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsNumericBooleanTrueFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5 == TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsNumericBooleanFalseFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("-1 == FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsFalseBooleanNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"MoXXi\" == FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsTrueBooleanNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"false\" == FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void equalsBooleanDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("FALSE == [date-time]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsNominalDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("\"NOMAD\" == [date-time]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsNumericMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1 == MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("1 == MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("1 == MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("1 == contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsNominalMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"Batman\" == MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("\"Beastmaster\" == MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("\"Phantomas\" == MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("\"Phantomas\" == contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsBoolMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("TRUE == MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("FALSE == MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("TRUE == MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("TRUE == contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsDateMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.getExpressionContext().setIndex(0);

		Expression expression = parser.parse("[date-time] == MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());

		expression = parser.parse("[date-time] == MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());

		expression = parser.parse("[date-time] == MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());

		expression = parser.parse("[date-time] == contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void equalsMissingMissing() throws ExpressionException {
		// same type missings
		Expression expression = AntlrParserTestUtils.getExpression("MISSING_NUMERIC == MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_NOMINAL == MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_DATE_TIME == MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("contains(MISSING_NOMINAL,\"test\") == contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		// different types missing
		expression = AntlrParserTestUtils.getExpression("MISSING_NUMERIC == MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_NUMERIC == MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_NOMINAL == MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_NOMINAL == contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	// not equals

	@Test
	public void notEqualsFalseNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"Moe\" != \"Moe\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsFalseBoolean() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("TRUE != TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsFalseDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("[date-time] != [date-time]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsFalseNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1.45 != 1.45");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsTrueNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"Moe\" != \"Mr.Szyslak\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsTrueBoolean() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("TRUE != FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsTrueDate() throws ExpressionException {
		Table table = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable())
				.addDateTime("other", i -> Instant.EPOCH).build(Belt.defaultContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		Expression expression = parser.parse("[date-time] != [other]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsTrueNumeric() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3.14 != 1");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsFalseNumericNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3 != \"3\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsTrueNumericNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("3 != \"Claptrap\"");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsTrueNumericDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("3 != [date-time]");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsTrueAgainNumericDate() throws ExpressionException {
		Table table = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable())
				.addInt53Bit("epoch-milli", i -> Instant.EPOCH.toEpochMilli()).build(Belt.defaultContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		Expression expression = parser.parse("[date-time] != [epoch-milli]");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsNumericBooleanFalseFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1 != TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsNumericBooleanTrueFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("0 != FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsNumericBooleanFalseTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("5 != TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsNumericBooleanTrueTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("-1 != FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsTrueBooleanNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"MoXXi\" != FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsFalseBooleanNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"false\" != FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsBooleanDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("[date-time] != FALSE");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsNominalDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		Expression expression = parser.parse("[date-time] != \"NOMAD\"");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsNumericMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("1 != MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("1 != MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("1 != MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsNominalMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("\"Batman\" != MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("\"Beastmaster\" != MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("\"Phantomas\" != MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsBoolMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("TRUE != MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("FALSE != MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("TRUE != MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("TRUE != contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsDateMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(AntlrParserTestUtils.getAllTypesTable());
		parser.getExpressionContext().setIndex(0);

		Expression expression = parser.parse("[date-time] != MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());

		expression = parser.parse("[date-time]  != MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());

		expression = parser.parse("[date-time]  != MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void notEqualsMissingMissing() throws ExpressionException {
		// same type missings
		Expression expression = AntlrParserTestUtils.getExpression("MISSING_NUMERIC != MISSING_NUMERIC");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_NOMINAL != MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_DATE_TIME != MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("contains(MISSING_NOMINAL,\"test\") != contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
		// different types missing
		expression = AntlrParserTestUtils.getExpression("MISSING_NUMERIC != MISSING_NOMINAL");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_NUMERIC != MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_NOMINAL != MISSING_DATE_TIME");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
		expression = AntlrParserTestUtils.getExpression("MISSING_NOMINAL != contains(MISSING_NOMINAL,\"test\")");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}
}
