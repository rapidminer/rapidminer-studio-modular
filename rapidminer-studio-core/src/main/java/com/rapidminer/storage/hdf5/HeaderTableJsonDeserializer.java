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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnIO;
import com.rapidminer.belt.column.Columns;
import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.container.Pair;


/**
 * Inverse of the {@link HeaderTableJsonSerializer}. Used by {@link com.rapidminer.operator.AbstractIOTableModel} for
 * storing in {@link com.rapidminer.repository.versioned.FilesystemRepositoryAdapter}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public class HeaderTableJsonDeserializer extends StdDeserializer<IOTable> {

	private static final Map<String, Column> EMPTY_COLUMNS = new HashMap<>();

	static {
		EMPTY_COLUMNS.put(Column.TypeId.INTEGER_53_BIT.toString(), Buffers.integer53BitBuffer(0).toColumn());
		EMPTY_COLUMNS.put(Column.TypeId.REAL.toString(), Buffers.realBuffer(0).toColumn());
		EMPTY_COLUMNS.put(Column.TypeId.TIME.toString(), Buffers.timeBuffer(0).toColumn());
		EMPTY_COLUMNS.put(Column.TypeId.DATE_TIME.toString(), Buffers.dateTimeBuffer(0, false).toColumn());
		EMPTY_COLUMNS.put(Column.TypeId.TEXT.toString(), Buffers.textBuffer(0).toColumn());
		EMPTY_COLUMNS.put(Column.TypeId.TEXT_SET.toString(), Buffers.textsetBuffer(0).toColumn());
		EMPTY_COLUMNS.put(Column.TypeId.TEXT_LIST.toString(), Buffers.textlistBuffer(0).toColumn());
	}

	public HeaderTableJsonDeserializer() {
		this(null);
	}

	public HeaderTableJsonDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public IOTable deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
		TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);
		int width = ((IntNode) treeNode.get(HeaderTableJsonSerializer.WIDTH)).intValue();
		ArrayNode columns = (ArrayNode) treeNode.get(HeaderTableJsonSerializer.COLUMNS);
		TableBuilder tableBuilder = Builders.newTableBuilder(0);
		for (int i = 0; i < width; i++) {
			JsonNode jsonNode = columns.get(i);
			String type = jsonNode.get(HeaderTableJsonSerializer.TYPE).asText();
			String name = jsonNode.get(HeaderTableJsonSerializer.NAME).asText();
			if (Column.TypeId.NOMINAL.toString().equals(type)) {
				tableBuilder.add(name, readNominal(jsonNode));
			} else {
				tableBuilder.add(name, EMPTY_COLUMNS.get(type));
			}
			addMetaData(tableBuilder, jsonNode, name);
		}
		Table table = tableBuilder.build(new SequentialContext());

		return new IOTable(table);
	}

	/**
	 * Reads the nominal dictionary and the positive value, if present.
	 */
	private Column readNominal(JsonNode jsonNode) {
		LinkedHashSet<String> dict = new LinkedHashSet<>();
		boolean foundGap = false;
		int pos = 0;
		for (Iterator<JsonNode> dictionary = jsonNode.get(HeaderTableJsonSerializer.DICTIONARY).elements();
			 dictionary.hasNext(); ) {
			JsonNode next = dictionary.next();
			if (next.isNull() && pos != 0) {
				foundGap = true;
			}
			String value = next.asText();
			dict.add(pos == 0 ? null : value);
			pos++;
		}
		if (foundGap) {
			dict.remove("null");
			return handleNominalWithGaps(dict, pos, jsonNode);
		}
		Column col;
		if (jsonNode.has(HeaderTableJsonSerializer.POSITIVE_VALUE)) {
			String positiveValue = jsonNode.get(HeaderTableJsonSerializer.POSITIVE_VALUE).asText();
			int index = Arrays.asList(dict.toArray(new String[0])).indexOf(positiveValue);
			col = ColumnIO.readNominal(dict, 0).toBooleanColumn(index);
		} else {
			col = ColumnIO.readNominal(dict, 0).toColumn();
		}
		return col;
	}

	/**
	 * Creates a nominal column with a dictionary with gaps.
	 */
	private Column handleNominalWithGaps(Set<String> distinctValues, int length, JsonNode jsonNode) {
		NominalBuffer nominalBuffer = Buffers.nominalBuffer(length - 1);
		int pos = 0;
		String lastNotContained = "";
		for (Iterator<JsonNode> dictionary = jsonNode.get(HeaderTableJsonSerializer.DICTIONARY).elements();
			 dictionary.hasNext(); ) {
			JsonNode next = dictionary.next();
			if (pos > 0) {
				if (next.isNull()) {
					String newValue = findNotContained(distinctValues, lastNotContained);
					distinctValues.add(newValue);
					lastNotContained = newValue;
					nominalBuffer.set(pos - 1, newValue);
					nominalBuffer.set(pos - 1, null);
				} else {
					nominalBuffer.set(pos - 1, next.asText());
				}
			}
			pos++;
		}
		return Columns.removeUnusedDictionaryValues(nominalBuffer.toColumn(), Columns.CleanupOption.REMOVE,
				new SequentialContext()).stripData();
	}

	private String findNotContained(Set<String> distinctValues, String lastNotContained) {
		String value = lastNotContained + "\u0000";
		while (distinctValues.contains(value)) {
			//probably never happens, but make sure
			value += "\u0000";
		}
		return value;
	}

	/**
	 * Reads the meta data.
	 */
	private void addMetaData(TableBuilder tableBuilder, JsonNode jsonNode, String name) {
		if (jsonNode.has(HeaderTableJsonSerializer.ROLE)) {
			String role = jsonNode.get(HeaderTableJsonSerializer.ROLE).asText();
			tableBuilder.addMetaData(name, ColumnRole.valueOf(role.toUpperCase(Locale.ROOT)));
		}
		if (jsonNode.has(HeaderTableJsonSerializer.COLUMN_REFERENCE) &&
				jsonNode.has(HeaderTableJsonSerializer.COLUMN_REFERENCE_VALUE)) {
			JsonNode referenceNode = jsonNode.get(HeaderTableJsonSerializer.COLUMN_REFERENCE);
			JsonNode valueNode = jsonNode.get(HeaderTableJsonSerializer.COLUMN_REFERENCE_VALUE);
			String reference = referenceNode.isNull() ? null : referenceNode.asText();
			String value = valueNode.isNull() ? null : valueNode.asText();
			tableBuilder.addMetaData(name, new ColumnReference(reference, value));
		}
		if (jsonNode.has(HeaderTableJsonSerializer.MD)) {
			for (Iterator<JsonNode> elements = jsonNode.get(HeaderTableJsonSerializer.MD).elements();
				 elements.hasNext(); ) {
				JsonNode next = elements.next();
				String mdType = next.get(HeaderTableJsonSerializer.MD_TYPE).asText();
				String mdValue = next.get(HeaderTableJsonSerializer.MD_VALUE).asText();
				addCustomMetaData(tableBuilder, name, mdType, mdValue);
			}
		}
	}

	private void addCustomMetaData(TableBuilder tableBuilder, String name, String mdType, String mdValue) {
		Pair<Class<?>, ColumnMetaDataStorageRegistry.Hdf5Deserializer> deserializer =
				ColumnMetaDataStorageRegistry.getDeserializer(mdType);
		if (deserializer != null) {
			try {
				ColumnMetaData columnMetaData =
						deserializer.getSecond().applyWithException(getAsClass(mdValue,
								deserializer.getFirst()));
				tableBuilder.addMetaData(name, columnMetaData);
			} catch (ColumnMetaDataStorageRegistry.IllegalHdf5FormatException e) {
				LogService.getRoot().warning("Could not read custom column meta data " + e.getMessage());
			}
		}
	}

	/**
	 * Converts the String value to the specified class which is one of {@code String.class, byte.class, short.class,
	 * int.class, double.class, long.class}.
	 */
	private Object getAsClass(String mdValue, Class<?> clazz) {
		if (int.class.equals(clazz)) {
			return Integer.parseInt(mdValue);
		}
		if (short.class.equals(clazz)) {
			return Short.parseShort(mdValue);
		}
		if (byte.class.equals(clazz)) {
			return Byte.parseByte(mdValue);
		}
		if (long.class.equals(clazz)) {
			return Long.parseLong(mdValue);
		}
		if (double.class.equals(clazz)) {
			return Double.parseDouble(mdValue);
		}
		return mdValue;
	}
}
