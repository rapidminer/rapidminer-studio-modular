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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.operator.performance.AbsoluteError;
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
import com.rapidminer.tools.math.kernels.DotKernel;
import com.rapidminer.tools.math.kernels.Kernel;


/**
 * Tests reading and writing {@link JsonStorableIOObject}s into the file system repository and copying/moving to/from
 * the legacy repository.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class JsonStorableIOObjectsTest {

	private static class TestIOObject extends ResultObjectAdapter implements JsonStorableIOObject {

		protected String someValue;
		private transient int ignore = 123456;

		private TestIOObject() {
		}

		private TestIOObject(String someValue) {
			this.someValue = someValue;
		}
	}

	private static class TestIOOEntry extends JsonIOObjectEntry<TestIOObject> {

		protected TestIOOEntry(String name, BasicFolder parent, Class<TestIOObject> dataType) {
			super(name, parent, dataType);
		}
	}

	private static class TestIOObjectHandler extends JsonStorableIOObjectHandler<TestIOObject, TestIOOEntry> {

		@Override
		public String getSuffix() {
			return "mytest";
		}

		@Override
		public Class<TestIOObject> getIOOClass() {
			return TestIOObject.class;
		}

		@Override
		public Class<TestIOOEntry> getEntryType() {
			return TestIOOEntry.class;
		}
	}

	private static class MyTestSubclass extends TestIOObject {

		private final int number;

		private MyTestSubclass() {
			number = 0;
		}

		private MyTestSubclass(String someValue, int number) {
			super(someValue);
			this.number = number;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			MyTestSubclass that = (MyTestSubclass) o;
			return number == that.number && Objects.equals(someValue, that.someValue);
		}

		@Override
		public int hashCode() {
			return Objects.hash(number);
		}
	}

	private static class MyBadSubclass extends TestIOObject {

		@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
		private Kernel something;

		private MyBadSubclass() {
		}

		private MyBadSubclass(String someValue, Kernel object) {
			super(someValue);
			this.something = object;
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
		new TestIOObjectHandler().register();
		JsonStorableIOObjectResolver.INSTANCE.register(MyTestSubclass.class);
		JsonStorableIOObjectResolver.INSTANCE.register(MyBadSubclass.class);
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

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		assertEquals("mytest", ((BasicEntry) entry1).getSuffix());

		myFolder.delete();
	}

	@Test
	public void testRead() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		assertEquals(MyTestSubclass.class, entry1.getObjectClass());
		assertEquals(subclass, entry1.retrieveData(null));

		myFolder.delete();
	}

	@Test
	public void testReadNewerVersion() throws RepositoryException, IOException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		Path realPath =
				((TestIOOEntry) entry1).getRepositoryAdapter().getRealPath((TestIOOEntry) entry1, AccessMode.WRITE);
		String asString = new String(Files.readAllBytes(realPath), StandardCharsets.UTF_8);
		asString = asString.replace(RapidMiner.getVersion().getShortLongVersion(),
				new VersionNumber(RapidMiner.getVersion().getMajorNumber(), RapidMiner.getVersion().getMinorNumber()+1).getShortLongVersion());
		Files.write(realPath, asString.getBytes(StandardCharsets.UTF_8));

		assertEquals(subclass, entry1.retrieveData(null));

		myFolder.delete();
	}

	@Test
	public void testWriteTransient() throws RepositoryException, IOException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		Path realPath =
				((TestIOOEntry) entry1).getRepositoryAdapter().getRealPath((TestIOOEntry) entry1, AccessMode.WRITE);
		String asString = new String(Files.readAllBytes(realPath), StandardCharsets.UTF_8);

		assertFalse(asString.contains("123456"));

		myFolder.delete();
	}

	@Test
	public void testReadInvalidVersion() throws RepositoryException, IOException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		Path realPath =
				((TestIOOEntry) entry1).getRepositoryAdapter().getRealPath((TestIOOEntry) entry1, AccessMode.WRITE);
		String asString = new String(Files.readAllBytes(realPath), StandardCharsets.UTF_8);
		asString = asString.replace(RapidMiner.getVersion().getShortLongVersion(),"3x5");
		Files.write(realPath, asString.getBytes(StandardCharsets.UTF_8));

		assertEquals(subclass, entry1.retrieveData(null));

		myFolder.delete();
	}

	@Test
	public void testReadNoVersion() throws RepositoryException, IOException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		Path realPath =
				((TestIOOEntry) entry1).getRepositoryAdapter().getRealPath((TestIOOEntry) entry1, AccessMode.WRITE);
		String asString = new String(Files.readAllBytes(realPath), StandardCharsets.UTF_8);
		asString = asString.replace("\"version\":\""+RapidMiner.getVersion().getShortLongVersion()+"\",","");
		Files.write(realPath, asString.getBytes(StandardCharsets.UTF_8));

		assertEquals(subclass, entry1.retrieveData(null));

		myFolder.delete();
	}

	@Test
	public void testObjectClass() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		nestedFolder.createIOObjectEntry("entry", subclass, null, null);
		JsonIOObjectEntry<TestIOObject> entry2 =
				Mockito.spy(new TestIOOEntry("entry.mytest", (BasicFolder) nestedFolder, TestIOObject.class));
		when(entry2.getSize()).thenReturn(1L);

		assertEquals(MyTestSubclass.class, entry2.getObjectClass());

		myFolder.delete();
	}

	@Test
	public void testSetData() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		MyTestSubclass subclass2 = new MyTestSubclass("blup", 31);
		entry.storeData(subclass2, null, null);

		assertEquals(MyTestSubclass.class, entry.getObjectClass());
		assertEquals(subclass2, entry.retrieveData(null));

		myFolder.delete();
	}

	@Test(expected = RepositoryException.class)
	public void testUnregistered() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		try {
			MyTestSubclass subclass = new MyTestSubclass("bla", 42) {
				@Override
				public String toString() {
					return "other class";
				}
			};
			nestedFolder.createIOObjectEntry("entry", subclass, null, null);
		} finally {
			myFolder.delete();
		}
	}

	@Test(expected = RepositoryException.class)
	public void testUnregisteredRead() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		try {
			MyTestSubclass subclass = new MyTestSubclass("bla", 42) {
				@Override
				public String toString() {
					return "other class";
				}
			};
			JsonStorableIOObjectResolver.INSTANCE.register(subclass.getClass());
			IOObjectEntry entry = nestedFolder.createIOObjectEntry("entry", subclass, null, null);
			JsonStorableIOObjectResolver.INSTANCE.unregister(subclass.getClass());
			entry.retrieveData(null);
		} finally {
			myFolder.delete();
		}
	}

	@Test(expected = RepositoryException.class)
	public void testBadRead() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyBadSubclass subclass = new MyBadSubclass("bla", new DotKernel());
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		assertEquals(MyBadSubclass.class, entry1.getObjectClass());
		try {
			entry1.retrieveData(null);
		} finally {
			myFolder.delete();
		}
	}

	@Test(expected = RepositoryException.class)
	public void testBadReadNewerVersion() throws RepositoryException, IOException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");

		MyBadSubclass subclass = new MyBadSubclass("bla", new DotKernel());
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("entry", subclass, null, null);

		Path realPath =
				((TestIOOEntry) entry1).getRepositoryAdapter().getRealPath((TestIOOEntry) entry1, AccessMode.WRITE);
		String asString = new String(Files.readAllBytes(realPath), StandardCharsets.UTF_8);
		asString = asString.replace(RapidMiner.getVersion().getShortLongVersion(),
				new VersionNumber(RapidMiner.getVersion().getMajorNumber(), RapidMiner.getVersion().getMinorNumber()+1).getShortLongVersion());
		Files.write(realPath, asString.getBytes(StandardCharsets.UTF_8));

		assertEquals(MyBadSubclass.class, entry1.getObjectClass());
		try {
			entry1.retrieveData(null);
		} finally {
			myFolder.delete();
		}
	}


	@Test
	public void testDeletion() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		MyTestSubclass subclass2 = new MyTestSubclass("blup", 422);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", subclass, null, null);
		IOObjectEntry entry2 = nestedFolder.createIOObjectEntry("second", subclass2, null, null);
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

	@Test
	public void testCopy() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", subclass, null, null);
		RepositoryManager.getInstance(null).copy(entry1.getLocation(), myFolder, "second", null);
		assertTrue(myFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				myFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(subclass, data);
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	@Test
	public void testCopyToOld() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", subclass, null, null);

		Folder newFolder = legacyTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).copy(entry1.getLocation(), newFolder, "second", null);
		assertTrue(newFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX, ((SimpleDataEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(subclass, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testCopyFromOld() throws RepositoryException {
		Folder myFolder = legacyTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", subclass, null, null);

		Folder newFolder = newTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).copy(entry1.getLocation(), newFolder, "second", null);
		assertTrue(newFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals("mytest", ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(subclass, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testMove() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", subclass, null, null);
		RepositoryManager.getInstance(null).move(entry1.getLocation(), myFolder, "second", null);
		assertTrue(myFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				myFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(subclass, data);
		myFolder.delete();
		assertEquals(1, newTestRepository.getSubfolders().size());
	}

	@Test
	public void testMoveToOld() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", subclass, null, null);

		Folder newFolder = legacyTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).move(entry1.getLocation(), newFolder, "second", null);
		assertTrue(newFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX, ((SimpleDataEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(subclass, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testMoveFromOld() throws RepositoryException {
		Folder myFolder = legacyTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", subclass, null, null);

		Folder newFolder = newTestRepository.createFolder("newFolder");

		RepositoryManager.getInstance(null).move(entry1.getLocation(), newFolder, "second", null);
		assertTrue(newFolder.containsData("second", BasicIODataTableEntry.class));
		DataEntry secondEntry =
				newFolder.getDataEntries().stream().filter(d -> "second".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals("mytest", ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(subclass, data);
		myFolder.delete();
		newFolder.delete();
	}

	@Test
	public void testOverwriteWithIOO() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", subclass, null, null);

		IOObject dummy = new AbsoluteError();
		RepositoryManager.getInstance(null).store(dummy, entry1.getLocation(), null, null);

		DataEntry secondEntry =
				nestedFolder.getDataEntries().stream().filter(d -> "first".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals(IOObjectEntry.IOO_SUFFIX.replace(".", ""), ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		RapidAssert.assertEquals(dummy, data);
		myFolder.delete();
		nestedFolder.delete();
	}

	@Test
	public void testOverwriteIOO() throws RepositoryException {
		Folder myFolder = newTestRepository.createFolder("myFolder");
		Folder nestedFolder = myFolder.createFolder("nestedFolder");
		IOObject dummy = new AbsoluteError();
		IOObjectEntry entry1 = nestedFolder.createIOObjectEntry("first", dummy, null, null);

		MyTestSubclass subclass = new MyTestSubclass("bla", 42);
		RepositoryManager.getInstance(null).store(subclass, entry1.getLocation(), null, null);

		DataEntry secondEntry =
				nestedFolder.getDataEntries().stream().filter(d -> "first".equals(d.getName())).findFirst().get();
		assertTrue(secondEntry instanceof IOObjectEntry);
		assertEquals("mytest", ((BasicEntry) secondEntry).getSuffix());
		IOObject data = ((IOObjectEntry) secondEntry).retrieveData(null);
		assertEquals(subclass, data);
		myFolder.delete();
		nestedFolder.delete();
	}

}
