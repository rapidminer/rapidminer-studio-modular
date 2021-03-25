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
package com.rapidminer.adaption.belt;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.Workload;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.tools.Tools;


/**
 * Utility functions to provide compatibility with the legacy data core.
 *
 * Please note that this class is not part of any public API and might be modified or removed in future releases without
 * prior warning.
 *
 * @author Michael Knopf
 * @since 9.0.0
 */
public class CompatibilityTools {

	private CompatibilityTools() {
		// Suppress default constructor to prevent instantiation
		throw new AssertionError();
	}

	/**
	 * Converts all datetime columns of the given table to numeric columns containing the epoch milliseconds.
	 *
	 * @param table
	 * 		the table containing datetime columns
	 * @param context
	 * 		the context to work with
	 * @return table with all datetime columns replaced by numeric columns
	 */
	public static Table convertDatetimeToMilliseconds(Table table, Context context) {
		// Check for datetime columns
		List<String> labels = table.labels();
		List<String> datetimeLabels = new ArrayList<>();
		for (int i = 0; i < table.width(); i++) {
			Column column = table.column(i);
			if (ColumnType.DATETIME.equals(column.type())) {
				datetimeLabels.add(labels.get(i));
			}
		}

		// Nothing to convert...
		if (datetimeLabels.isEmpty()) {
			return table;
		}

		// Replace datetime columns by numeric columns containing the epoch milliseconds
		TableBuilder builder = Builders.newTableBuilder(table);
		for (String label : datetimeLabels) {
			Column replacement = table.transform(label).workload(Workload.SMALL)
					.applyObjectToReal(Instant.class,
							v -> v != null ? BeltConverter.toEpochMilli(v) : Double.NaN,
							context).toColumn();
			builder.replace(label, replacement);
		}
		return builder.build(context);
	}

	/**
	 * Converts all time columns in the labels of interest of the given table to date-time columns using the settings
	 * timezone from {@link Tools#getPreferredTimeZone()}.
	 *
	 * @param table
	 * 		the table containing datetime columns
	 * @param labelsOfInterest
	 * 		the subset of labels to check
	 * @param context
	 * 		the context to work with
	 * @return table with all datetime columns replaced by numeric columns
	 * @since 9.9.0
	 */
	public static Table convertTimeToDatetime(Table table, List<String> labelsOfInterest, Context context) {
		// Check for time columns
		List<String> timeLabels = new ArrayList<>();
		for (String label : labelsOfInterest) {
			Column column = table.column(label);
			if (ColumnType.TIME.equals(column.type())) {
				timeLabels.add(label);
			}
		}

		// Nothing to convert...
		if (timeLabels.isEmpty()) {
			return table;
		}

		// Replace time columns by date-time columns containing the epoch milliseconds relative to settings timezone
		TableBuilder builder = Builders.newTableBuilder(table);
		for (String label : timeLabels) {
			Column replacement = table.transform(label).workload(Workload.SMALL)
					.applyObjectToDateTime(LocalTime.class,
							v -> v != null ?
									Instant.ofEpochMilli((long) BeltConverter.nanoOfDayToLegacyTime(v.toNanoOfDay())) :
									null,
							context).toColumn();
			builder.replace(label, replacement);
			builder.addMetaData(label, LegacyType.TIME);
		}
		return builder.build(context);
	}

}
