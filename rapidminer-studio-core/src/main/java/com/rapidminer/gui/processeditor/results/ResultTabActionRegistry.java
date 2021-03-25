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
package com.rapidminer.gui.processeditor.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.SecurityTools;


/**
 * The {@link ResultTabActionRegistry} provides a hook to add {@link ResourceAction}s to {@link ResultTab}s
 *
 * @author Andreas Timm
 * @since 9.1
 */
public enum ResultTabActionRegistry {
	/**
	 * The instance
	 */
	INSTANCE;

	private static final int MAX_ENTRY_COUNT = 3;
	/**
	 * All available actions
	 */
	private List<ResultActionGuiProvider> resultActions = new ArrayList<>();

	/**
	 * Add a {@link ResultActionGuiProvider} action to the ResultTab data view.
	 *
	 * <p>
	 * Internal usage only!
	 * </p>
	 *
	 * @param actionGuiProvider
	 * 		that creates a GUI JComponent for a later available ExampleSet
	 */
	public void addAction(ResultActionGuiProvider actionGuiProvider) {
		if (actionGuiProvider == null) {
			throw new IllegalArgumentException("actionGuiProvider must not be null");
		}
		if (resultActions.size() >= MAX_ENTRY_COUNT) {
			LogService.getRoot().log(Level.WARNING, "Adding more than {0} ResultTabActions is not supported.", MAX_ENTRY_COUNT);
			return;
		}
		try {
			// only signed extensions are allowed to register actions
			SecurityTools.requireInternalPermission();
		} catch (UnsupportedOperationException e) {
			return;
		}
		resultActions.add(actionGuiProvider);
	}

	/**
	 * Remove a registered action from the ResultTab.
	 *
	 * <p>
	 * Internal usage only!
	 * </p>
	 *
	 * @param actionGuiProvider
	 * 		the previously registered {@link ResultActionGuiProvider}
	 * @return {@code true} if the provider was removed
	 */
	public boolean removeAction(ResultActionGuiProvider actionGuiProvider) {
		try {
			// only signed extensions are allowed to register actions
			SecurityTools.requireInternalPermission();
		} catch (UnsupportedOperationException e) {
			return false;
		}
		return resultActions.remove(actionGuiProvider);
	}

	/**
	 * Get all available actions for the ResultTab
	 *
	 * @return List of {@link ResultActionGuiProvider}s
	 */
	public List<ResultActionGuiProvider> getActions() {
		return Collections.unmodifiableList(resultActions);
	}
}
