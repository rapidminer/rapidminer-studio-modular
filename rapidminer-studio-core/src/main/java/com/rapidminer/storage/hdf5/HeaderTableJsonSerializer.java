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
package com.rapidminer.storage.hdf5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Dictionary;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.container.Triple;


/**
 * Serializes a header {@link IOTable} to json ignoring the data. Used by
 * {@link com.rapidminer.operator.AbstractIOTableModel} for storing in
 * {@link com.rapidminer.repository.versioned.FilesystemRepositoryAdapter}.
 *
 * @author Jonas Wilms-Pfau, Gisa Meier
 * @since 9.10
 */
public class HeaderTableJsonSerializer extends JsonSerializer<IOTable> {

	/**
	 * The number of columns
	 */
	static final String WIDTH = "width";

	/**
	 * The column array
	 */
	static final String COLUMNS = "columns";

	/**
	 * The name of a column
	 */
	static final String NAME = "name";

	/**
	 * The {@link com.rapidminer.belt.column.ColumnType}
	 */
	static final String TYPE = "type";

	/**
	 * The positive value of a boolean column, might be {@code null}
	 */
	static final String POSITIVE_VALUE = "positiveValue";

	/**
	 * The referenced column name
	 *
	 * @see ColumnReference#getColumn() ()
	 */
	static final String COLUMN_REFERENCE = "colRef";

	/**
	 * The referenced column value
	 *
	 * @see ColumnReference#getValue()
	 */
	static final String COLUMN_REFERENCE_VALUE = "colRefVal";

	/**
	 * The {@link com.rapidminer.belt.util.ColumnRole ColumnRole}
	 */
	static final String ROLE = "role";

	/**
	 * The meta data array
	 */
	static final String MD = "md";

	/**
	 * The meta data type
	 */
	static final String MD_TYPE = "type";

	/**
	 * The meta data value
	 */
	static final String MD_VALUE = "value";

	/**
	 * The dictionary of a nominal column.
	 */
	static final String DICTIONARY = "dictionary";

	@Override
	public void serialize(IOTable ioTable, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		Table table = ioTable.getTable();
		gen.writeStartObject();

		gen.writeNumberField(WIDTH, table.width());

		gen.writeArrayFieldStart(COLUMNS);
		for (int i = 0; i < table.width(); i++) {
			gen.writeStartObject();
			writeColumnMetaData(gen, table, i);
			writeDictionary(gen, table.column(i));
			gen.writeEndObject();
		}
		gen.writeEndArray();
		gen.writeEndObject();
	}


	/**
	 * Writes the column metadata to json
	 *
	 * @param gen
	 * 		the json generator
	 * @param value
	 * 		the table
	 * @param i
	 * 		the current index
	 * @throws IOException
	 * 		if there is either an underlying I/O problem or encoding issue at format layer
	 */
	protected void writeColumnMetaData(JsonGenerator gen, Table value, int i) throws IOException {
		final String label = value.label(i);
		final Column column = value.column(i);
		gen.writeStringField(NAME, label);
		gen.writeStringField(TYPE, column.type().id().toString());
		if (column.type().id() == Column.TypeId.NOMINAL && column.getDictionary().isBoolean()) {
			gen.writeStringField(POSITIVE_VALUE,
					column.getDictionary().get(column.getDictionary().getPositiveIndex()));
		}

		List<ColumnMetaData> metaDataList = value.getMetaData(label);
		List<ColumnMetaData> otherMetaData = new ArrayList<>();
		for (ColumnMetaData metaData : metaDataList) {
			if (metaData instanceof ColumnReference) {
				gen.writeStringField(COLUMN_REFERENCE, ((ColumnReference) metaData).getColumn());
				gen.writeStringField(COLUMN_REFERENCE_VALUE, ((ColumnReference) metaData).getValue());
			} else if (metaData instanceof ColumnRole) {
				gen.writeStringField(ROLE, ((ColumnRole) metaData).name().toLowerCase(Locale.ROOT));
			} else {
				otherMetaData.add(metaData);
			}
		}
		if (!otherMetaData.isEmpty()) {
			List<Pair<String, String>> filteredMetaData = otherMetaData.stream().
					map(HeaderTableJsonSerializer::toKeyValuePair).filter(Objects::nonNull).
					collect(Collectors.toList());
			if (!filteredMetaData.isEmpty()) {
				gen.writeArrayFieldStart(MD);
				for (Pair<String, String> metaData : filteredMetaData) {
					gen.writeStartObject();
					gen.writeStringField(MD_TYPE, metaData.getFirst());
					gen.writeStringField(MD_VALUE, metaData.getSecond());
					gen.writeEndObject();
				}
				gen.writeEndArray();
			}
		}
	}

	/**
	 * Writes the dictionary values of nominal columns, starting with the {@code null} for the missing value.
	 */
	private void writeDictionary(JsonGenerator gen, Column column) throws IOException {
		if (column.type().id() == Column.TypeId.NOMINAL) {
			gen.writeArrayFieldStart(DICTIONARY);
			Dictionary dictionary = column.getDictionary();
			for (int i = 0; i <= dictionary.maximalIndex(); i++) {
				gen.writeString(dictionary.get(i));
			}
			gen.writeEndArray();
		}

	}

	/**
	 * Converts custom {@link ColumnMetaData} to String via the {@link ColumnMetaDataStorageRegistry}.
	 */
	private static Pair<String, String> toKeyValuePair(ColumnMetaData metaData) {
		Triple<String, Class<?>, ColumnMetaDataStorageRegistry.Hdf5Serializer> seri =
				ColumnMetaDataStorageRegistry.getSerializer(metaData.getClass());
		if (seri == null) {
			return null;
		}
		String value = Objects.toString(seri.getThird().apply(metaData), null);
		String type = seri.getFirst();
		if (value == null || type == null) {
			return null;
		}
		return new Pair<>(type, value);
	}

}
