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

import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import org.junit.Test;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.gui.processeditor.results.DisplayContext;
import com.rapidminer.operator.performance.AbsoluteError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataFactory;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;

import static org.junit.Assert.*;


/**
 * Tests for {@link BeltConversionTools}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class BeltConversionToolsTest {

	@Test
	public void testAsTable() {
		assertNotNull(BeltConversionTools.asIOTableOrNull(new IOTable(numericTable()), null));
		assertNotNull(BeltConversionTools.asIOTableOrNull(BeltConverter.convertSequentially(new IOTable(numericTable())), null));
		assertNull(BeltConversionTools.asIOTableOrNull(null, new SequentialConcurrencyContext()));
		assertNull(BeltConversionTools.asIOTableOrNull(new AbsoluteError(), new SequentialConcurrencyContext()));
	}

	@Test
	public void testAsES() {
		assertNotNull(BeltConversionTools.asExampleSetOrNull(new IOTable(numericTable())));
		assertNotNull(BeltConversionTools.asExampleSetOrNull(BeltConverter.convertSequentially(new IOTable(numericTable()))));
		assertNull(BeltConversionTools.asExampleSetOrNull(null));
		assertNull(BeltConversionTools.asExampleSetOrNull(new AbsoluteError()));
	}

	@Test
	public void testAsTableMD() {
		TableMetaData tmd = BeltConversionTools.asTableMetaDataOrNull(MetaDataFactory.getInstance()
				.createMetaDataforIOObject(new IOTable(numericTable()), false));
		assertNotNull(tmd);
		assertEquals(TableMetaData.class, tmd.getClass());
		assertNotNull(BeltConversionTools.asTableMetaDataOrNull(MetaDataFactory.getInstance()
				.createMetaDataforIOObject(BeltConverter.convertSequentially(new IOTable(numericTable())), false)));
		assertNull(BeltConversionTools.asTableMetaDataOrNull(null));
		assertNull(BeltConversionTools.asTableMetaDataOrNull(new MetaData(AbsoluteError.class)));
	}

	@Test
	public void testAsESMD() {
		ExampleSetMetaData esmd = BeltConversionTools.asExampleSetMetaDataOrNull(MetaDataFactory.getInstance()
				.createMetaDataforIOObject(new IOTable(numericTable()), false));
		assertNotNull(esmd);
		assertEquals(ExampleSetMetaData.class, esmd.getClass());
		assertNotNull(BeltConversionTools.asExampleSetMetaDataOrNull(MetaDataFactory.getInstance()
				.createMetaDataforIOObject(BeltConverter.convertSequentially(new IOTable(numericTable())), false)));
		assertNull(BeltConversionTools.asExampleSetMetaDataOrNull(null));
		assertNull(BeltConversionTools.asExampleSetMetaDataOrNull(new MetaData(AbsoluteError.class)));
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
}
