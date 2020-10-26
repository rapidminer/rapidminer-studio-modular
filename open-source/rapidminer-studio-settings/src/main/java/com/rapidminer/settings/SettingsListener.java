/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.settings;

/**
 * Interface for listener that want to listen to {@link Settings} changes. It only triggers if the value actually
 * changes.
 *
 * @author Marco Boeck
 * @since 9.8.0
 */
public interface SettingsListener {

	/**
	 * Called when a setting is changed or added for the context this listener is registered to. There is no distinction
	 * between adding or changing a setting. This method must return quickly, as multiple listeners may want to get
	 * informed in a timely manner.
	 *
	 * @param context the context, never {@code null}
	 * @param key     the key of the setting, never {@code null}
	 * @param value   the value, may be {@code null}
	 */
	void settingUpdated(String context, String key, String value);
}