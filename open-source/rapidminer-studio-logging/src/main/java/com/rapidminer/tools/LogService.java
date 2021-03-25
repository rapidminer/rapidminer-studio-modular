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
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.rapidminer.settings.Settings;
import com.rapidminer.settings.SettingsConstants;


/**
 * <p>
 * Utility class providing access to a logger (see {@link #getRoot()}.<br> If logging should also log to a file, set the
 * {@link SettingsConstants#LOGGING_LOG_FILE} in the {@link Settings} to a log file location. You can change the setting
 * at any time, the log file location will change for the next log call.
 *
 * <p>
 * If running inside RapidMiner Studio in UI mode, the root log messages will be presented in the log view.
 * </p>
 *
 * <p>
 * Log messages can be formatted by using the following macros:
 * </p>
 * <ul>
 * <li>&quot;$b&quot; and &quot;^b&quot; start and end bold mode respectively</li>
 * <li>&quot;$i&quot; and &quot;^i&quot; start and end italic mode respectively</li>
 * <li>&quot;$m&quot; and &quot;^m&quot; start and end monospace mode respectively</li>
 * <li>&quot;$n&quot; and &quot;^n&quot; start and end note color mode respectively</li>
 * <li>&quot;$w&quot; and &quot;^w&quot; start and end warning color mode respectively</li>
 * <li>&quot;$e&quot; and &quot;^e&quot; start and end error color mode respectively</li>
 * </ul>
 *
 * @author Ingo Mierswa
 */
public class LogService extends WrapperLoggingHandler {

	/** Indicates an unknown verbosity level. */
	public static final int UNKNOWN_LEVEL = -1;

	/**
	 * Indicates the lowest log verbosity. Should only be used for very detailed but not necessary
	 * logging.
	 */
	public static final int MINIMUM = 0;

	/**
	 * Indicates log messages concerning in- and output. Should only be used by the class Operator
	 * itself and not by its subclasses.
	 */
	public static final int IO = 1;

	/** The default log verbosity for all logging purposes of operators. */
	public static final int STATUS = 2;

	/**
	 * Only the most important logging messaged should use this log verbosity. Currently used only
	 * by the LogService itself.
	 */
	public static final int INIT = 3;

	/**
	 * Use this log verbosity for logging of important notes, i.e. things less important than
	 * warnings but important enough to see for all not interested in the detailed status messages.
	 */
	public static final int NOTE = 4;

	/** Use this log verbosity for logging of warnings. */
	public static final int WARNING = 5;

	/** Use this log verbosity for logging of errors. */
	public static final int ERROR = 6;

	/**
	 * Use this log verbosity for logging of fatal errors which will stop process running somewhere
	 * in the future.
	 */
	public static final int FATAL = 7;

	/**
	 * Normally this log verbosity should not be used by operators. Messages with this verbosity
	 * will always be displayed.
	 */
	public static final int MAXIMUM = 8;

	/** For switching off logging during testing. */
	public static final int OFF = 9;

	public static final String[] LOG_VERBOSITY_NAMES = { "all", "io", "status", "init", "notes", "warning", "error",
			"fatal", "almost_none", "off" };

	private static final Logger GLOBAL_LOGGER;

	private static final LogService GLOBAL_LOGGING;

	static {
		GLOBAL_LOGGER = Logger.getLogger("com.rapidminer");
		GLOBAL_LOGGING = new LogService(GLOBAL_LOGGER);

		Level previousLevel = GLOBAL_LOGGER.getLevel();
		GLOBAL_LOGGER.setLevel(Level.OFF);
		// this triggers I18N class init, which would log something, making it impossible to actually disable logging right from the start
		// therefore, we disable logging for this call, and resume afterwards again
		updateLoggerResourceBundle();
		GLOBAL_LOGGER.setLevel(previousLevel);
	}

	private FileHandler logFileHandler;
	private ConsoleHandler consoleHandler;


	private LogService(Logger logger) {
		super(logger);
		updateFileLogger(Settings.getSetting(SettingsConstants.LOGGING_LOG_FILE));
		updateConsoleLogger();

		// add listener to be notified about changes to the setting -> update file handler on the fly
		Settings.addSettingsListener((context, key, value) -> {
			if (SettingsConstants.LOGGING_LOG_FILE.equals(key)) {
				updateFileLogger(value);
			}
		});

		// add listener to be notified about changes to the setting -> register logging bundle again
		Settings.addSettingsListener((context, key, value) -> {
			if (SettingsConstants.I18N_LOCALE.equals(key)) {
				updateLoggerResourceBundle();
			}
		});

		// add listener to be notified about changes to the setting -> update console handler on the fly
		Settings.addSettingsListener((context, key, value) -> {
			if (SettingsConstants.LOGGING_TO_CONSOLE.equals(key) || SettingsConstants.LOGGING_CONSOLE_LEVEL.equals(key)) {
				updateConsoleLogger();
			}
		});
	}

