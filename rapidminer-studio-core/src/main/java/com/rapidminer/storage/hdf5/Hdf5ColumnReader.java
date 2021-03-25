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

import static com.rapidminer.storage.hdf5.Hdf5DatasetReader.getDatasetByAddress;
import static com.rapidminer.storage.hdf5.Hdf5DatasetReader.getMissingValue;
import static com.rapidminer.storage.hdf5.Hdf5DatasetReader.toDataAddress;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Map;

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.buffer.TimeBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnIO;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.DateTimeColumn;
import com.rapidminer.belt.column.io.DateTimeColumnBuilder;
import com.rapidminer.belt.column.io.NominalColumnBuilder;
import com.rapidminer.belt.column.io.NumericColumnBuilder;
import com.rapidminer.belt.column.io.TimeColumnBuilder;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.hdf5.BufferedInChannel;
import com.rapidminer.hdf5.file.ColumnDescriptor;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.tools.Tools;

import io.jhdf.GlobalHeap;
import io.jhdf.HdfFile;
import io.jhdf.HdfFileChannel;
import io.jhdf.Utils;
import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;
import io.jhdf.api.dataset.ContiguousDataset;
import io.jhdf.object.datatype.StringData;
import io.jhdf.object.datatype.VariableLength;


/**
 * A Reader that reads values from {@link Dataset}s into {@link Column}s.
 *
 * @author Gisa Meier
 * @see Hdf5TableReader
 * @since 9.9.0
 */
class Hdf5ColumnReader {

	private static final Column ZERO_REAL_COLUMN = Buffers.realBuffer(0).toColumn();
	private static final Column ZERO_INT_COLUMN = Buffers.integer53BitBuffer(0).toColumn();
	private static final Column ZERO_DATE_TIME_COLUMN = Buffers.dateTimeBuffer(0, true).toColumn();
	private static final Column ZERO_TIME_COLUMN = Buffers.timeBuffer(0).toColumn();
	static final int NANOS_PER_MILLI = 1_000_000;
	static final int MILLIS_PER_SECOND = 1000;

	private final int numberOfRows;
	private final HdfFile hdfFile;
	private final BufferedInChannel inChannel;
	private final Map<Long, GlobalHeap> heaps;

	Hdf5ColumnReader(int numberOfRows, HdfFile hdfFile, BufferedInChannel inChannel, Map<Long, GlobalHeap> heaps) {
		this.numberOfRows = numberOfRows;
		this.hdfFile = hdfFile;
		this.inChannel = inChannel;
		this.heaps = heaps;
	}

	/**
	 * Reads the dataset into a column of the specified column type.
	 *
	 * @param columnType
	 * 		the type of the column to create
	 * @param dataset
	 * 		the dataset to read
	 * @param dictionary
	 *        {@code null} or an ordered set of dictionary values starting with {@code null}
	 * @return the column created from the dataset
	 * @throws IOException
	 * 		if reading fails
	 */
	Column read(ColumnType<?> columnType, Dataset dataset, LinkedHashSet<String> dictionary) throws IOException {
		if (dataset instanceof ContiguousDataset) {
			ContiguousDataset contiguousDataset = (ContiguousDataset) dataset;
			long dataAddress = toDataAddress(hdfFile, contiguousDataset.getDataAddress());
			if (inChannel.position(dataAddress).position() != dataAddress) {
				throw new IOException("Cannot move to position " + dataAddress);
			}
			if (columnType.id() == Column.TypeId.NOMINAL) {
				return handleContiguousNominal(dictionary, contiguousDataset);
			}
			return handleContiguous(columnType, contiguousDataset);

		} else {
			throw new HdfReaderException(HdfReaderException.Reason.NON_CONTIGUOUS,
					"non-contigous dataset " + dataset.getPath());
		}
	}

