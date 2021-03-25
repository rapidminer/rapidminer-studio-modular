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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.parameter.ParameterHandler;


/**
 * A quick fix that allows to select an alternative column with the required type or category for a parameter.
 *
 * This class is a copy of {@link AttributeSelectionQuickFix} adjusted to {@link TableMetaData}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ColumnSelectionQuickFix extends DictionaryQuickFix {

	private final ParameterHandler handler;
	private final String parameterName;

	/**
	 * Creates a quickfix that replaces the column name for the parameter of the handler with another column instead of
	 * the current value.
	 *
	 * @param metaData
	 * 		the table meta data to check for alternative columns
	 * @param parameterName
	 * 		the name of the parameter to change
	 * @param handler
	 * 		the handler for the parameter
	 * @param currentValue
	 * 		the current value of the parameter
	 */
	public ColumnSelectionQuickFix(TableMetaData metaData, String parameterName, ParameterHandler handler,
								   String currentValue) {
		this(metaData, parameterName, handler, currentValue, (ColumnType<?>) null);
	}

	/**
	 * Creates a quickfix that replaces the column name for the parameter of the handler with another column of the
	 * required type instead of the current value.
	 *
	 * @param metaData
	 * 		the table meta data to check for alternative columns of the required type
	 * @param parameterName
	 * 		the name of the parameter to change
	 * @param handler
	 * 		the handler for the parameter
	 * @param currentValue
	 * 		the current value of the parameter
	 * @param requiredType
	 * 		the required type for alternative columns
	 */
	public ColumnSelectionQuickFix(TableMetaData metaData, String parameterName, ParameterHandler handler,
								   String currentValue, ColumnType<?> requiredType) {
		super(parameterName, requiredType == null ? metaData.labels() :
				metaData.selectByType(requiredType), currentValue, handler.getParameters()
				.getParameterType(parameterName).getDescription());
		this.handler = handler;
		this.parameterName = parameterName;
	}

	/**
	 * Creates a quickfix that replaces the column name for the parameter of the handler with another column of the
	 * required category instead of the current value.
	 *
	 * @param metaData
	 * 		the table meta data to check for alternative columns of the required type
	 * @param parameterName
	 * 		the name of the parameter to change
	 * @param handler
	 * 		the handler for the parameter
	 * @param currentValue
	 * 		the current value of the parameter
	 * @param requiredCategory
	 * 		the required category for alternative columns
	 */
	public ColumnSelectionQuickFix(TableMetaData metaData, String parameterName, ParameterHandler handler,
								   String currentValue, Column.Category requiredCategory) {
		super(parameterName, requiredCategory == null ? metaData.labels() :
				metaData.selectByCategory(requiredCategory), currentValue, handler.getParameters()
				.getParameterType(parameterName).getDescription());
		this.handler = handler;
		this.parameterName = parameterName;
	}

	@Override
	public void insertChosenOption(String chosenOption) {
		handler.setParameter(parameterName, chosenOption);
	}
}
