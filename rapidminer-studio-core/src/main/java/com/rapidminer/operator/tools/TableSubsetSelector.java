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
package com.rapidminer.operator.tools;

import static com.rapidminer.operator.preprocessing.filter.columns.TableSubsetSelectorFilter.SpecialFilterStrategy.FILTER;
import static com.rapidminer.operator.preprocessing.filter.columns.TableSubsetSelectorFilter.SpecialFilterStrategy.KEEP;
import static com.rapidminer.operator.preprocessing.filter.columns.TableSubsetSelectorFilter.SpecialFilterStrategy.REMOVE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.MetaDataChangeListener;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.table.FromTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.preprocessing.filter.columns.AllColumnFilter;
import com.rapidminer.operator.preprocessing.filter.columns.NoMissingValuesColumnFilter;
import com.rapidminer.operator.preprocessing.filter.columns.RegexColumnFilter;
import com.rapidminer.operator.preprocessing.filter.columns.SingleColumnFilter;
import com.rapidminer.operator.preprocessing.filter.columns.SubsetColumnFilter;
import com.rapidminer.operator.preprocessing.filter.columns.TableSubsetSelectorFilter;
import com.rapidminer.operator.preprocessing.filter.columns.ValueTypeColumnFilter;
import com.rapidminer.parameter.MetaDataProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * Subset Selector for a column subset of {@link Table}s.
 *
 * @author Kevin Majchrzak
 * @since 9.9.1
 */
public class TableSubsetSelector {

	/**
	 * {@link MetaDataProvider} that filters out forbidden types in order to make the gui only show the allowed types.
	 */
	private static final class TypeMetaDataProvider implements MetaDataProvider {
		private final InputPort inPort;
		private final String[] allowedTypes;

		private TypeMetaDataProvider(InputPort inPort, String[] allowedTypes) {
			this.inPort = inPort;
			this.allowedTypes = allowedTypes;
		}

		/**
		 * @return the meta data. Filters out the forbidden types. Converts TableMetaData to
		 * ExampleSetMetaData for compatibility.
		 */
		@Override
		public MetaData getMetaData() {
			MetaData md = inPort.getRawMetaData();
			if (allowedTypes != null && allowedTypes.length != 0) {
				// filters out the forbidden types
				TableMetaData tmd = BeltConversionTools.asTableMetaDataOrNull(md);
				if (md != null) {
					TableMetaData filtered = ValueTypeColumnFilter.filterMetaDataWithSettings(tmd,
							FILTER, false, allowedTypes);
					//must not return TableMetaData for compatibility reasons
					return FromTableMetaDataConverter.convert(filtered);
				}
			}
			return md;
		}

		/**
		 * @return the meta data cast to the desired class or {@code null} if it cannot be cast.
		 * Filters out the forbidden types. Converts ExampleSetMetaData to TableMetaData for
		 * compatibility.
		 */
		@Override
		public <T extends MetaData> T getMetaDataAsOrNull(Class<T> desiredClass) {
			try {
				if (allowedTypes != null && allowedTypes.length != 0) {
					// filters out the forbidden types for emd and tmd
					if (TableMetaData.class.equals(desiredClass)) {
						TableMetaData filtered = ValueTypeColumnFilter.filterMetaDataWithSettings(
								inPort.getMetaData(TableMetaData.class), FILTER, false, allowedTypes);
						return desiredClass.cast(filtered);
					}
					if (ExampleSetMetaData.class.equals(desiredClass)) {
						TableMetaData filtered = ValueTypeColumnFilter.filterMetaDataWithSettings(
								inPort.getMetaData(TableMetaData.class), FILTER, false, allowedTypes);
						ExampleSetMetaData emd = (FromTableMetaDataConverter.convert(filtered));
						return desiredClass.cast(emd);
					}
				}
				return inPort.getMetaData(desiredClass);
			} catch (IncompatibleMDClassException e) {
				// cannot cast to desired type
				return null;
			}
		}

		@Override
		public void addMetaDataChangeListener(MetaDataChangeListener l) {
			inPort.registerMetaDataChangeListener(l);
		}

		@Override
		public void removeMetaDataChangeListener(MetaDataChangeListener l) {
			inPort.removeMetaDataChangeListener(l);
		}
	}

	/**
	 * Parameter key for selected filter name.
	 */
	public static final String PARAMETER_FILTER_NAME = "attribute_filter_type";

