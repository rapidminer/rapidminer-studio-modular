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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.rapidminer.MacroHandler;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionParserBuilder;
import com.rapidminer.tools.belt.expression.ExpressionRegistry;
import com.rapidminer.tools.belt.expression.MacroResolver;
import com.rapidminer.tools.belt.expression.SyntaxException;
import com.rapidminer.tools.belt.expression.TableResolver;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants;


/**
 * Tests the FunctionExpressionLexer together with the FunctionExpressionParser on various inputs.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class ExpressionParserTest {

	private static final double DELTA = 0.0000001;

	private static final ExpressionParser parser =
			new ExpressionParserBuilder().withModules(ExpressionRegistry.INSTANCE.getAll()).build();

	@Test(expected = SyntaxException.class)
	public void emptyInput() throws ExpressionException {
		parser.parse("");
	}

	@Test
	public void integerInput() throws ExpressionException {
		assertEquals(2378423, parser.parse("2378423").evaluateNumerical(), DELTA);
	}

	@Test(expected = SyntaxException.class)
	public void integerWrongInput() throws ExpressionException {
		parser.parse("2378423)");
	}

	@Test(expected = SyntaxException.class)
	public void integerWrongBracket() throws ExpressionException {
		parser.parse("(2378423))");
	}

	@Test(expected = SyntaxException.class)
	public void exponentWrong() throws ExpressionException {
		parser.parse("2378423e-");
	}

	@Test(expected = SyntaxException.class)
	public void exponentWrongNewline() throws ExpressionException {
		parser.parse("2378423e-\n one + two");
	}

	@Test
	public void exponentRight() throws ExpressionException {
		assertEquals(2378423e-10, parser.parse("2378423e-10").evaluateNumerical(), DELTA);
	}

	@Test
	public void real() throws ExpressionException {
		assertEquals(123.141529, parser.parse("123.141529").evaluateNumerical(), DELTA);
	}

	@Test
	public void realWithoutNonFractionalDigits() throws ExpressionException {
		assertEquals(.141529, parser.parse(".141529").evaluateNumerical(), DELTA);
	}

	@Test
	public void realWithoutNonFractionalDigitsAndExponent() throws ExpressionException {
		assertEquals(.141529e12, parser.parse(".141529e12").evaluateNumerical(), DELTA);
	}

	@Test
	public void realWithNegativeExponent() throws ExpressionException {
		assertEquals(3.141529E-12, parser.parse("3.141529E-12").evaluateNumerical(), DELTA);
	}

	@Test
	public void realWithPositiveExponent() throws ExpressionException {
		assertEquals(3.141529E+12, parser.parse("3.141529E+12").evaluateNumerical(), DELTA);
	}

	@Test(expected = SyntaxException.class)
	public void integerWrong2Input() throws ExpressionException {
		parser.parse("2x");
	}

	@Test
	public void simpleAddition() throws ExpressionException {
		assertEquals(16, parser.parse("2+ 3+11").evaluateNumerical(), DELTA);
	}

	@Test
	public void simpleSubstraction() throws ExpressionException {
		assertEquals(3, parser.parse("11 -3-4- 1").evaluateNumerical(), DELTA);
	}

	@Test
	public void additionSubstraction() throws ExpressionException {
		assertEquals(11, parser.parse("11 +3-4+ 1").evaluateNumerical(), DELTA);
	}

	@Test
	public void multiplicationDivisionModulo() throws ExpressionException {
		assertEquals(0, parser.parse("3*4/5*5%2 *223424").evaluateNumerical(), DELTA);
	}

	@Test
	public void additionMultiplicationMixed() throws ExpressionException {
		assertEquals(-223400, parser.parse("3 +4/5*5%2-1 -223424+ 11*2").evaluateNumerical(), DELTA);
	}

	@Test
	public void rightAssociativityOfPower() throws ExpressionException {
		assertEquals(256, parser.parse("2^ 2^3").evaluateNumerical(), DELTA);
	}

	@Test
	public void unaryPlusMinus() throws ExpressionException {
		assertEquals(-4, parser.parse("3*-5+ -+ -+--11").evaluateNumerical(), DELTA);
	}

	@Test
	public void attributeTest() throws ExpressionException {
		assertEquals(1, parserWithAttributes("att2Blup").parse("att2Blup").evaluateNumerical(), DELTA);
	}

	@Test
	public void bigConstantTest() throws ExpressionException {
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_DAY, parser.parse("DATE_UNIT_DAY").evaluateNominal());
	}

	@Test(expected = SyntaxException.class)
	public void notConstantTest() throws ExpressionException {
		parser.parse("3DATE_UNIT_DATE");
	}

	@Test(expected = SyntaxException.class)
	public void notConstant2Test() throws ExpressionException {
		parser.parse("$DATE_UNIT_DATE");
	}

	@Test
	public void noParameterFunction() throws ExpressionException {
		assertNotNull(parser.parse("date_now()").evaluateInstant());
	}

	@Test
	public void oneParameterFunction() throws ExpressionException {
		assertEquals(Math.cos(1), parser.parse("cos(1 )").evaluateNumerical(), DELTA);
	}

	@Test
	public void nestedFunctions() throws ExpressionException {
		assertEquals(Math.sin(Math.cos(1)), parser.parse("sin(cos(1 ) )").evaluateNumerical(), DELTA);
	}

	@Test
	public void moreParameterFunction() throws ExpressionException {
		assertEquals(4, parser.parse("pow(2,  2)").evaluateNumerical(), DELTA);
	}

	@Test
	public void functionMixedWithCalculation() throws ExpressionException {
		assertEquals(1697, parserWithAttributes("att45")
				.parse("-pow(2,att45)*2+7*3^5").evaluateNumerical(), DELTA);
	}

	@Test
	public void parentheses() throws ExpressionException {
		assertEquals(21, parser.parse("3*(2+5)").evaluateNumerical(), DELTA);
	}

	@Test
	public void superfluousParentheses() throws ExpressionException {
		assertEquals(21, parser.parse("(3*(2+(5)))").evaluateNumerical(), DELTA);
	}

	@Test(expected = SyntaxException.class)
	public void wrongParentheses() throws ExpressionException {
		parser.parse("((3*(2+5))");
	}

	@Test
	public void attributeWithSpaces() throws ExpressionException {
		assertEquals(1, parserWithAttributes("my attribute! \" 1+2 \u0714 \u06dd \u199c $%-_")
				.parse("[my attribute! \" 1+2 \u0714 \u06dd \u199c $%-_]").evaluateNumerical(), DELTA);
	}

	@Test(expected = SyntaxException.class)
	public void attributeWithNewline() throws ExpressionException {
		parser.parse("[my attribute! \n 1+2]");
	}

	@Test(expected = SyntaxException.class)
	public void attributeMissingBracket() throws ExpressionException {
		parser.parse("[my attribute! 1+2");
	}

	@Test(expected = SyntaxException.class)
	public void attributeMissingOpeningBracket() throws ExpressionException {
		parser.parse("my attribute! 1+2]");
	}

	@Test
	public void stringWithNewline() throws ExpressionException {
		assertEquals("bla blup   !", parser.parse("\"bla blup \n !\"").evaluateNominal());

	}

	@Test(expected = SyntaxException.class)
	public void stringWithMissingClosing() throws ExpressionException {
		parser.parse("\"bla blup ?");
	}

	@Test(expected = SyntaxException.class)
	public void stringWithQuotes() throws ExpressionException {
		parser.parse("\"bla blup \" !\"");
	}

	@Test
	public void stringWithEscapedQuotes() throws ExpressionException {
		assertEquals("bla blup \" !", parser.parse("\"bla blup \\\" !\"").evaluateNominal());
	}

	@Test
	public void emptyString() throws ExpressionException {
		assertEquals("", parser.parse("\"\"").evaluateNominal());
	}

	@Test(expected = SyntaxException.class)
	public void StringWithBackslash() throws ExpressionException {
		parser.parse("\"bla\\a \"");
	}

	@Test
	public void StringWithEscapedBackslash() throws ExpressionException {
		assertEquals("bla\\a ", parser.parse("\"bla\\\\a \"").evaluateNominal());
	}

	@Test
	public void StringWithUnicode() throws ExpressionException {
		assertEquals("bla\u234f blup", parser.parse("\"bla\\u234f blup\"").evaluateNominal());
	}

	@Test
	public void macroWithSpaces() throws ExpressionException {
		assertEquals("value", parserWithMacro("my macro[\"3\"]\u0714")
				.parse("%{my macro[\"3\"]\u0714}").evaluateNominal());
	}

	@Test(expected = SyntaxException.class)
	public void macroWithTab() throws ExpressionException {
		parser.parse("%{my macro\t blup}");
	}

	@Test(expected = SyntaxException.class)
	public void macroWithoutClosing() throws ExpressionException {
		parser.parse("%{my macro blup");
	}

	@Test
	public void indirectMacroWithSpaces() throws ExpressionException {
		assertEquals(1, parserWithAttributesAndMacro("my macro[\"3\"]\u0714")
				.parse("#{my macro[\"3\"]\u0714}").evaluateNumerical(), DELTA);
	}

	@Test(expected = SyntaxException.class)
	public void indirectMacroWithTab() throws ExpressionException {
		parser.parse("#{my macro\t blup}");
	}

	@Test(expected = SyntaxException.class)
	public void indirectMacroWithoutClosing() throws ExpressionException {
		parser.parse("#{my macro blup");
	}

	@Test
	public void comparision() throws ExpressionException {
		assertTrue(parserWithAttributes("my attribute")
				.parse("3+4 > [my attribute]").evaluateBoolean());
	}

	@Test
	public void comparisionAndEquality() throws ExpressionException {
		assertTrue(parserWithAttributes("my attribute")
				.parse("TRUE == 3+4 > [my attribute]").evaluateBoolean());
	}

	@Test
	public void comparisionAndEqualityAndAND() throws ExpressionException {
		assertFalse(parserWithAttributesAndMacro("my macro","my attribute")
				.parse("TRUE == 3+4 > [my attribute] && 7< #{my macro}").evaluateBoolean());
	}

	@Test
	public void comparisionAndEqualityAndOR() throws ExpressionException {
		assertTrue(parserWithAttributesAndMacro("my macro","my attribute")
				.parse("TRUE == 3+4 > [my attribute] || 7< #{my macro}").evaluateBoolean());
	}

	@Test
	public void andAndOr() throws ExpressionException {
		assertTrue(parserWithAttributes("my attribute")
				.parse("TRUE || 3>4 && [my attribute]").evaluateBoolean());
	}

	@Test
	public void andAndOrAndNOT() throws ExpressionException {
		assertFalse(parserWithAttributes("my attribute")
				.parse("! TRUE || 3>4 && ![my attribute]").evaluateBoolean());
	}

	@Test(expected = SyntaxException.class)
	public void attributeWithWrongBracket() throws ExpressionException {
		parser.parse("[my attribut]e]");
	}

	@Test
	public void attributeWithEscapedBracket() throws ExpressionException {
		assertEquals(1, parserWithAttributes("my attribut]e")
				.parse("[my attribut\\]e]").evaluateNumerical(), DELTA);
	}

	@Test(expected = SyntaxException.class)
	public void attributeWithWrongOpenBracket() throws ExpressionException {
		parser.parse("[my attribut[e]");
	}

	@Test
	public void attributeWithEscapedOpenBracket() throws ExpressionException {
		assertEquals(1, parserWithAttributes("my attribut[e")
				.parse("[my attribut\\[e]").evaluateNumerical(), DELTA);
	}

	@Test
	public void twoAttributes() throws ExpressionException {
		assertEquals(2, parserWithAttributes("my attribut", "e")
				.parse("[my attribut]+[e]").evaluateNumerical(), DELTA);
	}

	@Test(expected = SyntaxException.class)
	public void attributeWithSingleBackslash() throws ExpressionException {
		parser.parse("[my attribut\\e]");
	}

	@Test(expected = SyntaxException.class)
	public void attributeWithSingleBackslashEnd() throws ExpressionException {
		parser.parse("[my attribut\\]");
	}

	@Test
	public void attributeWithDoubleBackslash() throws ExpressionException {
		assertEquals(1, parserWithAttributes("my attribut\\")
				.parse("[my attribut\\\\]").evaluateNumerical(), DELTA);
	}

	@Test(expected = SyntaxException.class)
	public void macroWithWrongBracket() throws ExpressionException {
		parser.parse("%{my mac}ro}");
	}

	@Test
	public void macroWithEscapedBracket() throws ExpressionException {
		assertEquals("value", parserWithMacro("my mac}ro")
				.parse("%{my mac\\}ro}").evaluateNominal());
	}

	@Test(expected = SyntaxException.class)
	public void macroWithWrongOpenBracket() throws ExpressionException {
		parser.parse("%{my mac{ro}");
	}

	@Test
	public void macroWithEscapedOpenBracket() throws ExpressionException {
		assertEquals("value", parserWithMacro("my mac{ro")
				.parse("%{my mac\\{ro}").evaluateNominal());
	}

	@Test(expected = SyntaxException.class)
	public void macroWithSingleBackslash() throws ExpressionException {
		parser.parse("%{my mac\\ro}");
	}

	@Test
	public void macroWithDoubleBackslash() throws ExpressionException {
		assertEquals("value", parserWithMacro("my mac\\ro")
				.parse("%{my mac\\\\ro}").evaluateNominal());
	}

	private static ExpressionParser parserWithMacro(String macro) {
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro(macro, "value");
		MacroResolver macroResolver = new MacroResolver(handler);
		return new ExpressionParserBuilder().withModules(ExpressionRegistry.INSTANCE.getAll())
				.withScope(macroResolver).build();
	}

	private static ExpressionParser parserWithAttributes(String... attributes){
		TableBuilder tableBuilder = Builders.newTableBuilder(1);
		for(String attribute : attributes){
			tableBuilder.addInt53Bit(attribute, i -> 1);
		}
		Table table = tableBuilder.build(Belt.defaultContext());
		ExpressionParser result = new ExpressionParserBuilder().withModules(ExpressionRegistry.INSTANCE.getAll())
				.withDynamics(new TableResolver(table)).build();
		result.getExpressionContext().setIndex(0);
		return result;
	}

	private static ExpressionParser parserWithAttributesAndMacro(String macro, String... attributes){
		MacroHandler handler = new MacroHandler(null);
		handler.addMacro(macro, "value");
		MacroResolver macroResolver = new MacroResolver(handler);

		TableBuilder tableBuilder = Builders.newTableBuilder(1);
		for(String attribute : attributes){
			tableBuilder.addInt53Bit(attribute, i -> 1);
		}
		tableBuilder.addInt53Bit("value", i -> 1);
		Table table = tableBuilder.build(Belt.defaultContext());
		ExpressionParser result = new ExpressionParserBuilder().withModules(ExpressionRegistry.INSTANCE.getAll())
				.withScope(macroResolver).withDynamics(new TableResolver(table)).build();
		result.getExpressionContext().setIndex(0);
		return result;
	}
}
