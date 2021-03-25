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
package com.rapidminer.tools;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import com.rapidminer.settings.Settings;
import com.rapidminer.settings.SettingsConstants;


/**
 * Utility methods for manipulating dates.
 *
 * @author Marco Boeck
 * @since 9.9
 */
public enum DateTools {
	INSTANCE;

	// ThreadLocal because DateFormat is NOT thread-safe and creating a new DateFormat is
	// EXTREMELY expensive
	private static final ThreadLocal<DateFormat> DATE_TIME_FORMAT = ThreadLocal.withInitial(() -> {
		// clone because getDateInstance uses an internal pool which can return the same
		// instance for multiple threads
		return (DateFormat) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.LONG, Locale.getDefault())
				.clone();
	});

	private TimeZone defaultTimeZone;


	DateTools() {
		setPreferredTimezone();

		// add listener to be notified about changes to the setting -> update default timezone on the fly
		Settings.addSettingsListener((context, key, value) -> {
			if (SettingsConstants.DEFAULT_TIMEZONE.equals(key)) {
				updatePreferredTimezone(value);
			}
		});
	}


	/**
	 * Format the given {@link Date} via the given timezone into a string.
	 *
	 * @param date     the date, must not be {@code null}
	 * @param timeZone the optional timezone, if {@code null} the preferred timezone will be used (see {@link
	 *                 SettingsConstants#DEFAULT_TIMEZONE)})
	 * @return the string representing the date
	 */
	public String formatDateTime(Date date, TimeZone timeZone) {
		ValidationUtilV2.requireNonNull(date, "date");

		DATE_TIME_FORMAT.get().setTimeZone(Optional.ofNullable(timeZone).orElse(defaultTimeZone));
		return DATE_TIME_FORMAT.get().format(date);
	}

	private void setPreferredTimezone() {
		updatePreferredTimezone(Settings.getSetting(SettingsConstants.DEFAULT_TIMEZONE));
	}

	private void updatePreferredTimezone(String timeZoneId) {
		this.defaultTimeZone = Optional.ofNullable(timeZoneId).map(t -> TimeZone.getTimeZone(timeZoneId)).orElse(TimeZone.getTimeZone("UTC"));
	}
}
