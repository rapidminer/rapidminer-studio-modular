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
package com.rapidminer.tools.belt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.gui.processeditor.results.DisplayContext;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;


/**
 * Tests for {@link BeltErrorTools}.
 *
 * @author Kevin Majchrzak
 * @since 9.8.0
 */
public class BeltErrorToolsTest {

	private static final Context CTX = new DisplayContext();

	@Test
	public void testOnlyNumericSuccess() throws UserError {
		BeltErrorTools.onlyNumeric(numericTable(), "origin", null);
	}

	@Test(expected = UserError.class)
	public void testOnlyNumericFail() throws UserError {
		BeltErrorTools.onlyNumeric(mixedTable(), "origin", null);
	}

	@Test
	public void testOnlyNominalSuccess() throws UserError {
		BeltErrorTools.onlyNominal(nominalTable(), "origin", null);
	}

	@Test(expected = UserError.class)
	public void testOnlyNominalFail() throws UserError {
		BeltErrorTools.onlyNominal(mixedTable(), "origin", null);
	}

	@Test
	public void testOnlyFiniteSuccess() throws OperatorException {
		BeltErrorTools.onlyFiniteValues(mixedTable(), true, "origin", CTX, null);
		BeltErrorTools.onlyFiniteValues(missingsTable(), true, "origin", CTX, null);
		BeltErrorTools.onlyFiniteValues(mixedTable(), false, "origin", CTX, null);
	}

	@Test(expected = UserError.class)
	public void testOnlyFiniteFailOnInfinite() throws OperatorException {
		BeltErrorTools.onlyFiniteValues(infiniteTable(), true, "origin", CTX, null);
	}

	@Test(expected = UserError.class)
	public void testOnlyFiniteFailOnMisings() throws OperatorException {
		BeltErrorTools.onlyFiniteValues(missingsTable(), false, "origin", CTX, null);
	}

	@Test
	public void testOnlyNonMissingSuccess() throws OperatorException {
		BeltErrorTools.onlyNonMissingValues(mixedTable(), "origin", CTX, null);
		BeltErrorTools.onlyNonMissingValues(infiniteTable(), "origin", CTX, null);
	}

	@Test(expected = UserError.class)
	public void testOnlyNonMissingFail() throws OperatorException {
		BeltErrorTools.onlyNonMissingValues(missingsTable(), "origin", CTX, null);
	}

	@Test
	public void testNonEmptySuccess() throws UserError {
		BeltErrorTools.nonEmpty(Builders.newTableBuilder(1).addReal("one", i -> 1).build(CTX), null);
	}

	@Test(expected = UserError.class)
	public void testNonEmptyFail() throws UserError {
		BeltErrorTools.nonEmpty(Builders.newTableBuilder(0).addReal("one", i -> 1).build(CTX), null);
	}

	@Test
	public void testHasRegularSuccess() throws UserError {
		BeltErrorTools.hasRegularColumns(mixedTable(), null);
	}

	@Test(expected = UserError.class)
	public void testHasRegularFail() throws UserError {
		Table onlySpecials = mixedTable().columns(Arrays.asList("two", "three"));
		BeltErrorTools.hasRegularColumns(onlySpecials, null);
	}

	@Test
	public void testCompatibleRegular() throws UserError {
		Table table = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addText("four", i -> null).addTime("five", i -> null)
				.addMetaData("four", ColumnRole.ID).addMetaData("five", ColumnRole.SCORE).build(Belt.defaultContext());
		Table schema = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addTextset("four", i -> null).addMetaData("four", ColumnRole.ID)
				.addTime("six", i -> null).addMetaData("six", ColumnRole.LABEL).build(Belt.defaultContext());
		BeltErrorTools.requireCompatibleRegulars(null, table, schema, Tables.ColumnSetRequirement.EQUAL,
				Tables.TypeRequirement.REQUIRE_MATCHING_TYPES, Tables.TypeRequirement.REQUIRE_SUB_DICTIONARIES);
	}

