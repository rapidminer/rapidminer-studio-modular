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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;

import org.junit.Test;

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.tools.belt.expression.FunctionInput;
import com.rapidminer.tools.belt.expression.TableResolver;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;
import com.rapidminer.tools.belt.expression.internal.function.AntlrParserTestUtils;


/**
 * Tests for {@link TableResolver}.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TableResolverTest {

	private static final double DELTA = 1e-15;

	@Test
	public void getAllVariablesTest() {
		TableBuilder tableBuilder = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable());
		Table table = tableBuilder.addMetaData("real", ColumnRole.WEIGHT).build(Belt.defaultContext());
		TableResolver resolver = new TableResolver(table);

		// add additional column to resolver
		NumericBuffer numericBuffer = Buffers.realBuffer(table.height());
		for (int i = 0; i < numericBuffer.size(); i++) {
			numericBuffer.set(i, i);
		}
		resolver.addColumn("newColumn", numericBuffer.toColumn());

		// override additional column in resolver
		NominalBuffer nominalBuffer = Buffers.nominalBuffer(table.height());
		for (int i = 0; i < nominalBuffer.size(); i++) {
			nominalBuffer.set(i, "" + i);
		}
		Column newAdditionalColumn = nominalBuffer.toColumn();
		resolver.addColumn("newColumn", newAdditionalColumn);

		// add (new) additional column to table
		tableBuilder = Builders.newTableBuilder(table);
		tableBuilder.add("newColumn", newAdditionalColumn);
		Table newTable = tableBuilder.build(Belt.defaultContext());

		// test if all variables are there with correct types
		Collection<FunctionInput> allVariables = resolver.getAllVariables();
		assertEquals(newTable.width(), allVariables.size());
		for (FunctionInput input : allVariables) {
			assertNotNull(newTable.column(input.getName()));
			assertEquals(ExpressionParserUtils.expressionTypeForColumnId(
					newTable.column(input.getName()).type().id()), input.getType());
		}
	}

	@Test
	public void getVariableTypeTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		TableResolver resolver = new TableResolver(table);

		// add additional column to resolver
		NumericBuffer numericBuffer = Buffers.realBuffer(table.height());
		for (int i = 0; i < numericBuffer.size(); i++) {
			numericBuffer.set(i, i);
		}
		resolver.addColumn("newColumn", numericBuffer.toColumn());

		// override additional column in resolver
		NominalBuffer nominalBuffer = Buffers.nominalBuffer(table.height());
		for (int i = 0; i < nominalBuffer.size(); i++) {
			nominalBuffer.set(i, "" + i);
		}
		Column newAdditionalColumn = nominalBuffer.toColumn();
		resolver.addColumn("newColumn", newAdditionalColumn);

		// add (new) additional column to table
		TableBuilder tableBuilder = Builders.newTableBuilder(table);
		tableBuilder.add("newColumn", newAdditionalColumn);
		Table newTable = tableBuilder.build(Belt.defaultContext());

		// test variable types
		for (String label : newTable.labels()) {
			assertEquals(ExpressionParserUtils.expressionTypeForColumnId(newTable.column(label).type().id()),
					resolver.getVariableType(label));
		}
	}

	@Test
	public void getDoubleValueTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		NumericReader realReader = Readers.numericReader(table.column("real"));
		NumericReader integerReader = Readers.numericReader(table.column("integer"));
		TableResolver resolver = new TableResolver(AntlrParserTestUtils.getAllTypesTable());
		int height = table.height();
		for (int i = 0; i < height; i++) {
			assertEquals(realReader.read(),
					resolver.getDoubleValue("real", i), DELTA);
			integerReader.setPosition(height - 2 - i);
			assertEquals(integerReader.read(),
					resolver.getDoubleValue("integer", height - 1 - i), DELTA);
		}
	}

	@Test
	public void getStringValueTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		ObjectReader<String> nominalReader = Readers.objectReader(table.column("nominal"), String.class);
		ObjectReader<String> textReader = Readers.objectReader(table.column("text"), String.class);
		TableResolver resolver = new TableResolver(AntlrParserTestUtils.getAllTypesTable());
		int height = table.height();
		for (int i = 0; i < height; i++) {
			assertEquals(nominalReader.read(), resolver.getStringValue("nominal", i));
			textReader.setPosition(height - 2 - i);
			assertEquals(textReader.read(), resolver.getStringValue("text", height - 1 - i));
		}
	}

	@Test
	public void getInstantValueTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		ObjectReader<Instant> instantReader = Readers.objectReader(table.column("date-time"), Instant.class);
		TableResolver resolver = new TableResolver(AntlrParserTestUtils.getAllTypesTable());
		int height = table.height();
		for (int i = 0; i < height; i++) {
			assertEquals(instantReader.read(), resolver.getInstantValue("date-time", i));
		}
	}

	@Test
	public void getLocalTimeValueTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		ObjectReader<LocalTime> timeReader = Readers.objectReader(table.column("time"), LocalTime.class);
		TableResolver resolver = new TableResolver(AntlrParserTestUtils.getAllTypesTable());
		int height = table.height();
		for (int i = 0; i < height; i++) {
			assertEquals(timeReader.read(), resolver.getLocalTimeValue("time", i));
		}
	}

	@Test
	public void getStringSetValueTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		ObjectReader<StringSet> stringSetReader = Readers.objectReader(table.column("text-set"), StringSet.class);
		TableResolver resolver = new TableResolver(AntlrParserTestUtils.getAllTypesTable());
		int height = table.height();
		for (int i = 0; i < height; i++) {
			assertEquals(stringSetReader.read(), resolver.getStringSetValue("text-set", i));
		}
	}

	@Test
	public void getStringListValueTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		ObjectReader<StringList> stringSetReader = Readers.objectReader(table.column("text-list"), StringList.class);
		TableResolver resolver = new TableResolver(AntlrParserTestUtils.getAllTypesTable());
		int height = table.height();
		for (int i = 0; i < height; i++) {
			assertEquals(stringSetReader.read(), resolver.getStringListValue("text-list", i));
		}
	}

	@Test (expected = IllegalStateException.class)
	public void wrongTypeDoubleTest() {
		new TableResolver(AntlrParserTestUtils.getAllTypesTable()).getDoubleValue("date-time", 0);
	}

	@Test (expected = IllegalStateException.class)
	public void wrongTypeStringTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		new TableResolver(table).getStringValue("text-list", table.height() - 1);
	}

	@Test (expected = IllegalStateException.class)
	public void wrongTypeInstantTest() {
		new TableResolver(AntlrParserTestUtils.getAllTypesTable()).getInstantValue("time", 0);
	}

	@Test (expected = IllegalStateException.class)
	public void wrongTypeLocalTimeTest() {
		new TableResolver(AntlrParserTestUtils.getAllTypesTable()).getLocalTimeValue("integer", -1);
	}

	@Test (expected = IllegalStateException.class)
	public void wrongTypeStringSetTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		new TableResolver(table).getStringSetValue("nominal", table.height());
	}

	@Test (expected = IllegalStateException.class)
	public void wrongTypeStringListTest() {
		new TableResolver(AntlrParserTestUtils.getAllTypesTable()).getStringListValue("text-set", 1);
	}

	@Test
	public void indexOutOfBoundsTest() {
		Table table = AntlrParserTestUtils.getAllTypesTable();
		TableResolver resolver = new TableResolver(table);

		// add additional column to resolver
		NumericBuffer numericBuffer = Buffers.realBuffer(table.height());
		for (int i = 0; i < numericBuffer.size(); i++) {
			numericBuffer.set(i, i);
		}
		Column newAdditionalColumn = numericBuffer.toColumn();
		resolver.addColumn("newColumn", newAdditionalColumn);

		assertEquals(Double.NaN, resolver.getDoubleValue("real", -1), DELTA);
		assertEquals(Double.NaN, resolver.getDoubleValue("real", table.height()), DELTA);

		assertEquals(Double.NaN, resolver.getDoubleValue("integer", -1), DELTA);
		assertEquals(Double.NaN, resolver.getDoubleValue("integer", table.height()), DELTA);

		assertEquals(Double.NaN, resolver.getDoubleValue("newColumn", -1), DELTA);
		assertEquals(Double.NaN, resolver.getDoubleValue("newColumn", table.height()), DELTA);

		assertNull(resolver.getStringValue("nominal", -1));
		assertNull(resolver.getStringValue("nominal", table.height()));

		assertNull(resolver.getStringValue("text", -1));
		assertNull(resolver.getStringValue("text", table.height()));

		assertNull(resolver.getStringSetValue("text-set", -1));
		assertNull(resolver.getStringSetValue("text-set", table.height()));

		assertNull(resolver.getStringListValue("text-list", -1));
		assertNull(resolver.getStringListValue("text-list", table.height()));

		assertNull(resolver.getLocalTimeValue("time", -1));
		assertNull(resolver.getLocalTimeValue("time", table.height()));

		assertNull(resolver.getInstantValue("date-time", -1));
		assertNull(resolver.getInstantValue("date-time", table.height()));
	}

}
