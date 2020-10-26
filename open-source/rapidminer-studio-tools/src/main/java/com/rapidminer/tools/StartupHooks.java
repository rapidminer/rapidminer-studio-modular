/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


/**
 * Utility class for dealing with startup hooks.
 *
 * @author Andreas Timm
 * @since 9.8.0
 */
public enum StartupHooks {
	;

	private static final List<Runnable> STARTUP_HOOKS = new LinkedList<>();

	private static boolean initialized = false;

	/**
	 * Adds the given shutdown hook. Executes once {@link #runStartupHooks()} is called.
	 *
	 * @param hook the hook, must not be {@code null}
	 */
	public static void addStartupHook(Runnable hook) {
		if (hook == null) {
			throw new IllegalArgumentException("hook must not be null!");
		}
		synchronized (STARTUP_HOOKS) {
			if (initialized) {
				runHook(hook);
			} else {
				STARTUP_HOOKS.add(hook);
			}
		}
	}

	/**
	 * Runs all startup hooks in the current thread. Any exceptions are logged, but all hooks are run and this method
	 * will return gracefully. This can only be called once, because afterwards the hooks are cleared.<n
	 */
	public static void runStartupHooks() {
		synchronized (STARTUP_HOOKS) {
			for (Runnable hook : STARTUP_HOOKS) {
				runHook(hook);
			}
			STARTUP_HOOKS.clear();
			initialized = true;
		}
	}

	/**
	 * Run the given {@link Runnable} hook and log its {@link Exception} if any
	 */
	private static void runHook(Runnable hook) {
		try {
			hook.run();
		} catch (Throwable t) {
			// catching Throwable because this also accounts for things like ExceptionInInitializerErrors
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.RapidMiner.executing_startup_hook_error", t.getMessage()), t);
		}
	}
}
