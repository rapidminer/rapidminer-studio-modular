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

import java.time.Instant;
import java.time.LocalTime;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionParserBuilder;
import com.rapidminer.tools.belt.expression.ExpressionRegistry;
import com.rapidminer.tools.belt.expression.MacroResolver;
import com.rapidminer.tools.belt.expression.TableResolver;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;


/**
 * Util methods for testing the AntlrParser.
 * <p>
 * The tests for each function should include at least:
 * <ul>
 * <li>a single correct value</li>
 * <li>two correct values</li>
 * <li>an incorrect value</li>
 * <li>combinations of correct and incorrect types</li>
 * <li>other special cases unique to the function or group of function</li>
 * </ul>
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class AntlrParserTestUtils {

	private static final Table allTypesTable = initAllTypesTable();
	private static final Table missingIntTable = initMissingIntTable();

	/**
	 * @return an expression parsed from the given expression string. Functions, macros and variables are not supported.
	 */
	public static Expression getExpressionWithoutContext(String expression) throws ExpressionException {
		AntlrParser parser = new AntlrParser(null);
		return parser.parse(expression);
	}

	/**
	 * @return an expression parsed from the given expression string using the default modules.
	 */
	public static Expression getExpression(String expression) throws ExpressionException {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		ExpressionParser parser = builder.withModules(ExpressionRegistry.INSTANCE.getAll()).build();
		return parser.parse(expression);
	}

	/**
	 * @return an expression parsed from the given expression string using the default modules and the given
	 * MacroResolver.
	 */
	public static Expression getExpression(String expression, MacroResolver resolver)
			throws ExpressionException {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		ExpressionParser parser = builder.withModules(ExpressionRegistry.INSTANCE.getAll()).withScope(resolver).build();
		return parser.parse(expression);
	}

	/**
	 * @return a parser for the given table using the default modules and a TableResolver.
	 */
	public static ExpressionParser getParser(Table table) {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		return builder.withModules(ExpressionRegistry.INSTANCE.getAll())
				.withDynamics(new TableResolver(table)).build();
	}

	/**
	 * @return a parser for the given table using the given MacroResolver, the default modules and a TableResolver.
	 */
	public static ExpressionParser getParser(Table table, MacroResolver macroResolver) {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		return builder.withModules(ExpressionRegistry.INSTANCE.getAll())
				.withDynamics(new TableResolver(table)).withScope(macroResolver).build();
	}

	/**
	 * @return a table of height one with a single integer column called "integer" with value {@code NaN}.
	 */
	public static Table getMissingIntegerTable() {
		return missingIntTable;
	}

	/**
	 * @return a table of height tree with one column for every possible type. The column names are "real", "integer",
	 * "nominal", "date-time", "time", "text", "text-set" and "text-list".
	 */
	public static Table getAllTypesTable() {
		return allTypesTable;
	}

	private static Table initAllTypesTable() {
		TableBuilder builder = Builders.newTableBuilder(3);
		builder.addInt53Bit("integer", i -> Math.random() * 1000);
		builder.addReal("real", i -> Math.random());
		builder.addNominal("nominal", i -> "" + Math.random());
		builder.addText("text", i -> "" + Math.random());
		builder.addDateTime("date-time", i -> Instant.now().plusSeconds((int) (1000 * Math.random())));
		builder.addTime("time", i -> LocalTime.now().plusSeconds((int) (1000 * Math.random())));
		builder.addTextset("text-set", i -> new StringSet(j -> "" + Math.random(), 3));
		builder.addTextlist("text-list", i -> new StringList(j -> "" + Math.random(), 3));
		// cannot use parallel context because of java bug that leads to deadlock
		// https://bugs.openjdk.java.net/browse/JDK-8143380
		return builder.build(new SequentialContext());
	}

	private static Table initMissingIntTable() {
		TableBuilder builder = Builders.newTableBuilder(1);
		builder.addInt53Bit("integer", i -> Double.NaN);
		return builder.build(new SequentialContext());
	}

}
