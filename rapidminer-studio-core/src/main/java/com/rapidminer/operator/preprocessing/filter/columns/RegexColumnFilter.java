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
package com.rapidminer.operator.preprocessing.filter.columns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.parameter.MetaDataProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * Column filter that uses the regular expression to filter the columns regarding their names. Only columns that match
 * the given regular expression are kept.
 *
 * @author Kevin Majchrzak
 * @since 9.9.1
 */
public class RegexColumnFilter implements TableSubsetSelectorFilter {

	public static final String PARAMETER_REGULAR_EXPRESSION = "expression";
	public static final String PARAMETER_EXCEPT_REGULAR_EXPRESSION = "exclude_expression";

	/**
	 * The Operator using the filter.
	 */
	private Operator operator;

	/**
	 * Expects the {@link Operator} using the filter. It is used to access the user input needed to configure the filter
	 * and to access the Operators's {@link com.rapidminer.core.concurrency.ConcurrencyContext}.
	 *
	 * @param operator
	 * 		The Operator using the filter.
	 */
	public RegexColumnFilter(Operator operator) {
		this.operator = operator;
	}

	@Override
	public Table filterTable(Table table, SpecialFilterStrategy strategy, boolean invertFilter) throws UserError {
		String regex = null;
		String exceptRegex = null;
		if (operator.isParameterSet(PARAMETER_REGULAR_EXPRESSION)) {
			regex = operator.getParameterAsString(PARAMETER_REGULAR_EXPRESSION);
		}
		if (operator.isParameterSet(PARAMETER_EXCEPT_REGULAR_EXPRESSION)) {
			exceptRegex = operator.getParameterAsString(PARAMETER_EXCEPT_REGULAR_EXPRESSION);
		}
		Pattern regexPattern = null;
		if (regex != null && !regex.isEmpty()) {
			try {
				regexPattern = Pattern.compile(regex);
			} catch (PatternSyntaxException e) {
				throw new UserError(operator, "subset_selector.regex", PARAMETER_REGULAR_EXPRESSION, e.getMessage());
			}
		}
		Pattern exceptPattern = null;
		if (exceptRegex != null && !exceptRegex.isEmpty()) {
			try {
				exceptPattern = Pattern.compile(exceptRegex);
			} catch (PatternSyntaxException e) {
				throw new UserError(operator, "subset_selector.regex", PARAMETER_EXCEPT_REGULAR_EXPRESSION,
						e.getMessage());
			}
		}
		return filterTableWithSettings(table, strategy, invertFilter, regexPattern, exceptPattern);
	}

	@Override
	public TableMetaData filterMetaData(TableMetaData metaData, SpecialFilterStrategy strategy, boolean invertFilter) {
		String regex = null;
		String exceptRegex = null;
		if (operator.isParameterSet(PARAMETER_REGULAR_EXPRESSION)) {
			try {
				regex = operator.getParameterAsString(PARAMETER_REGULAR_EXPRESSION);
			} catch (UndefinedParameterError undefinedParameterError) {
				// should never happen
			}
		}
		if (operator.isParameterSet(PARAMETER_EXCEPT_REGULAR_EXPRESSION)) {
			try {
				exceptRegex = operator.getParameterAsString(PARAMETER_EXCEPT_REGULAR_EXPRESSION);
			} catch (UndefinedParameterError undefinedParameterError) {
				// should never happen
			}
		}
		Pattern regexPattern = null;
		if (regex != null && !regex.isEmpty()) {
			try {
				regexPattern = Pattern.compile(regex);
			} catch (PatternSyntaxException e) {
				//ignore for metadata
			}
		}
		Pattern exceptPattern = null;
		if (exceptRegex != null && !exceptRegex.isEmpty()) {
			try {
				exceptPattern = Pattern.compile(exceptRegex);
			} catch (PatternSyntaxException e) {
				//ignore for metadata
			}
		}
		return filterMetaDataWithSettings(metaData, strategy, invertFilter, regexPattern, exceptPattern);
	}

