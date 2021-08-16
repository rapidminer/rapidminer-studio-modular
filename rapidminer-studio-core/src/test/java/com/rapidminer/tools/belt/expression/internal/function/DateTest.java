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
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for date functions.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateTest {

	private static final double DELTA = 10e-15;

	private static final long SOME_SECONDS = 12314231341123L;

	private static final Table testTable = createTestTable();

	// date_now
	@Test
	public void dateNowBasic() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_now()");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		// for testing we just assume that the dates are close enough, some delay might occur
		assertTrue("Dates aren't close enough to each other!",
				Math.abs(System.currentTimeMillis() - expression.evaluateInstant().toEpochMilli()) < 10);
	}

	@Test(expected = FunctionInputException.class)
	public void dateNowArgumentNumerical() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_now(5)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateNowArgumentString() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_now(\"bla\")");
	}

	// date_before
	@Test
	public void dateBeforeTRUE() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_before(date_time, date_time_after)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void dateBeforeFALSE() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_before(date_time, date_time_before)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void dateBeforeEqual() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_before(date_time, date_time)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void dateBeforeMissingFirstDynamic() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_before(date_time_missing, date_time)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void dateBeforeMissingSecondDynamic() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_before(date_time, date_time_missing)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void dateBeforeMissingFirstStatic() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_before(MISSING_DATE_TIME, date_time)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void dateBeforeMissingSecondStatic() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_before(date_time, MISSING_DATE_TIME)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void dateBeforeWrongFirst() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_before(\"bla\", date_now())");
	}

	@Test(expected = FunctionInputException.class)
	public void dateBeforeWrongSecond() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_before(date_now(), \"bla\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateBeforeMissingArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_before(date_now())");
	}

	@Test(expected = FunctionInputException.class)
	public void dateBeforeTooManyArgument() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_before(date_now(), date_now(), date_now())");
	}

	// date_after
	@Test
	public void dateAfterTRUE() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_after(date_time, date_time_before)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void dateAfterFALSE() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_after(date_time, date_time_after)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void dateAfterEqual() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_after(date_time, date_time)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void dateAfterMissingFirstStatic() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_after(MISSING_DATE_TIME, date_time)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void dateAfterMissingSecondStatic() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_after(date_time, MISSING_DATE_TIME)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void dateAfterMissingFirstDynamic() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_after(date_time_missing, date_time)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void dateAfterMissingSecondDynamic() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_after(date_time, date_time_missing)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void dateAfterMissingBoth() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_after(MISSING_DATE_TIME, MISSING_DATE_TIME)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void dateAfterWrongFirst() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_after(\"bla\", date_now())");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAfterWrongSecond() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_after(date_now(), \"bla\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAfterMissingArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_after(date_now())");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAfterTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_after(date_now())");
	}

	// date_diff
	@Test
	public void dateDiffPositive() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time_after, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(1, expression.evaluateNumerical(), DELTA);
	}

	@Test
	public void dateDiffNegative() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time_before, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(-1, expression.evaluateNumerical(), DELTA);
	}

	@Test
	public void dateDiffUnits() throws ExpressionException {
		ZonedDateTime dateTime = ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET"));

		assertEquals(-1, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 0, ZoneId.of("CET")),
				"DATE_UNIT_NANOSECOND", "CET"));
		assertEquals(1000, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				12, 9, 0, 2, 1, ZoneId.of("CET")),
				"DATE_UNIT_MILLISECOND", "CET"));
		assertEquals(-2, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				12, 8, 59, 59, 1, ZoneId.of("CET")),
				"DATE_UNIT_SECOND", "CET"));
		assertEquals(60, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				12, 10, 0, 1, 1, ZoneId.of("CET")),
				"DATE_UNIT_MINUTE", "CET"));
		assertEquals(1, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 1, ZoneId.of("UTC")),
				"DATE_UNIT_HOUR", "CET"));
		assertEquals(0, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 0, ZoneId.of("UTC")),
				"DATE_UNIT_HOUR", "CET"));
		assertEquals(17, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				29, 9, 0, 1, 1, ZoneId.of("CET")),
				"DATE_UNIT_DAY", "CET"));
		assertEquals(0, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				19, 9, 0, 1, 0, ZoneId.of("CET")),
				"DATE_UNIT_WEEK", "CET"));
		assertEquals(1, getDiff(dateTime, ZonedDateTime.of(2020, 2,
				19, 9, 0, 1, 1, ZoneId.of("CET")),
				"DATE_UNIT_WEEK", "CET"));
		assertEquals(-1, getDiff(dateTime, ZonedDateTime.of(2020, 1,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				"DATE_UNIT_MONTH", "CET"));
		assertEquals(20, getDiff(dateTime, ZonedDateTime.of(2040, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				"DATE_UNIT_YEAR", "CET"));
	}

	@Test
	public void dateDiffEqual() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(0, expression.evaluateNumerical(), DELTA);
	}

	@Test
	public void dateDiffMissingFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(MISSING_DATE_TIME, date_time, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateDiffMissingSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, MISSING_DATE_TIME, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateDiffMissingFirstAndSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(MISSING_DATE_TIME, MISSING_DATE_TIME, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateDiffMissingUnit() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time, " +
				"MISSING, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateDiffDynamicUnit() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(MISSING_DATE_TIME, MISSING_DATE_TIME, " +
				"date_unit, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateDiffMissingTimeZone() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time, " +
				"DATE_UNIT_NANOSECOND, MISSING)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test(expected = FunctionInputException.class)
	public void dateDiffWrongFirst() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_diff(\"bla\", date_now(), DATE_UNIT_NANOSECOND, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateDiffWrongSecond() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_diff(date_now(), \"bla\", DATE_UNIT_NANOSECOND, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateDiffMissingArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_diff(date_now(), \"bla\", DATE_UNIT_NANOSECOND)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateDiffTooManyArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_diff(date_now(), date_now(), DATE_UNIT_NANOSECOND, \"CET\", \"CET\")");
	}

	@Test (expected = FunctionInputException.class)
	public void dateDiffMillisOverflow() throws ExpressionException {
		getDiff(Instant.ofEpochMilli(Long.MIN_VALUE).atZone(ZoneId.of("CET")),
				Instant.ofEpochMilli(1).atZone(ZoneId.of("CET")),
				"DATE_UNIT_MILLISECOND", "CET");
	}

	@Test (expected = FunctionInputException.class)
	public void dateDiffNanoOverflow() throws ExpressionException {
		getDiff(Instant.EPOCH.atZone(ZoneId.of("CET")),
				ZonedDateTime.of(2266, 1, 1, 12,
						12, 12, 12, ZoneId.of("CET")),
				"DATE_UNIT_NANOSECOND", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateDiffTimeZoneWrongFormat() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time_after, " +
				"DATE_UNIT_NANOSECOND, \"FAIL\")");
		parser.getExpressionContext().setIndex(0);
		expression.evaluateNumerical();
	}

	@Test(expected = FunctionInputException.class)
	public void dateDiffUnitWrongFormat() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time_after, " +
				"\"FAIL\", \"CET\")");
		parser.getExpressionContext().setIndex(0);
		expression.evaluateNumerical();
	}

	@Test(expected = FunctionInputException.class)
	public void dateDiffTimeZoneWrongType() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_diff(date_time, date_time_after, " +
				"DATE_UNIT_NANOSECOND, 5)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateDiffUnitWrongType() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_diff(date_time, date_time_after, " +
				"5, \"CET\")");
	}

	@Test
	public void dateDiffTimeZoneMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time_after, " +
				"DATE_UNIT_NANOSECOND, MISSING)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateDiffUnitMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_diff(date_time, date_time_after, " +
				"MISSING, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	// date_add
	@Test
	public void dateAdd() throws ExpressionException {
		ZonedDateTime input = ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET"));

		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 0, 999_999_999, ZoneId.of("CET")),
				add(input, -2, "DATE_UNIT_NANOSECOND", "CET"));
		assertEquals(input.plusNanos(Long.MAX_VALUE),
				add(input, Long.MAX_VALUE, "DATE_UNIT_NANOSECOND", "CET"));
		assertEquals(input.plusNanos(Long.MIN_VALUE),
				add(input, Long.MIN_VALUE, "DATE_UNIT_NANOSECOND", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 1_000_001, ZoneId.of("CET")),
				add(input, 1, "DATE_UNIT_MILLISECOND", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				13, 9, 0, 1, 1, ZoneId.of("CET")),
				add(input, 1000 * 60 * 60 * 24, "DATE_UNIT_MILLISECOND", "CET"));

		// double check that millis work correctly
		ZonedDateTime inputPlusMaxMillis = input.plusNanos((Long.MAX_VALUE % 1000) * 1_000_000);
		inputPlusMaxMillis = inputPlusMaxMillis.plusSeconds(Long.MAX_VALUE / 1000);
		assertEquals(inputPlusMaxMillis,
				add(input, Long.MAX_VALUE, "DATE_UNIT_MILLISECOND", "CET"));

		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 2, 1, ZoneId.of("CET")),
				add(input, 1, "DATE_UNIT_SECOND", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				12, 8, 59, 1, 1, ZoneId.of("CET")),
				add(input, -1, "DATE_UNIT_MINUTE", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				14, 9, 0, 1, 1, ZoneId.of("CET")),
				add(input, 48, "DATE_UNIT_HOUR", "CET"));
		assertEquals(ZonedDateTime.of(2020, 3,
				1, 9, 0, 1, 1, ZoneId.of("CET")),
				add(input, 18, "DATE_UNIT_DAY", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				5, 9, 0, 1, 1, ZoneId.of("CET")),
				add(input, -1, "DATE_UNIT_WEEK", "CET"));
		assertEquals(ZonedDateTime.of(2022, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				add(input, 24, "DATE_UNIT_MONTH", "CET"));
		assertEquals(ZonedDateTime.of(-1000, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				add(input, -3020, "DATE_UNIT_YEAR", "CET"));
	}

	@Test
	public void dateAddTimeZoneMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(date_time, 1, " +
				"DATE_UNIT_NANOSECOND, MISSING)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateAddPositiveInfinity() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(date_time, INFINITY, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(Instant.MAX, expression.evaluateInstant());
	}

	@Test
	public void dateAddNegativeInfinity() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(date_time, -INFINITY, " +
				"DATE_UNIT_MILLISECOND, \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(Instant.MIN, expression.evaluateInstant());
	}

	@Test
	public void dateAddMissingFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(MISSING_DATE_TIME, 1, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateAddMissingSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(date_time, MISSING_NUMERIC, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateAddMissingThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(date_time, 1, " +
				"MISSING, \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateAddAllMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(MISSING_DATE_TIME, MISSING_NUMERIC, " +
				"MISSING, MISSING)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test(expected = FunctionInputException.class)
	public void dateAddNoArgs() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_add()");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAddTooManyArgs() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_add(date_now(), 1, DATE_UNIT_NANOSECOND, \"CET\", \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAddWrongFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_add(1, 1, " + "MISSING, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAddWrongSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_add(date_time, date_time, " +	"MISSING, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAddWrongThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_add(date_time, 1, " +
				"1, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAddWrongForth() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_add(date_time, 1, " + "MISSING, date_time)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateAddWrongThirdConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(date_time, 1, " +
				"\"abc\", \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateInstant();
	}

	@Test(expected = FunctionInputException.class)
	public void dateAddWrongForthConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_add(date_time, 1, " +
				"DATE_UNIT_NANOSECOND, \"TEST\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateInstant();
	}

	// date_set
	@Test
	public void dateSet() throws ExpressionException {
		ZonedDateTime input = ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET"));


		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 999_999, ZoneId.of("CET")),
				set(input, 999_999, "DATE_UNIT_NANOSECOND", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 0, ZoneId.of("CET")),
				set(input, 0, "DATE_UNIT_NANOSECOND", "CET"));

		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 999_000_001, ZoneId.of("CET")),
				set(input, 999, "DATE_UNIT_MILLISECOND", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 0, "DATE_UNIT_MILLISECOND", "CET"));


		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 2, 1, ZoneId.of("CET")),
				set(input, 2, "DATE_UNIT_SECOND", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 59, 1, ZoneId.of("CET")),
				set(input, 59, "DATE_UNIT_SECOND", "CET"));

		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 0, "DATE_UNIT_MINUTE", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				12, 9, 59, 1, 1, ZoneId.of("CET")),
				set(input, 59, "DATE_UNIT_MINUTE", "CET"));

		assertEquals(ZonedDateTime.of(2020, 2,
				12, 23, 0, 1, 1, ZoneId.of("CET")),
				set(input, 23, "DATE_UNIT_HOUR", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				12, 0, 0, 1, 1, ZoneId.of("CET")),
				set(input, 0, "DATE_UNIT_HOUR", "CET"));

		assertEquals(ZonedDateTime.of(2020, 2,
				1, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 1, "DATE_UNIT_DAY", "CET"));
		assertEquals(ZonedDateTime.of(2020, 2,
				29, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 29, "DATE_UNIT_DAY", "CET"));

		assertEquals(ZonedDateTime.of(2020, 1,
				1, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 1, "DATE_UNIT_WEEK", "CET"));
		assertEquals(ZonedDateTime.of(2020, 12,
				30, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 53, "DATE_UNIT_WEEK", "CET"));

		assertEquals(ZonedDateTime.of(2020, 1,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 0, "DATE_UNIT_MONTH", "CET"));
		assertEquals(ZonedDateTime.of(2020, 12,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 11, "DATE_UNIT_MONTH", "CET"));

		assertEquals(ZonedDateTime.of(-1000, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, -1000, "DATE_UNIT_YEAR", "CET"));
		assertEquals(ZonedDateTime.of(1000, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 1000, "DATE_UNIT_YEAR", "CET"));
		assertEquals(ZonedDateTime.of(-999_999_999, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, -999_999_999, "DATE_UNIT_YEAR", "CET"));
		assertEquals(ZonedDateTime.of(999_999_999, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET")),
				set(input, 999_999_999, "DATE_UNIT_YEAR", "CET"));
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetNanoTooLarge() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				1_000_000, "DATE_UNIT_NANOSECOND", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetNanoNegative() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1, "DATE_UNIT_NANOSECOND", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetMilliTooLarge() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				1_000, "DATE_UNIT_MILLISECOND", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetMilliNegative() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1, "DATE_UNIT_MILLISECOND", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetSecondTooLarge() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				60, "DATE_UNIT_SECOND", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetSecondNegative() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1, "DATE_UNIT_SECOND", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetMinuteTooLarge() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				60, "DATE_UNIT_MINUTE", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetMinuteNegative() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1, "DATE_UNIT_MINUTE", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetHourTooLarge() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				24, "DATE_UNIT_HOUR", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetHourNegative() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1, "DATE_UNIT_HOUR", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetDayTooLarge() throws ExpressionException {
		ZonedDateTime input = ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 1, ZoneId.of("CET"));
		set(input, 30, "DATE_UNIT_DAY", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetDayTooSmall() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				0, "DATE_UNIT_DAY", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetdayNegative() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1, "DATE_UNIT_DAY", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWeekTooLarge() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				59, "DATE_UNIT_WEEK", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWeekTooSmall() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				0, "DATE_UNIT_WEEK", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWeekNegative() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1, "DATE_UNIT_WEEK", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetMonthTooLarge() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				12, "DATE_UNIT_MONTH", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetMonthNegative() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1, "DATE_UNIT_MONTH", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetYearTooLarge() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				1_000_000_000, "DATE_UNIT_YEAR", "CET");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetYearTooSmall() throws ExpressionException {
		set(ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("CET")),
				-1_000_000_000, "DATE_UNIT_YEAR", "CET");
	}

	@Test
	public void dateSetTimeZoneMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_set(date_time, 1, " +
				"DATE_UNIT_NANOSECOND, MISSING)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateSetMissingFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_set(MISSING_DATE_TIME, 1, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateSetMissingSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_set(date_time, MISSING_NUMERIC, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateSetMissingThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_set(date_time, 1, " +
				"MISSING, \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateInstant());
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWrongFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_set(1, 1, " +
				"MISSING, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWrongSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_set(date_time, date_time, " +
				"MISSING, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWrongThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_set(date_time, 1, " +
				"1, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWrongForth() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_set(date_time, 1, " +
				"MISSING, date_time)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWrongThirdConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_set(date_time, 1, " +
				"\"abc\", \"CET\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateInstant();
	}

	@Test(expected = FunctionInputException.class)
	public void dateSetWrongForthConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_set(date_time, 1, " +
				"DATE_UNIT_NANOSECOND, \"TEST\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateInstant();
	}

	// date_get
	@Test
	public void dateGet() throws ExpressionException {
		ZonedDateTime input = ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 13_123_456, ZoneId.of("CET"));

		assertEquals(123_456, get(input, "DATE_UNIT_NANOSECOND", "CET"));
		assertEquals(13, get(input, "DATE_UNIT_MILLISECOND", "CET"));
		assertEquals(1, get(input, "DATE_UNIT_SECOND", "CET"));
		assertEquals(0, get(input, "DATE_UNIT_MINUTE", "CET"));
		assertEquals(9, get(input, "DATE_UNIT_HOUR", "CET"));
		assertEquals(12, get(input, "DATE_UNIT_DAY", "CET"));
		assertEquals(7, get(input, "DATE_UNIT_WEEK", "CET"));
		assertEquals(1, get(input, "DATE_UNIT_MONTH", "CET"));
		assertEquals(2020, get(input, "DATE_UNIT_YEAR", "CET"));

		assertEquals(8, get(input, "DATE_UNIT_HOUR", "UTC"));
	}

	@Test
	public void dateGetTimeZoneMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_get(date_time, " +
				"DATE_UNIT_NANOSECOND, MISSING)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateGetDynamicUnitConstantDate() throws ExpressionException {
		Table newTable = Builders.newTableBuilder(testTable)
				.addNominal("unit", i -> ExpressionParserConstants.DATE_TIME_UNIT_NANOSECOND)
				.build(new SequentialContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(newTable);
		Expression expression = parser.parse("date_get(MISSING_DATE_TIME, " +
				"unit, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateGetMissingFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_get(MISSING_DATE_TIME, " +
				"DATE_UNIT_NANOSECOND, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void dateGetMissingSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_get(date_time, " +
				"MISSING, \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test(expected = FunctionInputException.class)
	public void dateGetWrongFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_get(1, " + "DATE_UNIT_NANOSECOND, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateGetWrongSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_get(date_time, " + "1, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateGetWrongThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_get(date_time, " +	"DATE_UNIT_NANOSECOND, 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateGetMissingArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_get(MISSING_DATE_TIME, " +	"DATE_UNIT_NANOSECOND)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateGetTooManyArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_get(MISSING_DATE_TIME, " +	"DATE_UNIT_NANOSECOND, \"CET\", \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateGetWrongSecondConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_get(date_time, " +
				"\"abc\", \"CET\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateNumerical();
	}

	@Test(expected = FunctionInputException.class)
	public void dateGetWrongThirdConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_get(date_time, " +
				"DATE_UNIT_NANOSECOND, \"TEST\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateNumerical();
	}

	// date_time
	@Test
	public void dateMillis() throws ExpressionException {
		ZonedDateTime someTime = ZonedDateTime.of(2020, 2,
				12, 9, 0, 1, 13_123_456, ZoneId.of("CET"));
		ZonedDateTime cetEpoch = ZonedDateTime.of(1970, 1,
				1, 0, 0, 0, 0, ZoneId.of("CET"));
		ZonedDateTime longlongAgo = ZonedDateTime.of(-100_000, 1,
				1, 0, 0, 0, 0, ZoneId.of("UTC"));

		assertEquals(someTime.toInstant().toEpochMilli(), getMillis(someTime));
		assertEquals(cetEpoch.toInstant().toEpochMilli(), getMillis(cetEpoch));
		assertEquals(longlongAgo.toInstant().toEpochMilli(), getMillis(longlongAgo));
	}

	@Test
	public void dateMillisMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("date_millis_from_epoch(MISSING_DATE_TIME)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test (expected = FunctionInputException.class)
	public void dateMillisNoArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_millis_from_epoch()");
	}

	@Test (expected = FunctionInputException.class)
	public void dateMillisTooManyArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_millis_from_epoch(MISSING_DATE_TIME, MISSING_DATE_TIME)");
	}

	@Test (expected = FunctionInputException.class)
	public void dateMillisWrongInput() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("date_millis_from_epoch(1)");
	}

	private static Table createTestTable() {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("date_time", i -> Instant.ofEpochSecond(SOME_SECONDS));
		builder.addDateTime("date_time_after", i -> Instant.ofEpochSecond(SOME_SECONDS, 1));
		builder.addDateTime("date_time_before", i -> Instant.ofEpochSecond(SOME_SECONDS, -1));
		builder.addDateTime("date_time_missing", i -> null);
		builder.addNominal("date_unit", i -> ExpressionParserConstants.DATE_TIME_UNIT_SECOND);
		return builder.build(new SequentialContext());
	}

	private static long getDiff(ZonedDateTime left, ZonedDateTime right, String unit, String timeZone) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("left", i -> left.toInstant());
		builder.addDateTime("right", i -> right.toInstant());
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("date_diff(left, right, " + unit + ", \"" +
				timeZone + "\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return (long) expression.evaluateNumerical();
	}

	private static ZonedDateTime add(ZonedDateTime dateTime, long value, String unit, String timeZone) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> dateTime.toInstant());
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("date_add(dateTime, " + value + ", " + unit + ", \"" +
				timeZone + "\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return ZonedDateTime.ofInstant(expression.evaluateInstant(), ZoneId.of(timeZone));
	}

	private static ZonedDateTime set(ZonedDateTime dateTime, long value, String unit, String timeZone) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> dateTime.toInstant());
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("date_set(dateTime, " + value + ", " + unit + ", \"" +
				timeZone + "\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return ZonedDateTime.ofInstant(expression.evaluateInstant(), ZoneId.of(timeZone));
	}

	private static long get(ZonedDateTime dateTime, String unit, String timeZone) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> dateTime.toInstant());
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("date_get(dateTime, " + unit + ", \"" + timeZone + "\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return (long) expression.evaluateNumerical();
	}

	private static long getMillis(ZonedDateTime dateTime) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> dateTime.toInstant());
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("date_millis_from_epoch(dateTime)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return (long) expression.evaluateNumerical();
	}
}