	/**
	 * Parameter key for "invert filter".
	 */
	public static final String PARAMETER_INCLUDE_OR_EXCLUDE_SELECTION = "type";

	/**
	 * Parameter key for "filter special attributes"
	 */
	public static final String PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES = "also_apply_to_special_attributes_(id,_label..)";

	/**
	 * The parameter value for {@link #PARAMETER_FILTER_NAME} to include all columns.
	 */
	public static final String ALL_ATTRIBUTES_FILTER = "all attributes";

	private static final String EXCLUDE_ATTRIBUTES_STRING = "exclude attributes";
	private static final String INCLUDE_ATTRIBUTES_STRING = "include attributes";

	/**
	 * Maps the names of filters to their suppliers. This map determines which filters will be used for instances of
	 * TableSubsetSelector.
	 */
	private static final Map<String, Function<Operator, TableSubsetSelectorFilter>> nameToFilterMap;

	/**
	 * Holds the names of the filters in the order they have been registered.
	 */
	private static final List<String> filterNames;


	static {
		nameToFilterMap = new HashMap<>();
		filterNames = new ArrayList<>();
		registerFilter(ALL_ATTRIBUTES_FILTER, AllColumnFilter::new);
		registerFilter("one attribute", SingleColumnFilter::new);
		registerFilter("a subset", SubsetColumnFilter::new);
		registerFilter("regular expression", RegexColumnFilter::new);
		registerFilter("type(s) of values", ValueTypeColumnFilter::new);
		registerFilter("no missing values", NoMissingValuesColumnFilter::new);
	}

	private final Operator operator;
	private final String[] allowedTypes;

	private final MetaDataProvider metaDataProvider;

	/**
	 * Creates a subset selector for the given operator and port.
	 *
	 * @param operator
	 * 		the operator for which to filter
	 * @param inPort
	 * 		the port to take the meta data from for the parameter ui
	 */
	public TableSubsetSelector(Operator operator, InputPort inPort) {
		this.operator = ValidationUtilV2.requireNonNull(operator, "operator");
		this.allowedTypes = null;
		metaDataProvider = new ParameterTypeAttribute.InputPortMetaDataProvider(inPort);
	}

	/**
	 * Creates a subset selector for the given operator and port. Prefilters by the allowed types, so that the results
	 * from {@link #getSubset(Table, boolean)} and {@link #getMetaDataSubset(TableMetaData, boolean)} contain only
	 * columns with the allowed types.
	 *
	 * @param operator
	 * 		the operator for which to filter
	 * @param inPort
	 * 		the port to take the meta data from for the parameter ui
	 * @param allowedTypes
	 * 		the types from the {@link ValueTypeColumnFilter}, i.e. a subset of {@link ValueTypeColumnFilter#TYPE_REAL},
	 *        {@link ValueTypeColumnFilter#TYPE_INTEGER}, {@link ValueTypeColumnFilter#TYPE_BINOMINAL}, {@link
	 *        ValueTypeColumnFilter#TYPE_NON_BINOMINAL}, {@link ValueTypeColumnFilter#TYPE_DATE_TIME}, {@link
	 *        ValueTypeColumnFilter#TYPE_TIME}
	 */
	public TableSubsetSelector(Operator operator, InputPort inPort, String... allowedTypes) {
		this.operator = ValidationUtilV2.requireNonNull(operator, "operator");
		this.allowedTypes = allowedTypes;
		metaDataProvider = new TypeMetaDataProvider(inPort, allowedTypes);
	}

