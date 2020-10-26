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
 * List of well-known constants of {@link Settings} for the {@link Settings#CONTEXT_STUDIO_PREFERENCES}.
 *
 * @author Marco Boeck
 * @since 9.8.0
 */
public final class SettingsConstants {

	/** the i18n locale setting */
	public static final String I18N_LOCALE = "rapidminer.general.locale.language";
	/** the i18n translation helper file location setting */
	public static final String I18N_TRANSLATION_HELPER = "rapidminer.i18n.translation_helper";

	/**
	 * the logging log file location. If {@code null}, no log file will be created. Can be updated at runtime and will
	 * start(or stop, if {@code null}) logging to the specified file from then on.
	 */
	public static final String LOGGING_LOG_FILE = "rapidminer.logging.log-file";
	/**
	 * the logging resource file including full path inside the JAR used for i18n of logging. If {@code null}, no
	 * logging resource bundle will be used. See {@link java.util.logging.Logger#getLogger(String, String)}
	 */
	public static final String LOGGING_RESOURCE_FILE = "rapidminer.logging.resource-file-jar-path";

	/**
	 * if {@code true}, will by log to the console. To set the level, see {@link #LOGGING_CONSOLE_LEVEL}. Can be
	 * updated at runtime and will start/stop logging to the console from then on.
	 */
	public static final String LOGGING_TO_CONSOLE = "rapidminer.logging.log-to-console";

	/**
	 * the console log level with which the console logging is used, see {@link java.util.logging.Level#parse(String)}. If not
	 * specified, will use {@link java.util.logging.Level#INFO}. Can be updated at runtime and will use that level from
	 * then on.
	 */
	public static final String LOGGING_CONSOLE_LEVEL = "rapidminer.logging.default-log-level";

	/** whether the execution is in headless mode */
	public static final String EXECUTION_IS_HEADLESS = "rapidminer.execution.is_headless";
	/** whether the file system can be accessed */
	public static final String EXECUTION_CAN_ACCESS_FILESYSTEM = "rapidminer.execution.can_access_filesystem";
	/** the working directory for the execution */
	public static final String EXECUTION_WORKING_DIRECTORY = "rapidminer.execution.working_directory";

	/** the current version of the platform, whatever that references */
	public static final String PLATFORM_VERSION = "rapidminer.platform.version";


	private SettingsConstants() {
		throw new UnsupportedOperationException("static utility class");
	}
}
