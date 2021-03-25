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
import java.io.InputStream;
import java.nio.file.AccessMode;
import java.nio.file.Path;

import com.rapidminer.adaption.belt.IODataTable;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.processeditor.results.DisplayContext;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.storage.hdf5.ExampleSetHdf5Writer;
import com.rapidminer.storage.hdf5.Hdf5TableReader;
import com.rapidminer.storage.hdf5.IOTableHdf5Writer;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;


/**
 * The ExampleSet and IOTable Entry for the versioned repository. Supports reading an {@link IOTable} and writing an
 * {@link ExampleSet} or {@link IOTable} using the {@link Hdf5TableReader} and {@link ExampleSetHdf5Writer}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class BasicIODataTableEntry extends AbstractIOObjectEntry<IODataTable> {

	/**
	 * Create a new instance using {@link com.rapidminer.repository.Repository#createIOObjectEntry(String, IOObject,
	 * Operator, ProgressListener)}
	 *
	 * @param name
	 * 		full filename of the file without a path: "foo.bar"
	 * @param parent
	 *        {@link BasicFolder} is required
	 */
	protected BasicIODataTableEntry(String name, BasicFolder parent) {
		super(name, parent, IODataTable.class);
		dataClass = getDataType();
	}

	@Override
	protected IODataTable read(InputStream load) throws IOException {
		Path filePath = getRepositoryAdapter().getRealPath(this, AccessMode.READ);
		return Hdf5TableReader.read(filePath, new DisplayContext());
	}

	@Override
	protected void write(IODataTable data) throws IOException, RepositoryImmutableException {
		Path filePath = getRepositoryAdapter().getRealPath(this, AccessMode.WRITE);
		if (data instanceof IOTable) {
			IOTable table = (IOTable) data;
			new IOTableHdf5Writer(table).write(filePath);
		} else if (data instanceof ExampleSet) {
			ExampleSet exampleSet = (ExampleSet) data;
			exampleSet.recalculateAllAttributeStatistics();
			new ExampleSetHdf5Writer(exampleSet).write(filePath);
		} else {
			throw new AssertionError("iodata table must either be example set or iotable");
		}
	}

	@Override
	protected void setIOObjectData(IOObject data) throws RepositoryFileException, RepositoryImmutableException,
			RepositoryException {
		if (!(data instanceof ExampleSet) && !(data instanceof IOTable)) {
			throw new RepositoryException("Data must be data table!");
		}
		setData((IODataTable) data);
	}

	/**
	 * Checks whether the given {@link DataSummary} is {@link MetaData} compatible with {@link ExampleSet} or {@link
	 * IOTable}
	 */
	@Override
	protected boolean checkDataSummary(DataSummary dataSummary) {
		return super.checkDataSummary(dataSummary) || dataSummary instanceof ExampleSetMetaData ||
				dataSummary instanceof TableMetaData;
	}
}
