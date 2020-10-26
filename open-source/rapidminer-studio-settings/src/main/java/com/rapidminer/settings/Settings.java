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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.tools.SecurityTools;


/**
 * This class stores settings for different contexts. Note that changing the value of a setting here only affects
 * another component if it listens to changes here or reads the setting value again each time the setting is needed.
 * <p>
 * If a setting is registered as protected via {@link #registerProtectedSetting(String)}, setting its value requires
 * {@link SecurityTools#requireInternalPermission()}. Note that registering a setting as protected cannot be undone.
 * </p>
 * <p>
 * If the entire Studio Core is running, this class will contain all settings of the Preferences of Studio by default,
 * under the same key as they are registered in the preferences. This can be used to access the user settings, w/o
 * having to have knowledge of the ParameterService class. Changes here however will not reflect back into the
 * ParameterService.
 * </p>
 *
 * @author Marco Boeck
 * @since 9.8.0
 */
public final class Settings {

	private static final Map<String, Map<String, String>> SETTINGS = new ConcurrentHashMap<>();
	private static final Map<String, List<SettingsListener>> LISTENERS = new ConcurrentHashMap<>();
	private static final Set<String> PROTECTED_SETTINGS = Collections.synchronizedSet(new HashSet<>());

	private static final Logger LOGGER = Logger.getLogger(Settings.class.getName());

	/** context for all settings defined in the Preferences of RapidMiner Studio */
	public static final String CONTEXT_STUDIO_PREFERENCES = "rapidminer-studio-preferences";


	private Settings() {
		throw new UnsupportedOperationException("static utility class");
	}

	/**
	 * Returns the specified setting for the default context ({@link #CONTEXT_STUDIO_PREFERENCES}.
	 *
	 * @param key     the setting key, must not be {@code null}
	 * @return the setting value or {@code null} if not specified
	 */
	public static String getSetting(String key) {
		return getSetting(CONTEXT_STUDIO_PREFERENCES, key);
	}

