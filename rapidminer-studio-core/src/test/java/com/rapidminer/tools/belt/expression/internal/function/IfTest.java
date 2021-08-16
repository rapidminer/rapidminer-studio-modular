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

import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputException;
import com.rapidminer.tools.belt.expression.internal.function.logical.If;


/**
 * JUnit test for the {@link If}, Function of the antlr ExpressionParser
 *
 * @author Sabrina Kirstein, Kevin Majchrzak
 * @since 9.11
 */
public class IfTest {

	private static final Table testTable = createTestTable();

	@Test
	public void ifConstantConditionBooleanTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(TRUE,TRUE,FALSE)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionBooleanFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(FALSE,TRUE,FALSE)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionNumericTrue() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(1,TRUE,FALSE)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionNumericFalse() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(0,TRUE,FALSE)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test(expected = FunctionInputException.class)
	public void ifConstantConditionNominal() throws ExpressionException {
		AntlrParserTestUtils.getExpression("if(\"test\",TRUE,FALSE)");
	}

	@Test(expected = FunctionInputException.class)
	public void ifConstantConditionDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		parser.parse("if([date-time],TRUE,FALSE)");
	}

	@Test
	public void ifConstantConditionBooleanMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(contains(MISSING_NOMINAL,\"test\"),TRUE,FALSE)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionNumericMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(MISSING_NUMERIC,TRUE,FALSE)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionTrueIfBlockBoolean() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(TRUE,FALSE,4)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionTrueIfBlockBooleanMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(TRUE,contains(MISSING_NOMINAL,\"test\"),4)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionTrueIfBlockInteger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(TRUE,4,FALSE)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(4, expression.evaluateNumerical(), 0);
	}

	@Test
	public void ifConstantConditionTrueIfBlockDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(3==3,4.5,FALSE)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(4.5, expression.evaluateNumerical(), 0);
	}

	@Test
	public void ifConstantConditionTrueIfBlockDoubleMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(3==3,MISSING_NUMERIC,FALSE)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 0);
	}

	@Test
	public void ifConstantConditionTrueIfBlockDate() throws ExpressionException {
		Table table = testTable;
		ObjectReader<Instant> reader = Readers.objectReader(table.column("date-time"), Instant.class);

		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		Expression expression = parser.parse("if(TRUE,[date-time],FALSE)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertEquals(reader.read(), expression.evaluateInstant());
	}

	@Test
	public void ifConstantConditionTrueIfBlockDateMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(TRUE,MISSING_DATE_TIME,FALSE)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void ifConstantConditionTrueIfBlockNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(3==3,\"test\",FALSE)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("test", expression.evaluateNominal());
	}

	@Test
	public void ifConstantConditionTrueIfBlockNominalMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(3==3,MISSING_NOMINAL,FALSE)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void ifConstantConditionFalseElseBlockBoolean() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(FALSE,4,TRUE)");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionFalseElseBlockBooleanMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(FALSE,4,contains(MISSING_NOMINAL,\"test\"))");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertNull(expression.evaluateBoolean());
	}

	@Test
	public void ifConstantConditionFalseElseBlockInteger() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(FALSE,TRUE,4)");
		assertEquals(ExpressionType.INTEGER, expression.getExpressionType());
		assertEquals(4, expression.evaluateNumerical(), 0);
	}

	@Test
	public void ifConstantConditionFalseElseBlockDouble() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(3!=3,FALSE,4.5)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(4.5, expression.evaluateNumerical(), 0);
	}

	@Test
	public void ifConstantConditionFalseElseBlockDoubleMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(3!=3,FALSE,MISSING_NUMERIC)");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 0);
	}

	@Test
	public void ifConstantConditionFalseElseBlockDate() throws ExpressionException {
		Table table = testTable;
		ObjectReader<Instant> reader = Readers.objectReader(table.column("date-time"), Instant.class);

		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		Expression expression = parser.parse("if(FALSE,TRUE,[date-time])");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertEquals(reader.read(), expression.evaluateInstant());
	}

	@Test
	public void ifConstantConditionFalseElseBlockDateMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(FALSE,TRUE,MISSING_DATE_TIME)");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void ifConstantConditionFalseElseBlockNominal() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(3!=3,FALSE,\"test\")");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("test", expression.evaluateNominal());
	}

	@Test
	public void ifConstantConditionFalseElseBlockNominalMissing() throws ExpressionException {
		Expression expression = AntlrParserTestUtils.getExpression("if(3!=3,FALSE,MISSING_NOMINAL)");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	// check return type calculation for dynamic condition

	@Test
	public void ifDynamicConditionIfBlockEqualTypeElseBlock() throws ExpressionException {
		Table table = testTable;
		ObjectReader<Instant> reader = Readers.objectReader(table.column("date-time"), Instant.class);

		ExpressionParser parser = AntlrParserTestUtils.getParser(table);
		Expression expression = parser.parse("if([integer],[date-time],MISSING_DATE_TIME)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertEquals(reader.read(), expression.evaluateInstant());
	}

	@Test
	public void ifDynamicConditionIfBlockIntElseBlockDouble() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if([integer],1,3.4)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(1, expression.evaluateNumerical(), 0);
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockBoolean() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],TRUE,FALSE)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockString() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if([integer],TRUE,\"TEST\")");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("true", expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockInteger() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],TRUE,3)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("3", expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockDouble() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],TRUE,3.5)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("3.5", expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockDoubleInfinity() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],TRUE,INFINITY)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("\u221E", expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockDoubleMinusInfinity() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],TRUE,-INFINITY)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("-\u221E", expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockBooleanElseBlockDate() throws ExpressionException {
		ObjectReader<Instant> reader = Readers.objectReader(testTable.column("date-time"), Instant.class);

		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],TRUE,[date-time])");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(reader.read().toString(), expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockBoolean() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if([integer],\"TEST\",TRUE)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals("TEST", expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockMissingBoolean() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser
				.parse("if(![integer],\"TEST\",contains(MISSING_NOMINAL,MISSING_NOMINAL))");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockMissingDate() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],\"TEST\",MISSING_DATE_TIME)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockMissingNumeric() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],\"TEST\",MISSING_NUMERIC)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockStringElseBlockMissingNominal() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],\"TEST\",MISSING_NOMINAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockMissingBooleanElseBlockString() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if([integer],contains(MISSING_NOMINAL,MISSING_NOMINAL),\"TEST\")");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockMissingDateElseBlockString() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if([integer],MISSING_DATE_TIME,\"TEST\")");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockMissingNumericElseBlockString() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if([integer],MISSING_NUMERIC,\"TEST\")");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void ifDynamicConditionIfBlockMissingNominalElseBlockString() throws ExpressionException {
		ExpressionParser parser = AntlrParserTestUtils.getParser(testTable);
		Expression expression = parser.parse("if(![integer],\"TEST\",MISSING_NOMINAL)");
		parser.getExpressionContext().setIndex(0);
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	private static Table createTestTable(){
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addInt53Bit("integer", i -> 1);
		builder.addDateTime("date-time", i -> Instant.now());
		return builder.build(new SequentialContext());
	}

}
