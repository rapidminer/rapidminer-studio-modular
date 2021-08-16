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

import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.Path;
import java.util.logging.Level;

import com.rapidminer.adaption.belt.IODataTable;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.storage.hdf5.Hdf5TableReader;
import com.rapidminer.storage.hdf5.HdfReaderException;
import com.rapidminer.tools.LogService;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.FileTypeHandler;
import com.rapidminer.versioning.repository.FileTypeHandlerRegistry;
import com.rapidminer.versioning.repository.GeneralFile;
import com.rapidminer.versioning.repository.GeneralFolder;
import com.rapidminer.versioning.repository.RepositoryFile;


/**
 * Sub interface for {@link FileTypeHandler} that is specific to IOObjects
 *
 * @param <T>
 * 		the type of IOObject that is handled by the handler
 * @author Jan Czogalla
 * @since 9.7
 */
public interface IOObjectFileTypeHandler<T extends IOObject, U extends IOObjectEntry> extends FileTypeHandler<T> {

	/**
	 * The suffix for {@link ExampleSet}s and {@link IOTable}s written in hdf5 format,
	 * see {@link com.rapidminer.storage.hdf5.ExampleSetHdf5Writer ExampleSetHdf5Writer} and
	 * {@link com.rapidminer.storage.hdf5.IOTableHdf5Writer IOTableHdf5Writer}
	 */
	String DATA_TABLE_FILE_ENDING = "rmhdf5table";

	/**
	 * The suffix for {@link IOTableModel}s written in json format.
	 * 
	 */
	String TABLE_MODEL_FILE_ENDING = "rmtabmod";

	/**
	 * The suffix for {@link com.rapidminer.operator.IOObjectCollection}s written in zip format
	 */
	String COLLECTION_SUFFIX = "collection";

	/**
	 * The suffix for {@link PerformanceVector} written in json format.
	 */
	String PERFORMANCE_VECTOR_SUFFIX = "rmperf";

	/**
	 * The suffix for {@link AttributeWeights} written in json format.
	 */
	String ATTRIBUTE_WEIGHTS_SUFFIX = "rmweights";

	String ERROR_READING_FILE = "com.rapidminer.repository.versioned.IOObjectFileTypeHandler.error_reading_file";

	/** @return the suffix associated with this handler */
	String getSuffix();

	/** @return the topmost class associated with this handler */
	Class<T> getIOOClass();

	/**
	 * @return the specific entry type class this handler would create in the repository
	 */
	Class<U> getEntryType();

	default void register() {
		FileTypeHandlerRegistry.register(getSuffix(), this);
		IOObjectSuffixRegistry.register(getIOOClass(), getSuffix());
		IOObjectEntryTypeRegistry.register(getIOOClass(), getEntryType());
	}

	/**
	 * General {@link FileTypeHandler} for {@link IOObject} entries in the repository. Handles both entry creation
	 * ({@link #init(String, GeneralFolder)} as well as data summary extraction ({@link #createDataSummary(GeneralFile)}.
	 *
	 * @author Jan Czogalla
	 * @since 9.7
	 */
	enum LegacyIOOHandler implements IOObjectFileTypeHandler<IOObject, BasicIOObjectEntry> {
		INSTANCE;

		private static final String IOO_SUFFIX = IOObjectEntry.IOO_SUFFIX.replaceFirst("^\\.", "");

		@Override
		public String getSuffix() {
			return IOO_SUFFIX;
		}

		@Override
		public Class<IOObject> getIOOClass() {
			return IOObject.class;
		}

		@Override
		public Class<BasicIOObjectEntry> getEntryType() {
			return BasicIOObjectEntry.class;
		}

		@Override
		public RepositoryFile<IOObject> init(String filename, GeneralFolder parent) {
			return new BasicIOObjectEntry(filename, FilesystemRepositoryAdapter.toBasicFolder(parent), IOObject.class);
		}

		@Override
		public DataSummary createDataSummary(GeneralFile<IOObject> repositoryFile) {
			if (!(repositoryFile instanceof BasicIOObjectEntry)) {
				return FaultyDataSummary.wrongFileType(repositoryFile);
			}
			IOObject ioObject;
			try {
				ioObject = ((BasicIOObjectEntry) repositoryFile).retrieveData(null);
			} catch (RepositoryException e) {
				LogService.log(LogService.getRoot(), Level.WARNING, e, ERROR_READING_FILE,
						((BasicIOObjectEntry) repositoryFile).getName());
				return FaultyDataSummary.withCause(repositoryFile, e);
			}
			MetaData md = MetaData.forIOObject(ioObject);
			MetaData.shrinkValues(md);
			return md;
		}
	}

