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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionParserBuilder;
import com.rapidminer.tools.belt.expression.ExpressionRegistry;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants;
import com.rapidminer.tools.belt.expression.internal.StandardFunctionsWithConstants;


/**
 * Tests the results of {@link AntlrParser#parse(String)} for constants known by the BasicConstantsResolver or the
 * {@link StandardFunctionsWithConstants}.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class ConstantsTest {

	/**
	 * Parses string expressions into {@link Expression}s if those expressions use constants but
	 * don't use any functions, macros or attributes.
	 */
	private Expression getExpressionWithConstantContext(String expression) throws ExpressionException {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		ExpressionParser parser = builder.withModules(ExpressionRegistry.INSTANCE.getAll()).build();
		return parser.parse(expression);
	}

	@Test
	public void constantTrue() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("true");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void constantTRUE() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("TRUE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertTrue(expression.evaluateBoolean());
	}

	@Test
	public void constantFalse() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("false");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void constantFALSE() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("FALSE");
		assertEquals(ExpressionType.BOOLEAN, expression.getExpressionType());
		assertFalse(expression.evaluateBoolean());
	}

	@Test
	public void constantE() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("e");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.E, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void constantPi() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("pi");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Math.PI, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void constantInfinity() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("INFINITY");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.POSITIVE_INFINITY, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void constantNaN() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("NaN");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void constantNAN() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("NAN");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void constantMissingNumeric() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("MISSING_NUMERIC");
		assertEquals(ExpressionType.DOUBLE, expression.getExpressionType());
		assertEquals(Double.NaN, expression.evaluateNumerical(), 1e-15);
	}

	@Test
	public void constantMissing() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("MISSING");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void constantMissingNominal() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("MISSING_NOMINAL");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertNull(expression.evaluateNominal());
	}

	@Test
	public void constantMissingDate() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("MISSING_DATE_TIME");
		assertEquals(ExpressionType.INSTANT, expression.getExpressionType());
		assertNull(expression.evaluateInstant());
	}

	@Test
	public void constantDateUnitYear() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_YEAR");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_YEAR, expression.evaluateNominal());
	}

	@Test
	public void constantDateUnitMonth() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_MONTH");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_MONTH, expression.evaluateNominal());
	}

	@Test
	public void constantDateUnitWeek() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_WEEK");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_WEEK, expression.evaluateNominal());
	}

	@Test
	public void constantDateUnitDay() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_DAY");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_DAY, expression.evaluateNominal());
	}

	@Test
	public void constantDateUnitHour() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_HOUR");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_HOUR, expression.evaluateNominal());
	}

	@Test
	public void constantDateUnitMinute() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_MINUTE");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_MINUTE, expression.evaluateNominal());
	}

	@Test
	public void constantDateUnitSecond() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_SECOND");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_SECOND, expression.evaluateNominal());
	}

	@Test
	public void constantDateUnitMillisecond() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_MILLISECOND");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_MILLISECOND, expression.evaluateNominal());
	}

	@Test
	public void constantDateUnitNanosecond() throws ExpressionException {
		Expression expression = getExpressionWithConstantContext("DATE_UNIT_NANOSECOND");
		assertEquals(ExpressionType.STRING, expression.getExpressionType());
		assertEquals(ExpressionParserConstants.DATE_TIME_UNIT_NANOSECOND, expression.evaluateNominal());
	}

}
