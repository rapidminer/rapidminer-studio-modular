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

import java.util.Collection;

import org.junit.Test;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.tools.belt.BeltMetaDataTools;
import com.rapidminer.tools.belt.expression.FunctionInput;
import com.rapidminer.tools.belt.expression.TableMetaDataResolver;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;
import com.rapidminer.tools.belt.expression.internal.function.AntlrParserTestUtils;


/**
 * Tests for {@link TableMetaDataResolver}.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TableMetaDataResolverTest {

	@Test
	public void getAllVariablesTest() {
		TableBuilder tableBuilder = Builders.newTableBuilder(AntlrParserTestUtils.getAllTypesTable());
		tableBuilder.addMetaData("real", ColumnRole.WEIGHT);
		TableMetaData md = new TableMetaData(new IOTable(tableBuilder.build(Belt.defaultContext())), true);
		TableMetaDataResolver resolver = new TableMetaDataResolver(md);

		Collection<FunctionInput> allVariables = resolver.getAllVariables();
		assertEquals(md.labels().size(), allVariables.size());
		for (FunctionInput input : allVariables) {
			assertNotNull(md.column(input.getName()));
			assertEquals(ExpressionParserUtils.expressionTypeForColumnId(
					BeltMetaDataTools.getTypeId(md.column(input.getName()))), input.getType());
		}
	}

	@Test
	public void getVariableTypeTest() {
		TableMetaData md = new TableMetaData(new IOTable(AntlrParserTestUtils.getAllTypesTable()), true);
		TableMetaDataResolver resolver = new TableMetaDataResolver(md);
		for (String label : md.labels()) {
			assertEquals(ExpressionParserUtils.expressionTypeForColumnId(BeltMetaDataTools.getTypeId(md.column(label))),
					resolver.getVariableType(label));
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getDoubleValueTest() {
		TableMetaData md = new TableMetaData(new IOTable(AntlrParserTestUtils.getAllTypesTable()), true);
		new TableMetaDataResolver(md).getDoubleValue("real", 0);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getStringValueTest() {
		TableMetaData md = new TableMetaData(new IOTable(AntlrParserTestUtils.getAllTypesTable()), true);
		new TableMetaDataResolver(md).getStringValue("nominal", 0);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getInstantValueTest() {
		TableMetaData md = new TableMetaData(new IOTable(AntlrParserTestUtils.getAllTypesTable()), true);
		new TableMetaDataResolver(md).getInstantValue("date-time", 0);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getLocalTimeValueTest() {
		TableMetaData md = new TableMetaData(new IOTable(AntlrParserTestUtils.getAllTypesTable()), true);
		new TableMetaDataResolver(md).getLocalTimeValue("time", 0);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getStringSetValueTest() {
		TableMetaData md = new TableMetaData(new IOTable(AntlrParserTestUtils.getAllTypesTable()), true);
		new TableMetaDataResolver(md).getStringSetValue("text-set", 0);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getStringListValueTest() {
		TableMetaData md = new TableMetaData(new IOTable(AntlrParserTestUtils.getAllTypesTable()), true);
		new TableMetaDataResolver(md).getStringListValue("text-list", 0);
	}

}
