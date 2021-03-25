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
package com.rapidminer.operator.ports.metadata.table;

import static com.rapidminer.operator.ports.metadata.table.TablePrecondition.toLegacyType;

import java.util.Optional;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AbstractPrecondition;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * This precondition can be used, if a single column must be contained in the table. Three properties of the column
 * might be given: Name, Role and Type or Category. Role and Type/Category are optional. The column name is not given
 * explicitly, instead a parameter name of an operator is given, from which the column name is retrieved during
 * runtime.
 *
 * This class is a copy of {@link com.rapidminer.operator.ports.metadata.AttributeParameterPrecondition} adjusted to
 * {@link TableMetaData}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ColumnParameterPrecondition extends AbstractPrecondition {

	private final ParameterHandler handler;
	private final String parameterName;
	private final ColumnType<?> columnType;
	private final ColumnRole columnRole;
	private final Column.Category category;

	/**
	 * This precondition will only check the name. No Role and type checks will be performed.
	 */
	public ColumnParameterPrecondition(InputPort inport, ParameterHandler operator, String parameterName) {
		this(inport, operator, parameterName, null, (ColumnType<?>) null);
	}

	/**
	 * This precondition will not perform any role check.
	 */
	public ColumnParameterPrecondition(InputPort inport, ParameterHandler operator, String parameterName,
									   ColumnType<?> columnType) {
		this(inport, operator, parameterName, null, columnType);
	}

	/**
	 * This precondition will check name, role and type.
	 */
	public ColumnParameterPrecondition(InputPort inport, ParameterHandler operator, String parameterName,
									   ColumnRole columnRole,
									   ColumnType<?> columnType) {
		super(inport);
		this.handler = operator;
		this.parameterName = parameterName;
		this.columnType = columnType;
		this.columnRole = columnRole;
		this.category = null;
	}

	/**
	 * This precondition will check name, role and category. Role can be {@code null} if it should not be checked.
	 */
	public ColumnParameterPrecondition(InputPort inport, ParameterHandler operator, String parameterName,
									   ColumnRole columnRole,
									   Column.Category category) {
		super(inport);
		this.handler = operator;
		this.parameterName = parameterName;
		this.category = category;
		this.columnRole = columnRole;
		this.columnType = null;
	}

	@Override
	public void check(MetaData metaData) {
		final TableMetaData tmd = BeltConversionTools.asTableMetaDataOrNull(metaData);
		if (tmd == null) {
			return;
		}
		String columnName = getName();
		if (columnName != null) {
			// checking if attribute with name and type exists
			MetaDataInfo containsRelation = tmd.contains(columnName);
			if (containsRelation == MetaDataInfo.YES) {
				checkExistingColumn(tmd, columnName);
			} else if (containsRelation == MetaDataInfo.UNKNOWN) {
				createError(Severity.WARNING, "missing_attribute", columnName);
			} else {
				createError(Severity.ERROR, "missing_attribute", columnName);
			}
		}
		makeAdditionalChecks(tmd);

	}

	/**
	 * Checks for an existing column if it has the right type or category.
	 */
	private void checkExistingColumn(TableMetaData tmd, String columnName) {
		ColumnInfo columnInfo = tmd.column(columnName);
		if (category != null) {
			checkCategory(tmd, columnName, columnInfo);
		} else {
			// checking column type
			if (columnType == null || (columnInfo.getType().isPresent() &&
					columnType.equals(columnInfo.getType().orElse(null)))) {
				checkRole(tmd, columnName);
			} else {
				createError(columnInfo.getType().isPresent() ? Severity.ERROR : Severity.WARNING,
						"attribute_has_wrong_type", columnName, columnType.id().toString());
			}
		}
	}

	/**
	 * Checks if the column info has a type with the right category.
	 */
	private void checkCategory(TableMetaData tmd, String columnName, ColumnInfo columnInfo) {
		final Optional<ColumnType<?>> type = columnInfo.getType();
		if (type.filter(t -> t.category() == category).isPresent()) {
			checkRole(tmd, columnName);
		} else {
			createError(type.isPresent() ? Severity.ERROR : Severity.WARNING,
					"attribute_has_wrong_type", columnName, toLegacyType(category));
		}
	}

	/**
	 * Checks if the column with the name has the right role.
	 */
	private void checkRole(TableMetaData tmd, String columnName) {
		if (columnRole != null &&
				!columnRole.equals(tmd.getFirstColumnMetaData(columnName, ColumnRole.class))) {
			createError(Severity.ERROR, "attribute_must_have_role", columnName,
					BeltConverter.toStudioRole(columnRole));
		}
	}

	/**
	 * This method returns the name of the attribute that must be contained in the meta data. It might return {@code
	 * null}, if no check should be performed.
	 *
	 * @return the name that must be contained
	 */
	protected String getName() {
		try {
			return handler.getParameterAsString(parameterName);
		} catch (UndefinedParameterError e) {
			return null;
		}
	}

	@Override
	public void assumeSatisfied() {
		getInputPort().receiveMD(new TableMetaData());
	}

	/**
	 * Can be overridden by subclasses to make additional checks.
	 *
	 * @param tmd
	 * 		the meta data to check
	 */
	protected void makeAdditionalChecks(TableMetaData tmd) {
		//does nothing by default
	}

	@Override
	public String getDescription() {
		return "<em>expects:</em> ExampleSet";
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		return IOTable.class.isAssignableFrom(input.getObjectClass())
				|| AtPortConverter.isConvertible(input.getObjectClass(), IOTable.class);
	}

	@Override
	public MetaData getExpectedMetaData() {
		return new TableMetaData();
	}


}
