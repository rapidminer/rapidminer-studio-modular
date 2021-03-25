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
package com.rapidminer.repository.resource;

import static com.rapidminer.repository.RepositoryTools.NAME_FILTER;
import static com.rapidminer.repository.RepositoryTools.ONLY_BLOB_ENTRIES;
import static com.rapidminer.repository.RepositoryTools.ONLY_CONNECTION_ENTRIES;
import static com.rapidminer.repository.RepositoryTools.ONLY_IOOBJECT_ENTRIES;
import static com.rapidminer.repository.RepositoryTools.ONLY_PROCESS_ENTRIES;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.BlobEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.EntryCreator;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryLocationBuilder;
import com.rapidminer.repository.RepositoryLocationType;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;


/**
 * Reference on a folder in the repository.
 *
 * @author Simon Fischer, Jan Czogalla
 */
public class ResourceFolder extends ResourceEntry implements Folder {

	/**
	 * A map of {@link EntryCreator}, one for each {@link ResourceDataEntry}.
	 * @since 9.3
	 */
	private static final Map<String, EntryCreator<String[], ? extends ResourceDataEntry, ResourceFolder, ResourceRepository>> CREATOR_MAP;
	static {
		Map<String, EntryCreator<String[], ? extends ResourceDataEntry, ResourceFolder, ResourceRepository>> creatorMap = new HashMap<>();
		creatorMap.put(BlobEntry.BLOB_SUFFIX, (l, f, r) -> new ResourceBlobEntry(f, l[0], l[1], r));
		creatorMap.put(ProcessEntry.RMP_SUFFIX, (l, f, r) -> new ResourceProcessEntry(f, l[0], l[1], r));
		creatorMap.put(IOObjectEntry.IOO_SUFFIX, (l, f, r) -> new ResourceIOObjectEntry(f, l[0], l[1], r));
		creatorMap.put(ConnectionEntry.CON_SUFFIX, (l, f, r) -> new ResourceConnectionEntry(f, l[0], l[1], r));
		CREATOR_MAP = Collections.unmodifiableMap(creatorMap);
	}

	private List<Folder> folders;
	private List<DataEntry> data;

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();

	public ResourceFolder(ResourceFolder parent, String name, String resource, ResourceRepository repository) {
		super(parent, name, resource, repository);
	}