	/**
	 * Reads the data from the dataset into a column of the given non-nominal type.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private Column handleContiguous(ColumnType<?> nonNominalType, ContiguousDataset dataset) throws IOException {
		if (numberOfRows > 0) {
			if (nonNominalType.category() == Column.Category.NUMERIC) {
				return fillNumeric(nonNominalType, dataset);
			} else if (nonNominalType.id() == Column.TypeId.DATE_TIME) {
				return fillDateTime(dataset);
			} else if (nonNominalType.id() == Column.TypeId.TIME) {
				//handle the case that it is time stored as date-time in pre 9.9
				Attribute type = dataset.getAttribute(TableWriter.ATTRIBUTE_TYPE);
				if (type != null) {
					Object typeData = type.getData();
					if ((typeData instanceof String) &&
							typeData.equals(ColumnDescriptor.Hdf5ColumnType.DATE_TIME.toString())) {
						return fillTimeFromDateTime(dataset);
					}
				}
				return fillTime(dataset);
			} else {
				throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE,
						nonNominalType + " not allowed as type for " +
								dataset.getPath());
			}
		} else {
			switch (nonNominalType.id()) {
				case REAL:
					return ZERO_REAL_COLUMN;
				case INTEGER_53_BIT:
					return ZERO_INT_COLUMN;
				case DATE_TIME:
					return ZERO_DATE_TIME_COLUMN;
				case TIME:
					return ZERO_TIME_COLUMN;
				default:
					throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE,
							nonNominalType + " not allowed as type for " +
									dataset.getPath());
			}
		}
	}

	/**
	 * Reads nominal values from the dataset into a nominal column with the given dictionary. Supported types are
	 * mapping indices as {@code byte}, {@code short} or {@code int} or fixed or variable-length {@code String}s.
	 */
	private Column handleContiguousNominal(LinkedHashSet<String> dictionary, ContiguousDataset dataset) throws IOException {
		if (dataset.getJavaType().equals(String.class)) {
			return fillNominalStrings(dataset);
		} else {
			//via dictionary
			return fillNominalIndices(dictionary, dataset);
		}
	}

