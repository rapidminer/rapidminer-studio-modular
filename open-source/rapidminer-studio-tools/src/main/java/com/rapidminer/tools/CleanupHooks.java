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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


/**
 * Collection of cleanup actions to be run as cleanup during runtime
 *
 * @author Andreas Timm
 * @since 9.8.0
 */
public enum CleanupHooks {
	;

	private static final List<Runnable> CLEANUP_HOOKS = Collections.synchronizedList(new LinkedList<>());

	/**
	 * Add a runnable, not {@code null}, to be executed whenever RapidMiner cleanup is activated. If your cleanup hook
	 * was successful and does not need to be run again unregister it using {@link #removeCleanupHook(Runnable)}
	 *
	 * @param hook to be executed as cleanup sometime during runtime
	 */
	public static void addCleanupHook(Runnable hook) {
		if (hook == null) {
			throw new IllegalArgumentException("hook must not be null!");
		}
		CLEANUP_HOOKS.add(hook);
	}

	/**
	 * Remove a runnable, not {@code null}, from the cleanup hooks
	 *
	 * @param hook not to be executed as cleanup
	 */
	public static void removeCleanupHook(Runnable hook) {
		if (hook == null) {
			throw new IllegalArgumentException("hook must not be null!");
		}
		CLEANUP_HOOKS.remove(hook);
	}

	/**
	 * Run the all the currently available cleanup hooks, logging errors during execution.
	 */
	public static synchronized void runCleanup() {
		ArrayList<Runnable> cleanUpHooks = new ArrayList<>(CLEANUP_HOOKS);
		for (Runnable cleanUpHook : cleanUpHooks) {
			try {
				cleanUpHook.run();
			} catch (Throwable e) {
				removeCleanupHook(cleanUpHook);
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.CleanupHooks.run_cleanup.error", e);
			}
		}
	}
}