	/**
	 * Returns the specified setting for the given context.
	 *
	 * @param context the context. If {@code null}, {@link #CONTEXT_STUDIO_PREFERENCES} will be used
	 * @param key     the setting key, must not be {@code null}
	 * @return the setting value or {@code null} if not specified
	 */
	public static String getSetting(String context, String key) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null!");
		}
		if (context == null) {
			context = CONTEXT_STUDIO_PREFERENCES;
		}

		return SETTINGS.computeIfAbsent(context, s -> new ConcurrentHashMap<>()).get(key);
	}

	/**
	 * Sets the specified setting for the default context ({@link #CONTEXT_STUDIO_PREFERENCES}.
	 *
	 * @param key     the setting key, must not be {@code null}
	 * @param value   the setting value, can be {@code null}
	 */
	public static void setSetting(String key, String value) {
		setSetting(CONTEXT_STUDIO_PREFERENCES, key, value);
	}

	/**
	 * Sets the specified setting for the given context. If the key is protected (see {@link #isSettingProtected(String,
	 * String)}, elevated permissions are required for this call. If those are not met, does nothing.
	 *
	 * @param context the context. If {@code null}, {@link #CONTEXT_STUDIO_PREFERENCES} will be used
	 * @param key     the setting key, must not be {@code null}
	 * @param value   the setting value, can be {@code null}
	 */
	public static void setSetting(String context, String key, String value) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null!");
		}
		if (context == null) {
			context = CONTEXT_STUDIO_PREFERENCES;
		}

		if (isSettingProtected(key, context)) {
			try {
				// elevated permissions required to register protected settings
				SecurityTools.requireInternalPermission();
			} catch (UnsupportedOperationException e) {
				return;
			}
		}

		String previousValue = SETTINGS.computeIfAbsent(context, s -> new ConcurrentHashMap<>()).put(key, value);

		// only fire if new value is different than before
		if (!Objects.equals(value, previousValue)) {
			fireSettingUpdated(context, key, value);
		}
	}

	/**
	 * Adds the given settings listener for the default context ({@link #CONTEXT_STUDIO_PREFERENCES}.
	 *
	 * @param listener the listener, must not be {@code null}
	 */
	public static void addSettingsListener(SettingsListener listener) {
		addSettingsListener(CONTEXT_STUDIO_PREFERENCES, listener);
	}

	/**
	 * Adds the given settings listener.
	 *
	 * @param context  the settings context to which to listen. If {@code null}, {@link #CONTEXT_STUDIO_PREFERENCES}
	 *                 will be used
	 * @param listener the listener, must not be {@code null}
	 */
	public static void addSettingsListener(String context, SettingsListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		if (context == null) {
			context = CONTEXT_STUDIO_PREFERENCES;
		}

		LISTENERS.computeIfAbsent(context, s -> new CopyOnWriteArrayList<>()).add(listener);
	}

	/**
	 * Removes the given settings listener from the default context ({@link #CONTEXT_STUDIO_PREFERENCES}. If the
	 * listener was not registered, nothing happens.
	 *
	 * @param listener the listener, must not be {@code null}
	 */
	public static void removeSettingsListener(SettingsListener listener) {
		removeSettingsListener(CONTEXT_STUDIO_PREFERENCES, listener);
	}

	/**
	 * Removes the given settings listener. If the listener was not registered, nothing happens.
	 *
	 * @param context  the settings context from which to remove the listener. If {@code null}, {@link
	 *                 #CONTEXT_STUDIO_PREFERENCES} will be used
	 * @param listener the listener, must not be {@code null}
	 */
	public static void removeSettingsListener(String context, SettingsListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		if (context == null) {
			context = CONTEXT_STUDIO_PREFERENCES;
		}

		LISTENERS.computeIfAbsent(context, s -> new CopyOnWriteArrayList<>()).remove(listener);
	}

	/**
	 * This method checks if the given setting is protected for the default context ({@link
	 * #CONTEXT_STUDIO_PREFERENCES}, i.e. can only be changed with elevated permissions.
	 *
	 * @param key The key of the setting to check
	 * @return {@code true} if it is protected, {@code false} if it is not
	 */
	public static boolean isSettingProtected(String key) {
		return isSettingProtected(key, CONTEXT_STUDIO_PREFERENCES);
	}

	/**
	 * This method checks if the given setting is protected, i.e. can only be changed with elevated permissions.
	 *
	 * @param key     the key of the setting to check
	 * @param context the settings context to which the setting belongs
	 * @return {@code true} if it is protected, {@code false} if it is not
	 */
	public static boolean isSettingProtected(String key, String context) {
		return PROTECTED_SETTINGS.contains(key);
	}

	/**
	 * Registers a key as protected for the default context ({@link #CONTEXT_STUDIO_PREFERENCES}. That means it can only
	 * be set with elevated permissions. This cannot be undone for the remainder of program runtime!
	 *
	 * @param key the key of the setting to register as protected
	 */
	public static void registerProtectedSetting(String key) {
		registerProtectedSetting(key, CONTEXT_STUDIO_PREFERENCES);
	}

	/**
	 * Registers a key as protected. That means it can only be set with elevated permissions. This cannot be undone for
	 * the remainder of program runtime!
	 *
	 * @param key     the key of the setting to register as protected
	 * @param context the settings context to which the setting belongs
	 */
	public static void registerProtectedSetting(String key, String context) {
		try {
			// elevated permissions required to register protected settings
			SecurityTools.requireInternalPermission();
		} catch (UnsupportedOperationException e) {
			return;
		}
		PROTECTED_SETTINGS.add(key);
	}

	/**
	 * Call when a setting has been added/updated.
	 *
	 * @param context the context, never {@code null}
	 * @param key     the key, never {@code null}
	 * @param value   the value, can be {@code null}
	 */
	private static void fireSettingUpdated(String context, String key, String value) {
		for (SettingsListener listener : LISTENERS.computeIfAbsent(context, s -> new CopyOnWriteArrayList<>())) {
			try {
				listener.settingUpdated(context, key, value);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Settings listener failed: " + t.getMessage(), t);
			}
		}
	}
}