	@Test
	public void testMissingColumn(){
		Table table = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addNominal("three", i -> "val" +
						i).addText("four", i -> null).addTime("five", i -> null)
				.addMetaData("four", ColumnRole.ID).addMetaData("five", ColumnRole.SCORE).build(Belt.defaultContext());
		Table schema = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addTextset("four", i -> null).addMetaData("four", ColumnRole.ID)
				.addTime("six", i -> null).addMetaData("six", ColumnRole.LABEL).build(Belt.defaultContext());
		try {
			BeltErrorTools.requireCompatibleRegulars(null, table, schema, Tables.ColumnSetRequirement.EQUAL,
					Tables.TypeRequirement.REQUIRE_MATCHING_TYPES);
			fail();
		} catch (UserError e) {
			assertEquals("attribute_check.missing_attribute", e.getErrorIdentifier());
		}
	}

	@Test
	public void testWrongColumn() {
		Table table = Builders.newTableBuilder(10).addReal("one", i -> 1).addReal("two2", i -> 2)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addText("four", i -> null).addTime("five", i -> null)
				.addMetaData("four", ColumnRole.ID).addMetaData("five", ColumnRole.SCORE).build(Belt.defaultContext());
		Table schema = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addTextset("four", i -> null).addMetaData("four", ColumnRole.ID)
				.addTime("six", i -> null).addMetaData("six", ColumnRole.LABEL).build(Belt.defaultContext());
		try {
			BeltErrorTools.requireCompatibleRegulars(null, table, schema, Tables.ColumnSetRequirement.EQUAL,
					Tables.TypeRequirement.REQUIRE_MATCHING_TYPES);
			fail();
		} catch (UserError e) {
			assertEquals("attribute_check.misfitting_for_equal", e.getErrorIdentifier());
		}
	}

	@Test
	public void testWrongColumnSubset() {
		Table table = Builders.newTableBuilder(10).addReal("one", i -> 1).addReal("two2", i -> 2)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addText("four", i -> null).addTime("five", i -> null)
				.addMetaData("four", ColumnRole.ID).addMetaData("five", ColumnRole.SCORE).build(Belt.defaultContext());
		Table schema = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addTextset("four", i -> null).addMetaData("four", ColumnRole.ID)
				.addTime("six", i -> null).addMetaData("six", ColumnRole.LABEL).build(Belt.defaultContext());
		try {
			BeltErrorTools.requireCompatibleRegulars(null, table, schema, Tables.ColumnSetRequirement.SUBSET,
					Tables.TypeRequirement.REQUIRE_MATCHING_TYPES);
			fail();
		} catch (UserError e) {
			assertEquals("attribute_check.wrong_for_subset", e.getErrorIdentifier());
		}
	}

	@Test
	public void testWrongType() throws UserError {
		Table table = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addInt53Bit("two", i -> 2).addNominal("three", i -> "val" +
						i).addText("four", i -> null).addTime("five", i -> null)
				.addMetaData("four", ColumnRole.ID).addMetaData("five", ColumnRole.SCORE).build(Belt.defaultContext());
		Table schema = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addTextset("four", i -> null).addMetaData("four", ColumnRole.ID)
				.addTime("six", i -> null).addMetaData("six", ColumnRole.LABEL).build(Belt.defaultContext());
		try {
			BeltErrorTools.requireCompatibleRegulars(null, table, schema, Tables.ColumnSetRequirement.EQUAL,
					Tables.TypeRequirement.REQUIRE_MATCHING_TYPES);
			fail();
		} catch (UserError e) {
			assertEquals("type_check.not_equal", e.getErrorIdentifier());
		}
	}

