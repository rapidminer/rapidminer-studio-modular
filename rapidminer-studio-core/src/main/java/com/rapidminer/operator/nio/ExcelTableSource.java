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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.Table;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.io.AbstractReader;
import com.rapidminer.operator.nio.model.AbstractDataResultTableReader;
import com.rapidminer.operator.nio.model.DataResultSet;
import com.rapidminer.operator.nio.model.DataResultSetFactory;
import com.rapidminer.operator.nio.model.DataResultSetTranslationConfiguration;
import com.rapidminer.operator.nio.model.ExcelResultSetConfiguration;
import com.rapidminer.operator.nio.model.xlsx.XlsxResultSet;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.parameter.conditions.NonEqualStringCondition;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.io.Encoding;


/**
 * This operator can be used to load data from Microsoft Excel spreadsheets. This operator is able to reads data from
 * Excel 95, 97, 2000, XP, and 2003. The user has to define which of the spreadsheets in the workbook should be used as
 * data table. The table must have a format so that each line is an example and each column represents an attribute.
 * Please note that the first line might be used for attribute names which can be indicated by a parameter.
 *
 * The data table can be placed anywhere on the sheet and is allowed to contain arbitrary formatting instructions, empty
 * rows, and empty columns. Missing data values are indicated by empty cells or by cells containing only &quot;?&quot;.
 *
 * This operator is a copy of {@link ExcelExampleSource} and has exactly the same results apart from reading into an
 * {@link IOTable}. For that reason, old compatibility levels are kept.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ExcelTableSource extends AbstractDataResultTableReader {

	private static final String XLSX = "xlsx";
	private static final String XLS = "xls";
	public static final OperatorVersion CHANGE_5_0_4 = new OperatorVersion(5, 0, 4);
	public static final OperatorVersion CHANGE_5_0_11_NAME_SCHEMA = new OperatorVersion(5, 0, 11);

	/**
	 * Last version that used the old POI XLSX import
	 */
	public static final OperatorVersion CHANGE_6_2_0_OLD_XLSX_IMPORT = new OperatorVersion(6, 2, 0);

	/**
	 * The parameter name for &quot;The Excel spreadsheet file which should be loaded.&quot;
	 */
	public static final String PARAMETER_EXCEL_FILE = "excel_file";

	/**
	 * The parameter name for &quot;The sheet selection mode.&quot;
	 */
	public static final String PARAMETER_SHEET_SELECTION = "sheet_selection";

	/**
	 * The parameter name for &quot;The number of the sheet which should be imported.&quot;
	 */
	public static final String PARAMETER_SHEET_NUMBER = "sheet_number";

	/**
	 * The parameter name for &quot;The name of the sheet which should be imported.&quot;
	 */
	public static final String PARAMETER_SHEET_NAME = "sheet_name";

	/**
	 * {@link #SHEET_SELECTION_MODES} index - select by number
	 */
	public static final int SHEET_SELECT_BY_INDEX = 0;

	/**
	 * {@link #SHEET_SELECTION_MODES} index - select by name
	 */
	public static final int SHEET_SELECT_BY_NAME = 1;

	/**
	 * Selection modes for sheets
	 */
	private static final String[] SHEET_SELECTION_MODES =
			{PARAMETER_SHEET_NUMBER.replace("_", " "), PARAMETER_SHEET_NAME.replace("_", " ")};

	public static final String PARAMETER_IMPORTED_CELL_RANGE = "imported_cell_range";

	static {
		AbstractReader.registerReaderDescription(new ReaderDescription(XLS, ExcelTableSource.class,
				PARAMETER_EXCEL_FILE));
		AbstractReader
				.registerReaderDescription(new ReaderDescription(XLSX, ExcelTableSource.class, PARAMETER_EXCEL_FILE));
	}

	public ExcelTableSource(final OperatorDescription description) {
		super(description);
	}

	@Override
	protected IOTable transformDataResultSet(DataResultSet dataResultSet) throws OperatorException {
		IOTable ioTable;
		if (getCompatibilityLevel().isAbove(CHANGE_6_2_0_OLD_XLSX_IMPORT) && dataResultSet instanceof XlsxResultSet) {

			XlsxResultSet xlsxResultSet = (XlsxResultSet) dataResultSet;
			xlsxResultSet.setUseFirstRowAsNames(getParameterAsBoolean(PARAMETER_FIRST_ROW_AS_NAMES));

			ioTable = super.transformDataResultSet(dataResultSet);

			// Remove attributes if they are empty and no meta data is defined
			final List<String> emptyColumnNames = xlsxResultSet
					.getEmptyColumnNames(DataResultSetTranslationConfiguration.readColumnMetaData(this,
							shouldTrimAttributeNames()));
			if (!emptyColumnNames.isEmpty()) {
				List<String> remainingColumnNames = new ArrayList<>();
				for (String label : ioTable.getTable().labels()) {
					if (!emptyColumnNames.contains(label)) {
						remainingColumnNames.add(label);
					}
				}
				final Table remainingTable = ioTable.getTable().columns(remainingColumnNames);
				IOTable remainingIOTable = new IOTable(remainingTable);
				remainingIOTable.getAnnotations().putAll(ioTable.getAnnotations());
				remainingIOTable.setSource(ioTable.getSource());
				ioTable = remainingIOTable;
			}
		} else {
			ioTable = super.transformDataResultSet(dataResultSet);
		}

		return ioTable;
	}

	@Override
	protected DataResultSetFactory getDataResultSetFactory() throws OperatorException {
		return new ExcelResultSetConfiguration(this);
	}

	@Override
	protected NumberFormat getNumberFormat() {
		return null;
	}

	@Override
	protected String getFileParameterName() {
		return PARAMETER_EXCEL_FILE;
	}

	@Override
	protected String getFileExtension() {
		return XLS;
	}

	/**
	 * Returns the allowed file extensions.
	 */
	@Override
	protected String[] getFileExtensions() {
		return new String[]{XLSX, XLS};
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type = new ParameterTypeConfiguration(ExcelExampleSourceConfigurationWizardCreator.class, this);
		type.setExpert(false);
		types.add(type);

		types.add(makeFileParameterType());
		ParameterTypeCategory sheetSelection = new ParameterTypeCategory(PARAMETER_SHEET_SELECTION,
				"Select the sheet by index or by name.",
				SHEET_SELECTION_MODES, SHEET_SELECT_BY_INDEX);
		types.add(sheetSelection);

		ParameterTypeString selectBySheetName =
				new ParameterTypeString(PARAMETER_SHEET_NAME, "The name of the sheet which should be imported.", true,
						false);
		selectBySheetName.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_SHEET_SELECTION, false,
				String.valueOf(SHEET_SELECT_BY_NAME), SHEET_SELECTION_MODES[SHEET_SELECT_BY_NAME]));
		types.add(selectBySheetName);

		ParameterTypeInt selectBySheetNumber =
				new ParameterTypeInt(PARAMETER_SHEET_NUMBER, "The number of the sheet which should be imported.", 1,
						Integer.MAX_VALUE, 1, false);
		selectBySheetNumber.registerDependencyCondition(new NonEqualStringCondition(this, PARAMETER_SHEET_SELECTION,
				false, String.valueOf(SHEET_SELECT_BY_NAME), SHEET_SELECTION_MODES[SHEET_SELECT_BY_NAME]));
		types.add(selectBySheetNumber);

		types.add(new ParameterTypeString(PARAMETER_IMPORTED_CELL_RANGE,
				"Cells to import, in Excel notation, e.g. B2:D25 or B2 for an open interval.", "A1"));

		types.addAll(Encoding.getParameterTypes(this));

		types.addAll(super.getParameterTypes());

		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 3);
		changes[changes.length - 3] = CHANGE_5_0_4;
		changes[changes.length - 2] = CHANGE_5_0_11_NAME_SCHEMA;
		changes[changes.length - 1] = CHANGE_6_2_0_OLD_XLSX_IMPORT;
		return changes;
	}

	@Override
	public void configure(DataSource dataSource) throws DataSetException {
		// set sheet and cell range
		configure(dataSource, this);
	}

	/**
	 * Configures the operator with the specified data source.
	 *
	 * @param dataSource
	 * 		the data source to configure with
	 * @param operator
	 * 		the operator to configure
	 * @throws DataSetException
	 * 		if something goes wrong during configuration
	 */
	static void configure(DataSource dataSource, Operator operator) throws DataSetException {
		Map<String, String> configParameters = dataSource.getConfiguration().getParameters();

		operator.setParameter(PARAMETER_EXCEL_FILE,
				configParameters.get(ExcelResultSetConfiguration.EXCEL_FILE_LOCATION));


		String sheetSelectionMode = configParameters.get(ExcelResultSetConfiguration.EXCEL_SHEET_SELECTION_MODE);
		String sheetName = configParameters.get(ExcelResultSetConfiguration.EXCEL_SHEET_NAME);
		int sheet = Integer.parseInt(configParameters.get(ExcelResultSetConfiguration.EXCEL_SHEET));
		int columnOffset = Integer.parseInt(configParameters.get(ExcelResultSetConfiguration.EXCEL_COLUMN_OFFSET));
		int columnLast = Integer.parseInt(configParameters.get(ExcelResultSetConfiguration.EXCEL_COLUMN_LAST));
		int rowOffset = Integer.parseInt(configParameters.get(ExcelResultSetConfiguration.EXCEL_ROW_OFFSET));
		int rowLast = Integer.parseInt(configParameters.get(ExcelResultSetConfiguration.EXCEL_ROW_LAST));
		int headerRowIndex =
				Integer.parseInt(configParameters.get(ExcelResultSetConfiguration.EXCEL_HEADER_ROW_INDEX));

		if (rowOffset < 0) {
			rowOffset = 0;
		}

		String range = Tools.getExcelColumnName(columnOffset) + (rowOffset + 1);

		// only add end range to cell range parameter if user has specified it explicitly
		if (Integer.MAX_VALUE != columnLast && Integer.MAX_VALUE != rowOffset) {
			range += ":" + Tools.getExcelColumnName(columnLast) + (rowLast + 1);
		}

		operator.setParameter(PARAMETER_IMPORTED_CELL_RANGE, range);

		if (ExcelResultSetConfiguration.SheetSelectionMode.valueOf(sheetSelectionMode) ==
				ExcelResultSetConfiguration.SheetSelectionMode.BY_NAME) {
			operator.setParameter(PARAMETER_SHEET_SELECTION, SHEET_SELECTION_MODES[SHEET_SELECT_BY_NAME]);
			operator.setParameter(PARAMETER_SHEET_NAME, sheetName);
		} else {
			operator.setParameter(PARAMETER_SHEET_SELECTION, SHEET_SELECTION_MODES[SHEET_SELECT_BY_INDEX]);
			operator.setParameter(PARAMETER_SHEET_NUMBER, String.valueOf(sheet + 1));
		}

		// should be set to true when header row belongs is the first row of the selected content
		String firstRowAsNames = Boolean.toString(headerRowIndex == rowOffset);
		operator.setParameter(PARAMETER_FIRST_ROW_AS_NAMES, firstRowAsNames);

		// set meta data
		ImportWizardUtils.setMetaData(dataSource, operator);
	}

}
