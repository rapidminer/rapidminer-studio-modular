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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

import com.rapidminer.settings.Settings;
import com.rapidminer.settings.SettingsConstants;


/**
 * I18N class which handles getting labels, icons, etc in a way so that translations can inject their values for a given
 * key and language. Before this class is called for the first time, the setting {@link SettingsConstants#I18N_LOCALE} should
 * be set in the {@link Settings} to the desired locale. The locale is resolved by calling {@link Locale#forLanguageTag(String)}.
 *
 * @author Simon Fischer, Nils Woehler, Adrian Wilke, Marco Boeck
 */
public class I18N {

	/**
	 * Type of I18N message for the settings bundle. Provided toString() methods include a point
	 * ('.') as prefix, followed by the type in lower case. E.g. '.description'.
	 */
	public enum SettingsType {
		TITLE {

			@Override
			public String toString() {
				return SETTINGS_TYPE_TITLE_SUFFIX;
			}
		},
		DESCRIPTION {

			@Override
			public String toString() {
				return SETTINGS_TYPE_DESCRIPTION_SUFFIX;
			}
		}
	}

	public static final String SETTINGS_TYPE_TITLE_SUFFIX = ".title";
	public static final String SETTINGS_TYPE_DESCRIPTION_SUFFIX = ".description";
	public static final String ICON_SUFFIX = ".icon";

	private static ExtensibleResourceBundle USER_ERROR_BUNDLE;
	private static ExtensibleResourceBundle ERROR_BUNDLE;
	private static ExtensibleResourceBundle GUI_BUNDLE;
	private static ExtensibleResourceBundle SETTINGS_BUNDLE;
	private static ExtensibleResourceBundle LOGGING_BUNDLE;

	private static final Locale ORIGINAL_LOCALE = Locale.getDefault();

	/**
	 * The registration hook for new languages
	 * @since 9.8.0
	 */
	private static Consumer<String> languageRegistration;

	/**
	 * The locale used by the i18n.
	 */
	private static Locale i18nLocale;

	static Logger LOGGER = Logger.getLogger(I18N.class.getName());

	static {
		// we start with the settings locale, so that straight away we don't get NPEs
		// the actually selected language can be changed afterwards by calling the #setLocale() method again
		// even if the setting is null, it will default to the system locale
		setLocale(Settings.getSetting(SettingsConstants.I18N_LOCALE));

		// add listener to be notified about changes to the setting -> update locale again
		Settings.addSettingsListener((context, key, value) -> {
			if (SettingsConstants.I18N_LOCALE.equals(key)) {
				setLocale(value);
			}
		});
	}

	/**
	 * Sets the i18n locale.
	 * <p>
	 * NOTE: This must be called BEFORE any additional bundle registrations take place! Otherwise the additional bundles
	 * registered before this call are dropped again!
	 * </p>
	 *
	 * @param languageTagString an IETF BCP 47 language tag string; or {@code null}
	 * @since 9.9
	 */
	public static void setLocale(String languageTagString) {
		Locale locale = Locale.forLanguageTag(Objects.toString(languageTagString,""));
		if (!locale.getLanguage().isEmpty()) {
			Locale.setDefault(locale);
			LOGGER.log(Level.INFO, "Setting locale.");
		} else {
			locale = Locale.getDefault();
			LOGGER.log(Level.INFO, "No locale defined. Taking default system locale.");
		}
		i18nLocale = locale;
		LOGGER.log(Level.INFO, () -> String.format("Using locale %s.", i18nLocale));
		JComponent.setDefaultLocale(i18nLocale);

		ResourceBundle defaultUserErrorBundle = null;
		ResourceBundle defaultErrorBundle = null;
		ResourceBundle defaultGuiBundle = null;
		ResourceBundle defaultSettingsBundle = null;
		ResourceBundle defaultLoggerBundle = null;
		// try to see if the default Core bundles are there
		// needed for tests etc where i18n could not be set up prior to running easily
		try {
			defaultUserErrorBundle = ResourceBundle.getBundle(
					"com.rapidminer.resources.i18n.UserErrorMessages", locale, I18N.class.getClassLoader());
		} catch (MissingResourceException e) {
			// fine, don't care
		}
		try {
			defaultErrorBundle = ResourceBundle.getBundle("com.rapidminer.resources.i18n.Errors", locale,
					I18N.class.getClassLoader());
		} catch (MissingResourceException e) {
			// fine, don't care
		}
		try {
			defaultGuiBundle = ResourceBundle.getBundle(
					"com.rapidminer.resources.i18n.GUI", locale, I18N.class.getClassLoader());
		} catch (MissingResourceException e) {
			// fine, don't care
		}
		try {
			defaultSettingsBundle = ResourceBundle.getBundle(
					"com.rapidminer.resources.i18n.Settings", locale, I18N.class.getClassLoader());
		} catch (MissingResourceException e) {
			// fine, don't care
		}
		try {
			defaultErrorBundle = ResourceBundle.getBundle("com.rapidminer.resources.i18n.Errors", locale,
					I18N.class.getClassLoader());
		} catch (MissingResourceException e) {
			// fine, don't care
		}
		try {
			String logResourceFile = Settings.getSetting(SettingsConstants.LOGGING_RESOURCE_FILE);
			if (logResourceFile != null) {
				defaultLoggerBundle = ResourceBundle.getBundle(logResourceFile, Locale.getDefault(),
						I18N.class.getClassLoader());
			}
		} catch (MissingResourceException e) {
			// fine, don't care
		}
		USER_ERROR_BUNDLE = new ExtensibleResourceBundle(defaultUserErrorBundle);
		ERROR_BUNDLE = new ExtensibleResourceBundle(defaultErrorBundle);
		GUI_BUNDLE = new ExtensibleResourceBundle(defaultGuiBundle);
		SETTINGS_BUNDLE = new ExtensibleResourceBundle(defaultSettingsBundle);
		LOGGING_BUNDLE = new ExtensibleResourceBundle(defaultLoggerBundle);
	}

