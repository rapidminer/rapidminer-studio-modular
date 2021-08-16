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
import java.util.List;
import java.util.Set;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;


/**
 * Used for metadata transformation. Implements only {@link #getAllVariables()} and {@link #getVariableType(String)}.
 * Calling the other methods will lead to an exception.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class TableMetaDataResolver implements DynamicResolver {

	public static final String CANNOT_RESOLVE_VARIABLES_ERROR = "TableMetaDataResolver cannot resolve variable values";

	/**
	 * The table meta data that will be used to resolve the given column names.
	 */
	private final TableMetaData metaData;

	/**
	 * Creates a new resolver that uses the given {@link TableMetaData} to resolve dynamic variable names and types as
	 * well as the resulting expression type.
	 *
	 * @param metaData
	 * 		the given table metadata
	 */
	public TableMetaDataResolver(TableMetaData metaData) {
		this.metaData = metaData;
	}

	@Override
	public Collection<FunctionInput> getAllVariables() {
		Set<String> labels = metaData.labels();
		List<FunctionInput> functionInputs = new ArrayList<>(labels.size());
		for (String label : labels) {
			ColumnInfo column = metaData.column(label);
			if (column != null && column.getType().isPresent()) {
				ColumnRole role = metaData.getFirstColumnMetaData(label, ColumnRole.class);
				if (role == null) {
					functionInputs.add(new FunctionInput(FunctionInput.Category.DYNAMIC, TableResolver.KEY_ATTRIBUTES,
							label, ExpressionParserUtils.expressionTypeForColumnId(column.getType().get().id()),
							null));
				} else {
					functionInputs.add(new FunctionInput(FunctionInput.Category.DYNAMIC,
							TableResolver.KEY_SPECIAL_ATTRIBUTES, label,
							ExpressionParserUtils.expressionTypeForColumnId(column.getType().get().id()),
							role.name()));
				}
			}
		}
		return functionInputs;
	}

	@Override
	public ExpressionType getVariableType(String variableName) {
		ColumnInfo column = metaData.column(variableName);
		if (column == null || !column.getType().isPresent()) {
			return null;
		}
		return ExpressionParserUtils.expressionTypeForColumnId(column.getType().get().id());
	}

	/**
	 * @throws UnsupportedOperationException
	 * 		Not supported by this implementation.
	 */
	@Override
	public String getStringValue(String variableName, int index) {
		throw new UnsupportedOperationException(CANNOT_RESOLVE_VARIABLES_ERROR);
	}

	/**
	 * @throws UnsupportedOperationException
	 * 		Not supported by this implementation.
	 */
	@Override
	public double getDoubleValue(String variableName, int index) {
		throw new UnsupportedOperationException(CANNOT_RESOLVE_VARIABLES_ERROR);
	}

	/**
	 * @throws UnsupportedOperationException
	 * 		Not supported by this implementation.
	 */
	@Override
	public Instant getInstantValue(String variableName, int index) {
		throw new UnsupportedOperationException(CANNOT_RESOLVE_VARIABLES_ERROR);
	}

	/**
	 * @throws UnsupportedOperationException
	 * 		Not supported by this implementation.
	 */
	@Override
	public LocalTime getLocalTimeValue(String variableName, int index) {
		throw new UnsupportedOperationException(CANNOT_RESOLVE_VARIABLES_ERROR);
	}

	/**
	 * @throws UnsupportedOperationException
	 * 		Not supported by this implementation.
	 */
	@Override
	public StringSet getStringSetValue(String variableName, int index) {
		throw new UnsupportedOperationException(CANNOT_RESOLVE_VARIABLES_ERROR);
	}

	/**
	 * @throws UnsupportedOperationException
	 * 		Not supported by this implementation.
	 */
	@Override
	public StringList getStringListValue(String variableName, int index) {
		throw new UnsupportedOperationException(CANNOT_RESOLVE_VARIABLES_ERROR);
	}

}
