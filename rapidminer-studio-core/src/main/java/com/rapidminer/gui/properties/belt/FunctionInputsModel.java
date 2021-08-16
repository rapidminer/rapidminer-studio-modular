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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionDescription;
import com.rapidminer.tools.belt.expression.FunctionInput;
import com.rapidminer.tools.belt.expression.FunctionInput.Category;
import com.rapidminer.tools.belt.expression.TableResolver;


/**
 * This class is the model for the actual variables, macros and constants in the {@link ExpressionPropertyDialog}.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class FunctionInputsModel extends AbstractObservable<FunctionInputPanel> {

	/** contains the list with ALL inputs */
	private LinkedHashMap<String, List<FunctionInput>> modelMap = new LinkedHashMap<>();

	/** contains the filtered list of inputs */
	private LinkedHashMap<String, List<FunctionInput>> filteredModelMap = new LinkedHashMap<>();

	/** the function name filter {@link String} */
	private String filterNameString;

	private boolean nominalFilter = false;
	private boolean numericFilter = false;
	private boolean dateTimeFilter = false;

	/**
	 * Sorts the incoming {@link FunctionInput}s by category first
	 */
	private static final Comparator<FunctionInput> FUNCTION_INPUT_COMPARATOR = (o1, o2) -> {
		if (o1 == null && o2 == null) {
			return 0;
		} else if (o1 != null && o2 == null) {
			return 1;
		} else if (o1 == null) {
			return -1;
		} else {
			if (o1.getCategory() == Category.DYNAMIC && o2.getCategory() == Category.DYNAMIC) {

				// and I am not sure what the intended result should be
				if (o1.getCategoryName().equals(o2.getCategoryName())) {
					return o1.getName().compareToIgnoreCase(o2.getName());
				} else if (o1.getCategoryName().equals(TableResolver.KEY_ATTRIBUTES)) {
					return -1;
				} else if (o2.getCategoryName().equals(TableResolver.KEY_ATTRIBUTES)) {
					return 1;
				} else if (o1.getCategoryName().equals(TableResolver.KEY_SPECIAL_ATTRIBUTES)) {
					return -1;
				} else if (o2.getCategoryName().equals(TableResolver.KEY_SPECIAL_ATTRIBUTES)) {
					return 1;
				} else {
					return 0;
				}
			} else if (o1.getCategory() == Category.SCOPE && o2.getCategory() == Category.SCOPE) {
				return o1.useCustomIcon() ? -1 : 1;

			} else return Integer.compare(o2.getCategory().ordinal(), o1.getCategory().ordinal());
		}
	};

	/**
	 * Creates a model for the possible {@link FunctionDescription} inputs
	 */
	public FunctionInputsModel() {
		clearContent();
	}

	/**
	 * Clears all model content.
	 */
	public void clearContent() {
		modelMap = new LinkedHashMap<>();
		filteredModelMap = new LinkedHashMap<>();
		filterNameString = "";
		nominalFilter = false;
		numericFilter = false;
		dateTimeFilter = false;
	}

	/**
	 * Add the given inputs.
	 */
	public void addContent(List<FunctionInput> inputs) {
		inputs.sort(FUNCTION_INPUT_COMPARATOR);
		for (FunctionInput input : inputs) {
			if (input.isVisible()) {
				if (!modelMap.containsKey(input.getCategoryName())) {
					modelMap.put(input.getCategoryName(), new LinkedList<>());
				}
				modelMap.get(input.getCategoryName()).add(input);
			}
		}
		applyFilter();
	}

	/**
	 * @return the filtered model as map from String to List
	 */
	public Map<String, List<FunctionInput>> getFilteredModel() {
		return filteredModelMap;
	}

	/**
	 * returns the filtered map of Strings for one specific input type (defined by the type name)
	 */
	public List<FunctionInput> getFilteredModel(String type) {
		return filteredModelMap.get(type);
	}

	/**
	 * Filters the list of inputs using the filterNameString.
	 */
	public void setFilterNameString(String filterNameString) {
		// do nothing on equal filter name
		if (filterNameString.equals(this.filterNameString)) {
			return;
		}

		this.filterNameString = filterNameString;
		applyFilter();
		fireUpdate();
	}

	/**
	 * @return the filter name
	 */
	public String getFilterNameString() {
		return filterNameString;
	}

	/**
	 * Sets the filter to show the nominal variables and constants. The user controls the filter via checkboxes in the
	 * gui.
	 */
	public void setNominalFilter(boolean filterToggled) {
		nominalFilter = filterToggled;
		applyFilter();
		fireUpdate();
	}

	/**
	 * get the nominal filter state
	 *
	 * @return if the nominal filter is toggled
	 */
	public boolean isNominalFilterToggled() {
		return nominalFilter;
	}

	/**
	 * Sets the filter to show the numeric variables and constants. The user controls the filter via checkboxes in the
	 * gui.
	 */
	public void setNumericFilter(boolean filterToggled) {
		numericFilter = filterToggled;
		applyFilter();
		fireUpdate();
	}

	/**
	 * get the numeric filter state
	 *
	 * @return if the numeric filter is toggled
	 */
	public boolean isNumericFilterToggled() {
		return numericFilter;
	}

	/**
	 * Sets the filter to show the date-time and time variables and constants. The user controls the filter via
	 * checkboxes in the gui.
	 */
	public void setDateTimeFilter(boolean filterToggled) {
		dateTimeFilter = filterToggled;
		applyFilter();
		fireUpdate();
	}

	/**
	 * get the date time filter state
	 *
	 * @return if the date time filter is toggled
	 */
	public boolean isDateTimeFilterToggled() {
		return dateTimeFilter;
	}

	/**
	 * Applies the current filters.
	 */
	private void applyFilter() {

		filteredModelMap = new LinkedHashMap<>();
		for (Entry<String, List<FunctionInput>> entry : modelMap.entrySet()) {
			List<FunctionInput> newList = new LinkedList<>(entry.getValue());
			filteredModelMap.put(entry.getKey(), newList);
		}
		boolean anyFilterToggled = isNominalFilterToggled() || isNumericFilterToggled() || isDateTimeFilterToggled();

		// apply filter on non empty string
		for (Entry<String, List<FunctionInput>> modelEntry : filteredModelMap.entrySet()) {
			applyFilterToEntry(anyFilterToggled, modelEntry);
		}
		// if a function group has no fitting inputs, remove the input group
		for (String key : modelMap.keySet()) {
			List<FunctionInput> list = filteredModelMap.get(key);
			if (list.isEmpty()) {
				filteredModelMap.remove(key);
			}
		}
	}

	/**
	 * Applies the current filters to the given entry.
	 */
	private void applyFilterToEntry(boolean anyFilterToggled, Entry<String, List<FunctionInput>> modelEntry) {
		List<FunctionInput> list = modelEntry.getValue();
		// if the function group name already matches the search string, keep all group
		// inputs
		Iterator<FunctionInput> it = list.iterator();
		// remove the inputs that do not fit the search string
		while (it.hasNext()) {

			boolean alreadyRemoved = false;
			FunctionInput input = it.next();
			String entryName = input.getName();
			// check whether the input entry with the given type
			if (anyFilterToggled && !showInputEntry(input.getType())) {
				// should be shown
				it.remove();
				alreadyRemoved = true;
			}
			if (!getFilterNameString().isEmpty() && !alreadyRemoved
					&& !entryName.toLowerCase(Locale.ENGLISH).contains(filterNameString.toLowerCase(Locale.ENGLISH))
					&& !modelEntry.getKey().toLowerCase(Locale.ENGLISH).contains(filterNameString.toLowerCase(Locale.ENGLISH))) {
				it.remove();
			}
		}
	}

	/**
	 * Checks and returns whether an input entry is shown based on the entryType and the toggled filters
	 *
	 * @param entryType
	 * 		the entry's expression type
	 * @return if the entry should be shown
	 */
	private boolean showInputEntry(ExpressionType entryType) {
		switch (entryType) {
			case INTEGER:
			case DOUBLE:
				return isNumericFilterToggled();
			case INSTANT:
			case LOCAL_TIME:
				return isDateTimeFilterToggled();
			case STRING:
				return isNominalFilterToggled();
			default:
				return true;
		}
	}
}
