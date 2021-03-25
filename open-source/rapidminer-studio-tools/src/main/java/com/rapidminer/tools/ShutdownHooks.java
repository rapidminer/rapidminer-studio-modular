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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


/**
 * Utility class for dealing with shutdown hooks.
 *
 * @author Marco Boeck
 * @since 9.8.0
 */
public enum ShutdownHooks {
	;

	private static final List<Runnable> SHUTDOWN_HOOKS = Collections.synchronizedList(new LinkedList<>());


	/**
	 * Adds the given shutdown hook. Executes once {@link #runShutdownHooks()} is called.
	 *
	 * @param hook the hook, must not be {@code null}
	 */
	public static void addShutdownHook(Runnable hook) {
		if (hook == null) {
			throw new IllegalArgumentException("hook must not be null!");
		}

		SHUTDOWN_HOOKS.add(hook);
	}

	/**
	 * Runs all shutdown hooks in the current thread. Any exceptions are logged, but all hooks are run and this method
	 * will return gracefully. This can only be called once, because afterwards the hooks are cleared.
	 */
	public static void runShutdownHooks() {
		synchronized (SHUTDOWN_HOOKS) {
			for (Runnable hook : SHUTDOWN_HOOKS) {
				try {
					hook.run();
				} catch (Throwable t) {
					// catching Throwable because this also accounts for things like ExceptionInInitializerErrors
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.RapidMiner.executing_shutdown_hook_error", t.getMessage()), t);
				}
			}
			SHUTDOWN_HOOKS.clear();
		}
	}
}
