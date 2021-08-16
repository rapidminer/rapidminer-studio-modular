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

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for conversion functions.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class ConversionTest {

	@Test
	public void parse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("parse(\"4711\")");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(4711, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void parseEmptyOrInvalid() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("parse(\"\")");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void parseNoArg() throws ExpressionException {
		AntlrParserTestUtils.getExpression("parse()");
	}

	@Test(expected = FunctionInputException.class)
	public void parseWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("parse(777)");
	}

	@Test(expected = FunctionInputException.class)
	public void parseMoreArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("parse(\"4711\", \"333\")");
	}

	@Test
	public void parseMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("parse(MISSING_NOMINAL)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void parseMissingDate() throws ExpressionException {
		AntlrParserTestUtils.getExpression("parse(MISSING_DATE_TIME)");
	}

	@Test
	public void strInt() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("str(4711)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("4711", expression.evaluateNominal());
	}

	@Test
	public void strDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("str(4711.7)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("4711.700", expression.evaluateNominal());
	}

	@Test
	public void strInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("str(INFINITY)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("\u221E", expression.evaluateNominal());
	}

	@Test
	public void strInfinityParse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("parse(str(INFINITY))");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void strMinusInfinity() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("str(-INFINITY)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("-\u221E", expression.evaluateNominal());
	}

	@Test
	public void strMinusInfinityParse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("parse(str(-INFINITY))");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NEGATIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test(expected = FunctionInputException.class)
	public void strWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("str(\"abc\")");
	}

	@Test(expected = FunctionInputException.class)
	public void strNoArg() throws ExpressionException {
		AntlrParserTestUtils.getExpression("str()");
	}

	@Test(expected = FunctionInputException.class)
	public void strMoreArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("str(1, 2)");
	}

	@Test
	public void strMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("str(MISSING_NUMERIC)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test(expected = FunctionInputException.class)
	public void strMissingDate() throws ExpressionException {
		AntlrParserTestUtils.getExpression("str(MISSING_DATE_TIME)");
	}

	// date_parse
	@Test
	public void dateParseSomeMillis() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_parse(4156111665112)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertEquals(Instant.ofEpochMilli(4156111665112L), expression.evaluateInstant());
	}

	@Test
	public void dateParseSomeNegativeMillis() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_parse(-4156111665112)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertEquals(Instant.ofEpochMilli(-4156111665112L), expression.evaluateInstant());
	}

	@Test
	public void dateParseZeroMillis() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_parse(0)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertEquals(Instant.EPOCH, expression.evaluateInstant());
	}

	@Test
	public void dateParseMaxMillis() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_parse(" +
				Long.MAX_VALUE + ")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertEquals(Instant.ofEpochMilli(Long.MAX_VALUE), expression.evaluateInstant());
	}

	@Test
	public void dateParseMinMillis() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_parse(" +
				Long.MAX_VALUE + ")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertEquals(Instant.ofEpochMilli(Long.MAX_VALUE), expression.evaluateInstant());
	}

	@Test
	public void dateParseDynamic() throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addReal("millis", i -> 10);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		String expString = "date_parse(millis)";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(Instant.EPOCH.plusMillis(10), expression.evaluateInstant());
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_parse(\"Test\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_parse(1, 2)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseMissingArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_parse()");
	}

	@Test
	public void dateParseMissing() throws ExpressionException {
		assertNull(AntlrParserTestUtils
				.getExpression("date_parse(MISSING_NUMERIC)").evaluateInstant());
	}

	// date_parse
	@Test
	public void timeParse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("time_parse(4156111665112)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertEquals(LocalTime.of(1,9, 16, 111_665_112),
				expression.evaluateLocalTime());
	}

	@Test
	public void timeParseZero() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("time_parse(0)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertEquals(LocalTime.MIDNIGHT, expression.evaluateLocalTime());
	}

	@Test
	public void timeParseMax() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("time_parse(" +
				LocalTime.MAX.toNanoOfDay() + ")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertEquals(LocalTime.MAX, expression.evaluateLocalTime());
	}

	@Test
	public void timeParseDynamic() throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addReal("nanos", i -> 10);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		String expString = "time_parse(nanos)";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		assertEquals(LocalTime.ofNanoOfDay(10), expression.evaluateLocalTime());
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseNegative() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("time_parse(-1)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		expression.evaluateLocalTime();
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseTooLarge() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("time_parse(" +
				(LocalTime.MAX.toNanoOfDay() + 1) + ")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		expression.evaluateLocalTime();
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseWrongType() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_parse(\"Test\")");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_parse(1, 2)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseMissingArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_parse()");
	}

	@Test
	public void timeParseMissing() throws ExpressionException {
		assertNull(AntlrParserTestUtils
				.getExpression("time_parse(MISSING_NUMERIC)").evaluateLocalTime());
	}

	// date_parse_str
	@Test
	public void dateParseStr() throws ExpressionException {
		Instant expected = OffsetDateTime.of(2021, 5, 27, 14,
				7, 47, 332891000, ZoneOffset.ofHours(2)).toInstant();
		assertEquals(expected, dateParseString("2021-05-27T14:07:47.332891+02:00"));
		assertEquals(expected, dateParseString("2021-05-27 14:07:47.332891+02:00"));
		assertEquals(expected, dateParseString("2021-05-2714:07:47.332891+02:00"));
		assertEquals(expected, dateParseString("2021-05-27T14:07:47.332891+12:00[CET]"));
		assertEquals(expected, dateParseString("2021-05-27 14:07:47.332891+12:00[CET]"));
		assertEquals(expected, dateParseString("2021-05-2714:07:47.332891+12:00[CET]"));
		assertEquals(expected, dateParseString("2021-05-27T14:07:47.332891[CET]"));
		assertEquals(expected, dateParseString("2021-05-27 14:07:47.332891[CET]"));
		assertEquals(expected, dateParseString("2021-05-2714:07:47.332891[CET]"));
	}

	@Test
	public void dateParseStrDynamicOne() throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addNominal("instant", i -> "2021-05-27T14:07:47.332891[CET]");
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("date_parse_str(instant)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		Instant expected = OffsetDateTime.of(2021, 5, 27, 14,
				7, 47, 332891000, ZoneOffset.ofHours(2)).toInstant();
		parser.getExpressionContext().setIndex(0);
		assertEquals(expected, expression.evaluateInstant());
	}

	@Test
	public void dateParseStrDynamicTwo() throws ExpressionException {
		Instant expected = ZonedDateTime.of(2021, 2, 21, 11,
				12, 34, 123_400_000, ZoneId.of("CET")).toInstant();

		String instant = "Feb 21, 2021 11:12:34.1234 AM CET";
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";

		assertEquals(expected, dateParseStringDynamic(instant, false, pattern, false));
		assertEquals(expected, dateParseStringDynamic(instant, false, pattern, true));
		assertEquals(expected, dateParseStringDynamic(instant, true, pattern, false));
		assertEquals(expected, dateParseStringDynamic(instant, true, pattern, true));
	}

	@Test
	public void dateParseStrDynamicThree() throws ExpressionException {
		Instant expected = ZonedDateTime.of(2021, 2, 21, 11,
				12, 34, 123_400_000, ZoneId.of("CET")).toInstant();

		String instant = "févr. 21, 2021 11:12:34.1234 AM CET";
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		String locale = "fr";

		assertEquals(expected, dateParseStringDynamic(instant, false, pattern, false, locale, false));
		assertEquals(expected, dateParseStringDynamic(instant, false, pattern, false, locale, true));
		assertEquals(expected, dateParseStringDynamic(instant, false, pattern, true, locale, false));
		assertEquals(expected, dateParseStringDynamic(instant, false, pattern, true, locale, true));
		assertEquals(expected, dateParseStringDynamic(instant, true, pattern, false, locale, false));
		assertEquals(expected, dateParseStringDynamic(instant, true, pattern, false, locale, true));
		assertEquals(expected, dateParseStringDynamic(instant, true, pattern, true, locale, false));
		assertEquals(expected, dateParseStringDynamic(instant, true, pattern, true, locale, true));
	}


	@Test
	public void dateParseStrShort() throws ExpressionException {
		Instant noSubMinutes = OffsetDateTime.of(2021, 5, 27, 14,
				7, 0, 0, ZoneOffset.ofHours(2)).toInstant();
		Instant noSubSeconds = OffsetDateTime.of(2021, 5, 27, 14,
				7, 47, 0, ZoneOffset.ofHours(2)).toInstant();
		Instant noSubMillis = OffsetDateTime.of(2021, 5, 27, 14,
				7, 47, 300_000_000, ZoneOffset.ofHours(2)).toInstant();
		assertEquals(noSubMinutes, dateParseString("2021-05-27T14:07+02:00"));
		assertEquals(noSubMinutes, dateParseString("2021-05-27 14:07+02:00"));
		assertEquals(noSubMinutes, dateParseString("2021-05-27T14:07+12:00[CET]"));
		assertEquals(noSubMinutes, dateParseString("2021-05-2714:07+12:00[CET]"));
		assertEquals(noSubMinutes, dateParseString("2021-05-27T14:07[CET]"));
		assertEquals(noSubSeconds, dateParseString("2021-05-27 14:07:47[CET]"));
		assertEquals(noSubMillis, dateParseString("2021-05-2714:07:47.3[CET]"));
	}

	@Test
	public void dateParseStrCustomPattern() throws ExpressionException {
		Instant expected = ZonedDateTime.of(2021, 2, 21, 11,
				12, 34, 0, ZoneId.of("CET")).toInstant();
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		assertEquals(expected,
				dateParseString("Feb 21, 2021 11:12:34 AM CET", pattern));
		assertEquals(expected.plusNanos(123_400_000),
				dateParseString("Feb 21, 2021 11:12:34.1234 AM CET", pattern));
		assertEquals(expected.plusNanos(100_000_000),
				dateParseString("Feb 21, 2021 11:12:34.1 AM CET", pattern));
		assertEquals(expected.plusNanos(123_456_789),
				dateParseString("Feb 21, 2021 11:12:34.123456789 AM CET", pattern));
	}

	@Test
	public void dateParseStrCustomPatternAndLocale() throws ExpressionException {
		Instant expected = ZonedDateTime.of(2021, 2, 21, 11,
				12, 34, 0, ZoneId.of("CET")).toInstant();
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		assertEquals(expected.plusNanos(123_400_000),
				dateParseString("févr. 21, 2021 11:12:34.1234 AM CET", pattern, "fr"));
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrInvalidInstantOne() throws ExpressionException {
		// fails because '-' has been replaced with 'x'
		dateParseString("2021x05-27T14:07:47.332891+02:00");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrInvalidInstantTwo() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		// fails because french locale is not set
		dateParseString("févr. 21, 2021 11:12:34.1234 AM CET", pattern);
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrInvalidInstantThree() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		// fails because ':' have been replaced with '-'
		dateParseString("févr. 21, 2021 11-12-34.1234 AM CET", pattern, "fr");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrInvalidPatternOne() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		// fails time zone is missing
		dateParseString("Feb 21, 2021 11:12:34.1234 AM", pattern);
	}

	@Test (expected = FunctionInputException.class)
	public void dateParseStrInvalidPatternTwo() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a V";
		// fails because of syntax error in pattern ('V' instead of 'VV')
		dateParseString("févr. 21, 2021 11:12:34.1234 AM CET", pattern, "fr");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrWrongLocale() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		dateParseString("Feb 21, 2021 11:12:34.1234 AM CET", pattern, "fr");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrWrongTypeLocale() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		AntlrParserTestUtils
				.getExpression("date_parse_str(\"Feb 21, 2021 11:12:34.1234 AM CET\", \""
						+ pattern + "\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrWrongTypeInstantOne() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_parse_str(1)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrWrongTypeInstant() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		AntlrParserTestUtils
				.getExpression("date_parse_str(MISSING_DATE_TIME, \""
						+ pattern + "\", \"us\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrWrongTypePattern() throws ExpressionException {
		AntlrParserTestUtils
				.getExpression("date_parse_str(\"Feb 21, 2021 11:12:34.1234 AM CET\", 1, \"us\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrTooManyArguments() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		AntlrParserTestUtils
				.getExpression("date_parse_str(\"Feb 21, 2021 11:12:34.1234 AM CET\", \""
						+ pattern + "\", MISSING_NOMINAL, MISSING_NOMINAL)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateParseStrNoArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_parse_str()");
	}

	@Test
	public void dateParseStrMissingLocale() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		Expression expression = AntlrParserTestUtils
				.getExpression("date_parse_str(\"Feb 21, 2021 11:12:34.1234 AM CET\", \""
						+ pattern + "\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateParseStrMissingPatternOne() throws ExpressionException {
		Expression expression = AntlrParserTestUtils
				.getExpression("date_parse_str(\"Feb 21, 2021 11:12:34.1234 AM CET\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateParseStrMissingPatternTwo() throws ExpressionException {
		Expression expression = AntlrParserTestUtils
				.getExpression("date_parse_str(\"Feb 21, 2021 11:12:34.1234 AM CET\", MISSING_NOMINAL, \"us\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateParseStrMissingInstantOne() throws ExpressionException {
		Expression expression = AntlrParserTestUtils
				.getExpression("date_parse_str(MISSING_NOMINAL)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateParseStrMissingInstantTwo() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		Expression expression = AntlrParserTestUtils
				.getExpression("date_parse_str(MISSING_NOMINAL, \""
						+ pattern + "\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void dateParseStrMissingInstantThree() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a VV";
		Expression expression = AntlrParserTestUtils
				.getExpression("date_parse_str(MISSING_NOMINAL, \""
						+ pattern + "\", \"fr\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	// time_parse_str
	@Test
	public void timeParseStr() throws ExpressionException {
		assertEquals(LocalTime.of(14, 7, 47, 332891000),
				timeParseString("14:07:47.332891"));
	}

	@Test
	public void timeParseStrDynamicOne() throws ExpressionException {
		LocalTime expected = LocalTime.of(11, 12, 34, 123_400_000);
		String time = "11:12:34.1234";
		assertEquals(expected, timeParseStringDynamic(time, false));
		assertEquals(expected, timeParseStringDynamic(time, true));
	}

	@Test
	public void timeParseStrDynamicTwo() throws ExpressionException {
		LocalTime expected = LocalTime.of(11, 12, 34, 123_400_000);

		String time = "11:12:34.1234 AM";
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";

		assertEquals(expected, timeParseStringDynamic(time, false, pattern, false));
		assertEquals(expected, timeParseStringDynamic(time, false, pattern, true));
		assertEquals(expected, timeParseStringDynamic(time, true, pattern, false));
		assertEquals(expected, timeParseStringDynamic(time, true, pattern, true));
	}

	@Test
	public void timeParseStrDynamicThree() throws ExpressionException {
		LocalTime expected = LocalTime.of(11, 12, 34, 123_400_000);

		String time = "11:12:34.1234 上午";
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		String locale = "zh";

		assertEquals(expected, timeParseStringDynamic(time, false, pattern, false, locale, false));
		assertEquals(expected, timeParseStringDynamic(time, false, pattern, false, locale, true));
		assertEquals(expected, timeParseStringDynamic(time, false, pattern, true, locale, false));
		assertEquals(expected, timeParseStringDynamic(time, false, pattern, true, locale, true));
		assertEquals(expected, timeParseStringDynamic(time, true, pattern, false, locale, false));
		assertEquals(expected, timeParseStringDynamic(time, true, pattern, false, locale, true));
		assertEquals(expected, timeParseStringDynamic(time, true, pattern, true, locale, false));
		assertEquals(expected, timeParseStringDynamic(time, true, pattern, true, locale, true));
	}


	@Test
	public void timeParseStrShort() throws ExpressionException {
		assertEquals(LocalTime.of(14, 7), timeParseString("14:07"));
		assertEquals(LocalTime.of(14, 7, 47), timeParseString("14:07:47"));
		assertEquals(LocalTime.of(14, 7, 47, 300_000_000), timeParseString("14:07:47.3"));
	}

	@Test
	public void timeParseStrCustomPattern() throws ExpressionException {
		LocalTime expected = LocalTime.of(11, 12, 34, 0);
		String pattern = "H~mm~ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		assertEquals(expected, timeParseString("11~12~34 AM", pattern));
		assertEquals(expected.plusNanos(123_400_000), timeParseString("11~12~34.1234 AM", pattern));
		assertEquals(expected.plusNanos(100_000_000), timeParseString("11~12~34.1 AM", pattern));
		assertEquals(expected.plusNanos(123_456_789), timeParseString("11~12~34.123456789 AM", pattern));
	}

	@Test
	public void timeParseStrCustomPatternAndLocale() throws ExpressionException {
		String pattern = "H~mm~ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		assertEquals(LocalTime.of(11, 12, 34, 123_400_000),
				timeParseString("11~12~34.1234 AM", pattern, "es"));
		// for java 9+ we need to use:
		// timeParseString("11~12~34.1234 a. m.", pattern, "es"));
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrInvalidInstantOne() throws ExpressionException {
		// fails because '-' has been replaced with 'x'
		timeParseString("14x07:47.332891");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrInvalidInstantTwo() throws ExpressionException {
		String pattern = "H~mm~ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		// fails because spanish locale is not set
		timeParseString("11:12:34.1234 a. m.", pattern);
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrInvalidInstantThree() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		// fails because ':' have been replaced with '-'
		timeParseString("11-12-34.1234 AM", pattern, "us");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrInvalidPatternOne() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a V";
		// fails because of single V instead of VV
		timeParseString("11:12:34.1234 AM CET", pattern);
	}

	@Test (expected = FunctionInputException.class)
	public void timeParseStrInvalidPatternTwo() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] b";
		// fails because of syntax error in pattern ('b' instead of 'a')
		timeParseString("11:12:34.1234 AM", pattern, "fr");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrWrongLocale() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		timeParseString("11:12:34.1234 AM", pattern, "zh");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrWrongTypeLocale() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		AntlrParserTestUtils
				.getExpression("time_parse_str(\"11:12:34.1234 AM\", \""
						+ pattern + "\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrWrongTypeTimeOne() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_parse_str(1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrWrongTypeTimeTwo() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]";
		AntlrParserTestUtils
				.getExpression("time_parse_str(MISSING_TIME, \""
						+ pattern + "\", \"us\")");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrWrongTypePattern() throws ExpressionException {
		AntlrParserTestUtils
				.getExpression("time_parse_str(\"11:12:34.1234 AM\", 1, \"us\")");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrTooManyArguments() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		AntlrParserTestUtils
				.getExpression("time_parse_str(\"11:12:34.1234 AM\", \""
						+ pattern + "\", MISSING_NOMINAL, MISSING_NOMINAL)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeParseStrNoArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_parse_str()");
	}

	@Test
	public void timeParseStrMissingLocale() throws ExpressionException {
		String pattern = "MMM dd, yyyy H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]";
		Expression expression = AntlrParserTestUtils
				.getExpression("time_parse_str(\"11:12:34.1234\", \""
						+ pattern + "\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeParseStrMissingPatternOne() throws ExpressionException {
		Expression expression = AntlrParserTestUtils
				.getExpression("time_parse_str(\"11:12:34.1234 AM\", MISSING_NOMINAL)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeParseStrMissingPatternTwo() throws ExpressionException {
		Expression expression = AntlrParserTestUtils
				.getExpression("time_parse_str(\"11:12:34.1234\", MISSING_NOMINAL, \"us\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeParseStrMissingTimeOne() throws ExpressionException {
		Expression expression = AntlrParserTestUtils
				.getExpression("time_parse_str(MISSING_NOMINAL)");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeParseStrMissingTimeTwo() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S] a";
		Expression expression = AntlrParserTestUtils
				.getExpression("time_parse_str(MISSING_NOMINAL, \""
						+ pattern + "\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertNull(expression.evaluateLocalTime());
	}

	@Test
	public void timeParseStrMissingTimeThree() throws ExpressionException {
		String pattern = "H:mm:ss[.SSSSSSSSS][.SSSSSSSS]" +
				"[.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]";
		Expression expression = AntlrParserTestUtils
				.getExpression("time_parse_str(MISSING_NOMINAL, \""
						+ pattern + "\", \"fr\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		assertNull(expression.evaluateLocalTime());
	}

	// time_str
	@Test
	public void timeStr() throws ExpressionException {
		LocalTime time = LocalTime.of(13, 33, 0, 190);
		assertEquals("13:33:00.00000019", timeString(time));
		assertEquals("00:00:00", timeString(LocalTime.MIDNIGHT));

		assertEquals("01:33:00.000000190 PM", timeString(time, "hh:mm:ss.SSSSSSSSS a"));
		assertEquals("12:00:00.000000000 AM", timeString(LocalTime.MIDNIGHT, "hh:mm:ss.SSSSSSSSS a"));
		assertEquals("13_00", timeString(time, "HH_ss"));
		assertEquals("01:33:00", timeString(time, "hh:mm:ss"));
		assertEquals("12:00:00.000000000 AM", timeString(LocalTime.MIDNIGHT,
				"hh:mm:ss.SSSSSSSSS a", "fr"));
	}

	@Test
	public void timeStrDynamicOne() throws ExpressionException {
		String expected = "00:00:00";
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, false));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, true));
	}

	@Test
	public void timeStrDynamicTwo() throws ExpressionException {
		String expected = "00:00:00.000000000 AM";
		String pattern = "HH:mm:ss.SSSSSSSSS a";

		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, false, pattern, false));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, false, pattern, true));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, true, pattern, false));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, true, pattern, true));
	}

	@Test
	public void timeStrDynamicThree() throws ExpressionException {
		String expected = "00:00:00.000000000 上午";
		String pattern = "HH:mm:ss.SSSSSSSSS a";
		String locale = "zh";

		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, false, pattern, false, locale, false));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, false, pattern, false, locale, true));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, false, pattern, true, locale, false));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, false, pattern, true, locale, true));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, true, pattern, false, locale, false));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, true, pattern, false, locale, true));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, true, pattern, true, locale, false));
		assertEquals(expected, timeStringDynamic(LocalTime.MIDNIGHT, true, pattern, true, locale, true));
	}

	@Test
	public void timeStrMissingInstant() throws ExpressionException {
		assertNull(timeString(null));
		assertNull(timeString(null, "MMM"));
		assertNull(timeString(null, "MMM", "fr"));
	}

	@Test
	public void timeStrMissingPattern() throws ExpressionException {
		assertNull(timeString(LocalTime.now(), "MISSING_NOMINAL"));
		assertNull(timeString(LocalTime.now(), "MISSING_NOMINAL", "fr"));
	}

	@Test
	public void timeStrMissingLocale() throws ExpressionException {
		assertNull(timeString(LocalTime.now(), "MMM", "MISSING_NOMINAL"));
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrWrongTypeInstantOne() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str(1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrWrongTypeInstantTwo() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str(1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrWrongTypeInstantThree() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str(1, \"MMM\")");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrWrongTypeInstantFour() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str(1, \"MMM\", \"fr\")");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrWrongTypePatternOne() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str(MISSING_TIME, 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrWrongTypePatternTwo() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str(MISSING_TIME, 1, \"fr\")");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrWrongTypeLocaleTwo() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str(MISSING_TIME, \"MMM\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrInvalidPattern() throws ExpressionException {
		timeString(LocalTime.now(), "Test");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrPatterWithTimeZone() throws ExpressionException {
		timeString(LocalTime.now(), "hh:mm:ss VV");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrMissingArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str()");
	}

	@Test(expected = FunctionInputException.class)
	public void timeStrTooManyArguments() throws ExpressionException {
		AntlrParserTestUtils.getExpression("time_str(time_parse(0), \"hh:mm:ss\", \"de\", 4)");
	}

	// date_str
	@Test
	public void dateStr() throws ExpressionException {
		Instant instant = ZonedDateTime.of(1999, 12, 24,
				13, 33, 0, 190, ZoneId.of("CET")).toInstant();
		assertEquals("1999-12-24T12:33:00.00000019Z", string(instant));
		assertEquals("1970-01-01T00:00:00Z", string(Instant.EPOCH));

		assertEquals("1999-12-24T13:33:00.00000019+01:00",
				string(instant, "CET"));
		assertEquals("1999-12-24T12:33:00.00000019Z",
				string(instant, "UTC"));
		assertEquals("1970-01-01T01:00:00+01:00",
				string(Instant.EPOCH, "CET"));
		assertEquals("1969-12-31T20:00:00-04:00",
				string(Instant.EPOCH, "America/Barbados"));

		assertEquals("Dec 24, 1999 01:33:00.000000190 PM CET",
				string(instant, "CET", "MMM dd, yyyy hh:mm:ss.SSSSSSSSS a VV"));
		assertEquals("Jan 01, 1970 01:00:00.000000000 AM CET", string(Instant.EPOCH,
				"CET", "MMM dd, yyyy hh:mm:ss.SSSSSSSSS a VV"));
		assertEquals("13_Dec_33",
				string(instant, "CET", "HH_MMM_mm"));
		assertEquals("12/24/1999 01:33:00",
				string(instant, "CET", "MM/dd/yyyy hh:mm:ss"));

		assertEquals("janv. 01, 1970 01:00:00.000000000 AM CET", string(Instant.EPOCH,
				"CET", "MMM dd, yyyy hh:mm:ss.SSSSSSSSS a VV", "fr"));
	}

	@Test
	public void dateStrDynamicOne() throws ExpressionException {
		String expected = "1970-01-01T01:00:00+01:00";
		String timeZone = "CET";

		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, false));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, true));
	}

	@Test
	public void dateStrDynamicTwo() throws ExpressionException {
		String expected = "Jan 01, 1970 01:00:00.000000000 AM CET";
		String timeZone = "CET";
		String pattern = "MMM dd, yyyy hh:mm:ss.SSSSSSSSS a VV";

		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, false, pattern, false));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, false, pattern, true));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, true, pattern, false));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, true, pattern, true));
	}

	@Test
	public void dateStrDynamicThree() throws ExpressionException {
		String expected = "janv. 01, 1970 01:00:00.000000000 AM CET";
		String timeZone = "CET";
		String pattern = "MMM dd, yyyy hh:mm:ss.SSSSSSSSS a VV";
		String locale = "fr";

		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, false, pattern, false, locale, false));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, false, pattern, false, locale, true));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, false, pattern, true, locale, false));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, false, pattern, true, locale, true));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, true, pattern, false, locale, false));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, true, pattern, false, locale, true));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, true, pattern, true, locale, false));
		assertEquals(expected, stringDynamic(Instant.EPOCH, timeZone, true, pattern, true, locale, true));
	}

	@Test
	public void dateStrMissingInstant() throws ExpressionException {
		assertNull(string(null));
		assertNull(string(null, "CET"));
		assertNull(string(null, "CET", "MMM"));
		assertNull(string(null, "CET", "MMM", "fr"));
	}

	@Test
	public void dateStrMissingTimeZone() throws ExpressionException {
		assertNull(string(Instant.now(), "MISSING_NOMINAL"));
		assertNull(string(Instant.now(), "MISSING_NOMINAL", "MMM"));
		assertNull(string(Instant.now(), "MISSING_NOMINAL", "MMM", "fr"));
	}

	@Test
	public void dateStrMissingPattern() throws ExpressionException {
		assertNull(string(Instant.now(), "CET", "MISSING_NOMINAL"));
		assertNull(string(Instant.now(), "CET", "MISSING_NOMINAL", "fr"));
	}

	@Test
	public void dateStrMissingLocale() throws ExpressionException {
		assertNull(string(Instant.now(), "CET", "MMM", "MISSING_NOMINAL"));
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypeInstantOne() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(1)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypeInstantTwo() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(1, \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypeInstantThree() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(1, \"CET\", \"MMM\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypeInstantFour() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(1, \"CET\", \"MMM\", \"fr\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypeTimeZoneOne() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(date_now(), 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypeTimeZoneTwo() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(date_now(), 1, \"MMM\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypeTimeZoneThree() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(date_now(), MISSING_DATE_TIME, \"MMM\", \"fr\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypePatternOne() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(date_now(), \"CET\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypePatternTwo() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(date_now(), \"CET\", 1, \"fr\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrWrongTypeLocaleTwo() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(date_now(), \"CET\", \"MMM\", 1)");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrInvalidPattern() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(date_now(), \"CET\", \"Test\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateStrInvalidTimeZone() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_str(date_now(), \"Test\", \"MMM\")");
	}

	// date_to_time
	@Test
	public void dateToTime() throws ExpressionException {
		ZonedDateTime nowDateTime = ZonedDateTime.now(ZoneId.of("CET"));
		ZonedDateTime someDateTime = ZonedDateTime.of(2020, 2,
				12, 19, 5, 0, 1, ZoneId.of("CET"));
		ZonedDateTime sameTimeDifferentDate = ZonedDateTime.of(31, 7,
				28, 19, 5, 0, 1, ZoneId.of("CET"));
		ZonedDateTime plusAnHour = ZonedDateTime.of(2020, 2,
				12, 20, 5, 0, 1, ZoneId.of("CET"));

		assertEquals(nowDateTime.toLocalTime(), getTime(nowDateTime, "CET"));
		assertEquals(getTime(sameTimeDifferentDate, "CET")
				, getTime(someDateTime, "CET"));
		assertEquals(getTime(plusAnHour, "UTC")
				, getTime(someDateTime, "CET"));
	}

	@Test
	public void dateToTimeMissingFirst() throws ExpressionException {
		assertNull(AntlrParserTestUtils.getExpression("date_to_time(MISSING_DATE_TIME, \"CET\")")
				.evaluateLocalTime());
	}

	@Test
	public void dateToTimeMissingSecond() throws ExpressionException {
		assertNull(AntlrParserTestUtils.getExpression("date_to_time(date_now(), MISSING)")
				.evaluateLocalTime());
	}

	@Test(expected = FunctionInputException.class)
	public void dateToTimeTooManyArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_to_time(date_now(), \"CET\", \"CET\")");
	}

	@Test(expected = FunctionInputException.class)
	public void dateToTimeMissingArgs() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_to_time(date_now())");
	}

	@Test(expected = FunctionInputException.class)
	public void dateToTimeInvalidTimeZone() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_to_time(date_now(), \"TEST\")")
				.evaluateLocalTime();
	}

	@Test(expected = FunctionInputException.class)
	public void dateToTimeWrongTypeFirst() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_to_time(\"TEST\", \"TEST\")")
				.evaluateLocalTime();
	}

	@Test(expected = FunctionInputException.class)
	public void dateToTimeWrongTypeSecond() throws ExpressionException {
		AntlrParserTestUtils.getExpression("date_to_time(date_now(), 2)")
				.evaluateLocalTime();
	}

	/**
	 * Calls time_parse_str.
	 */
	private static LocalTime timeParseString(String instant, String pattern, String locale) throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("time_parse_str(\""
				+ instant + "\", \"" + pattern + "\", \"" + locale + "\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		return expression.evaluateLocalTime();
	}

	/**
	 * Calls time_parse_str.
	 */
	private static LocalTime timeParseString(String instant, String pattern) throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("time_parse_str(\""
				+ instant + "\", \"" + pattern + "\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		return expression.evaluateLocalTime();
	}

	/**
	 * Calls time_parse_str.
	 */
	private static LocalTime timeParseString(String instant) throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("time_parse_str(\""
				+ instant + "\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		return expression.evaluateLocalTime();
	}

	/**
	 * Calls date_parse_str.
	 */
	private static Instant dateParseString(String instant, String pattern, String locale) throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_parse_str(\""
				+ instant + "\", \"" + pattern + "\", \"" + locale + "\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		return expression.evaluateInstant();
	}

	/**
	 * Calls date_parse_str.
	 */
	private static Instant dateParseString(String instant, String pattern) throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_parse_str(\""
				+ instant + "\", \"" + pattern + "\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		return expression.evaluateInstant();
	}

	/**
	 * Calls date_parse_str.
	 */
	private static Instant dateParseString(String instant) throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("date_parse_str(\""
				+ instant + "\")");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		return expression.evaluateInstant();
	}

	/**
	 * Calls date_parse_str and creates dynamic variables where needed.
	 */
	private static Instant dateParseStringDynamic(String instant, boolean dynamicInstant, String pattern, boolean dynamicPattern,
												  String locale, boolean dynamicLocale) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addNominal("instant", i -> instant);
		builder.addNominal("pattern", i -> pattern);
		builder.addNominal("locale", i -> locale);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "date_parse_str(" +
				(dynamicInstant ? "instant" : '\"' + instant + '\"') +
				", " +
				(dynamicPattern ? "pattern" : '\"' + pattern + '\"') +
				", " +
				(dynamicLocale ? "locale" : '\"' + locale + '\"') +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateInstant();
	}

	/**
	 * Calls date_parse_str and creates dynamic variables where needed.
	 */
	private static Instant dateParseStringDynamic(String instant, boolean dynamicInstant,
												  String pattern, boolean dynamicPattern) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addNominal("instant", i -> instant);
		builder.addNominal("pattern", i -> pattern);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "date_parse_str(" +
				(dynamicInstant ? "instant" : '\"' + instant + '\"') +
				", " +
				(dynamicPattern ? "pattern" : '\"' + pattern + '\"') +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateInstant();
	}

	/**
	 * Calls date_str.
	 */
	private static String string(Instant instant) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> instant);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("date_str(dateTime)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls date_str.
	 */
	private static String string(Instant instant, String timeZone) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> instant);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse(("date_str(dateTime, \"" + timeZone + "\")")
				.replace("\"MISSING_NOMINAL\"", "MISSING_NOMINAL"));
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls date_str.
	 */
	private static String string(Instant instant, String timeZone, String pattern) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> instant);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse(("date_str(dateTime, \"" + timeZone + "\", \""
				+ pattern + "\")").replace("\"MISSING_NOMINAL\"", "MISSING_NOMINAL"));
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls date_str.
	 */
	private static String string(Instant instant, String timeZone, String pattern, String locale) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> instant);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse(("date_str(dateTime, \"" + timeZone + "\", \""
				+ pattern + "\", \"" + locale + "\")").replace("\"MISSING_NOMINAL\"", "MISSING_NOMINAL"));
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls time_str.
	 */
	private static String timeString(LocalTime time) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("time_str(time)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls time_str.
	 */
	private static String timeString(LocalTime time, String pattern) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse(("time_str(time, \""
				+ pattern + "\")").replace("\"MISSING_NOMINAL\"", "MISSING_NOMINAL"));
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls time_str.
	 */
	private static String timeString(LocalTime time, String pattern, String locale) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse(("time_str(time, \""
				+ pattern + "\", \"" + locale + "\")").replace("\"MISSING_NOMINAL\"", "MISSING_NOMINAL"));
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls date_str and creates dynamic variables where needed.
	 */
	private static String stringDynamic(Instant instant, String timeZone, boolean dynamicTimeZone,
										String pattern, boolean dynamicPattern,
										String locale, boolean dynamicLocale) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("instant", i -> instant);
		builder.addNominal("timeZone", i -> timeZone);
		builder.addNominal("pattern", i -> pattern);
		builder.addNominal("locale", i -> locale);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "date_str(instant, " +
				(dynamicTimeZone ? "timeZone" : '\"' + timeZone + '\"') +
				", " +
				(dynamicPattern ? "pattern" : '\"' + pattern + '\"') +
				", " +
				(dynamicLocale ? "locale" : '\"' + locale + '\"') +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls date_str and creates dynamic variables where needed.
	 */
	private static String stringDynamic(Instant instant, String timeZone, boolean dynamicTimeZone,
										String pattern, boolean dynamicPattern) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("instant", i -> instant);
		builder.addNominal("timeZone", i -> timeZone);
		builder.addNominal("pattern", i -> pattern);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "date_str(instant, " +
				(dynamicTimeZone ? "timeZone" : '\"' + timeZone + '\"') +
				", " +
				(dynamicPattern ? "pattern" : '\"' + pattern + '\"') +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls date_str and creates dynamic variables where needed.
	 */
	private static String stringDynamic(Instant instant, String timeZone, boolean dynamicTimeZone) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("instant", i -> instant);
		builder.addNominal("timeZone", i -> timeZone);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "date_str(instant, " +
				(dynamicTimeZone ? "timeZone" : '\"' + timeZone + '\"') +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls time_str and creates dynamic variables where needed.
	 */
	private static String timeStringDynamic(LocalTime time, boolean dynamicTime, String pattern, boolean dynamicPattern,
											String locale, boolean dynamicLocale) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		builder.addNominal("pattern", i -> pattern);
		builder.addNominal("locale", i -> locale);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_str(" +
				(dynamicTime ? "time" : "time_parse(" + time.toNanoOfDay() + ")") +
				", " +
				(dynamicPattern ? "pattern" : '\"' + pattern + '\"') +
				", " +
				(dynamicLocale ? "locale" : '\"' + locale + '\"') +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls time_str and creates dynamic variables where needed.
	 */
	private static String timeStringDynamic(LocalTime time, boolean dynamicTime,
											String pattern, boolean dynamicPattern) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		builder.addNominal("pattern", i -> pattern);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_str(" +
				(dynamicTime ? "time" : "time_parse(" + time.toNanoOfDay() + ")") +
				", " +
				(dynamicPattern ? "pattern" : '\"' + pattern + '\"') + ")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls time_str and creates dynamic variables where needed.
	 */
	private static String timeStringDynamic(LocalTime time, boolean dynamicTime) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addTime("time", i -> time);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_str(" +
				(dynamicTime ? "time" : "time_parse(" + time.toNanoOfDay() + ")") + ")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateNominal();
	}

	/**
	 * Calls time_parse_str and creates dynamic variables where needed.
	 */
	private static LocalTime timeParseStringDynamic(String time, boolean dynamicTime, String pattern, boolean dynamicPattern,
												  String locale, boolean dynamicLocale) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addNominal("time", i -> time);
		builder.addNominal("pattern", i -> pattern);
		builder.addNominal("locale", i -> locale);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_parse_str(" +
				(dynamicTime ? "time" : '\"' + time + '\"') +
				", " +
				(dynamicPattern ? "pattern" : '\"' + pattern + '\"') +
				", " +
				(dynamicLocale ? "locale" : '\"' + locale + '\"') +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateLocalTime();
	}

	/**
	 * Calls time_parse_str and creates dynamic variables where needed.
	 */
	private static LocalTime timeParseStringDynamic(String time, boolean dynamicTime,
													String pattern, boolean dynamicPattern) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addNominal("time", i -> time);
		builder.addNominal("pattern", i -> pattern);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_parse_str(" +
				(dynamicTime ? "time" : '\"' + time + '\"') +
				", " +
				(dynamicPattern ? "pattern" : '\"' + pattern + '\"') +
				")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateLocalTime();
	}

	/**
	 * Calls time_parse_str and creates dynamic variables where needed.
	 */
	private static LocalTime timeParseStringDynamic(String time, boolean dynamicTime) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addNominal("time", i -> time);
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));

		String expString = "time_parse_str(" +
				(dynamicTime ? "time" : '\"' + time + '\"') + ")";
		Expression expression = parser.parse(expString);
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateLocalTime();
	}

	/**
	 * Calls date_to_time.
	 */
	private static LocalTime getTime(ZonedDateTime dateTime, String timeZone) throws ExpressionException {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addDateTime("dateTime", i -> dateTime.toInstant());
		ExpressionParser parser = AntlrParserTestUtils.getParser(builder.build(new SequentialContext()));
		Expression expression = parser.parse("date_to_time(dateTime, \"" + timeZone + "\")");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		parser.getExpressionContext().setIndex(0);
		return expression.evaluateLocalTime();
	}

}
