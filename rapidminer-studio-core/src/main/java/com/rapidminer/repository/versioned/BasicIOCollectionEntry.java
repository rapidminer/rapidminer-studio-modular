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
package com.rapidminer.repository.versioned;

import static com.rapidminer.repository.versioned.IOCollectionHandler.COLLECTION_SUFFIX;
import static org.apache.commons.io.FileUtils.deleteQuietly;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.Action;

import org.apache.commons.io.FilenameUtils;

import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.external.alphanum.AlphanumComparator;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.PersistentContentMapperStore;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryEntryWrongTypeException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.FileUtils;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.TempFileTools;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileWasMissingException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;

/**
 * {@link AbstractIOObjectEntry} that represents {@link IOObjectCollection IOObjectCollections}.
 * Collections are stored as zip files, containing it's children as if they were stored in a regular
 * {@link FilesystemRepositoryAdapter}. Nested collections are represented as subfolders in that zip file.
 * <p>
 * To read/write, a temporary repository is created, utilizing the {@link PersistentContentMapperStore} to find
 * an appropriate cache location.
 *
 * @author Jan Czogalla
 * @since 9.8
 */
@SuppressWarnings("rawtypes")
public class BasicIOCollectionEntry extends AbstractIOObjectEntry<IOObjectCollection> {

	/**
	 * Container class to unify collection and collection MD building
	 *
	 * @author Jan Czogalla
	 * @since 9.8
	 */
	private static final class CollectionContainer {
		private CollectionContainer subMDContainer;
		private CollectionMetaData md;
		private IOObjectCollection<IOObject> collection;

		private CollectionContainer(CollectionMetaData md) {
			this.md = md;
		}

		private CollectionContainer(IOObjectCollection<IOObject> collection) {
			this.collection = collection;
		}

		private CollectionMetaData getMD() {
			if (subMDContainer == null) {
				return md;
			}
			Annotations annotations = md.getAnnotations();
			CollectionMetaData result = new CollectionMetaData(subMDContainer.getMD());
			result.setAnnotations(annotations);
			return result;
		}

		private IOObjectCollection<?> getCollection() {
			return collection;
		}

		private Annotations getAnnotations() {
			if (md != null) {
				return md.getAnnotations();
			}
			return collection.getAnnotations();
		}

		private void add(Object object) {
			// unpack nested container for real data
			if (object instanceof CollectionContainer && !isMD()) {
				CollectionContainer container = (CollectionContainer) object;
				object = container.isMD() ? container.getMD() : container.getCollection();
			}

			if (isMD() && object instanceof MetaData) {
				Annotations annotations = md.getAnnotations();
				md = new CollectionMetaData((MetaData) object);
				md.setAnnotations(annotations);
			} else if (isMD() && object instanceof CollectionContainer) {
				subMDContainer = (CollectionContainer) object;
			} else if (!isMD() && object instanceof IOObject) {
				collection.add((IOObject) object);
			}
		}

		private boolean isMD() {
			return md != null;
		}

		private CollectionContainer newSubContainer() {
			return md != null ? new CollectionContainer(new CollectionMetaData())
					: new CollectionContainer(new IOObjectCollection<>());
		}
	}

	/**
	 * Simple entry implementation that basically consists of a name to make things searchable/sortable
	 *
	 * @author Jan Czogalla
	 * @since 9.8
	 */
	private static final class DummyEntry implements Entry {

		private String name;

