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

import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;


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

}
