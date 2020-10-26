/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.tools.belt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.gui.processeditor.results.DisplayContext;


/**
 * Tests for {@link BeltTools}.
 *
 * @author Kevin Majchrzak
 * @since 9.8.0
 */
public class BeltToolsTest {

	private static final Context CTX = new DisplayContext();

	@Test
	public void regularAndSpecialColumnsTest() {
		Table table = Builders.newTableBuilder(10)
				.addReal("one", i -> 1)
				.addReal("two", i -> 2)
				.addMetaData("two", ColumnRole.ID)
				.addMetaData("two", new ColumnAnnotation("Annotation"))
				.addReal("three", i -> 3)
				.addMetaData("three", ColumnRole.BATCH).build(CTX);

		assertEquals(Arrays.asList("two", "three"), BeltTools.selectSpecialColumns(table).labels());
		assertEquals(Collections.singletonList("one"), BeltTools.selectRegularColumns(table).labels());
		assertEquals(Collections.singletonList("one"), BeltTools.regularSubtable(table).labels());
		assertFalse(BeltTools.isSpecial(table,"one"));
		assertTrue(BeltTools.isSpecial(table,"two"));
		assertTrue(BeltTools.isSpecial(table,"three"));
	}

	@Test
	public void containsColumnTypeTest() {
		Table table = Builders.newTableBuilder(10)
				.addReal("one", i -> 1)
				.addTime("time", i -> LocalTime.NOON).build(CTX);

		assertTrue(BeltTools.containsColumnType(table, Column.TypeId.TIME));
		assertTrue(BeltTools.containsColumnType(table, Column.TypeId.REAL));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.INTEGER_53_BIT));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.TEXT_SET));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.TEXT));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.DATE_TIME));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.NOMINAL));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.TEXT_LIST));

		table = Builders.newTableBuilder(table).addBoolean("boolean", i -> i % 2 == 0 ? "true" : "false",
				"true").build(CTX);

		assertTrue(BeltTools.containsColumnType(table, Column.TypeId.TIME));
		assertTrue(BeltTools.containsColumnType(table, Column.TypeId.REAL));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.INTEGER_53_BIT));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.TEXT_SET));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.TEXT));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.DATE_TIME));
		assertTrue(BeltTools.containsColumnType(table, Column.TypeId.NOMINAL));
		assertFalse(BeltTools.containsColumnType(table, Column.TypeId.TEXT_LIST));

	}

	@Test
	public void advancedTest() {
		Table table = Builders.newTableBuilder(10)
				.addReal("one", i -> 1)
				.addTextset("two", i -> new StringSet(Collections.singletonList("element")))
				.addReal("three", i -> 3).build(CTX);

		assertFalse(BeltTools.isAdvanced(table.column("one").type()));
		assertTrue(BeltTools.isAdvanced(table.column("two").type()));
		assertFalse(BeltTools.isAdvanced(table.column("three").type()));

		assertTrue(BeltTools.hasAdvanced(table));
		assertFalse(BeltTools.hasAdvanced(Builders.newTableBuilder(table).remove("two").build(CTX)));
	}

	@Test
	public void containsMissingValuesTest(){
		Table table = Builders.newTableBuilder(10)
				.addNominal("categorical_missing", i -> i == 0 ? null : "value")
				.addNominal("categorical", i -> "value")
				.addReal("numerical", i -> 2)
				.addReal("numerical_missing", i -> i == 2 ? Double.NaN : 2)
				.addTextset("object", i -> new StringSet(Collections.singletonList("")))
				.addTextset("object_missing", i -> null).build(new DisplayContext());

		assertTrue(BeltTools.containsMissingValues(table.column("categorical_missing")));
		assertTrue(BeltTools.containsMissingValues(table.column("numerical_missing")));
		assertTrue(BeltTools.containsMissingValues(table.column("object_missing")));
		assertFalse(BeltTools.containsMissingValues(table.column("categorical")));
		assertFalse(BeltTools.containsMissingValues(table.column("numerical")));
		assertFalse(BeltTools.containsMissingValues(table.column("object")));

	}


}