	@Override
	public BlobEntry createBlobEntry(String name) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public Folder createFolder(String name) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public IOObjectEntry createIOObjectEntry(String name, IOObject ioobject, Operator callingOperator,
											 ProgressListener newParam) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public ProcessEntry createProcessEntry(String name, String processXML) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public ConnectionEntry createConnectionEntry(String name, ConnectionInformation connectionInformation) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample repository. Cannot create new entries.");
	}

	@Override
	public List<DataEntry> getDataEntries() throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return Collections.unmodifiableList(data);
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return Collections.unmodifiableList(data);
		} finally {
			releaseWriteLock();
		}
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			return new RepositoryLocationBuilder().withLocationType(RepositoryLocationType.FOLDER).buildFromAbsoluteLocation(getRepository().getLocation().toString() + getPath());
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	protected boolean isLoaded() {
		return folders != null && data != null;
	}

	/**
	 * Makes sure the corresponding content is loaded. This method will perform write operations,
	 * you need to acquire the write lock before calling it.
	 */
	protected void ensureLoaded() throws RepositoryException {
		if (isLoaded()) {
			return;
		}
		this.folders = new LinkedList<>();
		this.data = new LinkedList<>();
		ensureLoaded(folders, data);
	}

	/**
	 * The actual loading of the content is done in this method. This allows subclasses to easily implement their own loading mechanisms.
	 *
	 * @param folders
	 * 		the folders list to fill
	 * @param data
	 * 		the data list to fill
	 * @throws RepositoryException
	 * 		if an error occurs
	 * @since 9.0
	 */
	protected void ensureLoaded(List<Folder> folders, List<DataEntry> data) throws RepositoryException {
		try (InputStream in = getResourceStream("/CONTENTS");
			 InputStreamReader reader = new InputStreamReader(in, "UTF-8")) {
			String[] lines = Tools.readTextFile(reader).split("\n");
			for (String line : lines) {
				line = line.trim();
				int space = line.indexOf(' ');
				if (line.isEmpty() || space == -1) {
					continue;
				}
				String name = line.substring(space + 1).trim();
				String errorSource = null;
				if (line.startsWith("FOLDER ")) {
					folders.add(new ResourceFolder(this, name, getPath() + "/" + name, getRepository()));
				} else if (line.startsWith("ENTRY")) {
					int suffixStart = name.lastIndexOf('.');
					String nameWOExt = name;
					String suffix = "";
					if (suffixStart >= 0) {
						nameWOExt = name.substring(0, suffixStart);
						suffix = name.substring(suffixStart);
					}
					if (!ConnectionEntry.CON_SUFFIX.equals(suffix) || isSpecialConnectionsFolder()) {
						//ignore connection entries outside special folder
						DataEntry entry = CREATOR_MAP.getOrDefault(suffix, EntryCreator.nullCreator())
								.create(new String[]{nameWOExt, getPath() + "/" + nameWOExt}, this, getRepository());
						if (entry != null) {
							data.add(entry);
						} else {
							errorSource = name;
						}
					}
				} else {
					errorSource = line;
				}
				if (errorSource != null) {
					throw new RepositoryException("Illegal entry type in folder '" + getName() + "': " + errorSource);
				}
			}
		} catch (Exception e) {
			throw new RepositoryException("Error reading contents of folder " + getName() + ": " + e, e);
		}
	}

	@Override
	public List<Folder> getSubfolders() throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return Collections.unmodifiableList(folders);
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return Collections.unmodifiableList(folders);
		} finally {
			releaseWriteLock();
		}
	}

	@Override
	public void refresh() throws RepositoryException {
		acquireWriteLock();
		try {
			folders = null;
			data = null;
		} finally {
			releaseWriteLock();
		}
		getRepository().fireRefreshed(this);
	}

	@Override
	public boolean containsFolder(String folderName) throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return containsFolderNotThreadSafe(folderName);
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return containsFolderNotThreadSafe(folderName);
		} finally {
			releaseWriteLock();
		}
	}

	@Override
	public boolean containsData(String dataName, Class<? extends DataEntry> expectedDataType) throws RepositoryException {
		acquireReadLock();
		try {
			if (isLoaded()) {
				return containsDataNotThreadSafe(dataName, expectedDataType);
			}
		} finally {
			releaseReadLock();
		}
		acquireWriteLock();
		try {
			ensureLoaded();
			return containsDataNotThreadSafe(dataName, expectedDataType);
		} finally {
			releaseWriteLock();
		}
	}

	@Override
	@Deprecated
	public boolean containsEntry(String name) throws RepositoryException {
		return containsData(name, DataEntry.class) || containsFolder(name);
	}

	@Override
	public String getDescription() {
		return getResource();
	}

	@Override
	public boolean canRefreshChildFolder(String folderName) throws RepositoryException {
		return containsFolder(folderName);
	}

	@Override
	public boolean canRefreshChildData(String dataName) throws RepositoryException {
		return containsData(dataName, DataEntry.class);
	}

	@Override
	@Deprecated
	public boolean canRefreshChild(String childName) throws RepositoryException {
		return canRefreshChildData(childName) || canRefreshChildFolder(childName);
	}

	/**
	 * Adds a folder to the list of folders.
	 *
	 * @param folder
	 *            the folder to add
	 * @throws RepositoryException
	 */
	void addFolder(Folder folder) throws RepositoryException {
		acquireWriteLock();
		try {
			folders.add(folder);
		} finally {
			releaseWriteLock();
		}
	}

	private boolean containsFolderNotThreadSafe(String name) {
		return folders.stream().anyMatch(f -> NAME_FILTER.test(f, name));
	}

	private boolean containsDataNotThreadSafe(String name, Class<? extends DataEntry> expectedDataType) {
		if (ProcessEntry.class.isAssignableFrom(expectedDataType)) {
			return containsData(name, ONLY_PROCESS_ENTRIES);
		} else if (ConnectionEntry.class.isAssignableFrom(expectedDataType)) {
			return containsData(name, ONLY_CONNECTION_ENTRIES);
		} else if (BlobEntry.class.isAssignableFrom(expectedDataType)) {
			return containsData(name, ONLY_BLOB_ENTRIES);
		} else if (IOObjectEntry.class.isAssignableFrom(expectedDataType)) {
			return containsData(name, ONLY_IOOBJECT_ENTRIES);
		} else {
			// either unknown or super type, prevent if anything is named like it to be sure
			return containsData(name, e -> true);
		}
	}

	private boolean containsData(String name, Predicate<DataEntry> instanceCheck) {
		return data.stream().filter(instanceCheck).anyMatch(d -> NAME_FILTER.test(d, name));
	}

	private void acquireReadLock() throws RepositoryException {
		try {
			readLock.lock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not get read lock", e);
		}
	}

	private void releaseReadLock() throws RepositoryException {
		try {
			readLock.unlock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not release read lock", e);
		}
	}

	private void acquireWriteLock() throws RepositoryException {
		try {
			writeLock.lock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not get write lock", e);
		}
	}

	private void releaseWriteLock() throws RepositoryException {
		try {
			writeLock.unlock();
		} catch (RuntimeException e) {
			throw new RepositoryException("Could not release write lock", e);
		}
	}
}
