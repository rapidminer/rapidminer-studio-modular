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
package com.rapidminer.repository.versioned.datasummary;

import java.io.IOException;
import java.nio.file.Path;

import com.rapidminer.operator.ports.metadata.table.FromTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.repository.versioned.IOObjectFileTypeHandler;
import com.rapidminer.storage.hdf5.ExampleSetHdf5Writer;
import com.rapidminer.storage.hdf5.Hdf5ExampleSetReader;
import com.rapidminer.storage.hdf5.Hdf5TableReader;
import com.rapidminer.storage.hdf5.IOTableHdf5Writer;
import com.rapidminer.versioning.repository.DataSummary;


/**
 * {@link DataSummarySerializer} for {@link com.rapidminer.adaption.belt.IOTable IOTables}, i.e. for (de)serializing
 * {@link TableMetaData}. Utilizes {@link ExampleSetHdf5Writer} and {@link Hdf5ExampleSetReader} and uses the
 * new hdf5 suffix {@value IOObjectFileTypeHandler#DATA_TABLE_FILE_ENDING}.
 *
 * @author Gisa Meier
 * @since 9.9
 */
public enum IOTableDataSummarySerializer implements DataSummarySerializer {

	INSTANCE;

	@Override
	public String getSuffix() {
		return IOObjectFileTypeHandler.DATA_TABLE_FILE_ENDING;
	}

	@Override
	public Class<? extends DataSummary> getSummaryClass() {
		return TableMetaData.class;
	}

	@Override
	public void serialize(Path path, DataSummary dataSummary) throws IOException {
		if (!(dataSummary instanceof TableMetaData)) {
			// noop
			return;
		}
		new IOTableHdf5Writer((TableMetaData) dataSummary).write(path);
	}

	@Override
	public DataSummary deserialize(Path path) throws IOException {
		return Hdf5TableReader.readMetaData(path);
	}
}