	/**
	 * Set the logging verbosity threshold to
	 * 0 = Level.ALL, 1 = Level.FINER, 2 = Level.FINE, 3 = Level.INFO, 4 = Level.INFO, 5 = Level.WARNING,
	 * 6 = Level.SEVERE, 7 = Level.SEVERE, 8 = Level.SEVERE, 9 = Level.OFF
	 * @param level 0-9
	 */
	public void setVerbosityLevel(int level) {
		Level newLevel = LEVELS[level];
		getRoot().setLevel(newLevel);
		if (logFileHandler != null) {
			logFileHandler.setLevel(newLevel);
		}
		if (consoleHandler != null) {
			consoleHandler.setLevel(newLevel);
		}
	}

	/**
	 * Updates the file logger.
	 *
	 * @param path the absolute path to the log file; or an empty string if no log file is desired
	 */
	private void updateFileLogger(String path) {
		path = Objects.toString(path, "").trim();

		// setup a log file handler if specified
		FileHandler newLogFileHandler = null;
		if (!path.isEmpty()) {
			try {
				newLogFileHandler = new FileHandler(path, false);
				newLogFileHandler.setLevel(Level.ALL);
				newLogFileHandler.setFormatter(new SimpleFormatter());
				// add it now (we have some log file handler overlap here, but we don't lose log entries which is important)
				LogService.getRoot().addHandler(newLogFileHandler);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.logservice.logfile.failed_to_init", e.getMessage());
			}
		}
		// we remove an existing file handler now
		if (logFileHandler != null) {
			getRoot().removeHandler(logFileHandler);
		}

		// finally, remember new file handler
		logFileHandler = newLogFileHandler;
	}

	/**
	 * Sets the console logging if defined.
	 */
	private void updateConsoleLogger() {
		if (Boolean.parseBoolean(Settings.getSetting(SettingsConstants.LOGGING_TO_CONSOLE))) {
			ConsoleHandler newHandler = new ConsoleHandler();
			Level defaultLevel = Level.INFO;

			// set log level
			String settingLevel = Settings.getSetting(SettingsConstants.LOGGING_CONSOLE_LEVEL);
			if (settingLevel != null) {
				try {
					defaultLevel = Level.parse(settingLevel);
				} catch (IllegalArgumentException e) {
					System.err.println("Tried to parse the default console log level, but failed: " + e.getMessage());
				}
			}
			newHandler.setLevel(defaultLevel);

			// we have to set the logger level as well, otherwise the logger might discard things before they get to the handler..
			getRoot().setLevel(defaultLevel);

			// add the new console handler
			getRoot().addHandler(newHandler);

			// remove an existing console handler
			if (consoleHandler != null) {
				getRoot().removeHandler(consoleHandler);
			}

			// finally, remember console handler
			consoleHandler = newHandler;
		} else {
			// no logging to console, remove console handler
			if (consoleHandler != null) {
				getRoot().removeHandler(consoleHandler);
				consoleHandler = null;
			}
		}
	}

	/**
	 * The methods in {@link Logger} do not provide a means to pass an exception AND I18N arguments,
	 * so this method provides a shortcut for this.
	 */
	public static void log(Logger logger, Level level, Throwable exception, String i18NKey, Object... arguments) {
		logger.log(level, I18N.getMessage(logger.getResourceBundle(), i18NKey, arguments), exception);
	}

	/**
	 * Returns the global logging.
	 */
	public static Logger getRoot() {
		return GLOBAL_LOGGER;
	}

	/**
	 * Returns the global logging.
	 *
	 * @deprecated use {@link #getRoot()} instead
	 */
	@Deprecated
	public static LogService getGlobal() {
		return GLOBAL_LOGGING;
	}

	private static void updateLoggerResourceBundle() {
		String logResourceFile = Settings.getSetting(SettingsConstants.LOGGING_RESOURCE_FILE);
		if (logResourceFile != null) {
			ResourceBundle loggingBaseBundle = ResourceBundle.getBundle(logResourceFile, Locale.getDefault(),
					LogService.class.getClassLoader());
			I18N.registerLoggingBundle(loggingBaseBundle);
		}
		GLOBAL_LOGGER.setResourceBundle(I18N.getLoggingBundle());
	}
}