	/**
	 * {@link FileTypeHandler} for {@link ExampleSet} and {@link IOTable} entries in the repository with the new HDF5 format.
	 * Handles both entry creation ({@link #init(String, GeneralFolder)} as well as data summary extraction
	 * ({@link #createDataSummary(GeneralFile)}.
	 *
	 * @author Jan Czogalla, Gisa Meier
	 * @since 9.9.0
	 */
	enum HDF5DataTableHandler implements IOObjectFileTypeHandler<IODataTable, BasicIODataTableEntry> {
		INSTANCE;

		@Override
		public String getSuffix() {
			return DATA_TABLE_FILE_ENDING;
		}

		@Override
		public Class<IODataTable> getIOOClass() {
			return IODataTable.class;
		}

		@Override
		public Class<BasicIODataTableEntry> getEntryType() {
			return BasicIODataTableEntry.class;
		}

		@Override
		public RepositoryFile<IODataTable> init(String filename, GeneralFolder parent) {
			return new BasicIODataTableEntry(filename, FilesystemRepositoryAdapter.toBasicFolder(parent));
		}

		@Override
		public DataSummary createDataSummary(GeneralFile<IODataTable> repositoryFile) {
			if (!(repositoryFile instanceof BasicIODataTableEntry)) {
				return FaultyDataSummary.wrongFileType(repositoryFile);
			}
			BasicIODataTableEntry entry = (BasicIODataTableEntry) repositoryFile;
			Path path = entry.getRepositoryAdapter().getRealPath(entry, AccessMode.READ);
			try {
				// try to read MD directly from file
				TableMetaData emd = Hdf5TableReader.readMetaData(path);
				if (emd != null) {
					return emd;
				}
			} catch (HdfReaderException e) {
				LogService.log(LogService.getRoot(), Level.WARNING, e, ERROR_READING_FILE, entry.getName());
				return FaultyDataSummary.withCause(repositoryFile, e);
			} catch (IOException e) {
				// ignore and continue
			}
			try {
				// try to read full table and create meta data from there
				IOObject ioObject = entry.retrieveData(null);
				MetaData md = MetaData.forIOObject(ioObject);
				MetaData.shrinkValues(md);
				return md;
			} catch (RepositoryException e) {
				LogService.log(LogService.getRoot(), Level.WARNING, e, ERROR_READING_FILE, entry.getName());
				return FaultyDataSummary.withCause(repositoryFile, e);
			}
		}
	}

	/**
	 * {@link FileTypeHandler} for {@link IOTableModel} entries in the repository with the json format.
	 *
	 * @author Gisa Meier
	 * @since 9.10
	 */
	final class IOTableModelHandler extends JsonStorableIOObjectHandler<IOTableModel, JsonIOTableModelEntry> {
		static final IOTableModelHandler INSTANCE = new IOTableModelHandler();

		private IOTableModelHandler() {
		}

		@Override
		public String getSuffix() {
			return TABLE_MODEL_FILE_ENDING;
		}

		@Override
		public Class<IOTableModel> getIOOClass() {
			return IOTableModel.class;
		}

		@Override
		public Class<JsonIOTableModelEntry> getEntryType() {
			return JsonIOTableModelEntry.class;
		}
	}

	/**
	 * {@link JsonStorableIOObjectHandler} for {@link PerformanceVector} entries in the repository with the new json
	 * format.
	 *
	 * @author Gisa Meier
	 * @since 9.10.0
	 */
	class PerformanceHandler extends JsonStorableIOObjectHandler<PerformanceVector, JsonPerformanceEntry> {
		static final PerformanceHandler INSTANCE = new PerformanceHandler();

		private PerformanceHandler(){
		}

		@Override
		public Class getEntryType() {
			return JsonPerformanceEntry.class;
		}

		public String getSuffix() {
			return PERFORMANCE_VECTOR_SUFFIX;
		}

		public Class<PerformanceVector> getIOOClass() {
			return PerformanceVector.class;
		}
	}

	/**
	 * {@link JsonStorableIOObjectHandler} for {@link AttributeWeights} entries in the repository with the new json
	 * format.
	 *
	 * @author Gisa Meier
	 * @since 9.10.0
	 */
	class WeightsHandler extends JsonStorableIOObjectHandler<AttributeWeights, JsonAttributeWeightsEntry> {
		static final WeightsHandler INSTANCE = new WeightsHandler();

		private WeightsHandler(){
		}

		@Override
		public Class getEntryType() {
			return JsonAttributeWeightsEntry.class;
		}

		public String getSuffix() {
			return ATTRIBUTE_WEIGHTS_SUFFIX;
		}

		@Override
		public Class<AttributeWeights> getIOOClass() {
			return AttributeWeights.class;
		}

	}
}
