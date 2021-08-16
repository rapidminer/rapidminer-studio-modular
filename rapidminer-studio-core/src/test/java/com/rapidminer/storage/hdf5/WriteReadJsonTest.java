/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.storage.hdf5;

import static com.rapidminer.storage.hdf5.WriteReadTest.createAllTypes;
import static com.rapidminer.storage.hdf5.WriteReadTest.createDifferentRoles;
import static com.rapidminer.storage.hdf5.WriteTableReadTest.createBooleanTable;
import static com.rapidminer.storage.hdf5.WriteTableReadTest.createDifferentRolesAndAnnotations;
import static com.rapidminer.storage.hdf5.WriteTableReadTest.createMoreColumnMD;
import static com.rapidminer.storage.hdf5.WriteTableReadTest.createWithGaps;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * Tests the different possibilities to write header tables using the {@link HeaderTableJsonSerializer} and read them
 * with the {@link HeaderTableJsonDeserializer}.
 *
 * @author Gisa Meier
 */
@RunWith(Enclosed.class)
public class WriteReadJsonTest {

	@RunWith(Parameterized.class)
	public static class FromExampleSet {

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public ExampleSet set;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"all types", createAllTypes()});
			sets.add(new Object[]{"all roles", createDifferentRoles()});
			return sets;
		}

		@Test
		public void testWriteAndRead() throws IOException {
			File f = File.createTempFile("test", ".json");
			f.deleteOnExit();
			IOTable headerTable = new IOTable(BeltConversionTools.asIOTableOrNull(set, null).getTable().stripData());
			IOTable read = readWritten(f, headerTable);
			RapidAssert.assertEquals(headerTable, read);
			for (String label : headerTable.getTable().labels()) {
				assertEquals(new HashSet<>(headerTable.getTable().getMetaData(label)),
						new HashSet<>(read.getTable().getMetaData(label)));
			}
		}

	}

	private static IOTable readWritten(File f, IOTable headerTable) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(IOTable.class, new HeaderTableJsonDeserializer());
		module.addSerializer(IOTable.class, new HeaderTableJsonSerializer());
		mapper.registerModule(module);
		mapper.writeValue(f, headerTable);
		return mapper.readValue(f, IOTable.class);
	}


	@RunWith(Parameterized.class)
	public static class ReadTableWrittenByWriter {

		@BeforeClass
		public static void setup() {
			RapidMiner.initAsserters();
		}

		@Parameterized.Parameter
		public String name;

		@Parameterized.Parameter(value = 1)
		public IOTable table;

		@Parameterized.Parameters(name = "{0}")
		public static Iterable<Object[]> sets() {
			List<Object[]> sets = new ArrayList<>();
			sets.add(new Object[]{"boolean", createBooleanTable(100)});
			sets.add(new Object[]{"nominal with gaps", createWithGaps()});
			sets.add(new Object[]{"all roles", createDifferentRolesAndAnnotations()});
			sets.add(new Object[]{"more column md", createMoreColumnMD()});
			return sets;
		}

		@Test
		public void testWriteAndRead() throws IOException {
			File f = File.createTempFile("test", ".json");
			f.deleteOnExit();
			IOTable headerTable = new IOTable(table.getTable().stripData());
			IOTable read = readWritten(f, headerTable);
			RapidAssert.assertEquals(headerTable, read);
			for (String label : headerTable.getTable().labels()) {
				assertEquals(new HashSet<>(headerTable.getTable().getMetaData(label)),
						new HashSet<>(read.getTable().getMetaData(label)));
			}
		}

	}
}