	@Test
	public void testWrongNumericType() {
		Table table = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addDateTime("two", i -> null).addNominal("three", i -> "val" +
						i).addText("four", i -> null).addTime("five", i -> null)
				.addMetaData("four", ColumnRole.ID).addMetaData("five", ColumnRole.SCORE).build(Belt.defaultContext());
		Table schema = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addTextset("four", i -> null).addMetaData("four", ColumnRole.ID)
				.addTime("six", i -> null).addMetaData("six", ColumnRole.LABEL).build(Belt.defaultContext());
		try {
			BeltErrorTools.requireCompatibleRegulars(null, table, schema, Tables.ColumnSetRequirement.EQUAL,
					Tables.TypeRequirement.REQUIRE_MATCHING_TYPES, Tables.TypeRequirement.ALLOW_INT_FOR_REAL);
			fail();
		} catch (UserError e) {
			assertEquals("type_check.not_subtype", e.getErrorIdentifier());
		}
	}

	@Test
	public void testWrongDict() {
		Table table = Builders.newTableBuilder(10).addReal("one", i -> 1)
				.addInt53Bit("two", i -> i).addNominal("three", i -> "val" +
						i).addText("four", i -> null).addTime("five", i -> null)
				.addMetaData("four", ColumnRole.ID).addMetaData("five", ColumnRole.SCORE).build(Belt.defaultContext());
		Table schema = Builders.newTableBuilder(1).addReal("one", i -> 1)
				.addReal("two", i -> 2).addNominal("three", i -> "val" +
						i).addTextset("four", i -> null).addMetaData("four", ColumnRole.ID)
				.addTime("six", i -> null).addMetaData("six", ColumnRole.LABEL).build(Belt.defaultContext());
		try {
			BeltErrorTools.requireCompatibleRegulars(null, table, schema, Tables.ColumnSetRequirement.EQUAL,
					Tables.TypeRequirement.REQUIRE_MATCHING_TYPES, Tables.TypeRequirement.ALLOW_INT_FOR_REAL, Tables.TypeRequirement.REQUIRE_SUB_DICTIONARIES);
			fail();
		} catch (UserError e) {
			assertEquals("type_check.require_sub_dictionary", e.getErrorIdentifier());
		}
	}

	private static Table numericTable() {
		return Builders.newTableBuilder(10)
				.addReal("one", i -> 1)
				.addReal("two", i -> 2)
				.addMetaData("two", ColumnRole.ID)
				.addMetaData("two", new ColumnAnnotation("Annotation"))
				.addReal("three", i -> 3)
				.addMetaData("three", ColumnRole.BATCH).build(new DisplayContext());
	}

	private static Table nominalTable() {
		return Builders.newTableBuilder(10)
				.addNominal("one", i -> "1")
				.addNominal("two", i -> "2")
				.addMetaData("two", ColumnRole.ID)
				.addMetaData("two", new ColumnAnnotation("Annotation"))
				.addNominal("three", i -> "3")
				.addMetaData("three", ColumnRole.BATCH).build(new DisplayContext());
	}

	private static Table mixedTable() {
		return Builders.newTableBuilder(10)
				.addReal("one", i -> 1)
				.addReal("two", i -> 2)
				.addMetaData("two", ColumnRole.ID)
				.addMetaData("two", new ColumnAnnotation("Annotation"))
				.addReal("three", i -> 3)
				.addMetaData("three", ColumnRole.BATCH)
				.addNominal("nom", i -> "" + i).build(new DisplayContext());
	}

	private static Table missingsTable() {
		return Builders.newTableBuilder(10)
				.addReal("one", i -> 1)
				.addReal("two", i -> i == 2 ? Double.NaN : 2)
				.addMetaData("two", ColumnRole.ID)
				.addMetaData("two", new ColumnAnnotation("Annotation"))
				.addReal("three", i -> 3)
				.addMetaData("three", ColumnRole.BATCH)
				.addNominal("nom", i -> "" + i).build(new DisplayContext());
	}

	private static Table infiniteTable() {
		return Builders.newTableBuilder(10)
				.addReal("one", i -> 1)
				.addReal("two", i -> i == 2 ? Double.NEGATIVE_INFINITY : 2)
				.addMetaData("two", ColumnRole.ID)
				.addMetaData("two", new ColumnAnnotation("Annotation"))
				.addReal("three", i -> 3)
				.addMetaData("three", ColumnRole.BATCH)
				.addNominal("nom", i -> "" + i).build(new DisplayContext());
	}

}
