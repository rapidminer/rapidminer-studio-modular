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
package com.rapidminer.storage.hdf5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnIO;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.hdf5.SeekableDataOutputByteChannel;
import com.rapidminer.hdf5.file.ColumnDescriptor;
import com.rapidminer.hdf5.file.NumericColumnDescriptor;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.hdf5.metadata.GlobalHeap;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDNumber.Relation;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;


/**
 * Writes {@link IOTable}s and {@link TableMetaData} to hdf5 using the {@link TableWriter}. Adds additional attributes
 * for {@link com.rapidminer.belt.util.ColumnMetaData} using the {@link ColumnMetaDataStorageRegistry}. Apart from that,
 * {@link com.rapidminer.belt.util.ColumnReference}s are added via the additional attributes {@link
 * IOTableHdf5Writer#ATTRIBUTE_COLUMN_REFERENCE_COLUMN} and {@link #ATTRIBUTE_COLUMN_REFERENCE_VALUE}. Additionally
 * writes statistics, see {@link TableStatisticsHandler}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class IOTableHdf5Writer extends TableWriter {

	/**
	 * Hdf5 attribute name to store type information that cannot be stored in {@link TableWriter#ATTRIBUTE_TYPE}
	 */
	public static final String ATTRIBUTE_LEGACY_TYPE = "legacy_type";

	/**
	 * Hdf5 attribute name to store role information that cannot be stored in {@link TableWriter#ATTRIBUTE_ROLE}
	 */
	public static final String ATTRIBUTE_LEGACY_ROLE = "legacy_role";

	/**
	 * Hdf5 attributes to store {@link com.rapidminer.belt.util.ColumnReference}s
	 */
	public static final String ATTRIBUTE_COLUMN_REFERENCE_COLUMN = "column_reference_column";
	public static final String ATTRIBUTE_COLUMN_REFERENCE_VALUE = "column_reference_value";

	/**
	 * Hdf5 attribute name to indicate whether statistics information are present
	 */
	public static final String ATTRIBUTE_HAS_STATISTICS = "has_statistics";

	/**
	 * Hdf5 attribute name to indicate whether the file is a meta data file
	 */
	public static final String ATTRIBUTE_IS_METADATA = "is_md";

	/**
	 * Hdf5 attribute name to indicate whether a column's nominal values were shrunk
	 */
	public static final String ATTRIBUTE_NOMINAL_SHRUNK = "nominal_data_shrunk";

	/**
	 * Hdf5 attribute name to indicate the {@link SetRelation} of a metadata file or its columns
	 */
	public static final String ATTRIBUTE_SET_RELATION = "set_relation";

	/**
	 * Hdf5 attribute name to indicate the {@link Relation} of a metadata file's row count.
	 */
	public static final String ATTRIBUTE_ROW_RELATION = "row_relation";

	private IOTable ioTable;
	private final TableMetaData md;

	/**
	 * Creates a new writer for the table
	 *
	 * @param ioTable
	 * 		an example set with up to date statistics already calculated
	 */
	public IOTableHdf5Writer(IOTable ioTable) {
		super(false);
		this.ioTable = ioTable;
		this.md = null;
	}

	/**
	 * Creates a new writer for the given {@link TableMetaData}. Can/will only write a metadata file.
	 *
	 * @param md
	 * 		a set of metadata for a table with all statistics up to date, must be directly calculated from a table or
	 * 		a hdf5 file, i.e. Ranges are either equal or unknown and all column types are known
	 */
	public IOTableHdf5Writer(TableMetaData md) {
		super(true);
		this.ioTable = null;
		this.md = md.clone();
	}

	/**
	 * Writes the {@link IOTable} or {@link TableMetaData} to the given path. Depending on the configuration
	 * of this writer, writes it as a full table or as a meta data file.
	 *
	 * @param path
	 * 		the path to write to
	 * @throws IOException
	 * 		if writing fails
	 */
	public void write(Path path) throws IOException {
		if (ioTable != null) {
			writeFromTable(path);
			return;
		}
		writeFromMetadata(path);
	}

	/**
	 * Write example set or metadata file from {@link IOTable} as a source
	 */
	private void writeFromTable(Path path) throws IOException {
		Table table = ioTable.getTable();
		int columnCount = table.width();
		int rowCount = table.height();
		//compact the dictionaries so that they have not gaps
		final Annotations annotations = ioTable.getAnnotations();
		Table compactedTable = TableViewCreator.INSTANCE.compactDictionaries(table);
		if (table != compactedTable) {
			table = compactedTable;
			Annotations anno = ioTable.getAnnotations();
			ioTable = new IOTable(table);
			ioTable.getAnnotations().addAll(anno);
		}

		ColumnDescriptor[] columnDescriptors = new ColumnDescriptor[columnCount];
		final List<String> labels = table.labels();
		for (int i = 0; i < columnDescriptors.length; i++) {
			String next = labels.get(i);
			columnDescriptors[i] = ColumnDescriptionCreator.create(next, table);
		}
		Map<String, Pair<Class<?>, Object>> additionalRootAttributes = new LinkedHashMap<>();
		additionalRootAttributes.put(ATTRIBUTE_HAS_STATISTICS, new ImmutablePair<>(byte.class, (byte) 1));
		write(columnDescriptors, annotations, rowCount, additionalRootAttributes, path);
	}

	/**
	 * Write metadata file from {@link TableMetaData} as a source
	 */
	private void writeFromMetadata(Path path) throws IOException {
		final Set<String> labels = md.labels();
		int columnCount = labels.size();
		SetRelation columnSetRelation = md.getColumnSetRelation();
		MDInteger numberOfRows = md.height();
		int rowCount = Optional.ofNullable(numberOfRows.getNumber()).orElse(0);

		ColumnDescriptor[] columnDescriptors = new ColumnDescriptor[columnCount];
		Iterator<String> iterator = labels.iterator();
		for (int i = 0; i < columnDescriptors.length; i++) {
			String next = iterator.next();
			columnDescriptors[i] = ColumnDescriptionCreator.create(next, md);
		}
		Map<String, Pair<Class<?>, Object>> additionalRootAttributes = new LinkedHashMap<>();
		additionalRootAttributes.put(ATTRIBUTE_HAS_STATISTICS, new ImmutablePair<>(byte.class, (byte) 1));
		additionalRootAttributes.put(ATTRIBUTE_IS_METADATA, new ImmutablePair<>(byte.class, (byte) 1));
		if (columnSetRelation != SetRelation.UNKNOWN) {
			additionalRootAttributes.put(ATTRIBUTE_SET_RELATION, new ImmutablePair<>(String.class,
					columnSetRelation.toString()));
		}
		additionalRootAttributes.put(ATTRIBUTE_ROW_RELATION, new ImmutablePair<>(String.class,
				numberOfRows.getRelation().getRepresentation()));
		write(columnDescriptors, md.getAnnotations(), rowCount, additionalRootAttributes, path);
	}

	@Override
	public void writeDoubleData(ColumnDescriptor columnDescriptor, SeekableDataOutputByteChannel channel) throws IOException {
		final Column column = ioTable.getTable().column(columnDescriptor.getName());
		int written = 0;
		while (written < column.size()) {
			ByteBuffer buffer = channel.getInternalBuffer();
			written += ColumnIO.putNumericDoubles(column, written, buffer);
		}
		channel.flush();
	}

	@Override
	protected void writeLongData(ColumnDescriptor columnDescriptor, SeekableDataOutputByteChannel channel) throws IOException {
		final Column column = ioTable.getTable().column(columnDescriptor.getName());
		if (column.type().id() == Column.TypeId.TIME) {
			int written = 0;
			while (written < column.size()) {
				ByteBuffer buffer = channel.getInternalBuffer();
				written += ColumnIO.putTimeLongs(column, written, buffer);
			}
		} else {
			int written = 0;
			while (written < column.size()) {
				ByteBuffer buffer = channel.getInternalBuffer();
				written += ColumnIO.putDateTimeLongs(column, written, buffer);
			}
		}
		channel.flush();
	}


	@Override
	protected void writeAdditionalData(NumericColumnDescriptor columnDescriptor,
									   SeekableDataOutputByteChannel channel) throws IOException {
		final Column column = ioTable.getTable().column(columnDescriptor.getName());
		int written = 0;
		while (written < column.size()) {
			ByteBuffer buffer = channel.getInternalBuffer();
			written += ColumnIO.putDateTimeNanoInts(column, written, buffer);
		}
		channel.flush();
	}


	@Override
	public void writeCategoryData(ColumnDescriptor columnDescriptor, SeekableDataOutputByteChannel channel) throws IOException {
		final Column column = ioTable.getTable().column(columnDescriptor.getName());
		int nBytes = columnDescriptor.getDataType().width();
		int mapSize = column.getDictionary().size();
		if (mapSize == 0) {
			channel.writeNulls(nBytes * ioTable.getTable().height());
		} else {
			int written = 0;
			switch (nBytes) {
				case Integer.BYTES:
					while (written < column.size()) {
						ByteBuffer buffer = channel.getInternalBuffer();
						written += ColumnIO.putCategoricalIntegers(column, written, buffer);
					}
					channel.flush();
					break;
				case Short.BYTES:
					while (written < column.size()) {
						ByteBuffer buffer = channel.getInternalBuffer();
						written += ColumnIO.putCategoricalShorts(column, written, buffer);
					}
					channel.flush();
					break;
				case Byte.BYTES:
					while (written < column.size()) {
						ByteBuffer buffer = channel.getInternalBuffer();
						written += ColumnIO.putCategoricalBytes(column, written, buffer);
					}
					channel.flush();
					break;
				default:
					throw new AssertionError("Unexpected number of bytes " + nBytes);
			}
		}
	}

	@Override
	public void writeFixedLengthStrings(ColumnDescriptor columnDescriptor, SeekableDataOutputByteChannel channel) throws IOException {
		String missingRepresentation = columnDescriptor.getMissingRepresentation();
		final Column column = ioTable.getTable().column(columnDescriptor.getName());
		int maxLength = columnDescriptor.getMaxStringLength();
		final ObjectReader<String> stringReader = Readers.objectReader(column, String.class);
		while (stringReader.hasRemaining()) {
			final String read = stringReader.read();
			writeFixedLengthString(read == null ? missingRepresentation : read, maxLength, channel);
		}
	}

	@Override
	public void writeVarLengthStrings(ColumnDescriptor columnDescriptor, GlobalHeap<String> globalHeap,
									  SeekableDataOutputByteChannel channel)
			throws IOException {
		String missingRepresentation = columnDescriptor.getMissingRepresentation();
		final Column column = ioTable.getTable().column(columnDescriptor.getName());
		final ObjectReader<String> stringReader = Readers.objectReader(column, String.class);
		while (stringReader.hasRemaining()) {
			final String read = stringReader.read();
			writeVarLengthString(read == null ? missingRepresentation : read, globalHeap, channel);
		}
	}

}