	/**
	 * Set the logger to be used for missing keys.
	 *
	 * @param logger the logger, must not be {@code null}
	 * @since 9.8.0
	 */
	public static void setLogger(Logger logger) {
		if (logger != null) {
			LOGGER = logger;
		}
	}

	/** Returns the resource bundle for error messages and quick fixes. */
	public static ResourceBundle getErrorBundle() {
		return ERROR_BUNDLE;
	}

	public static ResourceBundle getGUIBundle() {
		return GUI_BUNDLE;
	}

	public static ResourceBundle getUserErrorMessagesBundle() {
		return USER_ERROR_BUNDLE;
	}

	public static ResourceBundle getSettingsBundle() {
		return SETTINGS_BUNDLE;
	}

	/**
	 * @return the logging bundle, never {@code null}
	 * @since 9.9
	 */
	public static ResourceBundle getLoggingBundle() {
		return LOGGING_BUNDLE;
	}

	/** registers the properties of the given bundle on the global error bundle */
	public static void registerErrorBundle(ResourceBundle bundle) {
		registerErrorBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global gui bundle */
	public static void registerGUIBundle(ResourceBundle bundle) {
		registerGUIBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global userError bundle */
	public static void registerUserErrorMessagesBundle(ResourceBundle bundle) {
		registerUserErrorMessagesBundle(bundle, false);
	}

	/** registers the properties of the given bundle on the global settings bundle */
	public static void registerSettingsBundle(ResourceBundle bundle) {
		registerSettingsBundle(bundle, false);
	}

	/**
	 * Registers the properties of the given bundle on the global logging bundle.
	 *
	 * @since 9.9
	 **/
	public static void registerLoggingBundle(ResourceBundle bundle) {
		registerLoggingBundle(bundle, false);
	}

	/**
	 * Registers the properties of the given bundle on the global error bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	public static void registerErrorBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(ERROR_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers the properties of the given bundle on the global gui bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	public static void registerGUIBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(GUI_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers the properties of the given bundle on the global userError bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	public static void registerUserErrorMessagesBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(USER_ERROR_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers the properties of the given bundle on the global settings bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	public static void registerSettingsBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(SETTINGS_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers the properties of the given bundle on the global logging bundle.
	 *
	 * @param bundle    bundle to register
	 * @param overwrite always false, internal api
	 * @since 9.9
	 */
	public static void registerLoggingBundle(ResourceBundle bundle, boolean overwrite) {
		registerBundle(LOGGING_BUNDLE, bundle, overwrite);
	}

	/**
	 * Registers a new language tag to select from. This needs to be initialized first by calling {@link
	 * #setLanguageRegistration(Consumer)}, otherwise this will ahve no effect.
	 *
	 * <p>
	 * If de-AT is the selected language, first de-AT files are checked, second de, finally {@link Locale#ROOT}
	 *
	 * @param languageTag An IETF BCP 47 language tag, i.e. "fr", "zh", "de" or "en-GB"
	 * @throws IllegalArgumentException if the given {@code languageTag} is not valid
	 * @throws NullPointerException     if {@code languageTag} is {@code null}
	 * @since 9.1.0
	 */
	public static void registerLanguage(String languageTag) {
		if (languageRegistration == null) {
			LOGGER.log(Level.SEVERE, "com.rapidminer.tools.I18N.add_language_failed", languageTag);
			return;
		}
		languageRegistration.accept(languageTag);
	}

	/**
	 * Sets the registration hook for new languages
	 *
	 * @since 9.8.0
	 * @see #registerLanguage(String)
	 */
	static void setLanguageRegistration(Consumer<String> languageRegistration) {
		I18N.languageRegistration = languageRegistration;
	}

	/**
	 * Registers the given bundle in the targetBundle
	 *
	 * @param targetBundle the target bundle
	 * @param bundle bundle to register
	 * @param overwrite always false, internal api
	 */
	private static void registerBundle(ExtensibleResourceBundle targetBundle, ResourceBundle bundle, boolean overwrite) {
		if (overwrite) {
			targetBundle.addResourceBundleAndOverwrite(bundle);
		} else {
			targetBundle.addResourceBundle(bundle);
		}
	}

	/**
	 * Returns a message if found or the key if not found. Arguments <b>can</b> be specified which
	 * will be used to format the String. In the {@link ResourceBundle} the String '{0}' (without ')
	 * will be replaced by the first argument, '{1}' with the second and so on.
	 *
	 * Catches the exception thrown by ResourceBundle in the latter case.
	 * @param bundle the bundle that contains the key
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 **/
	public static String getMessage(ResourceBundle bundle, String key, Object... arguments) {
		try {
			return getMessageOptimistic(bundle, key, arguments);
		} catch (MissingResourceException e) {
			LOGGER.log(Level.FINEST, "com.rapidminer.tools.I18N.missing_key", key);
			return key;
		}
	}

	/**
	 * Convenience method to call {@link #getMessage(ResourceBundle, String, Object...)} with return
	 * value from {@link #getGUIBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getGUIMessage(String key, Object... arguments) {
		return getMessage(getGUIBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessageOrNull(ResourceBundle, String, Object...)} with
	 * return value from {@link #getGUIBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if the key does not exists in the bundle
	 */
	public static String getGUIMessageOrNull(String key, Object... arguments) {
		return getMessageOrNull(getGUIBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessage(ResourceBundle, String, Object...)} with return
	 * value from {@link #getErrorBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getErrorMessage(String key, Object... arguments) {
		return getMessage(getErrorBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessageOrNull(ResourceBundle, String, Object...)} with
	 * return value from {@link #getErrorBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if the key does not exists in the bundle
	 */
	public static String getErrorMessageOrNull(String key, Object... arguments) {
		return getMessageOrNull(getErrorBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessage(ResourceBundle, String, Object...)} with return
	 * value from {@link #getUserErrorMessagesBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getUserErrorMessage(String key, Object... arguments) {
		return getMessage(getUserErrorMessagesBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessageOrNull(ResourceBundle, String, Object...)} with
	 * return value from {@link #getUserErrorMessagesBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if the key does not exists in the bundle
	 */
	public static String getUserErrorMessageOrNull(String key, Object... arguments) {
		return getMessageOrNull(getUserErrorMessagesBundle(), key, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessage(ResourceBundle, String, Object...)} with return
	 * value from {@link #getSettingsBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getSettingsMessage(String key, SettingsType type, Object... arguments) {
		return getMessage(getSettingsBundle(), key + type, arguments);
	}

	/**
	 * Convenience method to call {@link #getMessageOrNull(ResourceBundle, String, Object...)} with
	 * return value from {@link #getSettingsBundle()} as {@link ResourceBundle}.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if the key does not exists in the bundle
	 */
	public static String getSettingsMessageOrNull(String key, SettingsType type, Object... arguments) {
		return getMessageOrNull(getSettingsBundle(), key + type, arguments);
	}

	/**
	 * Returns a message if found or <code>null</code> if not.
	 *
	 * Arguments <b>can</b> be specified which will be used to format the String. In the
	 * {@link ResourceBundle} the String '{0}' (without ') will be replaced by the first argument,
	 * '{1}' with the second and so on.
	 * @param bundle the bundle that contains the key
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or null if key not exists in the bundle
	 */
	public static String getMessageOrNull(ResourceBundle bundle, String key, Object... arguments) {
		try {
			return getMessageOptimistic(bundle, key, arguments);
		} catch (MissingResourceException e) {
			return null;
		}
	}

	/**
	 * This will return the value of the property gui.label.-key- of the GUI bundle or the key
	 * itself if unknown.
	 *
	 * @param key key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key, or the key if it does not exists in the bundle
	 */
	public static String getGUILabel(String key, Object... arguments) {
		return getMessage(GUI_BUNDLE, "gui.label." + key, arguments);
	}

	/**
	 * Gets the original result of {@link Locale#getDefault()} (before it is exchanged with a custom locale due to our
	 * language chooser).
	 *
	 * @return the original default locale object before it is changed due to our I18N combobox
	 * @since 8.2.1
	 */
	public static Locale getOriginalLocale() {
		return ORIGINAL_LOCALE;
	}

	/**
	 * Gets the locale used by the i18n system. Either it's the user defined locale or the default one.
	 *
	 * @return the locale used by the i18n service
	 * @since 9.9.0
	 */
	public static Locale getI18nLocale() {
		return i18nLocale;
	}

	/**
	 * Returns a message if found or the key if not found. Arguments <b>can</b> be specified which will be used to
	 * format the String. In the {@link ResourceBundle} the String '{0}' (without ') will be replaced by the first
	 * argument, '{1}' with the second and so on.
	 *
	 * @param bundle    the bundle that contains the key
	 * @param key       key – the key for the desired string
	 * @param arguments optional arguments for message formatter
	 * @return the formatted string for the given key
	 * @throws MissingResourceException if no object for the given key can be found
	 */
	private static String getMessageOptimistic(ResourceBundle bundle, String key, Object... arguments) throws MissingResourceException {
		if (bundle == null) {
			throw new MissingResourceException("No resource bundle defined", I18N.class.getName(), key);
		}
		String message = bundle.getString(key);
		if (arguments == null || arguments.length == 0) {
			return message;
		}
		return MessageFormat.format(message, arguments);
	}

}
