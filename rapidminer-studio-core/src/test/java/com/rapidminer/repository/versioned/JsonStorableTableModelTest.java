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
package com.rapidminer.repository.versioned;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.RapidMiner;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.WrappedIOTableModel;
import com.rapidminer.operator.WrappedIOTablePredictionModel;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.preprocessing.IOTablePreprocessingModel;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.repository.local.SimpleDataEntry;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Tests reading and writing {@link com.rapidminer.operator.IOTableModel}s into the file system repository and
 * copying/moving to/from the legacy repository.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class JsonStorableTableModelTest {

	private static class TestPredictionModel extends IOTablePredictionModel {

		private final double value;

		private TestPredictionModel(IOTable trainingTable, double value) {
			super(trainingTable, Tables.ColumnSetRequirement.EQUAL, Tables.TypeRequirement.REQUIRE_MATCHING_TYPES,
					Tables.TypeRequirement.ALLOW_INT_FOR_REAL);
			this.value = value;
		}

		private TestPredictionModel(){
			//default constructor for json
			this.value = Double.NaN;
		}

		@Override
		protected Column performPrediction(Table adapted, Map<String, Column> confidences, Operator operator) throws OperatorException {
			return Buffers.sparseRealBuffer(value, adapted.height()).toColumn();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestPredictionModel that = (TestPredictionModel) o;
			boolean tableEquals = true;
			try{
				RapidAssert.assertEquals(that.getTrainingHeader(), getTrainingHeader());
			}catch (Exception e){
				tableEquals = false;
			}
			return Double.compare(that.value, value) == 0 && tableEquals;
		}

	}

	private static class TestPreprocessingModel extends IOTablePreprocessingModel {

		private final double value;

		private TestPreprocessingModel(IOTable trainingTable, double value) {
			super(trainingTable);
			this.value = value;
		}

		private TestPreprocessingModel() {
			//default constructor for json
			this.value = Double.NaN;
		}

		@Override
		public void applyOnData(Table adjusted, TableBuilder builder, Operator operator) throws OperatorException {
		}

		@Override
		protected boolean needsRemapping() {
			return false;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestPreprocessingModel that = (TestPreprocessingModel) o;
			boolean tableEquals = true;
			try{
				RapidAssert.assertEquals(that.getTrainingHeader(), getTrainingHeader());
			}catch (Exception e){
				tableEquals = false;
			}
			return Double.compare(that.value, value) == 0 && tableEquals;
		}
	}

	private Path tempDirectoryNew;
	private Repository newTestRepository;
	private Path tempDirectoryLegacy;
	private Repository legacyTestRepository;


	@BeforeClass
	public static void setup() {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		RepositoryManager.init();
		RapidMiner.initAsserters();
		JsonStorableIOObjectResolver.INSTANCE.register(TestPredictionModel.class);
		JsonStorableIOObjectResolver.INSTANCE.register(TestPreprocessingModel.class);
	}

	@Before
	public void createTestRepos() throws IOException, RepositoryException {
		tempDirectoryNew = Files.createTempDirectory(UUID.randomUUID().toString());
		FilesystemRepositoryFactory.createRepository("Test Local", tempDirectoryNew,
				EncryptionProvider.DEFAULT_CONTEXT);
		newTestRepository = RepositoryManager.getInstance(null).getRepository("Test Local");
		tempDirectoryLegacy = Files.createTempDirectory(UUID.randomUUID().toString());
		RepositoryManager.getInstance(null).addRepository(new LocalRepository("Test Local Old",
				tempDirectoryLegacy.toFile()));
		legacyTestRepository = RepositoryManager.getInstance(null).getRepository("Test Local Old");
	}

	@After
	public void deleteTestRepos() {
		RepositoryManager.getInstance(null).removeRepository(newTestRepository);
		FileUtils.deleteQuietly(tempDirectoryNew.toFile());
		RepositoryManager.getInstance(null).removeRepository(legacyTestRepository);
		FileUtils.deleteQuietly(tempDirectoryLegacy.toFile());
	}

	@Test
	public void testFileEnding() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", model, null, null);

		assertEquals("rmtabmod", ((BasicEntry) entry1).getSuffix());

		myFolder.delete();
	}

	@Test
	public void testRead() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", model, null, null);

		assertEquals(TestPredictionModel.class, entry1.getObjectClass());
		assertEquals(model, entry1.retrieveData(null));

		myFolder.delete();
	}

	@Test
	public void testAdapterRead() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel modelNew = new TestPredictionModel(new IOTable(table), 1.2);
		PredictionModel model = new WrappedIOTablePredictionModel(modelNew);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", model, null, null);

		assertEquals(((WrappedIOTablePredictionModel)model).getDefiningModel(),
				((WrappedIOTablePredictionModel)entry1.retrieveData(null)).getDefiningModel());

		myFolder.delete();
	}

	@Test
	public void testAdapterRead2() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPreprocessingModel modelNew = new TestPreprocessingModel(new IOTable(table), 1.2);
		Model model = new WrappedIOTableModel(modelNew);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", model, null, null);

		assertEquals(((WrappedIOTableModel)model).getDefiningModel(),
				((WrappedIOTableModel)entry1.retrieveData(null)).getDefiningModel());

		myFolder.delete();
	}

	@Test
	public void testReadPreprocessing() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPreprocessingModel model = new TestPreprocessingModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", model, null, null);

		assertEquals(TestPreprocessingModel.class, entry1.getObjectClass());
		assertEquals(model, entry1.retrieveData(null));

		myFolder.delete();
	}

	@Test
	public void testObjectClass() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		nestedFolder.createIOObjectEntry("entry", model, null, null);
		JsonIOObjectEntry<IOTableModel> entry2 =
				Mockito.spy(new JsonIOTableModelEntry("entry.rmtabmod", (BasicFolder) nestedFolder, IOTableModel.class));
		when(entry2.getSize()).thenReturn(1L);

		assertEquals(TestPredictionModel.class, entry2.getObjectClass());

		myFolder.delete();
	}

	@Test
	public void testSetData() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry = nestedFolder.createIOObjectEntry("entry", model, null, null);

		Table table2 = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"No").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model2 = new TestPredictionModel(new IOTable(table2), 2.2);
		entry.storeData(model2, null, null);

		assertEquals(TestPredictionModel.class, entry.getObjectClass());
		assertEquals(model2, entry.retrieveData(null));

		myFolder.delete();
	}

	@Test
	public void testCopy() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", model, null, null);
		RepositoryManager.getInstance(null).copy(entry1.getLocation(), myFolder, "second", null);
		assertTrue(myFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				myFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(model, data);
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	@Test
	public void testCopyToOld() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", model, null, null);

		Folder newFolder = legacyTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).copy(entry1.getLocation(), newFolder, "second", null);
		assertTrue(newFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX, ((SimpleDataEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(model, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testCopyFromOld() throws RepositoryException {
		Folder myFolder = legacyTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", model, null, null);

		Folder newFolder = newTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).copy(entry1.getLocation(), newFolder, "second", null);
		assertTrue(newFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals("rmtabmod", ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(model, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testMove() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", model, null, null);
		RepositoryManager.getInstance(null).move(entry1.getLocation(), myFolder, "second", null);
		assertTrue(myFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				myFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(model, data);
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	@Test
	public void testMoveToOld() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", model, null, null);

		Folder newFolder = legacyTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).move(entry1.getLocation(), newFolder, "second", null);
		assertTrue(newFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX, ((SimpleDataEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(model, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testMoveFromOld() throws RepositoryException {
		Folder myFolder = legacyTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		Table table = Builders.newTableBuilder(11).addNominal("label", i -> i % 2 == 0 ? "Yes" :
				"NO").addMetaData("label", ColumnRole.LABEL).addReal("rand", i -> Math.random()).build(Belt.defaultContext());
		TestPredictionModel model = new TestPredictionModel(new IOTable(table), 1.2);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", model, null, null);

		Folder newFolder = newTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).move(entry1.getLocation(), newFolder, "second", null);
		assertTrue(newFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals("rmtabmod", ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(model, data);
		myFolder.delete();
		newFolder.delete();
	}

}
