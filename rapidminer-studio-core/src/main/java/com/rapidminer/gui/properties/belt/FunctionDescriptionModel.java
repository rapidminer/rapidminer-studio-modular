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
package com.rapidminer.gui.properties.belt;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.rapidminer.gui.properties.ExpressionPropertyDialog;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.belt.expression.FunctionDescription;


/**
 * This class is the model for the {@link FunctionDescription}s in the {@link ExpressionPropertyDialog}
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class FunctionDescriptionModel extends AbstractObservable<FunctionDescription> {

	/** contains the list with ALL {@link FunctionDescription}s */
	private Map<String, List<FunctionDescription>> modelMap = new LinkedHashMap<>();

	/** contains the filtered list of {@link FunctionDescription}s */
	private Map<String, List<FunctionDescription>> filteredModelMap = new LinkedHashMap<>();

	/** the function name filter {@link String} */
	private String filterNameString;

	/** the function name filter {@link String} in lower case */
	private String filterNameStringLowerCase;

	/**
	 * comparator which is used to compare the importance of a {@link FunctionDescription} in regard to the {@link
	 * #filterNameString}. If the search string is contained in the name it's more important as if the search string is
	 * contained in the description.
	 */
	private Comparator<FunctionDescription> functionComparator = (o1, o2) -> {
		if (filterNameString.isEmpty()) {
			// if there is no filtering, keep the initial sorting
			return 0;
		} else {
			// matching names come with higher priority than matching descriptions
			if (o1.getDisplayName().toLowerCase(Locale.ENGLISH).contains(filterNameStringLowerCase)
					&& !o2.getDisplayName().toLowerCase(Locale.ENGLISH).contains(filterNameStringLowerCase)) {
				return -1;
			} else if (!o1.getDisplayName().toLowerCase(Locale.ENGLISH).contains(filterNameStringLowerCase)
					&& o2.getDisplayName().toLowerCase(Locale.ENGLISH).contains(filterNameStringLowerCase)) {
				return +1;
			} else {
				return 0;
			}
		}
	};

	public FunctionDescriptionModel() {
		clearContent();
	}

	/**
	 * Clears all model content.
	 */
	public void clearContent() {
		modelMap.clear();
		filteredModelMap.clear();
		filterNameString = "";
		filterNameStringLowerCase = "";
	}

	/**
	 * Add the given functions for the key (function group name). Applies filtering and sorting
	 * implicitly.
	 *
	 * @param functions
	 *            list of functions in the given function group
	 */
	public void addContent(List<FunctionDescription> functions) {
		for (FunctionDescription function : functions) {
			if (modelMap.containsKey(function.getGroupName())) {
				modelMap.get(function.getGroupName()).add(function);
			} else {
				modelMap.put(function.getGroupName(), new LinkedList<>());
				modelMap.get(function.getGroupName()).add(function);
			}
		}
		applyFilter();
		applySorting();
	}

	/**
	 * @return the filtered {@link Map} of {@link List}s of {@link FunctionDescription}s.
	 */
	public Map<String, List<FunctionDescription>> getFilteredModel() {
		return filteredModelMap;
	}

	/**
	 * Returns the filtered list of {@link com.rapidminer.tools.belt.expression.FunctionDescription}s for one specific
	 * function group (defined by the group name)
	 *
	 * @param functionGroupName
	 * 		the group name
	 * @return filtered list of function descriptions
	 */
	public List<FunctionDescription> getFilteredModel(String functionGroupName) {
		return filteredModelMap.get(functionGroupName);
	}

	/**
	 * @return the filter name
	 */
	public String getFilterNameString() {
		return filterNameString;
	}

	/**
	 * Filters the list of {@link FunctionDescription}s using the filterNameString.
	 *
	 * @param filterNameString
	 *            search word which is used to filter functions
	 */
	public void setFilterNameString(String filterNameString) {
		if (filterNameString == null) {
			filterNameString = "";
		}

		// do nothing on equal filter name
		if (filterNameString.equals(this.filterNameString)) {
			return;
		}

		this.filterNameString = filterNameString;
		this.filterNameStringLowerCase = filterNameString.toLowerCase(Locale.ENGLISH);
		applyFilter();
		applySorting();
		fireUpdate();
	}

	/**
	 * Applies the current filter.
	 */
	private void applyFilter() {

		filteredModelMap.clear();
		for (Entry<String, List<FunctionDescription>> entry : modelMap.entrySet()) {
			List<FunctionDescription> newList = new LinkedList<>(entry.getValue());
			filteredModelMap.put(entry.getKey(), newList);
		}

		if (!getFilterNameString().isEmpty()) {
			// apply filter on non empty string
			for (Entry<String, List<FunctionDescription>> entry : filteredModelMap.entrySet()) {
				filterEntry(entry);
			}
			// if a function group has no fitting functions, remove the function group
			for (String key : modelMap.keySet()) {
				List<FunctionDescription> list = filteredModelMap.get(key);
				if (list.isEmpty()) {
					filteredModelMap.remove(key);
				}
			}
		}
	}

	/**
	 * Helper method used in {@link #applyFilter()} to filter an entry.
	 */
	private void filterEntry(Entry<String, List<FunctionDescription>> entry) {
		// if the function group name already matches the search string, keep all group
		// functions
		if (entry.getKey().toLowerCase(Locale.ENGLISH).contains(filterNameStringLowerCase)) {
			return;
		}
		// remove the functions that do not fit the search string
		for (int i = entry.getValue().size() - 1; i >= 0; i--) {
			FunctionDescription function = entry.getValue().get(i);
			if (!function.getDisplayName().toLowerCase(Locale.ENGLISH).contains(filterNameStringLowerCase)
					&& !function.getHelpTextName().toLowerCase(Locale.ENGLISH).contains(filterNameStringLowerCase)
					&& !function.getFunctionNameWithParameters().toLowerCase(Locale.ENGLISH)
							.contains(filterNameStringLowerCase)
					&& !function.getDescription().toLowerCase(Locale.ENGLISH).contains(filterNameStringLowerCase)) {
				filteredModelMap.get(entry.getKey()).remove(i);
			}
		}
	}

	/**
	 * Sorts the {@link FunctionDescription} by matching the search string. Matching names come with higher priority
	 * than matching descriptions.
	 */
	private void applySorting() {
		for (List<FunctionDescription> list : filteredModelMap.values()) {
			list.sort(functionComparator);
		}
	}

}
