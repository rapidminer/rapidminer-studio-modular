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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.GeneralFile;
import com.rapidminer.versioning.repository.GeneralFolder;
import com.rapidminer.versioning.repository.RepositoryFile;


/**
 * Super {@link IOObjectFileTypeHandler} for storing as {@link JsonIOObjectEntry} with a certain file ending. To define
 * the file ending for a subclass of {@link JsonStorableIOObject}s, subclass this class and register it. This is
 * necessary so IOO suffixes and entry types can be distinct.
 *
 * @param <T>
 * 		the subclass of {@link JsonStorableIOObject}s that should have the same file ending
 * @author Gisa Meier, Jan Czogalla
 * @see JsonStorableIOObject
 * @since 9.10.0
 */
@SuppressWarnings("rawtypes")
abstract class JsonStorableIOObjectHandler<T extends JsonStorableIOObject, E extends JsonIOObjectEntry<T>>
		implements IOObjectFileTypeHandler<T, E> {

	@Override
	public RepositoryFile<T> init(String filename, GeneralFolder parent) {
		try {
			Class<E> entryClass = getEntryType();
			Constructor<E> entryConstructor = entryClass.getDeclaredConstructor(String.class, BasicFolder.class, Class.class);
			return entryConstructor.newInstance(filename, FilesystemRepositoryAdapter.toBasicFolder(parent), getIOOClass());
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
			throw new IllegalArgumentException("Cannot create repository entry", e);
		}
	}

	@Override
	public DataSummary createDataSummary(GeneralFile<T> repositoryFile) {
		if (!(repositoryFile instanceof JsonIOObjectEntry)) {
			return FaultyDataSummary.wrongFileType(repositoryFile);
		}
		JsonIOObjectEntry entry = (JsonIOObjectEntry) repositoryFile;
		IOObject ioObject;
		try {
			ioObject = entry.retrieveData(null);
		} catch (RepositoryException e) {
			LogService.log(LogService.getRoot(), Level.WARNING, e, ERROR_READING_FILE,
					entry.getName());
			return FaultyDataSummary.withCause(repositoryFile, e);
		}
		return MetaData.forIOObject(ioObject);
	}

}