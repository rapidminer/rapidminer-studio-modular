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
package com.rapidminer.tools.belt.expression.internal.antlr;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalTime;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.MacroHandler;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.DateTimeBuffer;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.buffer.ObjectBuffer;
import com.rapidminer.belt.buffer.TimeBuffer;
import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionParserBuilder;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.MacroResolver;
import com.rapidminer.tools.belt.expression.TableResolver;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for dynamic variables and macros.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class DynamicVariablesAndMacrosTest {

	private static Table table;
	private static ExpressionParser parser;

	@BeforeClass
	public static void setUpForAll() {
		table = makeTable();
		TableResolver resolver = new TableResolver(table);

		MacroHandler handler = new MacroHandler(null);
		handler.addMacro("my macro", "my value");
		handler.addMacro("Number_macro", "5");
		handler.addMacro("Attribute Macro", "numerical");
		handler.addMacro("my\\ {bracket}", "bracket");
		MacroResolver macroResolver = new MacroResolver(handler);

		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		parser = builder.withDynamics(resolver).withScope(macroResolver).build();
	}

	private static Table makeTable() {
		TableBuilder builder = Builders.newTableBuilder(3);

		NominalBuffer nominalBuffer = Buffers.nominalBuffer(3);
		nominalBuffer.set(0, "cat");
		nominalBuffer.set(1, "dog");
		nominalBuffer.set(2, null);
		builder.add("nominal", nominalBuffer.toColumn());

		NumericBuffer numericBuffer = Buffers.realBuffer(3);
		numericBuffer.set(0, Math.random());
		numericBuffer.set(1, Math.random());
		numericBuffer.set(2, Double.NaN);
		builder.add("numerical", numericBuffer.toColumn());

		DateTimeBuffer dateTimeBuffer = Buffers.dateTimeBuffer(3, true);
		dateTimeBuffer.set(0, Instant.now().getEpochSecond(), (int) (Math.random() * 1000));
		dateTimeBuffer.set(1, Instant.now().getEpochSecond(), (int) (Math.random() * 1000));
		dateTimeBuffer.set(2, null);
		builder.add("date_time", dateTimeBuffer.toColumn());

		NumericBuffer realBuffer = Buffers.realBuffer(3);
		realBuffer.set(0, Math.random());
		realBuffer.set(1, Math.random());
		realBuffer.set(2, 0.13445e-12);
		builder.add("real]", realBuffer.toColumn());

		NumericBuffer intBuffer = Buffers.integer53BitBuffer(3);
		intBuffer.set(0, Math.random() * 1000);
		intBuffer.set(1, Math.random() * 1000);
		intBuffer.set(2, Double.NaN);
		builder.add("integer", intBuffer.toColumn());

		TimeBuffer timeBuffer = Buffers.timeBuffer(3);
		timeBuffer.set(0, LocalTime.now());
		timeBuffer.set(1, LocalTime.now());
		timeBuffer.set(2, null);
		builder.add("t[i]m\\e", timeBuffer.toColumn());

		ObjectBuffer<StringSet> textSetBuffer = Buffers.textsetBuffer(3);
		textSetBuffer.set(0, new StringSet(i -> "1." + i, 3));
		textSetBuffer.set(1, new StringSet(i -> "2." + i, 3));
		textSetBuffer.set(2, null);
		builder.add("text_set", textSetBuffer.toColumn());

		ObjectBuffer<StringList> textListBuffer = Buffers.textlistBuffer(3);
		textListBuffer.set(0, new StringList(i -> "1." + i, 3));
		textListBuffer.set(1, new StringList(i -> "2." + i, 3));
		textListBuffer.set(2, null);
		builder.add("text_list", textListBuffer.toColumn());

		ObjectBuffer<String> textBuffer = Buffers.textBuffer(3);
		textBuffer.set(0, "value0");
		textBuffer.set(1, "value1");
		textBuffer.set(2, null);
		builder.add("text", textBuffer.toColumn());

		return builder.build(Belt.defaultContext());
	}

	private Expression getExpressionWithExamplesAndMacros(String expression) throws ExpressionException {
		return parser.parse(expression);
	}

	@Test
	public void nominalAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("nominal");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		ObjectReader<String> reader = Readers.objectReader(table.column("nominal"), String.class);
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateNominal());
		}
	}

	@Test
	public void numericalAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("[numerical]");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		NumericReader reader = Readers.numericReader(table.column("numerical"));
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateNumerical(), 1e-15);
		}
	}

	@Test
	public void realAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("[real\\]]");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		NumericReader reader = Readers.numericReader(table.column("real]"));
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateNumerical(), 1e-15);
		}
	}

	@Test
	public void integerAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("integer");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		NumericReader reader = Readers.numericReader(table.column("integer"));
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateNumerical(), 1e-15);
		}
	}

	@Test
	public void dateTimeAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("[date_time]");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		ObjectReader<Instant> reader = Readers.objectReader(table.column("date_time"), Instant.class);
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateInstant());
		}
	}

	@Test
	public void timeAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("[t\\[i\\]m\\\\e]");
		assertEquals(ExpressionType.LOCAL_TIME, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		ObjectReader<LocalTime> reader = Readers.objectReader(table.column("t[i]m\\e"), LocalTime.class);
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateLocalTime());
		}
	}

	@Test
	public void textSetAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("text_set");
		assertEquals(ExpressionType.STRING_SET, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		ObjectReader<StringSet> reader = Readers.objectReader(table.column("text_set"), StringSet.class);
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateStringSet());
		}
	}

	@Test
	public void textListAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("text_list");
		assertEquals(ExpressionType.STRING_LIST, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		ObjectReader<StringList> reader = Readers.objectReader(table.column("text_list"), StringList.class);
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateStringList());
		}
	}

	@Test
	public void textAttribute() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("text");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		ObjectReader<String> reader = Readers.objectReader(table.column("text"), String.class);
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateNominal());
		}
	}

	@Test
	public void myMacro() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("%{my macro}");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("my value", expression.evaluateNominal());
	}

	@Test
	public void myBracketMacro() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("%{my\\\\ \\{bracket\\}}");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("bracket", expression.evaluateNominal());
	}

	@Test
	public void numberMacro() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("%{Number_macro}");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("5", expression.evaluateNominal());
	}

	@Test
	public void attributeMacro() throws ExpressionException {
		Expression expression = getExpressionWithExamplesAndMacros("#{Attribute Macro}");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		ExpressionContext expressionContext = parser.getExpressionContext();
		NumericReader reader = Readers.numericReader(table.column("numerical"));
		for (int row = 0; row < table.height(); row++) {
			expressionContext.setIndex(row);
			assertEquals(reader.read(), expression.evaluateNumerical(), 1e-15);
		}
	}

	@Test(expected = UnknownScopeConstantException.class)
	public void unknownMacro() throws ExpressionException {
		getExpressionWithExamplesAndMacros("%{unknown}");
	}

	@Test(expected = UnknownDynamicVariableException.class)
	public void unknownAttribute() throws ExpressionException {
		getExpressionWithExamplesAndMacros("[unknown]");
	}

	@Test(expected = UnknownVariableException.class)
	public void unknownVariable() throws ExpressionException {
		getExpressionWithExamplesAndMacros("unknown");
	}

}
