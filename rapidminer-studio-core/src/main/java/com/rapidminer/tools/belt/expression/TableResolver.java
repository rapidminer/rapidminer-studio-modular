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
package com.rapidminer.tools.belt.expression;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.ObjectReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;


/**
 * Resolves dynamic variables via the give {@link Table} and index (row number). Additional columns can be added on the
 * fly via {@link #addColumn(String, Column)}. Please note: This class (and therefore the overall expression parser) is
 * not thread safe.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TableResolver implements DynamicResolver {

	public static final String KEY_ATTRIBUTES = I18N.getGUIMessage("gui.dialog.function_input.regular_attributes");
	public static final String KEY_SPECIAL_ATTRIBUTES = I18N.getGUIMessage("gui.dialog.function_input.special_attributes");

	/**
	 * The table that will be used to resolve the given column names.
	 */
	private final Table table;

	private Map<String, Column> additionalColumns;

	private final Map<Column, NumericReader> numericReaders;
	private final Map<Column, ObjectReader<String>> stringReaders;
	private final Map<Column, ObjectReader<Instant>> instantReaders;
	private final Map<Column, ObjectReader<LocalTime>> localTimeReaders;
	private final Map<Column, ObjectReader<StringSet>> stringSetReaders;
	private final Map<Column, ObjectReader<StringList>> stringListReaders;

	/**
	 * Creates a new table resolver that resolves dynamic variables via the given table's columns.
	 *
	 * @param table
	 * 		the given table
	 */
	public TableResolver(Table table) {
		this.table = table;
		additionalColumns = null;
		numericReaders = new HashMap<>();
		stringReaders = new HashMap<>();
		instantReaders = new HashMap<>();
		localTimeReaders = new HashMap<>();
		stringSetReaders = new HashMap<>();
		stringListReaders = new HashMap<>();
	}

	@Override
	public Collection<FunctionInput> getAllVariables() {
		List<FunctionInput> functionInputs = new ArrayList<>(table.width());
		for (String label : table.labels()) {
			Column column = table.column(label);
			ColumnRole role = table.getFirstMetaData(label, ColumnRole.class);
			if (role == null) {
				functionInputs.add(new FunctionInput(FunctionInput.Category.DYNAMIC, KEY_ATTRIBUTES,
						label, ExpressionParserUtils.expressionTypeForColumnId(column.type().id()), null));
			} else {
				functionInputs.add(new FunctionInput(FunctionInput.Category.DYNAMIC,
						KEY_SPECIAL_ATTRIBUTES, label,
						ExpressionParserUtils.expressionTypeForColumnId(column.type().id()), role.name()));
			}
		}
		if (additionalColumns != null) {
			for (Map.Entry<String, Column> entry : additionalColumns.entrySet()) {
				String label = entry.getKey();
				Column column = entry.getValue();
				functionInputs.add(new FunctionInput(FunctionInput.Category.DYNAMIC, KEY_ATTRIBUTES,
						label, ExpressionParserUtils.expressionTypeForColumnId(column.type().id()), null));
			}
		}
		return functionInputs;
	}

	@Override
	public ExpressionType getVariableType(String variableName) {
		Column column = getColumnForName(variableName);
		if (column != null) {
			return ExpressionParserUtils.expressionTypeForColumnId(column.type().id());
		}
		return null;
	}

	@Override
	public String getStringValue(String variableName, int index) {
		Column column = getColumnForName(variableName);
		if (column == null) {
			return null;
		}
		ExpressionType variableType = ExpressionParserUtils.expressionTypeForColumnId(column.type().id());
		if (variableType != ExpressionType.STRING) {
			wrongTypeException(variableName, "string");
		}
		if (index < 0 || index >= column.size()) {
			return null;
		}
		ObjectReader<String> reader = stringReaders.computeIfAbsent(column,
				c -> Readers.objectReader(c, String.class));
		if (reader.position() != index - 1) {
			reader.setPosition(index - 1);
		}
		return reader.read();
	}

	@Override
	public double getDoubleValue(String variableName, int index) {
		Column column = getColumnForName(variableName);
		if (column == null) {
			return Double.NaN;
		}
		ExpressionType variableType = ExpressionParserUtils.expressionTypeForColumnId(column.type().id());
		if (variableType != ExpressionType.DOUBLE && variableType != ExpressionType.INTEGER) {
			wrongTypeException(variableName, "double");
		}
		if (index < 0 || index >= column.size()) {
			return Double.NaN;
		}
		NumericReader reader = numericReaders.computeIfAbsent(column, Readers::numericReader);
		if (reader.position() != index - 1) {
			reader.setPosition(index - 1);
		}
		return reader.read();
	}

	@Override
	public Instant getInstantValue(String variableName, int index) {
		Column column = getColumnForName(variableName);
		if (column == null) {
			return null;
		}
		ExpressionType variableType = ExpressionParserUtils.expressionTypeForColumnId(column.type().id());
		if (variableType != ExpressionType.INSTANT) {
			wrongTypeException(variableName, "date-time");
		}
		if (index < 0 || index >= column.size()) {
			return null;
		}
		ObjectReader<Instant> reader = instantReaders.computeIfAbsent(column,
				c -> Readers.objectReader(c, Instant.class));
		if (reader.position() != index - 1) {
			reader.setPosition(index - 1);
		}
		return reader.read();
	}

	@Override
	public LocalTime getLocalTimeValue(String variableName, int index) {
		Column column = getColumnForName(variableName);
		if (column == null) {
			return null;
		}
		ExpressionType variableType = ExpressionParserUtils.expressionTypeForColumnId(column.type().id());
		if (variableType != ExpressionType.LOCAL_TIME) {
			wrongTypeException(variableName, "local-time");
		}
		if (index < 0 || index >= column.size()) {
			return null;
		}
		ObjectReader<LocalTime> reader = localTimeReaders.computeIfAbsent(column,
				c -> Readers.objectReader(c, LocalTime.class));
		if (reader.position() != index - 1) {
			reader.setPosition(index - 1);
		}
		return reader.read();
	}

	@Override
	public StringSet getStringSetValue(String variableName, int index) {
		Column column = getColumnForName(variableName);
		if (column == null) {
			return null;
		}
		ExpressionType variableType = ExpressionParserUtils.expressionTypeForColumnId(column.type().id());
		if (variableType != ExpressionType.STRING_SET) {
			wrongTypeException(variableName, "text-set");
		}
		if (index < 0 || index >= column.size()) {
			return null;
		}
		ObjectReader<StringSet> reader = stringSetReaders.computeIfAbsent(column,
				c -> Readers.objectReader(c, StringSet.class));
		if (reader.position() != index - 1) {
			reader.setPosition(index - 1);
		}
		return reader.read();
	}

	@Override
	public StringList getStringListValue(String variableName, int index) {
		Column column = getColumnForName(variableName);
		if (column == null) {
			return null;
		}
		ExpressionType variableType = ExpressionParserUtils.expressionTypeForColumnId(column.type().id());
		if (variableType != ExpressionType.STRING_LIST) {
			wrongTypeException(variableName, "text-list");
		}
		if (index < 0 || index >= column.size()) {
			return null;
		}
		ObjectReader<StringList> reader = stringListReaders.computeIfAbsent(column,
				c -> Readers.objectReader(c, StringList.class));
		if (reader.position() != index - 1) {
			reader.setPosition(index - 1);
		}
		return reader.read();
	}

	/**
	 * This method can be used to add more columns to the resolver on the fly. (Can be useful, for example, to add newly
	 * generated columns.) If the given label already exists, the new column will hide the old column when resolving
	 * an expression.
	 *
	 * @param label
	 * 		the new column's label used to resolve variable names
	 * @param column
	 * 		the new column
	 */
	public void addColumn(String label, Column column) {
		ValidationUtilV2.requireNonNull(label, "label");
		ValidationUtilV2.requireNonNull(column, "column");
		if (additionalColumns == null) {
			additionalColumns = new HashMap<>();
		}
		additionalColumns.put(label, column);
	}

	/**
	 * Returns the column with the given label or {@code null} if the name is unknowns to the resolver. Searches the
	 * additional columns first and then the table.
	 *
	 * @param variableName
	 * 		the column label
	 * @return the column or {@code null}
	 */
	private Column getColumnForName(String variableName) {
		if (additionalColumns != null) {
			Column additionalColumn = additionalColumns.get(variableName);
			if (additionalColumn != null) {
				return additionalColumn;
			}
		}
		if (table.contains(variableName)) {
			return table.column(variableName);
		}
		return null;
	}

	/**
	 * Helper method throwing an IllegalStateException (used when the variable type does not match the requested type).
	 *
	 * @param variableName
	 * 		the variable name
	 * @param type
	 * 		type string used for the exception description
	 */
	private void wrongTypeException(String variableName, String type) {
		throw new IllegalStateException("the variable " + variableName + " does not have a " + type + " value");
	}

}
