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
package com.rapidminer.operator.nio;

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.nio.model.AbstractDataResultSetReader;
import com.rapidminer.operator.nio.model.CSVResultSetConfiguration;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.StrictDecimalFormat;


/**
 *
 * <p>
 * This operator can be used to load data from .csv files. The user can specify the delimiter and
 * various other parameters.
 * </p>
 *
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Loh, Sebastian Land, Simon Fischer, Marco Boeck
 * @deprecated since 9.9 use {@link CSVTableSource} instead
 */
@Deprecated
public class CSVExampleSource extends AbstractDataResultSetReader {

	public static final String PARAMETER_CSV_FILE = CSVTableSource.PARAMETER_CSV_FILE;
	public static final String PARAMETER_TRIM_LINES = CSVTableSource.PARAMETER_TRIM_LINES;
	public static final String PARAMETER_SKIP_COMMENTS = CSVTableSource.PARAMETER_SKIP_COMMENTS;
	public static final String PARAMETER_COMMENT_CHARS = CSVTableSource.PARAMETER_COMMENT_CHARS;
	public static final String PARAMETER_USE_QUOTES = CSVTableSource.PARAMETER_USE_QUOTES;
	public static final String PARAMETER_QUOTES_CHARACTER = CSVTableSource.PARAMETER_QUOTES_CHARACTER;
	public static final String PARAMETER_COLUMN_SEPARATORS = CSVTableSource.PARAMETER_COLUMN_SEPARATORS;
	public static final String PARAMETER_ESCAPE_CHARACTER = CSVTableSource.PARAMETER_ESCAPE_CHARACTER;
	public static final String PARAMETER_STARTING_ROW = CSVTableSource.PARAMETER_STARTING_ROW;

	/**
	 * Values will be trimmed for guessing after this version
	 *
	 * @since 9.2.0
	 */
	public static final OperatorVersion BEFORE_VALUE_TRIMMING_GUESSING = CSVTableSource.BEFORE_VALUE_TRIMMING_GUESSING;

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription("csv", CSVExampleSource.class,
				PARAMETER_CSV_FILE));
	}

	public CSVExampleSource(final OperatorDescription description) {
		super(description);
	}

	@Override
	protected DataResultSetFactory getDataResultSetFactory() throws OperatorException {
		return new CSVResultSetConfiguration(this);
	}

	@Override
	protected NumberFormat getNumberFormat() throws OperatorException {
		return StrictDecimalFormat.getInstance(this, true);
	}

	@Override
	protected boolean supportsEncoding() {
		return true;
	}

	@Override
	protected String getFileParameterName() {
		return PARAMETER_CSV_FILE;
	}

	@Override
	protected String getFileExtension() {
		return "csv";
	}


	/**
	 * Whether attributes should be trimmed for guessing
	 *
	 * @return {@code true} if compatibility level is above {@link #BEFORE_VALUE_TRIMMING_GUESSING}
	 * @since 9.2.0
	 */
	@Override
	public boolean trimForGuessing() {
		return getCompatibilityLevel().isAbove(BEFORE_VALUE_TRIMMING_GUESSING);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// Trim the date format, if the values are trimmed
		String dateFormat = getParameter(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT);
		if (dateFormat != null && trimForGuessing()) {
			setParameter(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT, dateFormat.trim());
		}
		return super.createExampleSet();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		LinkedList<ParameterType> types = new LinkedList<>();

		ParameterType type = new ParameterTypeConfiguration(CSVExampleSourceConfigurationWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);
		types.add(makeFileParameterType());

		// Separator
		types.add(new ParameterTypeString(PARAMETER_COLUMN_SEPARATORS,
				"Column separators for data files (regular expression)", ";", false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_TRIM_LINES,
				"Indicates if lines should be trimmed (empty spaces are removed at the beginning and the end) before the column split is performed. This option might be problematic if TABs are used as a seperator.",
				false));
		// Quotes
		types.add(new ParameterTypeBoolean(PARAMETER_USE_QUOTES, "Indicates if quotes should be regarded.", true, false));
		type = new ParameterTypeChar(PARAMETER_QUOTES_CHARACTER, "The quotes character.",
				LineParser.DEFAULT_QUOTE_CHARACTER, false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_QUOTES, false, true));
		types.add(type);
		type = new ParameterTypeChar(PARAMETER_ESCAPE_CHARACTER,
				"The character that is used to escape quotes and column seperators",
				LineParser.DEFAULT_QUOTE_ESCAPE_CHARACTER, true);
		types.add(type);

		// Comments
		types.add(new ParameterTypeBoolean(PARAMETER_SKIP_COMMENTS, "Indicates if a comment character should be used.",
				false, false));
		type = new ParameterTypeString(PARAMETER_COMMENT_CHARS, "Lines beginning with these characters are ignored.", "#",
				false);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SKIP_COMMENTS, false, true));
		types.add(type);

		// Starting row
		types.add(new ParameterTypeInt(PARAMETER_STARTING_ROW, "The first row where reading should start, everything before it will be skipped.",
				1, Integer.MAX_VALUE, 1, true));

		// Number formats
		types.addAll(StrictDecimalFormat.getParameterTypes(this, true));
		type = new ParameterTypeDateFormat();
		type.setDefaultValue(ParameterTypeDateFormat.DATE_FORMAT_YYYY_MM_DD);
		types.add(type);

		types.addAll(super.getParameterTypes());
		return types;
	}

	@Override
	public void configure(DataSource dataSource) throws DataSetException {
		// set general csv import configParameters
		CSVTableSource.configure(dataSource, this);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[]{
						BEFORE_VALUE_TRIMMING_GUESSING
				});
	}
}
