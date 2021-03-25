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
package com.rapidminer.repository.versioned;

import static com.rapidminer.repository.versioned.ExampleSetsInRepoTest.createDataSet;
import static com.rapidminer.repository.versioned.IOObjectFileTypeHandler.COLLECTION_SUFFIX;
import static com.rapidminer.repository.versioned.datasummary.IOCollectionDataSummarySerializerTest.TMD_COMPARATOR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.performance.AbsoluteError;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataFactory;
import com.rapidminer.operator.ports.metadata.ToTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
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

import junit.framework.TestCase;


/**
 * Tests reading and writing {@link IOObjectCollection}s into the file system repository and copying/moving to/from the
 * legacy repository.
 *
 * @author Jan Czogalla
 * @since 9.8
 */
public class IOCollectionsInRepoTest extends TestCase {

	private Path tempDirectoryNew;
	private Repository newTestRepository;
	private Path tempDirectoryLegacy;
	private Repository legacyTestRepository;


	static {
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.TEST);
		RepositoryManager.init();
		RapidMiner.initAsserters();
	}

	@Override
	public void setUp() throws IOException, RepositoryException {
		tempDirectoryNew = Files.createTempDirectory(UUID.randomUUID().toString());
		FilesystemRepositoryFactory.createRepository("Test Local", tempDirectoryNew, EncryptionProvider.DEFAULT_CONTEXT);
		newTestRepository = RepositoryManager.getInstance(null).getRepository("Test Local");
		tempDirectoryLegacy = Files.createTempDirectory(UUID.randomUUID().toString());
		RepositoryManager.getInstance(null).addRepository(new LocalRepository("Test Local Old",
				tempDirectoryLegacy.toFile()));
		legacyTestRepository = RepositoryManager.getInstance(null).getRepository("Test Local Old");
	}

	@Override
	public void tearDown() {
		RepositoryManager.getInstance(null).removeRepository(newTestRepository);
		FileUtils.deleteQuietly(tempDirectoryNew.toFile());
		RepositoryManager.getInstance(null).removeRepository(legacyTestRepository);
		FileUtils.deleteQuietly(tempDirectoryLegacy.toFile());
	}

	public void testFileEnding() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectCollection<ExampleSet> collection2 = createCollection(100, 5);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);
		IOObjectEntry entry2 = myFolder.createIOObjectEntry("secondCol", collection2, null, null);
		assertEquals(COLLECTION_SUFFIX, ((BasicEntry) entry1).getSuffix());
		assertEquals(COLLECTION_SUFFIX, ((BasicEntry) entry2).getSuffix());
		myFolder.delete();
	}

	public void testDeletion() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectCollection<ExampleSet> collection2 = createCollection(100, 5);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);
		IOObjectEntry entry2 = nestedFolder.createIOObjectEntry("secondCol", collection2, null, null);
		assertNotNull("myFolder was null", myFolder);
		assertNotNull("nestedFolder was null", nestedFolder);
		assertNotNull("entry1 was null", entry1);
		assertNotNull("entry2 was null", entry2);

		assertEquals(1, myFolder.getSubfolders().size());
		assertEquals(0, myFolder.getDataEntries().size());
		assertEquals(0, nestedFolder.getSubfolders().size());

		assertEquals(2, nestedFolder.getDataEntries().size());
		entry1.delete();
		assertEquals(1, nestedFolder.getDataEntries().size());

		nestedFolder.delete();
		assertEquals(0, myFolder.getSubfolders().size());

		// conn folder is always there, so we expect 2 now
		assertEquals(2, newTestRepository.getSubfolders().size());
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	public void testCopy() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);
		RepositoryManager.getInstance(null).copy(entry1.getLocation(), myFolder, "secondCol", null);
		assertTrue(myFolder.containsData("secondCol", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				myFolder.getDataEntries().stream().filter(d -> "secondCol".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(collection, data);
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	public void testCopyToOld() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);

		Folder newFolder = legacyTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).copy(entry1.getLocation(), newFolder, "secondCol", null);
		assertTrue(newFolder.containsData("secondCol", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "secondCol".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX, ((SimpleDataEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(collection, data);
		myFolder.delete();
		newFolder.delete();
	}

	public void testCopyFromOld() throws RepositoryException {
		Folder myFolder = legacyTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);

		Folder newFolder = newTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).copy(entry1.getLocation(), newFolder, "secondCol", null);
		assertTrue(newFolder.containsData("secondCol", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "secondCol".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(COLLECTION_SUFFIX, ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(collection, data);
		myFolder.delete();
		newFolder.delete();
	}

	public void testMove() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);
		RepositoryManager.getInstance(null).move(entry1.getLocation(), myFolder, "secondCol", null);
		assertTrue(myFolder.containsData("secondCol", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				myFolder.getDataEntries().stream().filter(d -> "secondCol".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(collection, data);
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	public void testMoveToOld() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);

		Folder newFolder = legacyTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).move(entry1.getLocation(), newFolder, "secondCol", null);
		assertTrue(newFolder.containsData("secondCol", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "secondCol".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX, ((SimpleDataEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(collection, data);
		myFolder.delete();
		newFolder.delete();
	}

	public void testMoveFromOld() throws RepositoryException {
		Folder myFolder = legacyTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);

		Folder newFolder = newTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).move(entry1.getLocation(), newFolder, "secondCol", null);
		assertTrue(newFolder.containsData("secondCol", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "secondCol".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(COLLECTION_SUFFIX, ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(collection, data);
		myFolder.delete();
		newFolder.delete();
	}

	public void testOverwriteWithIOO() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("firstCol", collection, null, null);

		IOObject dummy = new AbsoluteError();
		RepositoryManager.getInstance(null).store(dummy, entry1.getLocation(), null, null);

		DataEntry secondEntry =
				nestedFolder.getDataEntries().stream().filter(d -> "firstCol".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX.replace(".",""), ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(dummy, data);
		myFolder.delete();
		nestedFolder.delete();
	}

	public void testOverwriteIOO() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObject dummy = new AbsoluteError();
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", dummy, null, null);

		IOObjectCollection<ExampleSet> collection = createCollection(10, 20);
		RepositoryManager.getInstance(null).store(collection, entry1.getLocation(), null, null);

		DataEntry secondEntry =
				nestedFolder.getDataEntries().stream().filter(d -> "first".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(COLLECTION_SUFFIX, ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(collection, data);
		myFolder.delete();
		nestedFolder.delete();
	}

	public void testNestedCollectionWithAnnotations() throws RepositoryException {
		IOObjectCollection<ExampleSet> innerCollection = createCollection(10, 20);
		innerCollection.getAnnotations().setAnnotation("anno", "inner");
		IOObjectCollection<IOObject> collection = new IOObjectCollection<>(new IOObject[]{innerCollection});
		collection.getAnnotations().setAnnotation("anno", "outer");

		Folder myFolder = newTestRepository.createFolder("myFolder");
		IOObjectEntry entry1 = myFolder.createIOObjectEntry("firstCol", collection, null, null);

		IOObject data = entry1.retrieveData(null);
		RapidAssert.assertEquals(collection, data);
		assertEquals(collection.getAnnotations(), data.getAnnotations());
		assertEquals(collection.getObjects().get(0).getAnnotations(), ((IOObjectCollection<IOObject>) data).getObjects().get(0).getAnnotations());
	}

	public void testDataSummaryWithoutCache() throws RepositoryException {
		IOObjectCollection<ExampleSet> innerCollection = createCollection(10, 20);
		innerCollection.getAnnotations().setAnnotation("anno", "inner");
		IOObjectCollection<IOObject> collection = new IOObjectCollection<>(new IOObject[]{innerCollection});
		collection.getAnnotations().setAnnotation("anno", "outer");
		CollectionMetaData originalMD =
				(CollectionMetaData) MetaDataFactory.getInstance().createMetaDataforIOObject(collection, false);
		assertEquals(collection.getAnnotations(), originalMD.getAnnotations());
		assertEquals(collection.getObjects().get(0).getAnnotations(),
				originalMD.getElementMetaData().getAnnotations());

		Folder myFolder = newTestRepository.createFolder("myFolder");
		IOObjectEntry entry1 = myFolder.createIOObjectEntry("firstCol", collection, null, null);

		CollectionMetaData loadedMetaData = (CollectionMetaData) entry1.retrieveMetaData();
		assertEquals(originalMD.getAnnotations(), loadedMetaData.getAnnotations());
		assertEquals(originalMD.getElementMetaData().getAnnotations(),
				loadedMetaData.getElementMetaData().getAnnotations());
		assertEquals(0,
				TMD_COMPARATOR.compare(ToTableMetaDataConverter.convert((ExampleSetMetaData) originalMD.getElementMetaDataRecursive()),
				(TableMetaData) loadedMetaData.getElementMetaDataRecursive()));
	}

	public void testDataSummaryWithCache() throws RepositoryException {
		IOObjectCollection<ExampleSet> innerCollection = createCollection(10, 20);
		innerCollection.getAnnotations().setAnnotation("anno", "inner");
		IOObjectCollection<IOObject> collection = new IOObjectCollection<>(new IOObject[]{innerCollection});
		collection.getAnnotations().setAnnotation("anno", "outer");
		CollectionMetaData originalMD = (CollectionMetaData) MetaDataFactory.getInstance().createMetaDataforIOObject(collection, false);
		assertEquals(collection.getAnnotations(), originalMD.getAnnotations());
		assertEquals(collection.getObjects().get(0).getAnnotations(), originalMD.getElementMetaData().getAnnotations());

		Folder myFolder = newTestRepository.createFolder("myFolder");
		IOObjectEntry entry1 = myFolder.createIOObjectEntry("firstCol", collection, null, null);
		entry1.retrieveData(null);

		CollectionMetaData loadedMetaData = (CollectionMetaData) entry1.retrieveMetaData();
		assertEquals(originalMD.getAnnotations(), loadedMetaData.getAnnotations());
		assertEquals(originalMD.getElementMetaData().getAnnotations(),
				loadedMetaData.getElementMetaData().getAnnotations());
		assertEquals(0,
				TMD_COMPARATOR.compare(ToTableMetaDataConverter.convert((ExampleSetMetaData) originalMD.getElementMetaDataRecursive()),
				(TableMetaData) loadedMetaData.getElementMetaDataRecursive()));
	}

	static IOObjectCollection<ExampleSet> createCollection(int i, int i2) {
		ExampleSet exampleSet = createDataSet(i, i2);
		return new IOObjectCollection<>(new ExampleSet[]{exampleSet});
	}
}
