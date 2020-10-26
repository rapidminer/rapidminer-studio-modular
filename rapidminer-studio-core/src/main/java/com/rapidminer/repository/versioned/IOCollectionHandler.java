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

import java.io.IOException;

import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.GeneralFile;
import com.rapidminer.versioning.repository.GeneralFolder;
import com.rapidminer.versioning.repository.RepositoryFile;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileWasMissingException;


/**
 * {@link IOObjectFileTypeHandler} that handles {@link IOObjectCollection IOObjectCollections}.
 * Collections will be represented by an {@link BasicIOCollectionEntry}.
 *
 * @author Jan Czogalla
 * @since 9.8
 */
enum IOCollectionHandler implements IOObjectFileTypeHandler<IOObjectCollection, BasicIOCollectionEntry> {
	INSTANCE;

	@Override
	public String getSuffix() {
		return COLLECTION_SUFFIX;
	}

	@Override
	public Class<IOObjectCollection> getIOOClass() {
		return IOObjectCollection.class;
	}

	@Override
	public Class<BasicIOCollectionEntry> getEntryType() {
		return BasicIOCollectionEntry.class;
	}

	@Override
	public RepositoryFile<IOObjectCollection> init(String filename, GeneralFolder parent) {
		return new BasicIOCollectionEntry(filename, FilesystemRepositoryAdapter.toBasicFolder(parent));
	}

	@Override
	public DataSummary createDataSummary(GeneralFile<IOObjectCollection> repositoryFile) {
		if (!(repositoryFile instanceof BasicIOCollectionEntry)) {
			return FaultyDataSummary.wrongFileType(repositoryFile);
		}
		BasicIOCollectionEntry entry = (BasicIOCollectionEntry) repositoryFile;
		try {
			return entry.readDataSummary();
		} catch (IOException e) {
			Throwable cause = e;
			if (e.getCause() instanceof RepositoryFileWasMissingException) {
				cause = e.getCause();
			}
			return FaultyDataSummary.withCause(entry, cause);

		}
	}
}
