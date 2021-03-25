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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.rapidminer.adaption.belt.AtPortConverter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ModelApplier;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AbstractPrecondition;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.InputMissingMetaDataError;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ChangeAttributeRoleQuickFix;
import com.rapidminer.operator.ports.quickfix.OperatorInsertionQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.preprocessing.IdTagging;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * Precondition for {@link IOTable}s, analog to {@link ExampleSetPrecondition}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class TablePrecondition extends AbstractPrecondition {

	private final ColumnRole[] requiredRoles;
	private final Column.Category allowedCategory;
	private final Set<String> ignoreForCategoryCheck;
	private final Column.Category allowedSpecialsCategory;
	private final List<String> requiredNames;
	private boolean optional = false;

	/**
	 * Precondition that adds an error if there is no meta data or if it is not for a table.
	 *
	 * @param inputPort
	 * 		the port to check
	 */
	public TablePrecondition(InputPort inputPort) {
		this(inputPort, null, (ColumnRole[]) null);
	}

	/**
	 * Precondition that checks the types of the regular columns and requires the given roles.
	 *
	 * @param inputPort
	 * 		the port to check
	 * @param allowedCategoryForRegulars
	 * 		the allowed category for the column types of the columns without roles. Can be {@code null} if the category is
	 * 		not restricted.
	 * @param requiredRoles
	 * 		the required column roles
	 */
	public TablePrecondition(InputPort inputPort, Column.Category allowedCategoryForRegulars,
							 ColumnRole... requiredRoles) {
		this(inputPort, Collections.emptyList(), allowedCategoryForRegulars, null, null,
				requiredRoles);
	}

	/**
	 * Precondition that checks for columns with the given required names, the types of the regular columns and the
	 * given roles.
	 *
	 * @param inputPort
	 * 		the port to check
	 * @param requiredColumnNames
	 * 		the names of required columns
	 * @param allowedCategoryForRegulars
	 * 		the allowed category for the column types of the columns without roles. Can be {@code null} if the category is
	 * 		not restricted.
	 * @param requiredRoles
	 * 		the required column roles
	 */
	public TablePrecondition(InputPort inputPort, List<String> requiredColumnNames,
							 Column.Category allowedCategoryForRegulars,
							 ColumnRole... requiredRoles) {
		this(inputPort, requiredColumnNames, allowedCategoryForRegulars, null, null,
				requiredRoles);
	}

	/**
	 * Precondition that checks that there is a column with the required role and type category.
	 *
	 * @param inputPort
	 * 		the port to check
	 * @param requiredRole
	 * 		the required role
	 * @param requiredCategoryForRole
	 * 		the required category for the column with the role. Can be {@code null} if the category is not restricted.
	 */
	public TablePrecondition(InputPort inputPort, ColumnRole requiredRole, Column.Category requiredCategoryForRole) {
		this(inputPort, Collections.emptyList(), null, null, requiredCategoryForRole,
				requiredRole);
	}

	/**
	 * Precondition that checks all the conditions defined by the parameters.
	 *
	 * @param inputPort
	 * 		the port to check
	 * @param requiredColumnNames
	 * 		the names of required columns
	 * @param allowedCategoryForRegulars
	 * 		the allowed category for the column types of the columns without roles. Can be {@code null} if the category is
	 * 		not restricted.
	 * @param ignoreForCategoryCheck
	 * 		the names of the columns that should not be checked for their type category
	 * @param allowedCategoryForSpecials
	 * 		the allowed category for the column types of the columns with roles. Can be {@code null} if the category is not
	 * 		restricted.
	 * @param requiredRoles
	 * 		the required column roles
	 */
	public TablePrecondition(InputPort inputPort, List<String> requiredColumnNames,
							 Column.Category allowedCategoryForRegulars,
							 Set<String> ignoreForCategoryCheck, Column.Category allowedCategoryForSpecials,
							 ColumnRole... requiredRoles) {
		super(inputPort);
		this.allowedCategory = allowedCategoryForRegulars;
		this.requiredRoles = requiredRoles;
		this.requiredNames = requiredColumnNames;
		this.allowedSpecialsCategory = allowedCategoryForSpecials;
		this.ignoreForCategoryCheck = ignoreForCategoryCheck;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	@Override
	public void assumeSatisfied() {
		getInputPort().receiveMD(new TableMetaData());
	}

	@Override
	public void check(MetaData metaData) {
		final InputPort inputPort = getInputPort();
		if (metaData == null) {
			if (!optional) {
				// on purpose using ExampleSet for consistent display
				inputPort.addError(new InputMissingMetaDataError(inputPort, ExampleSet.class, null));
			}
		} else {
			TableMetaData tmd = BeltConversionTools.asTableMetaDataOrNull(metaData);
			if (tmd != null) {
				checkColumnNames(tmd);
				checkTypes(tmd);
				checkRoles(inputPort, tmd);
				try {
					makeAdditionalChecks(tmd);
				} catch (UndefinedParameterError e) {
					//ignore
				}
			} else {
				inputPort.addError(new InputMissingMetaDataError(inputPort, ExampleSet.class,
						metaData.getObjectClass()));
			}
		}
	}

	/**
	 * Can be implemented by subclasses.
	 *
	 * @throws UndefinedParameterError
	 * 		if some required parameter is not set
	 */
	public void makeAdditionalChecks(TableMetaData emd) throws UndefinedParameterError {
		// does nothing by default
	}

	@Override
	public String getDescription() {
		// on purpose using ExampleSet for consistent display
		return "<em>expects:</em> ExampleSet";
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		return null != input && (IOTable.class.isAssignableFrom(input.getObjectClass()) ||
				AtPortConverter.isConvertible(input.getObjectClass(), IOTable.class));
	}

	@Override
	public MetaData getExpectedMetaData() {
		return new TableMetaData();
	}

	/**
	 * Checks the required types for regulars.
	 */
	private void checkTypes(TableMetaData tmd) {
		if (allowedCategory != null) {
			for (String label : tmd.labels()) {
				// check regulars that should not be ignored
				if (tmd.getFirstColumnMetaData(label, ColumnRole.class) == null &&
						(ignoreForCategoryCheck == null || !ignoreForCategoryCheck.contains(label))) {

					final Optional<ColumnType<?>> type = tmd.column(label).getType();
					if (type.isPresent() && allowedCategory != type.get().category()) {
						createError(Severity.ERROR, "regular_type_mismatch",
								toLegacyType(allowedCategory));
						break;
					}
				}
			}
		}
	}

	/**
	 * Checks for the required roles and adds appropriate quick fixes and errors.
	 */
	private void checkRoles(InputPort inputPort, TableMetaData tmd) {
		if (requiredRoles != null) {
			for (ColumnRole role : requiredRoles) {
				MetaDataInfo has = tmd.hasColumnMetaData(role);
				switch (has) {
					case NO:
						handleRoleNotFound(inputPort, role);
						break;
					case UNKNOWN:
						createError(Severity.WARNING, "special_unknown", BeltConverter.toStudioRole(role));
						break;
					case YES:
						// checking type
						final Set<String> strings = tmd.selectByColumnMetaData(role);
						String name = strings.iterator().next(); //handle only the case with roles appearing once
						final Optional<ColumnType<?>> type = tmd.column(name).getType();
						if (allowedSpecialsCategory != null && type.isPresent() && allowedSpecialsCategory !=
								type.get().category()) {
							createError(Severity.ERROR, "special_attribute_has_wrong_type", name,
									role, toLegacyType(allowedSpecialsCategory));
						}
						break;
				}
			}
		}
	}

	/**
	 * Handles the errors and quickfixes for the case that the required role is not there.
	 */
	private void handleRoleNotFound(InputPort inputPort, ColumnRole role) {
		List<QuickFix> fixes = new ArrayList<>();
		// ID-Tagging
		if (role == ColumnRole.ID) {
			OperatorDescription[] ods =
					OperatorService.getOperatorDescriptions(IdTagging.class);
			fixes.add(new OperatorInsertionQuickFix("insert_id_tagging",
					new Object[]{ods.length > 0 ? ods[0].getName() : ""},
					10, inputPort) {

				@Override
				public Operator createOperator() throws OperatorCreationException {
					return OperatorService.createOperator(IdTagging.class);
				}
			});
		}
		// Prediction
		if (role == ColumnRole.PREDICTION) {
			OperatorDescription[] ods =
					OperatorService.getOperatorDescriptions(ModelApplier.class);
			if (ods.length > 0) {
				fixes.add(new OperatorInsertionQuickFix("insert_model_applier",
						new Object[]{ods[0].getName()}, 10, inputPort, 1, 0) {

					@Override
					public Operator createOperator() throws OperatorCreationException {
						return OperatorService.createOperator(ModelApplier.class);
					}
				});
			}
		}

		// General Attribute Role Change
		fixes.add(new ChangeAttributeRoleQuickFix(inputPort, BeltConverter.toStudioRole(role),
				"change_attribute_role",
				role));

		if (!fixes.isEmpty()) {
			inputPort.addError(new SimpleMetaDataError(Severity.ERROR, inputPort, fixes,
					"exampleset.missing_role", BeltConverter.toStudioRole(role)));
		} else {
			createError(Severity.ERROR, "special_missing", BeltConverter.toStudioRole(role));
		}
	}

	/**
	 * Checks for required column labels and adds errors.
	 */
	private void checkColumnNames(TableMetaData tmd) {
		for (String columnName : requiredNames) {
			MetaDataInfo attInfo = tmd.contains(columnName);
			if (attInfo == MetaDataInfo.NO) {
				createError(Severity.WARNING, "missing_attribute", columnName);
			}
		}
	}

	/**
	 * Converts category to legacy type for error messages.
	 */
	static String toLegacyType(Column.Category category) {
		switch (category) {
			case NUMERIC:
				return Ontology.VALUE_TYPE_NAMES[Ontology.NUMERICAL];
			case CATEGORICAL:
				return Ontology.VALUE_TYPE_NAMES[Ontology.NOMINAL];
			case OBJECT:
			default:
				return "object";
		}
	}
}
