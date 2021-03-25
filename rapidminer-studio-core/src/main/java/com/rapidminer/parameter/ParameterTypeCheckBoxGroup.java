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
package com.rapidminer.parameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * ParameterType showing groups of checkboxes to the user. Use the {@link #add(String, String)} method to add a checkbox
 * to a named checkbox group. If the name of the checkbox group is {@code null} the checkbox is added to the unnamed
 * checkbox group. The user can select any combination of these checkboxes. The selected checkboxes can be obtained via
 * {@link #stringToSelection(String)}.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
public class ParameterTypeCheckBoxGroup extends ParameterTypeSingle {

	public static final char CHECKBOX_SEPARATOR = Parameters.RECORD_SEPARATOR;

	private final Map<String, List<String>> checkBoxGroups;

	/**
	 * Creates a new instance of this ParameterType.
	 *
	 * @param key         the parameter key
	 * @param description the operator description
	 */
	public ParameterTypeCheckBoxGroup(String key, String description) {
		super(key, description);
		setOptional(false);
		checkBoxGroups = new LinkedHashMap<>();
	}

	/**
	 * Adds a checkbox with the specified name to the specified group.
	 *
	 * @param group    the group to add the checkbox to (can be {@code null}) for the unnamed group
	 * @param checkBox the label of the checkbox (cannot be {@code null})
	 */
	public void add(String group, String checkBox) {
		List<String> checkBoxes = checkBoxGroups.computeIfAbsent(group, k -> new ArrayList<>());
		checkBoxes.add(checkBox);
	}

	/**
	 * Returns a set holding the names of all groups (including {@code null} if the unnamed group has been used). The
	 * set itself is never {@code null}.
	 *
	 * @return the names of all groups
	 */
	public Set<String> getGroups() {
		return Collections.unmodifiableSet(checkBoxGroups.keySet());
	}

	/**
	 * Returns a list holding the names of the checkboxes contained in the given group. {@code null} if the group does
	 * not exist.
	 *
	 * @param group the name of the group. {@code null} can be used to reference the unnamed group.
	 * @return the names of the checkboxes contained in the given group
	 */
	public List<String> getCheckBoxNames(String group) {
		return checkBoxGroups.get(group);
	}

	/**
	 * Returns null.
	 */
	@Override
	public String getRange() {
		return null;
	}

	/**
	 * Returns null.
	 */
	@Override
	public Object getDefaultValue() {
		return null;
	}

	/**
	 * Does nothing. The default value is always null.
	 */
	@Override
	public void setDefaultValue(Object defaultValue) {
		// does nothing
	}

	/**
	 * Returns false.
	 */
	@Override
	public boolean isNumerical() {
		return false;
	}

	/**
	 * Converts the parameter types String representation to an array holding the selected checkbox names.
	 *
	 * @param parameterAsString the parameter types string representation (e.g. obtained via {@link
	 *                          com.rapidminer.operator.Operator#getParameterAsString(String)})
	 * @return the names of the selected checkboxes
	 */
	public static String[] stringToSelection(String parameterAsString) {
		if (parameterAsString == null || parameterAsString.isEmpty()) {
			return new String[0];
		}
		return parameterAsString.split(String.valueOf(CHECKBOX_SEPARATOR));
	}

}