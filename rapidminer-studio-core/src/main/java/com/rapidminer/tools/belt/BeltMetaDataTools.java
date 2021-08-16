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
package com.rapidminer.tools.belt;

import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.quickfix.QuickFixSupplier;


/**
 * Provides some convenience methods for using {@link com.rapidminer.operator.ports.metadata.table.TableMetaData}.
 *
 * @author Kevin Majchrzak
 * @since 9.9.0
 */
public final class BeltMetaDataTools {

	/**
	 * Disallow instances of this class.
	 */
	private BeltMetaDataTools() {
		throw new AssertionError("No com.rapidminer.tools.belt.BeltMetaDataTools instances for you!");
	}

	/**
	 * Checks if the column with the given label is a special column (column with a {@link
	 * com.rapidminer.belt.util.ColumnRole}).
	 *
	 * @param metaData
	 * 		the meta data holding the column information
	 * @param label
	 * 		the label of the column
	 * @return {@code true} iff the column has a role
	 */
	public static boolean isSpecial(TableMetaData metaData, String label) {
		return metaData.getFirstColumnMetaData(label, ColumnRole.class) != null;
	}

	/**
	 * Returns the subtable of the given table meta data holing all columns without a {@link ColumnRole}).
	 *
	 * @param tableMetaData
	 * 		the given table meta data
	 * @return a {@link TableMetaData} holding exactly the columns without a role of the given table meta data
	 * @since 9.10
	 */
	public static TableMetaData regularSubtable(TableMetaData tableMetaData) {
		return tableMetaData.columns(tableMetaData.labels().stream()
				.filter(l -> tableMetaData.getFirstColumnMetaData(l, ColumnRole.class) == null)
				.collect(Collectors.toList()));
	}

	/**
	 * Returns the column info's type id or {@code null} if the type is unknown.
	 *
	 * @param info
	 * 		the column info
	 * @return type id or {@code null}
	 * @since 9.10
	 */
	public static Column.TypeId getTypeId(ColumnInfo info) {
		return info.getType().map(ColumnType::id).orElse(null);
	}


	/**
	 * Checks if the meta data contains the role exactly once and adds a meta data error to the port if not.
	 *
	 * @param role
	 * 		the role to check for
	 * @param metaData
	 * 		the meta data that should contain the role exactly once
	 * @param port
	 * 		the port to add the error to
	 * @since 9.10
	 */
	public static void requireUniqueRole(ColumnRole role, TableMetaData metaData, InputPort port) {
		if (metaData.hasUniqueColumnMetaData(role) == MetaDataInfo.NO) {
			String newRole = "regular";
			if (metaData.hasColumnMetaData(role) == MetaDataInfo.NO) {
				newRole = role.name().toLowerCase(Locale.ROOT);
			}
			port.addError(new SimpleMetaDataError(ProcessSetupError.Severity.ERROR, port,
					Collections.singletonList(QuickFixSupplier.getSetRoleQuickFix(port, newRole,
							"change_attribute_role", newRole)),
					"require_unique_role", role.name().toLowerCase(Locale.ROOT)));
		}
	}

}