		private DummyEntry(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getType() {
			return null;
		}

		@Override
		public String getOwner() {
			return null;
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public boolean isReadOnly() {
			return false;
		}

		@Override
		public boolean rename(String newName) throws RepositoryException {
			return false;
		}

		@Override
		public boolean move(Folder newParent) throws RepositoryException {
			return false;
		}

		@Override
		public boolean move(Folder newParent, String newName) throws RepositoryException {
			return false;
		}

		@Override
		public Folder getContainingFolder() {
			return null;
		}

		@Override
		public boolean willBlock() {
			return false;
		}

		@Override
		public RepositoryLocation getLocation() {
			return null;
		}

		@Override
		public void delete() throws RepositoryException {}

		@Override
		public Collection<Action> getCustomActions() {
			return null;
		}
	}

	public static final String ANNOTATIONS_FILE_NAME = "ANNOTATIONS";
	public static final String TIMESTAMP_FILE_NAME = ".timestamp";

	/** Lowest data entry */
	private static final Entry LOW_ENTRY = new DummyEntry("0");
	/** Entry to select only the 0 entry */
	private static final Entry ONE_ENTRY = new DummyEntry("1");
	/** (Exclusive) highest data entry, the first string that is no longer a number */
	private static final Entry HIGH_ENTRY;
	static {
		char high = '9' + 1;
		HIGH_ENTRY = new DummyEntry(high + "");
	}
	/** An entry representing the annotations file */
	private static final Entry ANNOTATIONS_ENTRY = new DummyEntry(ANNOTATIONS_FILE_NAME);

	/**
	 * Create a new instance using {@link Repository#createIOObjectEntry(String, IOObject, Operator, ProgressListener)}
	 *
	 * @param name
	 * 		full filename of the file without a path: "foo.bar"
	 * @param parent
	 *        {@link BasicFolder} is required
	 */
	protected BasicIOCollectionEntry(String name, BasicFolder parent) {
		super(name, parent, IOObjectCollection.class);
		dataClass = getDataType();
	}

	@Override
	protected void setIOObjectData(IOObject data) throws RepositoryFileException, RepositoryImmutableException, RepositoryException {
		if (!(data instanceof IOObjectCollection)) {
			throw new RepositoryException("Data must be IOObject collection!");
		}
		setData((IOObjectCollection) data);
	}

	/**
	 * @see FileUtils#extractZipStream(Path, ZipInputStream)
	 */
	@Override
	protected IOObjectCollection<?> read(InputStream load) throws IOException {
		RepositoryLocation location = getLocation();
		Path cache = PersistentContentMapperStore.INSTANCE.retrieve(COLLECTION_SUFFIX, p -> p, location);
		// check cache; if not existent or out of date, extract
		boolean cacheInvalid = cache == null || !Files.exists(cache) || !Files.isDirectory(cache);
		boolean isOutOfDate = !cacheInvalid && isOutOfDate(cache);
		if (cacheInvalid || isOutOfDate) {
			if (((cache != null && !Files.isDirectory(cache)) || isOutOfDate) && !deleteQuietly(cache.toFile())) {
				throw new IOException("Cannot extract collection content");
			}
			try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(load), StandardCharsets.UTF_8)) {
				PersistentContentMapperStore.INSTANCE.store(COLLECTION_SUFFIX, zis, this::extractZipStream, location);
			}
			cache = PersistentContentMapperStore.INSTANCE.retrieve(COLLECTION_SUFFIX, p -> p, location);
		}
		return buildCollection(cache, new CollectionContainer(new IOObjectCollection<>())).getCollection();
	}