	@Override
	public List<ParameterType> getParameterTypes(MetaDataProvider metaDataProvider) {
		List<ParameterType> types = new ArrayList<>();
		types.add(createParameterTypeRegexp(PARAMETER_REGULAR_EXPRESSION,
				"A regular expression for the names of the attributes which should be kept.", false,
				metaDataProvider));
		types.add(createParameterTypeRegexp(PARAMETER_EXCEPT_REGULAR_EXPRESSION,
				"A regular expression for the names of the attributes which should be filtered out although matching the above regular expression.",
				true, metaDataProvider));
		return types;
	}

	/**
	 * Filters the given table by matching the column names to the given regular expressions.
	 *
	 * @param table
	 * 		the table to be filtered
	 * @param strategy
	 * 		describes how the filter should handle special columns. See {@link SpecialFilterStrategy}.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa)
	 * @param regex
	 * 		Columns that do not match this regular expression pattern are filtered out. If this is {@code null} it
	 * 		accepts	all columns.
	 * @param exceptRegex
	 * 		Columns that match this regular expression pattern are filtered out. If this is {@code null} it accepts
	 * 		all	columns.
	 * @return the filtered table
	 */
	public static Table filterTableWithSettings(Table table, SpecialFilterStrategy strategy, boolean invertFilter,
												Pattern regex, Pattern exceptRegex) {
		Predicate<String> filter;
		if (regex != null) {
			filter = columnName -> regex.matcher(columnName).matches();
		} else {
			filter = columnName -> true;
		}
		if (exceptRegex != null) {
			filter = filter.and(columnName -> !exceptRegex.matcher(columnName).matches());
		}
		filter = FilterUtils.addDefaultFilters(table, strategy, invertFilter, filter);
		return table.columns(table.labels().stream().filter(filter).collect(Collectors.toList()));
	}

	/**
	 * Filters the given meta data by matching the column names to the given regular expressions.
	 *
	 * @param metaData
	 * 		the meta data to be filtered
	 * @param strategy
	 * 		describes how the filter should handle special columns. See {@link SpecialFilterStrategy}.
	 * @param invertFilter
	 * 		inverts the result of the filter (keeps the columns that would usually be removed and vice versa)
	 * @param regex
	 * 		Columns that do not match this regular expression pattern are filtered out. If this is {@code null} it
	 * 		accepts	all columns.
	 * @param exceptRegex
	 * 		Columns that match this regular expression pattern are filtered out. If this is {@code null} it accepts
	 * 		all	columns.
	 * @return the filtered table
	 */
	public static TableMetaData filterMetaDataWithSettings(TableMetaData metaData, SpecialFilterStrategy strategy,
														   boolean invertFilter, Pattern regex, Pattern exceptRegex) {
		Predicate<String> filter;
		if (regex != null) {
			filter = columnName -> regex.matcher(columnName).matches();
		} else {
			filter = columnName -> true;
		}
		if (exceptRegex != null) {
			filter = filter.and(columnName -> !exceptRegex.matcher(columnName).matches());
		}
		filter = FilterUtils.addDefaultFilters(metaData, strategy, invertFilter, filter);
		return metaData.columns(metaData.labels().stream().filter(filter).collect(Collectors.toList()));
	}

	/**
	 * Helper method that creates a {@link ParameterTypeRegexp} with a preview list that is populated according to the
	 * given metaDataProvider.
	 *
	 * @param key
	 * 		the parameter types key
	 * @param description
	 * 		the parameter types description
	 * @param isExpert
	 *        {@code true} iff the result should be an expert parameter
	 * @param metaDataProvider
	 * 		the {@link MetaDataProvider} holding the metadata of the columns that will be filtered
	 * @return a ParameterTypeRegexp with previewList
	 */
	private static ParameterTypeRegexp createParameterTypeRegexp(String key, String description, boolean isExpert,
																 MetaDataProvider metaDataProvider) {
		return new ParameterTypeRegexp(key, description, true, isExpert) {
			private static final long serialVersionUID = 8133149560984042644L;

			@Override
			public Collection<String> getPreviewList() {
				if (metaDataProvider == null) {
					return Collections.emptyList();
				}
				TableMetaData metaData = metaDataProvider.getMetaDataAsOrNull(TableMetaData.class);
				if (metaData == null) {
					return Collections.emptyList();
				}
				return metaData.labels();
			}
		};
	}
}
