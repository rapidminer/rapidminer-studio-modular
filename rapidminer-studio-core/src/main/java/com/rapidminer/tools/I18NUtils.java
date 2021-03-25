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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.settings.Settings;
import com.rapidminer.settings.SettingsConstants;


/**
 * Used for some i18n manipulations.
 *
 * @author Marco Boeck
 * @since 9.8.0
 */
public class I18NUtils {

	/**
	 * File that contains the keys that weren't available in the users language
	 */
	private static final String TRANSLATION_HELPER_FILE = Objects.toString(Settings.getSetting(SettingsConstants.I18N_TRANSLATION_HELPER), "");

	/**
	 * Log only if the translation helper file exists and the users language is not english
	 */
	private static final boolean LOG_MISSING_TRANSLATIONS = Files.exists(Paths.get(TRANSLATION_HELPER_FILE)) &&
			!Locale.getDefault().getLanguage().isEmpty() && !Locale.getDefault().getLanguage().equals(Locale.ENGLISH.getLanguage());

	static {
		ExtensibleResourceBundle.setLogMissingTranslations(LOG_MISSING_TRANSLATIONS);
		if (LOG_MISSING_TRANSLATIONS) {
			ShutdownHooks.addShutdownHook(() -> {
				try {
					Files.write(Paths.get(TRANSLATION_HELPER_FILE), ExtensibleResourceBundle.getMissingTranslationKeys(), StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
				} catch (IOException e) {
					// bad luck
					LogService.getRoot().log(Level.INFO, e, () -> I18N.getErrorMessage("com.rapidminer.tools.I18NUtils.store_missing_keys_failed", TRANSLATION_HELPER_FILE));
				}
			});
		}
	}

	private I18NUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Initializes the utils by adding itself as a language registration hook to {@link I18N}.
	 *
	 * @since 9.8.0
	 */
	public static void init() {
		I18N.setLanguageRegistration(I18NUtils::registerLanguage);
		I18N.setLogger(LogService.getRoot());
	}

	/**
	 * <p>
	 * Registers a new language tag to be shown in the RapidMiner Settings
	 * </p>
	 *
	 * <p>
	 * Important: Use underscore for the .properties files, but hyphen for the language tag!<br/> Examples:
	 * <table border="1">
	 * <tr>
	 * <th>Language Tag</th><th>Filename</th>
	 * </tr>
	 * <tr>
	 * <td>English Fallback</td> <td>MyExtGUI.properties</td>
	 * </tr>
	 * <tr>
	 * <td>"de"</td> <td>MyExtGUI_de.properties</td>
	 * </tr>
	 * <tr>
	 * <td>"de-AT"</td> <td>MyExtGUI_de_AT.properties</td>
	 * </tr>
	 * </table>
	 * If de-AT is the selected language, first de-AT files are checked, second de, finally {@link Locale#ROOT}
	 *
	 * @param languageTag An IETF BCP 47 language tag, i.e. "fr", "zh", "de" or "en-GB"
	 * @throws IllegalArgumentException if the given {@code languageTag} is not valid
	 * @throws NullPointerException     if {@code languageTag} is {@code null}
	 * @since 9.1.0
	 */
	public static void registerLanguage(String languageTag) {
		// Check if language key is valid
		if (Locale.forLanguageTag(languageTag).getLanguage().isEmpty()) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.I18N.add_language_wrong_format", String.valueOf(languageTag));
			throw new IllegalArgumentException(languageTag + " is not a valid IETF BCP 47 language tag.");
		}

		ParameterType type = ParameterService.getParameterType(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_LOCALE_LANGUAGE);
		if (!(type instanceof ParameterTypeCategory)) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.tools.I18N.add_language_failed", languageTag);
			return;
		}
		ParameterTypeCategory lng = ((ParameterTypeCategory) type);
		if (ArrayUtils.contains(lng.getValues(), languageTag)) {
			//already registered
			return;
		}
		// Add language key to the end, not sorted
		String[] languages = (String[]) ArrayUtils.add(lng.getValues(), languageTag);
		ParameterService.registerParameter(new ParameterTypeCategory(lng.getKey(), lng.getDescription(), languages, lng.getDefault(), lng.isExpert()));
	}


}