	/**
	 * Returns filtered {@link TableMetaData} according to the parameter settings.
	 *
	 * @param metaData
	 * 		the metadata which should be filtered
	 * @param keepSpecialIfNotIncluded
	 * 		if the user decides not to include special columns in the filtering, they are either always kept or always
	 * 		removed based on this parameter
	 * @return the filtered BeltMetaData
	 */
	public TableMetaData getMetaDataSubset(TableMetaData metaData, boolean keepSpecialIfNotIncluded) {
		boolean filterSpecial = operator.getParameterAsBoolean(PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES);
		boolean invert = false;
		String filterName = filterNames.get(0);
		try {
			invert = EXCLUDE_ATTRIBUTES_STRING.
					equals(operator.getParameterAsString(PARAMETER_INCLUDE_OR_EXCLUDE_SELECTION));
		} catch (UndefinedParameterError undefinedParameterError) {
			// nothing to do
		}
		try {
			filterName = operator.getParameterAsString(PARAMETER_FILTER_NAME);
			if (!nameToFilterMap.containsKey(filterName)) {
				filterName = filterNames.get(0);
			}
		} catch (UndefinedParameterError undefinedParameterError) {
			// nothing to do
		}
		if (allowedTypes != null) {
			metaData = ValueTypeColumnFilter.filterMetaDataWithSettings(
					metaData, FILTER, false, allowedTypes);
		}
		TableSubsetSelectorFilter filter = nameToFilterMap.get(filterName).apply(operator);
		if (filterSpecial) {
			return filter.filterMetaData(metaData, FILTER, invert);
		} else {
			return filter.filterMetaData(metaData, keepSpecialIfNotIncluded ? KEEP : REMOVE, invert);
		}
	}

	/**
	 * Returns a Belt table filtered according to the parameter settings.
	 *
	 * @param table
	 * 		the table which should be filtered
	 * @param keepSpecialIfNotIncluded
	 * 		if the user decides not to include special columns in the filtering, they are either always kept or always
	 * 		removed based on this parameter
	 * @return the filtered Belt table
	 * @throws UserError
	 * 		the user errors are generated by the selected filter if the user input is invalid for it
	 */
	public Table getSubset(Table table, boolean keepSpecialIfNotIncluded) throws UserError {
		boolean filterSpecial = operator.getParameterAsBoolean(PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES);
		boolean invert = EXCLUDE_ATTRIBUTES_STRING.
				equals(operator.getParameterAsString(PARAMETER_INCLUDE_OR_EXCLUDE_SELECTION));
		String filterName = operator.getParameterAsString(PARAMETER_FILTER_NAME);
		if (!nameToFilterMap.containsKey(filterName)) {
			filterName = filterNames.get(0);
		}
		if (allowedTypes != null) {
			table = ValueTypeColumnFilter.filterTableWithSettings(table, FILTER, false, allowedTypes);
		}
		TableSubsetSelectorFilter filter = nameToFilterMap.get(filterName).apply(operator);
		if (filterSpecial) {
			return filter.filterTable(table, FILTER, invert);
		} else {
			return filter.filterTable(table, keepSpecialIfNotIncluded ? KEEP : REMOVE, invert);
		}
	}

	/**
	 * This method allows registering filters defined in plugins.
	 *
	 * @param name
	 * 		The name that will be used to present the filter to the user. It should indicate what the filter does.
	 * @param supplier
	 * 		A function that takes an {@link Operator} and return a new filter instance. Will be used to create the filter
	 * 		used by the subset selector.
	 */
	public static void registerFilter(String name, Function<Operator, TableSubsetSelectorFilter> supplier) {
		if (!nameToFilterMap.containsKey(name)) {
			filterNames.add(name);
		}
		nameToFilterMap.put(name, supplier);
	}

	/**
	 * This method creates the parameter types needed to filter columns from belt tables. Provide these parameter types
	 * to the user before using {@link #getSubset(Table, boolean)} and {@link #getMetaDataSubset(TableMetaData,
	 * boolean)}.
	 */
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new ArrayList<>();

		ParameterType type = new ParameterTypeCategory(PARAMETER_INCLUDE_OR_EXCLUDE_SELECTION,
				"This parameter determines if the selected attributes are kept or removed.",
				new String[]{INCLUDE_ATTRIBUTES_STRING, EXCLUDE_ATTRIBUTES_STRING}, 0);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_FILTER_NAME,
				"This parameter selects a filter type.",
				filterNames.toArray(new String[0]), 0);
		type.setExpert(false);
		types.add(type);

		for (String name : filterNames) {
			List<ParameterType> filterTypes = nameToFilterMap.get(name).apply(operator).getParameterTypes(metaDataProvider);
			if (filterTypes != null) {
				for (ParameterType filterType : filterTypes) {
					filterType.registerDependencyCondition(new EqualStringCondition(operator, PARAMETER_FILTER_NAME,
							!filterType.isOptional(), name));
					types.add(filterType);
				}
			}
		}

		type = new ParameterTypeBoolean(PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES,
				"Indicates if the operator should also be applied to special attributes.",
				false);
		type.setExpert(false);
		types.add(type);

		return types;
	}
}