	/**
	 * Fills the category indices into a nominal column with the given dictionary.
	 */
	private Column fillNominalIndices(LinkedHashSet<String> dictionary, ContiguousDataset dataset) throws IOException {
		NominalColumnBuilder builder = ColumnIO.readNominal(dictionary, numberOfRows);

		fillNominalData(dataset, builder);

		Attribute positiveIndex = dataset.getAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX);
		if (positiveIndex != null) {
			int positive = getPositiveIndex(positiveIndex);
			try {
				return builder.toBooleanColumn(positive);
			} catch (IllegalArgumentException e) {
				//handle wrong positive index
				return convertException(dataset, e);
			}
		} else {
			return builder.toColumn();
		}
	}

	/**
	 * Fills the category indices into the nominal column builder.
	 */
	private void fillNominalData(ContiguousDataset dataset, NominalColumnBuilder builder) throws IOException {
		if (dataset.getJavaType().equals(byte.class)) {
			try {
				while (builder.position() < numberOfRows) {
					builder.putBytes(inChannel.next(1));
				}
			} catch (IllegalArgumentException e) {
				//handle category indices not matching dictionary
				convertException(dataset, e);
			}
		} else if (dataset.getJavaType().equals(short.class)) {
			try {
				while (builder.position() < numberOfRows) {
					builder.putShorts(inChannel.next(Short.BYTES));
				}
			} catch (IllegalArgumentException e) {
				//handle category indices not matching dictionary
				convertException(dataset, e);
			}
		} else if (dataset.getJavaType().equals(int.class)) {
			try {
				while (builder.position() < numberOfRows) {
					builder.putIntegers(inChannel.next(Integer.BYTES));
				}
			} catch (IllegalArgumentException e) {
				//handle category indices not matching dictionary
				convertException(dataset, e);
			}
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE,
					dataset.getJavaType() + " not " +
							"supported for nominal column, dataset: " + dataset.getPath());
		}
	}

	/**
	 * Retrieves the positive index value from the hdf5 attribute.
	 */
	private int getPositiveIndex(Attribute positiveIndex) {
		final Object indexData = positiveIndex.getData();
		if (!(indexData instanceof Number)) {
			throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE,
					TableWriter.ATTRIBUTE_POSITVE_INDEX + " attribute must have a number value");
		}
		return ((Number) indexData).intValue();
	}

	/**
	 * Creates a nominal column from a String valued dataset.
	 */
	private Column fillNominalStrings(ContiguousDataset dataset) throws IOException {
		String missingReplace = getMissingValue(dataset);
		NominalBuffer nominalBuffer = Buffers.nominalBuffer(numberOfRows);
		if (dataset.getDataType() instanceof StringData) {
			fillNominalFixedStrings(dataset, missingReplace, nominalBuffer);
		} else if (dataset.getDataType() instanceof VariableLength) {
			fillNominalVarStrings(dataset, missingReplace, nominalBuffer);
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE,
					"unsupported String datatype " + dataset.getDataType() + " for dataset " + dataset.getPath());
		}
		Attribute positiveIndex = dataset.getAttribute(TableWriter.ATTRIBUTE_POSITVE_INDEX);
		if (positiveIndex != null) {
			int positive = getPositiveIndex(positiveIndex);
			String positiveValue = null;
			if (positive > 0 && positive < 3) {
				// positive value must be at most 2, so have to ignore null values
				// this is a bit of effort, but usually one would not store binominal as raw strings but with a
				// dictionary instead
				int count = 0;
				for (int i = 0; i < nominalBuffer.size(); i++) {
					final String value = nominalBuffer.get(i);
					if (value != null) {
						count++;
						if (count == positive) {
							positiveValue = value;
							break;
						}
					}
				}
			}
			try {
				return nominalBuffer.toBooleanColumn(positiveValue);
			} catch (IllegalArgumentException e) {
				//handle wrong positive index
				return convertException(dataset, e);
			}
		} else {
			return nominalBuffer.toColumn();
		}
	}

	/**
	 * Fills variable length strings from the dataset into the nominal buffer.
	 */
	private void fillNominalVarStrings(ContiguousDataset dataset, String missingReplace, NominalBuffer nominalBuffer) throws IOException {
		final Charset charset = ((VariableLength) dataset.getDataType()).getEncoding();
		final HdfFileChannel hdfFc = hdfFile.getHdfChannel();
		for (int i = 0; i < numberOfRows; i++) {
			inChannel.readInt();//unused size
			long address = inChannel.readLong(hdfFc.getSizeOfOffsets());
			int index = inChannel.readInt();
			GlobalHeap heap = heaps.computeIfAbsent(address, add -> new GlobalHeap(hdfFc, add));
			ByteBuffer bb = heap.getObjectData(index);
			String value = charset.decode(bb).toString();
			if (value.equals(missingReplace)) {
				value = null;
			}
			nominalBuffer.set(i, value);
		}
	}

	/**
	 * Fills fixed length strings from the dataset into the nominal buffer.
	 */
	private void fillNominalFixedStrings(ContiguousDataset dataset, String missingReplace,
										 NominalBuffer nominalBuffer) throws IOException {
		int width = dataset.getDataType().getSize();
		final StringData.StringPaddingHandler paddingHandler =
				((StringData) dataset.getDataType()).getStringPaddingHandler();
		final Charset charset = ((StringData) dataset.getDataType()).getCharset();
		ByteBuffer byteBuffer = inChannel.next(width);
		for (int i = 0; i < numberOfRows; i++) {
			if (byteBuffer.remaining() < width) {
				byteBuffer = inChannel.next(width);
			}
			ByteBuffer elementBuffer = Utils.createSubBuffer(byteBuffer, width);
			paddingHandler.setBufferLimit(elementBuffer);
			String value = charset.decode(elementBuffer).toString();
			if (value.equals(missingReplace)) {
				value = null;
			}
			nominalBuffer.set(i, value);
		}
	}

	/**
	 * Fills date-time data from the dataset into the date-time attribute. If the dataset has the additional hdf5
	 * attribute for nanoseconds, the nanosecond data is added after reading the seconds.
	 *
	 * @throws IOException
	 * 		if reading fails
	 */
	private Column fillDateTime(ContiguousDataset dataset) throws IOException {
		if (!dataset.getJavaType().equals(long.class)) {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE, "date-time seconds must be " +
					"stored as long values for " + dataset.getPath());
		}

		final DateTimeColumnBuilder builder = ColumnIO.readDateTime(numberOfRows);
		try {
			while (builder.position() < numberOfRows) {
				builder.putSeconds(inChannel.next(Long.BYTES));
			}
		} catch (IllegalArgumentException e) {
			//handle seconds not in range
			return convertException(dataset, e);
		}
		Attribute nanos = dataset.getAttribute(TableWriter.ATTRIBUTE_ADDITIONAL);
		if (nanos != null) {
			fillNanos(dataset, builder, nanos);
		}
		return builder.toColumn();
	}

	/**
	 * Fills a time column from a date-time hdf5 column using the belt converter.
	 */
	private Column fillTimeFromDateTime(ContiguousDataset dataset) throws IOException {
		if (!dataset.getJavaType().equals(long.class)) {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE, "date-time seconds must be " +
					"stored as long values for " + dataset.getPath());
		}

		Attribute nanos = dataset.getAttribute(TableWriter.ATTRIBUTE_ADDITIONAL);
		if (nanos == null) {
			return fillTimeFromDateSeconds();
		} else {
			long[] seconds = new long[numberOfRows];
			int end = 0;
			while (end < numberOfRows) {
				ByteBuffer byteBuffer = inChannel.next(Long.BYTES);
				LongBuffer buffer = byteBuffer.asLongBuffer();
				int length = Math.min(numberOfRows - end, buffer.remaining());
				buffer.get(seconds, end, length);
				byteBuffer.position(byteBuffer.position() + length * Long.BYTES);
				end += length;
			}
			return fillTimeFromSecondsAndNanos(dataset, nanos, seconds);
		}


	}

	/**
	 * Fills a time column from a date-time hdf5 column with seconds and nanos.
	 */
	private Column fillTimeFromSecondsAndNanos(ContiguousDataset dataset, Attribute nanos, long[] seconds) throws IOException {
		if (!nanos.getJavaType().equals(Long.class)) {
			throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE,
					TableWriter.ATTRIBUTE_ADDITIONAL + " attribute must contain a reference to another dataset " +
							"for dataset " + dataset.getPath());
		}
		long address = (long) nanos.getData();
		Dataset nanoData = getDatasetByAddress(hdfFile, address);
		if (nanoData instanceof ContiguousDataset) {
			ContiguousDataset contiguousDataset = (ContiguousDataset) nanoData;
			if (!nanoData.getJavaType().equals(int.class)) {
				throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE,
						"date-time nanoseconds must be " +
								"stored as int values for " + nanoData.getPath());
			}
			long dataAddress = toDataAddress(hdfFile, contiguousDataset.getDataAddress());
			if (inChannel.position(dataAddress).position() == dataAddress) {
				return fillTimeBuffer(seconds);
			} else {
				throw new IOException("Cannot move to position " + dataAddress);
			}
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.NON_CONTIGUOUS,
					"non-contigous dataset " + dataset.getPath());
		}
	}

	/**
	 * Fills second and nano date-time date into a time buffer with conversion.
	 */
	private Column fillTimeBuffer(long[] seconds) throws IOException {
		TimeBuffer timeBuffer = Buffers.timeBuffer(numberOfRows, false);
		Calendar calendar = Tools.getPreferredCalendar();
		ByteBuffer byteBuffer = inChannel.next(Integer.BYTES);
		IntBuffer buffer = byteBuffer.asIntBuffer();
		for (int i = 0; i < numberOfRows; i++) {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * Integer.BYTES);
				byteBuffer = inChannel.next(Integer.BYTES);
				buffer = byteBuffer.asIntBuffer();
			}
			int nanoseconds = buffer.get();
			if (seconds[i] == DateTimeColumn.MISSING_VALUE) {
				timeBuffer.set(i, null);
			} else {
				int millis = nanoseconds / NANOS_PER_MILLI;
				timeBuffer.set(i, BeltConverter.legacyTimeDoubleToNanoOfDay(
						seconds[i] * ((double) MILLIS_PER_SECOND) + millis, calendar));
			}
		}
		return timeBuffer.toColumn();
	}

	/**
	 * Fills a time column from a date-time hdf5 column with just seconds.
	 */
	private Column fillTimeFromDateSeconds() throws IOException {
		TimeBuffer timeBuffer = Buffers.timeBuffer(numberOfRows, false);
		ByteBuffer byteBuffer = inChannel.next(Long.BYTES);
		LongBuffer buffer = byteBuffer.asLongBuffer();
		Calendar calendar = Tools.getPreferredCalendar();
		for (int i = 0; i < numberOfRows; i++) {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * Long.BYTES);
				byteBuffer = inChannel.next(Long.BYTES);
				buffer = byteBuffer.asLongBuffer();
			}
			long seconds = buffer.get();
			if (seconds == DateTimeColumn.MISSING_VALUE) {
				timeBuffer.set(i, null);
			} else {
				timeBuffer.set(i, BeltConverter.legacyTimeDoubleToNanoOfDay(seconds * MILLIS_PER_SECOND, calendar));
			}
		}
		return timeBuffer.toColumn();
	}

	/**
	 * Fills the nanoseconds into the builder.
	 */
	private void fillNanos(ContiguousDataset dataset, DateTimeColumnBuilder builder, Attribute nanos) throws IOException {
		if (!nanos.getJavaType().equals(Long.class)) {
			throw new HdfReaderException(HdfReaderException.Reason.ILLEGAL_TYPE,
					TableWriter.ATTRIBUTE_ADDITIONAL + " attribute must contain a reference to another dataset " +
							"for dataset " + dataset.getPath());
		}
		long address = (long) nanos.getData();
		Dataset nanoData = getDatasetByAddress(hdfFile, address);
		if (nanoData instanceof ContiguousDataset) {
			ContiguousDataset contiguousDataset = (ContiguousDataset) nanoData;
			if (!nanoData.getJavaType().equals(int.class)) {
				throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE,
						"date-time nanoseconds must be " +
								"stored as int values for " + nanoData.getPath());
			}
			long dataAddress = toDataAddress(hdfFile, contiguousDataset.getDataAddress());
			if (inChannel.position(dataAddress).position() == dataAddress) {
				try {
					while (builder.nanoPosition() < numberOfRows) {
						builder.putNanos(inChannel.next(Integer.BYTES));
					}
				} catch (IllegalArgumentException e) {
					//handle nanos not in range
					convertException(nanoData, e);
				}
			} else {
				throw new IOException("Cannot move to position " + dataAddress);
			}
		} else {
			throw new HdfReaderException(HdfReaderException.Reason.NON_CONTIGUOUS,
					"non-contigous dataset " + dataset.getPath());
		}
	}

	/**
	 * Fills the long nanosecond of the day values from the dataset into a time column.
	 */
	private Column fillTime(ContiguousDataset dataset) throws IOException {
		if (!dataset.getJavaType().equals(long.class)) {
			throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE, "time nanoseconds must be " +
					"stored as long values for " + dataset.getPath());
		}
		final TimeColumnBuilder builder = ColumnIO.readTime(numberOfRows);
		try {
			while (builder.position() < numberOfRows) {
				builder.put(inChannel.next(Long.BYTES));
			}
		} catch (IllegalArgumentException e) {
			//handle nanoseconds out of range
			return convertException(dataset, e);
		}
		return builder.toColumn();
	}

	/**
	 * Fills numeric values from the dataset into a column of the given type. Supported data types are {@code double},
	 * {@code float}, {@code int}, {@code long}.
	 */
	private Column fillNumeric(ColumnType<?> numericType, ContiguousDataset dataset) throws IOException {
		if (dataset.getJavaType().equals(double.class)) {
			NumericColumnBuilder builder;
			if (numericType.id() == Column.TypeId.INTEGER_53_BIT) {
				builder = ColumnIO.readInteger53Bit(numberOfRows);
			} else {
				builder = ColumnIO.readReal(numberOfRows);
			}
			while (builder.position() < numberOfRows) {
				builder.put(inChannel.next(Double.BYTES));
			}
			return builder.toColumn();
		} else {
			NumericBuffer numericBuffer;
			if (numericType.id() == Column.TypeId.INTEGER_53_BIT) {
				numericBuffer = Buffers.integer53BitBuffer(numberOfRows);
			} else {
				numericBuffer = Buffers.realBuffer(numberOfRows);
			}

			if (dataset.getJavaType().equals(float.class)) {
				fillNumericBufferFloat(numericBuffer);
			} else if (dataset.getJavaType().equals(int.class)) {
				fillNumericBufferInt(numericBuffer);
			} else if (dataset.getJavaType().equals(long.class)) {
				fillNumericBufferLong(numericBuffer);
			} else {
				throw new HdfReaderException(HdfReaderException.Reason.UNSUPPORTED_TYPE,
						dataset.getJavaType() + " not supported for numeric column, dataset: " + dataset.getPath());
			}
			return numericBuffer.toColumn();
		}
	}

	/**
	 * Fills long values into the buffer.
	 */
	private void fillNumericBufferLong(NumericBuffer numericBuffer) throws IOException {
		ByteBuffer byteBuffer = inChannel.next(Long.BYTES);
		LongBuffer buffer = byteBuffer.asLongBuffer();
		for (int i = 0; i < numberOfRows; i++) {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * Long.BYTES);
				byteBuffer = inChannel.next(Long.BYTES);
				buffer = byteBuffer.asLongBuffer();
			}
			numericBuffer.set(i, buffer.get());
		}
	}

	/**
	 * Fills int values into the buffer.
	 */
	private void fillNumericBufferInt(NumericBuffer numericBuffer) throws IOException {
		ByteBuffer byteBuffer = inChannel.next(Integer.BYTES);
		IntBuffer buffer = byteBuffer.asIntBuffer();
		for (int i = 0; i < numberOfRows; i++) {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * Integer.BYTES);
				byteBuffer = inChannel.next(Integer.BYTES);
				buffer = byteBuffer.asIntBuffer();
			}
			numericBuffer.set(i, buffer.get());
		}
	}

	/**
	 * Fills float values into the buffer.
	 */
	private void fillNumericBufferFloat(NumericBuffer numericBuffer) throws IOException {
		ByteBuffer byteBuffer = inChannel.next(Float.BYTES);
		FloatBuffer buffer = byteBuffer.asFloatBuffer();
		for (int i = 0; i < numberOfRows; i++) {
			if (buffer.remaining() < 1) {
				byteBuffer.position(byteBuffer.position() + buffer.position() * Float.BYTES);
				byteBuffer = inChannel.next(Float.BYTES);
				buffer = byteBuffer.asFloatBuffer();
			}
			numericBuffer.set(i, buffer.get());
		}
	}

	/**
	 * Converts {@link IllegalArgumentException} coming from belt into {@link HdfReaderException}.
	 */
	private static Column convertException(Dataset dataset, IllegalArgumentException e) {
		throw new HdfReaderException(HdfReaderException.Reason.INCONSISTENT_FILE,
				e.getMessage() + " for " + dataset.getPath());
	}

}