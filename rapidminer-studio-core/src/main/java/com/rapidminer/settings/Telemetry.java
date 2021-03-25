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
package com.rapidminer.settings;

import com.rapidminer.tools.SecurityTools;


/**
 * This is the control for all telemetry of Studio. All sending and requesting online access can be controlled via
 * parameters. Changing parameters leads to changes in this class and all the places that access online services need to
 * ask their specific setting if access is allowed.
 *
 * @author Andreas Timm
 * @since 9.0.0
 */
public enum Telemetry {

	/**
	 * Set ALL_TELEMETRY to true to prohibit any communication from Studio.
	 * Reacts to Parameter change for the configured key to update the value.
	 */
	ACCOUNT("rapidminer.studio.deny.account", false),
	ALL_TELEMETRY("rapidminer.studio.deny.allcommunication", false),
	CTA("rapidminer.studio.deny.cta", false),
	MARKETPLACE("rapidminer.studio.deny.marketplace", false),
	NEWS("rapidminer.studio.deny.news", false),
	USAGESTATS("rapidminer.studio.deny.usagestats", false),
	WISDOM_OF_CROWDS("rapidminer.studio.deny.woc", false),
	EDUCATION("rapidminer.studio.deny.education", false);

	private String key;
	private boolean value;

	Telemetry(String key, boolean defaultValue) {
		this.key = key;
		this.value = defaultValue;

		final String settingsValue = Settings.getSetting(key);
		if (settingsValue != null) {
			value = Boolean.parseBoolean(settingsValue);
		}

		SettingsListener listener = (context, settingsKey, value) -> {
			if (this.key.equals(settingsKey)) {
				try {
					SecurityTools.requireInternalPermission();
					this.value = Boolean.parseBoolean(value);
				} catch (UnsupportedOperationException e) {
					// ignore
				}
			}
		};
		Settings.addSettingsListener(listener);
	}

	/**
	 * Check if this setting is prohibited. Always checks if ALL_TELEMETRY is prohibited.
	 *
	 * @return true if this {@link Telemetry} or the ALL_TELEMETRY setting was denied.
	 */
	public boolean isDenied() {
		return ALL_TELEMETRY.value || value;
	}

	/**
	 * Getter for the key which is used to be able to change the value through the {@link Settings}
	 *
	 * @return the key for this instance
	 */
	public String getKey() {
		return key;
	}
}
