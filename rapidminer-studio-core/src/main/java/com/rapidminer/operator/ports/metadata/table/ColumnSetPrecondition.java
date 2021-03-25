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
package com.rapidminer.operator.ports.metadata.table;

import static com.rapidminer.operator.ports.metadata.table.TablePrecondition.toLegacyType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AbstractPrecondition;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.quickfix.ColumnSelectionQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * This precondition might be used to ensure that a number of columns is contained in the table at the given port. If
 * the columns(s) are not contained, only a warning will be given. Since this Precondition does not register errors
 * beside the warning, it might be used in addition to the {@link TablePrecondition}.
 *
 * An implementation of the {@link ColumnNameProvider} might be used to provide the names of columns unknown during
 * creation time.
 *
 * This class is a copy of {@link com.rapidminer.operator.ports.metadata.AttributeSetPrecondition} adjusted to work for
 * {@link TableMetaData}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ColumnSetPrecondition extends AbstractPrecondition {

	/**
	 * Interface for required column names that are only available later.
	 */
	public interface ColumnNameProvider {

		/**
		 * Fetches the required column names.
		 *
		 * @return the required column names
		 */
		String[] getRequiredColumnNames();
	}

	/**
	 * A {@link ColumnNameProvider} that fetches the required names from parameters of a handler.
	 */
	private static class ParameterColumnNameProvider implements ColumnNameProvider {

		private final ParameterHandler handler;
		private final String[] parameterKeys;

		public ParameterColumnNameProvider(ParameterHandler handler, String... parameterKeys) {
			this.handler = handler;
			this.parameterKeys = parameterKeys;
		}

		@Override
		public String[] getRequiredColumnNames() {
			List<String> names = new ArrayList<>();
			for (String key : parameterKeys) {
				try {
					names.add(handler.getParameterAsString(key));
				} catch (UndefinedParameterError e) {
					//ignore
				}
			}
			return names.toArray(new String[0]);
		}

		public ParameterHandler getHandler() {
			return this.handler;
		}

		public String[] getParameterKeys() {
			return this.parameterKeys;
		}
	}

	/**
	 * A {@link ColumnNameProvider} that fetches the required names from certain entries of a list parameter.
	 */
	private static class ParameterListColumnNameProvider implements ColumnNameProvider {

		private final ParameterHandler handler;
		private final String parameterKey;
		private final int entry;

		public ParameterListColumnNameProvider(ParameterHandler handler, String parameterKey, int entry) {
			this.handler = handler;
			this.parameterKey = parameterKey;
			this.entry = entry;
		}

		@Override
		public String[] getRequiredColumnNames() {
			try {
				if (handler.isParameterSet(parameterKey)) {
					List<String[]> parameterList = handler.getParameterList(parameterKey);
					String[] columnNames = new String[parameterList.size()];
					int i = 0;
					for (String[] pair : parameterList) {
						columnNames[i] = pair[entry];
						i++;
					}
					return columnNames;
				}
			} catch (UndefinedParameterError e) {
				//ignore
			}
			return new String[0];
		}

		public ParameterHandler getHandler() {
			return this.handler;
		}

		public String getParameterKey() {
			return this.parameterKey;
		}
	}

	/** i18n key for wrong attribute type */
	private static final String ATTRIBUTE_HAS_WRONG_TYPE = "attribute_has_wrong_type";

	private final String[] requiredNames;
	private final ColumnNameProvider requiredNameProvider;
	private final ColumnType<?> requiredColumnType;
	private final Column.Category requiredColumnCategory;

	/**
	 * A precondition that checks the given fixed required names.
	 */
	public ColumnSetPrecondition(InputPort inputPort, String... requiredColumnNames) {
		this(inputPort, null, requiredColumnNames);
	}

	/**
	 * A precondition that checks the given fixed required names and those coming from the name provider.
	 */
	public ColumnSetPrecondition(InputPort inputPort, ColumnNameProvider columnNameProvider,
								 String... requiredColumnNames) {
		this(inputPort, columnNameProvider, null, null, requiredColumnNames);
	}

	/**
	 * A precondition that checks the given fixed required names and those coming from the name provider. If they are
	 * found, they must be of the required column type. The name provider can be {@code null} to just check the fixed
	 * names.
	 */
	public ColumnSetPrecondition(InputPort inputPort, ColumnNameProvider columnNameProvider,
								 ColumnType<?> typeOfRequiredColumns, String... requiredColumnNames) {
		this(inputPort, columnNameProvider, typeOfRequiredColumns, null, requiredColumnNames);
	}

	/**
	 * A precondition that checks the given fixed required names and those coming from the name provider. If they are
	 * found, they must be of the required column category. The name provider can be {@code null} to just check the
	 * fixed names.
	 */
	public ColumnSetPrecondition(InputPort inputPort, ColumnNameProvider columnNameProvider,
								 Column.Category requiredColumnCategory, String... requiredColumnNames) {
		this(inputPort, columnNameProvider, null, requiredColumnCategory, requiredColumnNames);
	}

	private ColumnSetPrecondition(InputPort inputPort, ColumnNameProvider columnNameProvider,
								  ColumnType<?> typeOfRequiredColumn, Column.Category requiredColumnCategory,
								  String... requiredColumnNames) {
		super(inputPort);
		this.requiredNames = requiredColumnNames;
		this.requiredNameProvider = columnNameProvider;
		this.requiredColumnType = typeOfRequiredColumn;
		this.requiredColumnCategory = requiredColumnCategory;
	}

	@Override
	public void assumeSatisfied() {
		getInputPort().receiveMD(new TableMetaData());
	}

	@Override
	public void check(MetaData metaData) {
		final TableMetaData tmd = BeltConversionTools.asTableMetaDataOrNull(metaData);
		if (tmd != null) {
			// checking column names
			checkColumnNames(requiredNames, tmd);

			// checking provider names
			if (requiredNameProvider != null) {
				checkColumnNames(requiredNameProvider.getRequiredColumnNames(), tmd);
			}
			makeAdditionalChecks(tmd);
		}
	}


	/**
	 * Gets the quick fixes in case a {@link ParameterColumnNameProvider} is used.
	 * Can be overridden by subclasses in order to specify quickfixes for other cases.
	 *
	 * @throws UndefinedParameterError
	 * 		if the implementing subclass cannot find a parameter
	 */
	protected QuickFix getQuickFix(TableMetaData tmd) throws UndefinedParameterError {
		if (requiredNameProvider instanceof ParameterColumnNameProvider) {
			ParameterColumnNameProvider provider = (ParameterColumnNameProvider) requiredNameProvider;
			if (provider.getParameterKeys().length == 1 && provider.getRequiredColumnNames().length == 1) {
				if (requiredColumnCategory != null) {
					return new ColumnSelectionQuickFix(tmd, provider.getParameterKeys()[0], provider.getHandler(),
							provider.getRequiredColumnNames()[0], requiredColumnCategory);
				} else {
					return new ColumnSelectionQuickFix(tmd, provider.getParameterKeys()[0], provider.getHandler(),
							provider.getRequiredColumnNames()[0], requiredColumnType);
				}
			}
		}
		return null;
	}

	/**
	 * Can be overridden by subclasses to make additional checks.
	 *
	 * @param tmd
	 * 		the meta data to check
	 */
	protected void makeAdditionalChecks(TableMetaData tmd) {
		//does nothing by default
	}

	@Override
	public String getDescription() {
		return "<em>expects:</em> ExampleSet";
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		return IOTable.class.isAssignableFrom(input.getObjectClass())
				|| AtPortConverter.isConvertible(input.getObjectClass(), IOTable.class);
	}

	@Override
	public MetaData getExpectedMetaData() {
		return new TableMetaData();
	}

	/**
	 * Creates a column name provider from a parameter handler and one or more parameter keys.
	 *
	 * @param handler
	 * 		the handler to ask for parameter values
	 * @param parameterKeys
	 * 		the parameter keys to use
	 * @return a column name provider to use in the constructor of {@link ColumnSetPrecondition}
	 */
	public static ColumnNameProvider getColumnsByParameter(ParameterHandler handler, String... parameterKeys) {
		return new ParameterColumnNameProvider(handler, parameterKeys);
	}

	/**
	 * Creates a column name provider that can extract one column of a ParameterTypeList for use them as column names.
	 *
	 * @param handler
	 * 		the handler to ask for parameter values
	 * @param parameterListKey
	 * 		the key for the list parameter
	 * @param entry
	 * 		the column index of the values to extract
	 * @return a column name provider to use in the constructor of {@link ColumnSetPrecondition}
	 */
	public static ColumnNameProvider getColumnsByParameterListEntry(ParameterHandler handler,
																	String parameterListKey,
																	int entry) {
		return new ParameterListColumnNameProvider(handler, parameterListKey, entry);
	}

	/**
	 * Checks if the required names are present and if defined of right type/category.
	 */
	private void checkColumnNames(String[] requiredNames, TableMetaData tmd) {
		for (String columnName : requiredNames) {
			if (columnName != null && columnName.length() > 0 && !columnName.contains("%{")) {
				MetaDataInfo attInfo = tmd.contains(columnName);
				if (attInfo == MetaDataInfo.YES) {
					handleExisting(tmd, columnName);
				} else {
					handleNotExisting(tmd, columnName);
				}
			}
		}
	}

	/**
	 * Checks the type or category of an existing column.
	 */
	private void handleExisting(TableMetaData tmd, String columnName) {
		final ColumnInfo column = tmd.column(columnName);
		if (requiredColumnType != null && (!column.getType().isPresent() ||
				!requiredColumnType.equals(column.getType().orElse(null)))) {
			handleWrongType(tmd, columnName, column);
		} else if (requiredColumnCategory != null && (!column.getType().isPresent() ||
				requiredColumnCategory != column.getType().get().category())) {
			handleWrongCategory(tmd, columnName, column);
		}
	}

	/**
	 * Creates errors for a wrong category.
	 */
	private void handleWrongCategory(TableMetaData tmd, String columnName, ColumnInfo column) {
		final Severity severity = column.getType().isPresent() ? Severity.ERROR : Severity.WARNING;
		QuickFix fix = getQuickFixSafe(tmd);
		if (fix != null) {
			createError(severity, Collections.singletonList(fix), ATTRIBUTE_HAS_WRONG_TYPE,
					columnName, toLegacyType(requiredColumnCategory));
		} else {
			createError(severity, ATTRIBUTE_HAS_WRONG_TYPE, columnName,
					toLegacyType(requiredColumnCategory));
		}
	}

	/**
	 * Creates errors for a wrong type.
	 */
	private void handleWrongType(TableMetaData tmd, String columnName, ColumnInfo column) {
		final Severity severity = column.getType().isPresent() ? Severity.ERROR : Severity.WARNING;
		QuickFix fix = getQuickFixSafe(tmd);
		if (fix != null) {
			createError(severity, Collections.singletonList(fix), ATTRIBUTE_HAS_WRONG_TYPE,
					columnName, requiredColumnType.id());
		} else {
			createError(severity, ATTRIBUTE_HAS_WRONG_TYPE, columnName,
					requiredColumnType.id());
		}
	}

	/**
	 * Creates errors for a not existing required column name.
	 */
	private void handleNotExisting(TableMetaData tmd, String columnName) {
		QuickFix fix = getQuickFixSafe(tmd);
		Severity sev = Severity.WARNING;
		if (tmd.getColumnSetRelation() == SetRelation.EQUAL) {
			sev = Severity.ERROR;
		}
		if (fix != null) {
			createError(sev, Collections.singletonList(fix), "missing_attribute", columnName);
		} else {
			createError(sev, "missing_attribute", columnName);
		}
	}

	/**
	 * Gets the quickfix from the overridable method and ignores possible {@link UndefinedParameterError}s.
	 */
	private QuickFix getQuickFixSafe(TableMetaData tmd) {
		QuickFix fix = null;
		try {
			fix = getQuickFix(tmd);
		} catch (UndefinedParameterError e) {
			//ignore
		}
		return fix;
	}

}
