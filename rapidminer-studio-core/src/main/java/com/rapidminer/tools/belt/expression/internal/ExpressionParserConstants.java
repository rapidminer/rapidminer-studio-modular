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
package com.rapidminer.tools.belt.expression.internal;

/**
 * Some constants used to configure the expression parser date-time and time functions.
 *
 * @author Marco Boeck, Kevin Majchrzak
 * @since 9.11
 */
public enum ExpressionParserConstants {

	; // no instance enum

	// date constants
	public static final String DATE_TIME_UNIT_NANOSECOND = "EPConstants_date_time_unit_nanosecond";
	public static final String DATE_TIME_UNIT_MILLISECOND = "EPConstants_date_time_unit_millisecond";
	public static final String DATE_TIME_UNIT_SECOND = "EPConstants_date_time_unit_second";
	public static final String DATE_TIME_UNIT_MINUTE = "EPConstants_date_time_unit_minute";
	public static final String DATE_TIME_UNIT_HOUR = "EPConstants_date_time_unit_hour";
	public static final String DATE_TIME_UNIT_DAY = "EPConstants_date_time_unit_day";
	public static final String DATE_TIME_UNIT_WEEK = "EPConstants_date_time_unit_week";
	public static final String DATE_TIME_UNIT_MONTH = "EPConstants_date_time_unit_month";
	public static final String DATE_TIME_UNIT_YEAR = "EPConstants_date_time_unit_year";
}