	/**
	 * Get an abbreviated {@link IOObjectCollection} for {@link com.rapidminer.versioning.repository.DataSummary
	 * DataSummary} creation. This will be a collection containing only the (sub)structure until the first leaf. If a
	 * cache entry is already present for the collection, this will be used as a basis, otherwise only the needed entry
	 * is extracted to a temporary folder, together with all {@link Annotations}.
	 *
	 * @return an abbreviated collection for data summary creation
	 * @throws IOException if an error occurs extracting the  reading the collection
	 * @see #checkMDEntry(ZipEntry)
	 * @since 9.8
	 */
	@SuppressWarnings("unchecked")
	CollectionMetaData readDataSummary() throws IOException {
		RepositoryLocation location = getLocation();
		Path cache = PersistentContentMapperStore.INSTANCE.retrieve(COLLECTION_SUFFIX, p -> p, location);
		// if cache does not exist or is out of date, only extract necessary sub structure
		if (cache == null || !Files.exists(cache) || !Files.isDirectory(cache) || isOutOfDate(cache)) {
			Path tempDirectory = Files.createTempDirectory("col-md-");
			TempFileTools.registerCleanup(tempDirectory);
			try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(getParent().getRepository().load(this)))) {
				FileUtils.extractZipStream(tempDirectory, zis, BasicIOCollectionEntry::checkMDEntry);
			} catch (RepositoryFileWasMissingException e) {
				// extraction failed, do not proceed
				throw new IOException(e);
			}
			cache = tempDirectory;
		}
		return buildCollection(cache, new CollectionContainer(new CollectionMetaData())).getMD();
	}

	/**
	 * Checks whether the file named {@value #TIMESTAMP_FILE_NAME} is present in the given folder and if it corresponds
	 * to the {@link #getLastModified() last modified timestamp} of this entry.
	 *
	 * @return {@code true} iff the the timestamp file does not exist or represent a different timestamp than this
	 * collections modification timestamp
	 */
	private boolean isOutOfDate(Path folder) {
		if (folder == null) {
			return false;
		}
		Path timestampPath = folder.resolve(TIMESTAMP_FILE_NAME);
		if (!Files.exists(timestampPath) || !Files.isRegularFile(timestampPath)) {
			return true;
		}
		try {
			List<String> lines = Files.readAllLines(timestampPath);
			if (lines.size() != 1) {
				return true;
			}
			String tsLine = lines.get(0);
			long ts = Long.parseLong(tsLine);
			return ts == 0 || ts != getLastModified();
		} catch (IOException | NumberFormatException e) {
			// cannot read or not a number: invalid
			return true;
		}
	}

	/**
	 * Build a collection from the given path. Will create a transient (aka fake) repository from the given folder and
	 * collect annotations and {@link IOObject IOObjects} in a (possibly nested) {@link IOObjectCollection}.
	 */
	private CollectionContainer buildCollection(Path cache, CollectionContainer container) throws IOException {
		boolean onlyForMD = container.isMD();
		Repository repository = null;
		try {
			repository = FilesystemRepositoryFactory.createRepository(getName() + "-" + UUID.randomUUID(),
					cache, false, true, getRepositoryAdapter().getEncryptionContext());
			List<Pair<Folder, CollectionContainer>> folderCollections = new ArrayList<>();
			folderCollections.add(new Pair<>(repository, container));
			while (!folderCollections.isEmpty()) {
				Pair<Folder, CollectionContainer> fc = folderCollections.remove(0);
				Folder folder = fc.getFirst();
				CollectionContainer collection = fc.getSecond();
				NavigableSet<Entry> entries = new TreeSet<>(Comparator.comparing(Entry::getName, new AlphanumComparator()));
				entries.addAll(folder.getDataEntries());
				entries.addAll(folder.getSubfolders());
				SortedSet<Entry> collectionEntries = entries.subSet(LOW_ENTRY, onlyForMD ? ONE_ENTRY : HIGH_ENTRY);
				Entry annoEntry = entries.ceiling(ANNOTATIONS_ENTRY);
				if (annoEntry instanceof BasicBinaryEntry && annoEntry.getName().equals(ANNOTATIONS_FILE_NAME)) {
					try (InputStream annoStream = ((BasicBinaryEntry) annoEntry).openInputStream()) {
						collection.getAnnotations().putAll(Annotations.fromPropertyStyle(annoStream));
					} catch (IOException e) {
						// ignore annotations problems?
					}
				}
				if (collectionEntries.isEmpty()) {
					continue;
				}
				String lastName = collectionEntries.last().getName();
				if (!lastName.equals((collectionEntries.size() - 1) + "")) {
					LogService.getRoot().log(Level.SEVERE,
							"com.rapidminer.repository.versioned.BasicIOCollectionEntry.mismatched_entries",
							new Object[] {folder.getLocation().makeRelative(repository.getLocation()), collectionEntries.size() - 1, lastName});
					throw new IOException("Mismatched entries; expected " + collectionEntries.size() + ", but found entry with number " + lastName);
				}
				for (Entry entry : collectionEntries) {
					Object object;
					if (entry instanceof Folder) {
						CollectionContainer subCollection = container.newSubContainer();
						folderCollections.add(new Pair<>((Folder) entry, subCollection));
						object = subCollection;
					} else if (entry instanceof IOObjectEntry && !(entry instanceof ConnectionEntry)) {
						// only allow IOOs that are not connections
						// file objects are allowed to be read, but this usually does not happen, since writing them is not allowed
						if (onlyForMD) {
							object = ((IOObjectEntry) entry).retrieveMetaData();
						} else {
							object = ((IOObjectEntry) entry).retrieveData(null);
						}
					} else {
						throw new RepositoryException("Mismatched type in collection; expected IOObject entry, but was " + entry.getType());
					}
					collection.add(object);
				}
			}
		} catch (RepositoryException e) {
			throw new IOException(e);
		} finally {
			if (repository != null) {
				try {
					repository.delete();
				} catch (RepositoryException e) {
					// ignore
				}
			}
		}
		return container;
	}

	/**
	 * @see FileUtils#pack(Path, ZipOutputStream, Predicate)
	 */
	@Override
	protected void write(IOObjectCollection data) throws IOException, RepositoryImmutableException {
		checkContent(data);
		try (ZipOutputStream zos = new ZipOutputStream(getOutputStream())) {
			// reset to empty zip file; automatically checks for read only access
		} catch (IOException e) {
			// ignore?
		}
		RepositoryLocation location = getLocation();
		PersistentContentMapperStore.INSTANCE.store(COLLECTION_SUFFIX, null, null, location);
		PersistentContentMapperStore.INSTANCE.store(COLLECTION_SUFFIX, new Object(), (path, o) -> Files.createDirectory(path), location);
		Path cache = PersistentContentMapperStore.INSTANCE.retrieve(COLLECTION_SUFFIX, p -> p, location);
		if (cache == null || !Files.exists(cache) || !Files.isDirectory(cache)) {
			throw new IOException("Could not create collection structure");
		}
		Repository repository = null;
		try {
			repository = FilesystemRepositoryFactory.createRepository(getName(), cache,
					true, true,
					getRepositoryAdapter().getEncryptionContext());
			List<Pair<Folder, IOObjectCollection<IOObject>>> folderCollections = new ArrayList<>();
			folderCollections.add(new Pair<>(repository, data));
			while (!folderCollections.isEmpty()) {
				Pair<Folder, IOObjectCollection<IOObject>> fc = folderCollections.remove(0);
				Folder folder = fc.getFirst();
				IOObjectCollection<IOObject> collection = fc.getSecond();
				if (!collection.getAnnotations().isEmpty()) {
					try {
						BinaryEntry annoEntry = folder.createBinaryEntry(ANNOTATIONS_FILE_NAME);
						try (OutputStream annoStream = annoEntry.openOutputStream()) {
							annoStream.write(collection.getAnnotations().asPropertyStyle().getBytes(StandardCharsets.UTF_8));
						}
					} catch (IOException | RepositoryException e) {
						// ignore annotations problems?
					}
				}
				int counter = 0;
				for (IOObject object : collection.getObjects()) {
					if (object == null) {
						continue;
					}
					String name = counter + "";
					if (object instanceof IOObjectCollection) {
						Folder subFolder = folder.createFolder(name);
						folderCollections.add(new Pair<>(subFolder, (IOObjectCollection) object));
					} else {
						folder.createIOObjectEntry(name, object, null, null);
					}
					// null entries will be ignored and not counted
					counter++;
				}
				try (ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(getOutputStream()), StandardCharsets.UTF_8)) {
					FileUtils.pack(cache, zipStream, BasicIOCollectionEntry::checkPath);
					try {
						// write timestamp file
						BinaryEntry tsEntry = repository.createBinaryEntry(TIMESTAMP_FILE_NAME);
						FileTime lastModifiedTime = Files.getLastModifiedTime(cache);
						try (OutputStream tsos = tsEntry.openOutputStream()) {
							tsos.write((lastModifiedTime.toMillis() + "").getBytes());
						}
					} catch (RepositoryException | IOException e) {
						// ignore; no timestamp file
					}
				}
			}
		} catch (RepositoryException e) {
			throw new IOException(e);
		} finally {
			if (repository != null) {
				try {
					repository.delete();
				} catch (RepositoryException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Checks if the given collection complies with rules such that it can be read again successfully. This means that
	 * the collection and any sub collection cannot contain neither {@link FileObject FileObjects} nor {@link
	 * ConnectionInformationContainerIOObject ConnectionInformations}.
	 *
	 * @param data collection to check
	 * @throws IOException if any (sub) collection contains objects that are not allowed
	 */
	private void checkContent(IOObjectCollection<?> data) throws IOException {
		List<IOObjectCollection<?>> toCheck = new ArrayList<>();
		toCheck.add(data);
		while (!toCheck.isEmpty()) {
			IOObjectCollection<?> collection = toCheck.remove(0);
			for (IOObject object : collection.getObjects()) {
				if (object instanceof FileObject) {
					throw new IOException(new RepositoryEntryWrongTypeException("Cannot store file objects in a collection"));
				}
				if (object instanceof ConnectionInformationContainerIOObject) {
					throw new IOException(new RepositoryEntryWrongTypeException("Cannot store connections in a collection"));
				}
				if (object instanceof IOObjectCollection) {
					toCheck.add((IOObjectCollection<?>) object);
				}
			}
		}
	}

	/**
	 * Calls {@link FileUtils#extractZipStream(Path, ZipInputStream, Predicate)} with {@link #checkEntry(ZipEntry)}
	 * as predicate. Shorthand so it can be used as lambda. Also puts a file named {@value #TIMESTAMP_FILE_NAME} in the
	 * folder to indicate the last changed timestamp of the collection. The timestamp does not represent the extraction
	 * timestamp.
	 */
	private void extractZipStream(Path extractToPath, ZipInputStream zipStream) throws IOException {
		FileUtils.extractZipStream(extractToPath, zipStream, BasicIOCollectionEntry::checkEntry);
		Path timestampPath = extractToPath.resolve(TIMESTAMP_FILE_NAME);
		long lastModified = getLastModified();
		if (lastModified <= 0) {
			return;
		}
		try {
			Files.write(timestampPath, Collections.singletonList("" + lastModified));
		} catch (IOException e) {
			// ignore; no timestamp might mean that it will be extracted again
		}

	}

	/**
	 * Checks whether a given {@link Path} matches the collection style naming pattern
	 * An entry matches, if it is
	 * <ul>
	 *     <li>a folder with a number as a name</li>
	 *     <li>a regular file called {@value BasicIOCollectionEntry#ANNOTATIONS_FILE_NAME} </li>
	 *     <li>a regular file with a number as a name, plus/minus a suffix</li>
	 * </ul>
	 */
	private static boolean checkPath(Path file) {
		String name = file.getFileName().toString();
		if (Files.isRegularFile(file) && ANNOTATIONS_FILE_NAME.equals(name)) {
			return true;
		}
		if (Files.isRegularFile(file)) {
			name = FilenameUtils.getBaseName(name);
		}
		try {
			Integer.parseInt(name);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Checks whether a given {@link ZipEntry} matches the collection style naming pattern
	 * An entry matches, if it is
	 * <ul>
	 *     <li>a directory with a number as a name</li>
	 *     <li>a regular file called {@value BasicIOCollectionEntry#ANNOTATIONS_FILE_NAME} </li>
	 *     <li>a regular file with a number as a name, plus/minus a suffix</li>
	 * </ul>
	 */
	private static boolean checkEntry(ZipEntry entry) {
		String name = FilenameUtils.getName(entry.getName());
		if (!entry.isDirectory() && ANNOTATIONS_FILE_NAME.equals(name)) {
			return true;
		}
		if (!entry.isDirectory()) {
			name = FilenameUtils.getBaseName(name);
		}
		try {
			Integer.parseInt(name);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Checks whether a given {@link ZipEntry} matches the collection style naming pattern and reduces it to annotations
	 * and 0 entries. An entry matches, if it is
	 * <ul>
	 *     <li>a directory with name "0"</li>
	 *     <li>a regular file called {@value BasicIOCollectionEntry#ANNOTATIONS_FILE_NAME} </li>
	 *     <li>a regular file with name "0", plus/minus a suffix</li>
	 * </ul>
	 */
	private static boolean checkMDEntry(ZipEntry entry) {
		String name = FilenameUtils.getName(entry.getName());
		if (!entry.isDirectory() && ANNOTATIONS_FILE_NAME.equals(name)) {
			return true;
		}
		if (!entry.isDirectory()) {
			name = FilenameUtils.getBaseName(name);
		}
		return name.equals("0");
	}
}
