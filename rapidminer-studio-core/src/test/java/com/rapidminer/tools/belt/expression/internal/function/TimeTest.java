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
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

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
 * Tests the results of {@link AntlrParser#parse(String)} for time functions.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TimeTest {

	private static final double DELTA = 10e-15;

	private static final long SOME_NANOS = 12314231341123L;

	private static final Table testTable = createTestTable();

	// time_diff
	@Test
	public void timeDiffPositive() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(time, time_after, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(1, expression.evaluateNumerical(), DELTA);
	}

	@Test
	public void timeDiffNegative() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(time, time_before, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(-1, expression.evaluateNumerical(), DELTA);
	}

	@Test
	public void timeDiffUnits() throws ExpressionException {
		LocalTime time = LocalTime.of(9, 0, 1, 1);

		assertEquals(-1, getDiff(time, LocalTime.of(9, 0, 1, 0),
				"DATE_UNIT_NANOSECOND"));
		assertEquals(1000, getDiff(time, LocalTime.of(9, 0, 2, 1),
				"DATE_UNIT_MILLISECOND"));
		assertEquals(-2, getDiff(time, LocalTime.of(8, 59, 59, 1),
				"DATE_UNIT_SECOND"));
		assertEquals(60, getDiff(time, LocalTime.of(10, 0, 1, 1),
				"DATE_UNIT_MINUTE"));
		assertEquals(0, getDiff(time, LocalTime.of(9, 0, 1, 0),
				"DATE_UNIT_HOUR"));
	}

	@Test
	public void timeDiffDynamic() throws ExpressionException {
		LocalTime left = LocalTime.of(10, 4, 19, 143215);
		LocalTime right = LocalTime.of(20, 8, 21, 143215);

		assertEquals(10, timeDiffDynamic(left, false, right, false, "DATE_UNIT_HOUR", false));
		assertEquals(10, timeDiffDynamic(left, false, right, false, "DATE_UNIT_HOUR", true));
		assertEquals(10, timeDiffDynamic(left, false, right, true, "DATE_UNIT_HOUR", false));
		assertEquals(10, timeDiffDynamic(left, false, right, true, "DATE_UNIT_HOUR", true));
		assertEquals(10, timeDiffDynamic(left, true, right, false, "DATE_UNIT_HOUR", false));
		assertEquals(10, timeDiffDynamic(left, true, right, false, "DATE_UNIT_HOUR", true));
		assertEquals(10, timeDiffDynamic(left, true, right, true, "DATE_UNIT_HOUR", false));
		assertEquals(10, timeDiffDynamic(left, true, right, true, "DATE_UNIT_HOUR", true));

		assertEquals(-10, timeDiffDynamic(right, false, left, false, "DATE_UNIT_HOUR", false));
		assertEquals(-10, timeDiffDynamic(right, false, left, false, "DATE_UNIT_HOUR", true));
		assertEquals(-10, timeDiffDynamic(right, false, left, true, "DATE_UNIT_HOUR", false));
		assertEquals(-10, timeDiffDynamic(right, false, left, true, "DATE_UNIT_HOUR", true));
		assertEquals(-10, timeDiffDynamic(right, true, left, false, "DATE_UNIT_HOUR", false));
		assertEquals(-10, timeDiffDynamic(right, true, left, false, "DATE_UNIT_HOUR", true));
		assertEquals(-10, timeDiffDynamic(right, true, left, true, "DATE_UNIT_HOUR", false));
		assertEquals(-10, timeDiffDynamic(right, true, left, true, "DATE_UNIT_HOUR", true));

		assertEquals(36242000000000L, timeDiffDynamic(left, false, right, false, "DATE_UNIT_NANOSECOND", false));
		assertEquals(36242000000000L, timeDiffDynamic(left, false, right, false, "DATE_UNIT_NANOSECOND", true));
		assertEquals(36242000000000L, timeDiffDynamic(left, false, right, true, "DATE_UNIT_NANOSECOND", false));
		assertEquals(36242000000000L, timeDiffDynamic(left, false, right, true, "DATE_UNIT_NANOSECOND", true));
		assertEquals(36242000000000L, timeDiffDynamic(left, true, right, false, "DATE_UNIT_NANOSECOND", false));
		assertEquals(36242000000000L, timeDiffDynamic(left, true, right, false, "DATE_UNIT_NANOSECOND", true));
		assertEquals(36242000000000L, timeDiffDynamic(left, true, right, true, "DATE_UNIT_NANOSECOND", false));
		assertEquals(36242000000000L, timeDiffDynamic(left, true, right, true, "DATE_UNIT_NANOSECOND", true));
	}

	@Test
	public void timeDiffEqual() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(time, time, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(0, expression.evaluateNumerical(), DELTA);
	}

	@Test
	public void timeDiffMissingFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(MISSING_TIME, time, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void timeDiffMissingSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(time, MISSING_TIME, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void timeDiffMissingFirstAndSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(MISSING_TIME, MISSING_TIME, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void timeDiffMissingUnit() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(time, time, " +
				"MISSING)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void timeDiffDynamicUnit() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(MISSING_TIME, MISSING_TIME, " +
				"date_unit)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test(expected = FunctionInputException.class)
	public void timeDiffWrongFirst() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_diff(\"bla\", time_parse(0), DATE_UNIT_NANOSECOND)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeDiffWrongSecond() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_diff(time_parse(0), \"bla\", DATE_UNIT_NANOSECOND)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeDiffMissingArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_diff(time_parse(0), time_parse(0))");
	}

	@Test(expected = FunctionInputException.class)
	public void timeDiffTooManyArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_diff(time_parse(0), time_parse(0), DATE_UNIT_NANOSECOND, time_parse(0))");
	}

	@Test(expected = FunctionInputException.class)
	public void timeDiffUnitWrongFormat() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(time, time_after, " +
				"\"FAIL\")");
		parser.getExpressionContext().setIndex(0);
		expression.evaluateNumerical();
	}

	@Test(expected = FunctionInputException.class)
	public void timeDiffUnitWrongType() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_diff(time, time_after, 5)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeDiffUnitInvalid() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression exp = parser.parse("time_diff(time, time_after, DATE_UNIT_DAY)");
		assertEquals(ExpressionType.INTEGER, exp.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		exp.evaluateNumerical();
	}

	@Test
	public void timeDiffUnitMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_diff(time, time_after, " +
				"MISSING)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	// time_add
	@Test
	public void timeAdd() throws ExpressionException {
		LocalTime input = LocalTime.of(9, 0, 1, 1);

		assertEquals(LocalTime.of(9, 0, 0, 999_999_999),
				add(input, -2, "DATE_UNIT_NANOSECOND"));
		assertEquals(LocalTime.of(9, 0, 1, 1_000_001),
				add(input, 1, "DATE_UNIT_MILLISECOND"));
		assertEquals(LocalTime.of(9, 0, 1, 1),
				add(input, 1000 * 60 * 60 * 24, "DATE_UNIT_MILLISECOND"));
		assertEquals(LocalTime.of(9, 0, 2, 1),
				add(input, 1, "DATE_UNIT_SECOND"));
		assertEquals(LocalTime.of(8, 59, 1, 1),
				add(input, -1, "DATE_UNIT_MINUTE"));
		assertEquals(LocalTime.of(10, 0, 1, 1),
				add(input, 25, "DATE_UNIT_HOUR"));
		assertEquals(LocalTime.of(23, 0, 1, 1),
				add(input, -34, "DATE_UNIT_HOUR"));

		assertEquals(input.plusNanos(Long.MIN_VALUE),
				add(input, Long.MIN_VALUE, "DATE_UNIT_NANOSECOND"));
		assertEquals(input.plusNanos(Long.MAX_VALUE),
				add(input, Long.MAX_VALUE, "DATE_UNIT_NANOSECOND"));
		assertEquals(input.plus(Long.MIN_VALUE, ChronoUnit.MILLIS),
				add(input, Long.MIN_VALUE, "DATE_UNIT_MILLISECOND"));
		assertEquals(input.plus(Long.MAX_VALUE, ChronoUnit.MILLIS),
				add(input, Long.MAX_VALUE, "DATE_UNIT_MILLISECOND"));
		assertEquals(input.plusSeconds(Long.MIN_VALUE),
				add(input, Long.MIN_VALUE, "DATE_UNIT_SECOND"));
		assertEquals(input.plusSeconds(Long.MAX_VALUE),
				add(input, Long.MAX_VALUE, "DATE_UNIT_SECOND"));
		assertEquals(input.plusMinutes(Long.MIN_VALUE),
				add(input, Long.MIN_VALUE, "DATE_UNIT_MINUTE"));
		assertEquals(input.plusMinutes(Long.MAX_VALUE),
				add(input, Long.MAX_VALUE, "DATE_UNIT_MINUTE"));
		assertEquals(input.plusHours(Long.MIN_VALUE),
				add(input, Long.MIN_VALUE, "DATE_UNIT_HOUR"));
		assertEquals(input.plusHours(Long.MAX_VALUE),
				add(input, Long.MAX_VALUE, "DATE_UNIT_HOUR"));
	}

	@Test
	public void timeAddDynamic() throws ExpressionException {
		LocalTime input = LocalTime.of(9, 0, 1, 1);
		LocalTime expected = input.plusSeconds(76);
		assertEquals(expected, timeAddDynamic(input, false, 76, false, "DATE_UNIT_SECOND", false));
		assertEquals(expected, timeAddDynamic(input, false, 76, false, "DATE_UNIT_SECOND", true));
		assertEquals(expected, timeAddDynamic(input, false, 76, true, "DATE_UNIT_SECOND", false));
		assertEquals(expected, timeAddDynamic(input, false, 76, true, "DATE_UNIT_SECOND", true));
		assertEquals(expected, timeAddDynamic(input, true, 76, false, "DATE_UNIT_SECOND", false));
		assertEquals(expected, timeAddDynamic(input, true, 76, false, "DATE_UNIT_SECOND", true));
		assertEquals(expected, timeAddDynamic(input, true, 76, true, "DATE_UNIT_SECOND", false));
		assertEquals(expected, timeAddDynamic(input, true, 76, true, "DATE_UNIT_SECOND", true));
	}

	@Test
	public void timeAddPositiveInfinity() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_add(time, INFINITY, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(LocalTime.ofNanoOfDay(SOME_NANOS).plusNanos(Long.MAX_VALUE),
				expression.evaluateLocalTime());
	}

	@Test
	public void timeAddNegativeInfinity() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_add(time, -INFINITY, " +
				"DATE_UNIT_MILLISECOND)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(LocalTime.ofNanoOfDay(SOME_NANOS).plus(-25_975_808, ChronoUnit.MILLIS),
				expression.evaluateLocalTime());
	}

	@Test
	public void timeAddMissingFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_add(MISSING_TIME, 1, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeAddMissingSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_add(time, MISSING_NUMERIC, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeAddMissingThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_add(time, 1, " +
				"MISSING)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeAddAllMissing() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_add(MISSING_TIME, MISSING_NUMERIC, " +
				"MISSING)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateLocalTime());
	}

	@Test(expected = FunctionInputException.class)
	public void timeAddNoArgs() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_add()");
	}

	@Test(expected = FunctionInputException.class)
	public void timeAddTooManyArgs() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_add(time_parse(0), 1, DATE_UNIT_NANOSECOND, DATE_UNIT_NANOSECOND)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeAddWrongFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_add(1, 1, " + "MISSING)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeAddWrongSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_add(time, time, " + "MISSING)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeAddWrongThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_add(time, 1, " +
				"1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeAddWrongThirdConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_add(time, 1, " +
				"\"abc\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateLocalTime();
	}

	@Test(expected = FunctionInputException.class)
	public void timeAddWrongInvalidUnit() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_add(time, 1, DATE_UNIT_DAY)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateLocalTime();
	}

	// time_set
	@Test
	public void timeSet() throws ExpressionException {
		LocalTime input = LocalTime.of(9, 0, 1, 1);

		assertEquals(LocalTime.of(9, 0, 1, 999_999),
				set(input, 999_999, "DATE_UNIT_NANOSECOND"));
		assertEquals(LocalTime.of(9, 0, 1, 0),
				set(input, 0, "DATE_UNIT_NANOSECOND"));

		assertEquals(LocalTime.of(9, 0, 1, 999_000_001),
				set(input, 999, "DATE_UNIT_MILLISECOND"));
		assertEquals(LocalTime.of(9, 0, 1, 1),
				set(input, 0, "DATE_UNIT_MILLISECOND"));


		assertEquals(LocalTime.of(9, 0, 2, 1),
				set(input, 2, "DATE_UNIT_SECOND"));
		assertEquals(LocalTime.of(9, 0, 59, 1),
				set(input, 59, "DATE_UNIT_SECOND"));

		assertEquals(LocalTime.of(9, 0, 1, 1),
				set(input, 0, "DATE_UNIT_MINUTE"));
		assertEquals(LocalTime.of(9, 59, 1, 1),
				set(input, 59, "DATE_UNIT_MINUTE"));

		assertEquals(LocalTime.of(23, 0, 1, 1),
				set(input, 23, "DATE_UNIT_HOUR"));
		assertEquals(LocalTime.of(0, 0, 1, 1),
				set(input, 0, "DATE_UNIT_HOUR"));
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetNanoTooLarge() throws ExpressionException {
		set(LocalTime.now(), 1_000_000, "DATE_UNIT_NANOSECOND");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetNanoNegative() throws ExpressionException {
		set(LocalTime.now(), -1, "DATE_UNIT_NANOSECOND");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetMilliTooLarge() throws ExpressionException {
		set(LocalTime.now(), 1_000, "DATE_UNIT_MILLISECOND");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetMilliNegative() throws ExpressionException {
		set(LocalTime.now(), -1, "DATE_UNIT_MILLISECOND");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetSecondTooLarge() throws ExpressionException {
		set(LocalTime.now(), 60, "DATE_UNIT_SECOND");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetSecondNegative() throws ExpressionException {
		set(LocalTime.now(), -1, "DATE_UNIT_SECOND");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetMinuteTooLarge() throws ExpressionException {
		set(LocalTime.now(),
				60, "DATE_UNIT_MINUTE");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetMinuteNegative() throws ExpressionException {
		set(LocalTime.now(),
				-1, "DATE_UNIT_MINUTE");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetHourTooLarge() throws ExpressionException {
		set(LocalTime.now(), 24, "DATE_UNIT_HOUR");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetHourNegative() throws ExpressionException {
		set(LocalTime.now(), -1, "DATE_UNIT_HOUR");
	}

	@Test
	public void timeSetMissingFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_set(MISSING_TIME, 1, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeSetMissingSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_set(time, MISSING_NUMERIC, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeSetMissingThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_set(time, 1, " +
				"MISSING)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertNull(expression.evaluateLocalTime());
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetWrongFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_set(1, 1, MISSING)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetWrongSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_set(time, time, MISSING)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetWrongThird() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_set(time, 1, 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetWrongThirdConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_set(time, 1, " +
				"\"abc\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateLocalTime();
	}

	@Test(expected = FunctionInputException.class)
	public void timeSetInvalidUnit() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_set(time, 1, DATE_UNIT_DAY)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateLocalTime();
	}

	// time_get
	@Test
	public void timeGet() throws ExpressionException {
		LocalTime input = LocalTime.of(9, 0, 1, 13_123_456);

		assertEquals(123_456, get(input, "DATE_UNIT_NANOSECOND"));
		assertEquals(13, get(input, "DATE_UNIT_MILLISECOND"));
		assertEquals(1, get(input, "DATE_UNIT_SECOND"));
		assertEquals(0, get(input, "DATE_UNIT_MINUTE"));
		assertEquals(9, get(input, "DATE_UNIT_HOUR"));
	}

	@Test
	public void timeGetDynamic() throws ExpressionException {
		LocalTime input = LocalTime.of(9, 0, 17, 1);
		assertEquals(17, timeGetDynamic(input, false, "DATE_UNIT_SECOND", false));
		assertEquals(17, timeGetDynamic(input, false,  "DATE_UNIT_SECOND", true));
		assertEquals(17, timeGetDynamic(input, true,  "DATE_UNIT_SECOND", false));
		assertEquals(17, timeGetDynamic(input, true,  "DATE_UNIT_SECOND", true));
	}

	@Test
	public void timeGetDynamicUnitConstantTime() throws ExpressionException {
		Table newTable = Builders.newTableBuilder(testTable)
				.addNominal("unit", i -> ExpressionParserConstants.DATE_TIME_UNIT_NANOSECOND)
				.build(new SequentialContext());
		ExpressionParser parser = AntlrParserTestUtils.getParser(newTable);
		Expression expression = parser.parse("time_get(MISSING_TIME, " +
				"unit)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void timeGetMissingFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_get(MISSING_TIME, " +
				"DATE_UNIT_NANOSECOND)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test
	public void timeGetMissingSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_get(time, " +
				"MISSING)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertTrue(Double.isNaN(expression.evaluateNumerical()));
	}

	@Test(expected = FunctionInputException.class)
	public void timeGetWrongFirst() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_get(1, " + "DATE_UNIT_NANOSECOND)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeGetWrongSecond() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("time_get(time, " + "1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeGetMissingArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_get(MISSING_TIME)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeGetTooManyArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_get(MISSING_TIME, " + "DATE_UNIT_NANOSECOND, DATE_UNIT_NANOSECOND)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeGetWrongSecondConstant() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_get(time, " +
				"\"abc\")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateNumerical();
	}

	@Test(expected = FunctionInputException.class)
	public void timeGetInvalidUnit() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("time_get(time, DATE_UNIT_YEAR)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		expression.evaluateNumerical();
	}

	private static Table createTestTable() {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> LocalTime.ofNanoOfDay(SOME_NANOS));
		builder.addTime("time_after", i -> LocalTime.ofNanoOfDay(SOME_NANOS + 1));
		builder.addTime("time_before", i -> LocalTime.ofNanoOfDay(SOME_NANOS - 1));
		builder.addTime("time_missing", i -> null);
		builder.addNominal("date_unit", i -> ExpressionParserConstants.DATE_TIME_UNIT_SECOND);
		return builder.build(new SequentialContext());
	}

	private static long getDiff(LocalTime left, LocalTime right, String unit) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("left", i -> left);
		builder.addTime("right", i -> right);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("time_diff(left, right, " + unit + ")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return (long) expression.evaluateNumerical();
	}

	private static LocalTime add(LocalTime time, long value, String unit) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("time_add(time, " + value + ", " + unit + ")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateLocalTime();
	}

	private static LocalTime set(LocalTime time, long value, String unit) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("time_set(time, " + value + ", " + unit + ")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateLocalTime();
	}

	private static long get(LocalTime time, String unit) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("time_get(time, " + unit + ")");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return (long) expression.evaluateNumerical();
	}

	/**
	 * Calls time_add and creates dynamic variables where needed.
	 */
	private static LocalTime timeAddDynamic(LocalTime time, boolean dynamicTime, long value, boolean dynamicValue,
											String unit, boolean dynamicUnit) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		builder.addInt53Bit("value", i -> value);
		builder.addNominal("unit", i -> constantToString(unit));
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_add(" +
				(dynamicTime ? "time" : "time_parse(" + time.toNanoOfDay() + ")") +
				", " +
				(dynamicValue ? "value" : value) +
				", " +
				(dynamicUnit ? "unit" : unit) +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateLocalTime();
	}

	/**
	 * Calls time_get and creates dynamic variables where needed.
	 */
	private static long timeGetDynamic(LocalTime time, boolean dynamicTime,
											String unit, boolean dynamicUnit) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		builder.addNominal("unit", i -> constantToString(unit));
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_get(" +
				(dynamicTime ? "time" : "time_parse(" + time.toNanoOfDay() + ")") +
				", " +
				(dynamicUnit ? "unit" : unit) +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return (long) expression.evaluateNumerical();
	}

	/**
	 * Calls time_diff and creates dynamic variables where needed.
	 */
	private static long timeDiffDynamic(LocalTime left, boolean dynamicLeft, LocalTime right, boolean dynamicRight,
									   String unit, boolean dynamicUnit) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("left", i -> left);
		builder.addTime("right", i -> right);
		builder.addNominal("unit", i -> constantToString(unit));
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_diff(" +
				(dynamicLeft ? "left" : "time_parse(" + left.toNanoOfDay() + ")") +
				", " +
				(dynamicRight ? "right" : "time_parse(" + right.toNanoOfDay() + ")") +
				", " +
				(dynamicUnit ? "unit" : unit) +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return (long) expression.evaluateNumerical();
	}

	/**
	 * @return the corresponding String for the given constant
	 */
	private static String constantToString(String constant) {
		try {
			return AntlrParserTestUtils.getExpression(constant).evaluateNominal();
		} catch (ExpressionException e) {
			throw new IllegalArgumentException("Invalid constant");
		}
	}
}
