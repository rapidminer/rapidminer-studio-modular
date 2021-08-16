/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator.ports.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.TableCapability;
import com.rapidminer.operator.TableCapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TablePrecondition;
import com.rapidminer.operator.ports.quickfix.OperatorInsertionQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFixSupplier;
import com.rapidminer.operator.preprocessing.discretization.AbstractDiscretizationOperator;
import com.rapidminer.operator.preprocessing.filter.Date2Numerical;
import com.rapidminer.operator.preprocessing.filter.MissingValueReplenishment;
import com.rapidminer.operator.preprocessing.filter.NominalToBinominal;
import com.rapidminer.operator.preprocessing.filter.NominalToNumeric;
import com.rapidminer.operator.preprocessing.filter.attributes.RegexpAttributeFilter;
import com.rapidminer.operator.preprocessing.filter.attributes.SingleAttributeFilter;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.belt.BeltMetaDataTools;


/**
 * A precondition for {@link InputPort}s that ensures that the capabilities given by an operator are matched by the
 * delivered table. Also provides a number of quickfixes.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public class TableCapabilityPrecondition extends TablePrecondition {

	protected final TableCapabilityProvider capabilityProvider;

	public TableCapabilityPrecondition(TableCapabilityProvider capabilityProvider, InputPort inputPort) {
		super(inputPort);
		this.capabilityProvider = capabilityProvider;
	}

	@Override
	public void makeAdditionalChecks(TableMetaData metaData) {
		Set<TableCapability> unsupported = capabilityProvider.unsupported();
		Set<TableCapability> supported = capabilityProvider.supported();
		if (unsupported == null) {
			unsupported = Collections.emptySet();
		}
		if (supported == null) {
			supported = Collections.emptySet();
		}

		checkRegulars(metaData, unsupported, supported);

		if (capabilityProvider.isLearner()) {
			checkLabelPreconditions(metaData);

			// weighted rows
			if (unsupported.contains(TableCapability.WEIGHTED_ROWS) &&
					metaData.hasColumnMetaData(ColumnRole.WEIGHT) == MetaDataInfo.YES) {
				createError(Severity.WARNING, "learner_does_not_support_weights");
			}
		}

		// missing values
		checkMissingsInRegulars(metaData, unsupported);
	}

	/**
	 * Checks the label capabilities. Only called if the capability provider {@link TableCapabilityProvider#isLearner()}.
	 *
	 * @param metaData
	 * 		the metadata to check
	 */
	protected void checkLabelPreconditions(TableMetaData metaData) {

		Set<TableCapability> unsupported = capabilityProvider.unsupported();
		Set<TableCapability> supported = capabilityProvider.supported();
		if (unsupported == null) {
			unsupported = Collections.emptySet();
		}
		if (supported == null) {
			supported = Collections.emptySet();
		}

		switch (metaData.hasColumnMetaData(ColumnRole.LABEL)) {
			case UNKNOWN:
				if (unsupported.contains(TableCapability.NO_LABEL)) {
					addErrorWithSetRole(Severity.WARNING, "special_unknown");
				}
				break;
			case NO:
				if (unsupported.contains(TableCapability.NO_LABEL)) {
					addErrorWithSetRole(Severity.ERROR, "special_missing");
				}
				break;
			case YES:
				checkExistingLabels(metaData, unsupported, supported);
		}

	}




	/**
	 * This method has to return a collection of quick fixes which are appropriate when classification is supported and
	 * the data needs regression. The default implementation will return fixes for discretization.
	 */
	protected List<QuickFix> getFixesForRegressionWhenClassificationSupported(String labelMD) {
		return AbstractDiscretizationOperator.createDiscretizationFixes(getInputPort(), labelMD);
	}

	/**
	 * This method has to return a collection of quick fixes which are appropriate when regression
	 * is supported and the data needs classification.
	 */
	protected Collection<QuickFix> getFixesForClassificationWhenRegressionSupported() {
		return Collections.emptyList();
	}

	/**
	 * This has to return a list of appropriate quick fixes in the case, that only binominal labels are supported but
	 * the data contains polynomials.
	 */
	protected Collection<QuickFix> getFixesForPolynomialClassificationWhen2ClassSupported() {
		return Collections.emptyList();
	}

	/**
	 * Creates a quickfix to convert to nominal.
	 *
	 * @param labelName
	 * 		If null, regular attributes will be converted. Otherwise the special attribute with the given name will be
	 * 		converted.
	 */
	protected QuickFix createTo2ClassFix(final String labelName) {
		return new OperatorInsertionQuickFix(
				"insert_nominal_to_binominal_" + (labelName != null ? "label" : "attributes"),
				new Object[0], 10, getInputPort()) {

			@Override
			public Operator createOperator() throws OperatorCreationException {
				Operator op = OperatorService.createOperator(NominalToBinominal.class);
				if (labelName != null) {
					op.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
							AttributeSubsetSelector.CONDITION_NAMES[AttributeSubsetSelector.CONDITION_SINGLE]);
					op.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
					op.setParameter(SingleAttributeFilter.PARAMETER_ATTRIBUTE, labelName);
				}
				return op;
			}
		};
	}

	/**
	 * Creates a quickfix to convert to numerical.
	 *
	 * @param labelName
	 *            If null, regular attributes will be converted. Otherwise the special attribute
	 *            with the given name will be converted.
	 */
	protected QuickFix createToNumericalFix(final String labelName) {
		return new OperatorInsertionQuickFix(
				"insert_nominal_to_numerical_" + (labelName != null ? "label" : "attributes"),
				new Object[0], 10, getInputPort()) {

			@Override
			public Operator createOperator() throws OperatorCreationException {
				Operator op = OperatorService.createOperator(NominalToNumeric.class);
				if (labelName != null) {
					op.setParameter(AttributeSubsetSelector.PARAMETER_FILTER_TYPE,
							AttributeSubsetSelector.CONDITION_NAMES[AttributeSubsetSelector.CONDITION_REGULAR_EXPRESSION]);
					op.setParameter(AttributeSubsetSelector.PARAMETER_INCLUDE_SPECIAL_ATTRIBUTES, "true");
					op.setParameter(RegexpAttributeFilter.PARAMETER_REGULAR_EXPRESSION, labelName);
				}
				return op;
			}
		};
	}

	/**
	 * Creates a quickfix to convert to numerical.
	 *
	 * @param labelName
	 *            If null, regular attributes will be converted. Otherwise the special attribute
	 *            with the given name will be converted.
	 */
	protected QuickFix createDateToNumericalFix(final String labelName) {
		return new OperatorInsertionQuickFix(
				"insert_nominal_to_numerical_" + (labelName != null ? "label" : "attributes"),
				new Object[0], 10, getInputPort()) {

			@Override
			public Operator createOperator() throws OperatorCreationException {
				Operator op = OperatorService.createOperator(Date2Numerical.class);
				if (labelName != null) {
					op.setParameter(Date2Numerical.PARAMETER_ATTRIBUTE_NAME, labelName);
				}
				return op;
			}
		};
	}

	/**
	 * Creates a quickfix to convert to numerical.
	 *
	 * @param labelName
	 *            If null, regular attributes will be converted. Otherwise the special attribute
	 *            with the given name will be converted.
	 */
	protected QuickFix createTimeToNumericalFix(final String labelName) {
		return new OperatorInsertionQuickFix(
				"insert_nominal_to_numerical_" + (labelName != null ? "label" : "attributes"),
				new Object[0], 10, getInputPort()) {

			@Override
			public Operator createOperator() throws OperatorCreationException {
				Operator op = OperatorService.createOperator(Date2Numerical.class);
				if (labelName != null) {
					op.setParameter(Date2Numerical.PARAMETER_ATTRIBUTE_NAME, labelName);
				}
				return op;
			}
		};
	}

	/**
	 * Creates an error at the port.
	 *
	 * @param description
	 * 		the capability description
	 * @param list
	 * 		the quickfix list
	 * @param id
	 * 		the i18n key
	 */
	protected void createOperatorError(String description, List<? extends QuickFix> list, String id) {
		createError(Severity.ERROR, list, "learner_cannot_handle", id, description);
	}

	/**
	 * Creates error that defaults to "Operator" if operator name not there.
	 */
	private void createOperatorError(String description, List<? extends QuickFix> list) {
		String id = "Operator";
		if (capabilityProvider instanceof Operator) {
			id = ((Operator) capabilityProvider).getOperatorDescription().getName();
		}
		createOperatorError(description, list, id);
	}

	/**
	 * Creates error that defaults to "Learner" if operator name not there.
	 */
	private void createLearnerError(String description, List<? extends QuickFix> list) {
		String id = "Learner";
		if (capabilityProvider instanceof Operator) {
			id = ((Operator) capabilityProvider).getOperatorDescription().getName();
		}
		createOperatorError(description, list, id);
	}

	/**
	 * Checks that the types of the regular columns match the compatibilities.
	 */
	private void checkRegulars(TableMetaData metaData, Set<TableCapability> unsupported,
							   Set<TableCapability> supported) {
		checkNominalRegulars(metaData, unsupported, supported);
		checkNumericRegulars(metaData, unsupported, supported);
		//date-time
		if (unsupported.contains(TableCapability.DATE_TIME_COLUMNS)
				&& (metaData.containsType(ColumnType.DATETIME, false) == MetaDataInfo.YES)) {
			List<QuickFix> fixes = new ArrayList<>(1);
			if (supported.contains(TableCapability.NUMERIC_COLUMNS)) {
				fixes.add(createDateToNumericalFix(null));
			}
			createOperatorError(TableCapability.DATE_TIME_COLUMNS.getDescription(), fixes);
		}
		//time
		if (unsupported.contains(TableCapability.TIME_COLUMNS) &&
				(metaData.containsType(ColumnType.TIME, false) == MetaDataInfo.YES)) {
			List<QuickFix> fixes = new ArrayList<>(1);
			if (supported.contains(TableCapability.NUMERIC_COLUMNS)) {
				fixes.add(createTimeToNumericalFix(null));
			}
			createOperatorError(TableCapability.TIME_COLUMNS.getDescription(), fixes);
		}
		//advanced
		if (unsupported.contains(TableCapability.ADVANCED_COLUMNS) &&
				hasRegularAdvanced(metaData)) {
			createOperatorError(TableCapability.ADVANCED_COLUMNS.getDescription(), Collections.emptyList());
		}
	}

	/**
	 * Checks for missing values in regular columns if that is unsupported.
	 */
	private void checkMissingsInRegulars(TableMetaData metaData, Set<TableCapability> unsupported) {
		if (unsupported.contains(TableCapability.MISSING_VALUES)) {
			for (String label : metaData.labels()) {

				ColumnMetaData firstColumnMetaData = metaData.getFirstColumnMetaData(label, ColumnRole.class);
				if (firstColumnMetaData == null && metaData.column(label).hasMissingValues() == MetaDataInfo.YES) {
					createOperatorError(TableCapability.MISSING_VALUES.getDescription(),
							Collections.singletonList(new OperatorInsertionQuickFix(
									"insert_missing_value_replenishment", new String[0], 1, getInputPort()) {

								@Override
								public Operator createOperator() throws OperatorCreationException {
									return OperatorService.createOperator(MissingValueReplenishment.class);
								}
							}));
					break;
				}

			}
		}
	}

	/**
	 * Checks for regular numeric columns if they are unsupported and adds discretization quickfix if nominal are
	 * supported.
	 */
	private void checkNumericRegulars(TableMetaData metaData, Set<TableCapability> unsupported,
									  Set<TableCapability> supported) {
		if (unsupported.contains(TableCapability.NUMERIC_COLUMNS)
				&& (metaData.containsType(ColumnType.REAL, false) == MetaDataInfo.YES ||
				metaData.containsType(ColumnType.INTEGER_53_BIT, false) == MetaDataInfo.YES)) {
			List<QuickFix> fixes = new ArrayList<>(1);
			if (supported.contains(TableCapability.NOMINAL_COLUMNS)) {
				fixes.addAll(AbstractDiscretizationOperator.createDiscretizationFixes(getInputPort(), null));
			}
			createOperatorError(TableCapability.NUMERIC_COLUMNS.getDescription(), fixes);
		}
	}

	/**
	 * Checks for regular nominal columns if they are unsupported and adds appropriate quickfixes.
	 */
	private void checkNominalRegulars(TableMetaData metaData, Set<TableCapability> unsupported,
									  Set<TableCapability> supported) {
		if (unsupported.contains(TableCapability.NOMINAL_COLUMNS) &&
				metaData.containsType(ColumnType.NOMINAL, false) == MetaDataInfo.YES) {
			// has nominal but nominal not supported
			boolean hasRegularNonTwoClass = hasRegularNonTwoClass(metaData);
			if (unsupported.contains(TableCapability.TWO_CLASS_COLUMNS)) {
				// neither nominal nor 2-class are supported
				createErrorForBothNotSupported(supported, hasRegularNonTwoClass);
			} else if (hasRegularNonTwoClass) {
				// 2-class supported but there are non-2-class
				List<QuickFix> fixes = new ArrayList<>();
				if (supported.contains(TableCapability.NUMERIC_COLUMNS)) {
					fixes.add(createToNumericalFix(null));
				}
				if (supported.contains(TableCapability.TWO_CLASS_COLUMNS)) {
					fixes.add(createTo2ClassFix(null));
				}
				createOperatorError(TableCapability.NOMINAL_COLUMNS.getDescription(), fixes);
			}
		}

	}

	/**
	 * Creates the errors and quickfixes if nominal and 2-class regular columns are unsupported.
	 */
	private void createErrorForBothNotSupported(Set<TableCapability> supported, boolean hasRegularNonTwoClass) {
		List<QuickFix> fixes = new ArrayList<>();
		if (supported.contains(TableCapability.NUMERIC_COLUMNS)) {
			fixes.add(createToNumericalFix(null));
		}
		if (!hasRegularNonTwoClass) {
			// all are 2-class but 2-class is not supported and neither is nominal
			createOperatorError(TableCapability.TWO_CLASS_COLUMNS.getDescription(), fixes);
		} else {
			// some are not 2-class
			createOperatorError(TableCapability.NOMINAL_COLUMNS.getDescription(), fixes);
		}
	}

	/**
	 * Finds all regular columns with dictionary with more than 2 values.
	 */
	private boolean hasRegularNonTwoClass(TableMetaData metaData) {
		return metaData.labels().stream()
				.anyMatch(l -> !BeltMetaDataTools.isSpecial(metaData, l)
						&& metaData.column(l).getDictionary().getValueSet().size() > 2);
	}

	/**
	 * Checks for regular advanced columns in the metaData.
	 */
	private boolean hasRegularAdvanced(TableMetaData metaData) {
		return metaData.labels().stream().anyMatch(l -> !BeltMetaDataTools.isSpecial(metaData, l) &&
				!BeltConverter.STANDARD_TYPES.contains(metaData.column(l).getType().orElse(ColumnType.NOMINAL).id()));
	}

	/**
	 * Checks the case that there are label columns in the meta data.
	 */
	private void checkExistingLabels(TableMetaData metaData, Set<TableCapability> unsupported,
									 Set<TableCapability> supported) {
		Set<String> labels = metaData.selectByColumnMetaData(ColumnRole.LABEL);
		if (labels.size() > 1 &&
				unsupported.contains(TableCapability.MULTIPLE_LABELS)) {
			createLearnerError(TableCapability.MULTIPLE_LABELS.getDescription(),
					Collections.singletonList(
							QuickFixSupplier.getSetRoleQuickFix(getInputPort(), "regular",
									"change_attribute_role", "regular")));
		}
		for (String label : labels) {
			ColumnInfo labelInfo = metaData.column(label);
			if (labelInfo.getType().isPresent()) {
				ColumnType<?> columnType = labelInfo.getType().get();
				if (columnType.id() == Column.TypeId.NOMINAL) {
					checkNominalLabel(unsupported, supported, label, labelInfo);

				} else if (columnType.category() == Column.Category.NUMERIC) {
					if (unsupported.contains(TableCapability.NUMERIC_LABEL)) {
						createLearnerError(TableCapability.NUMERIC_LABEL.getDescription(),
								getFixesForRegressionWhenClassificationSupported(label));
					}
				} else {
					createLearnerError(I18N.getGUIMessage("gui.non_capability.description.label_type",
							columnType.id().toString().toLowerCase(Locale.ROOT)), Collections.emptyList());
				}

			}
			checkLabelMissings(unsupported, labelInfo);
		}
	}

	/**
	 * Checks the nominal label.
	 */
	private void checkNominalLabel(Set<TableCapability> unsupported, Set<TableCapability> supported,
								   String label, ColumnInfo labelInfo) {

		if (supported.contains(TableCapability.ONE_CLASS_LABEL)
				&& unsupported.contains(TableCapability.TWO_CLASS_LABEL)
				&& unsupported.contains(TableCapability.NOMINAL_LABEL)) {
			checkOnlyOneClassLabel(labelInfo);
		} else {
			boolean isOneClassLabel = labelInfo.getDictionary().getValueSet().size() == 1 &&
					labelInfo.getValueSetRelation() == SetRelation.EQUAL;
			if (isOneClassLabel) {
				if (unsupported.contains(TableCapability.ONE_CLASS_LABEL)) {
					createError(Severity.ERROR, "no_polynomial_label");
				}
			} else {
				checkNominalVs2ClassLabel(unsupported, supported, label, labelInfo);
			}
		}
	}

	/**
	 * Checks the nominal label in case it is not 1-class.
	 */
	private void checkNominalVs2ClassLabel(Set<TableCapability> unsupported, Set<TableCapability> supported,
										   String label, ColumnInfo labelInfo) {
		List<QuickFix> fixes = new ArrayList<>();
		if (supported.contains(TableCapability.NUMERIC_LABEL)) {
			fixes.addAll(getFixesForClassificationWhenRegressionSupported());
		}

		// if two or more classes are present
		if (labelInfo.getValueSetRelation() == SetRelation.EQUAL
				&& labelInfo.getDictionary().getValueSet().size() == 2) {
			if (unsupported.contains(TableCapability.TWO_CLASS_LABEL)
					&& unsupported.contains(TableCapability.NOMINAL_LABEL)) {
				createLearnerError(TableCapability.TWO_CLASS_LABEL.getDescription(), fixes);
			}

		} else if (unsupported.contains(TableCapability.NOMINAL_LABEL)) {
			createNominalLabelError(supported, label, fixes);
		}
	}

	/**
	 * Creates errors and fixes in case the nominal label is not 2-class.
	 */
	private void createNominalLabelError(Set<TableCapability> supported, String label, List<QuickFix> fixes) {
		if (supported.contains(TableCapability.TWO_CLASS_LABEL)) {
			fixes.add(createTo2ClassFix(label));
			fixes.addAll(getFixesForPolynomialClassificationWhen2ClassSupported());
		}
		if (supported.contains(TableCapability.NUMERIC_LABEL)) {
			fixes.add(createToNumericalFix(label));
		}
		createLearnerError(TableCapability.NOMINAL_LABEL.getDescription(),
				fixes);
	}

	/**
	 * Adds an error if the label is not 1-class.
	 */
	private void checkOnlyOneClassLabel(ColumnInfo labelInfo) {
		// if it only supports one class label
		if (labelInfo.getDictionary().getValueSet().size() > 1 &&
				labelInfo.getValueSetRelation() != SetRelation.UNKNOWN) {
			createError(Severity.ERROR, "one_class_label_invalid",
					labelInfo.getValueSetRelation()
							.toString() + " " +
							labelInfo.getDictionary().getValueSet().size());
		}
	}

	/**
	 * Checks if the label column contains missings and this is unsupported.
	 */
	private void checkLabelMissings(Set<TableCapability> unsupported, ColumnInfo labelInfo) {
		if (unsupported.contains(TableCapability.MISSINGS_IN_LABEL) &&
				labelInfo.hasMissingValues() == MetaDataInfo.YES) {
			createLearnerError(TableCapability.MISSINGS_IN_LABEL.getDescription(),
					Collections.singletonList(new OperatorInsertionQuickFix(
							"insert_missing_value_replenishment", new String[0], 1, getInputPort()) {

						@Override
						public Operator createOperator() throws OperatorCreationException {
							return OperatorService.createOperator(MissingValueReplenishment.class);
						}
					}));
		}
	}

	/**
	 * Adds an error with set role quickfix.
	 */
	private void addErrorWithSetRole(Severity warning, String key) {
		getInputPort().addError(
				new SimpleMetaDataError(warning, getInputPort(), Collections.singletonList(
						QuickFixSupplier.getSetRoleQuickFix(getInputPort(), Attributes.LABEL_NAME,
								"change_attribute_role", Attributes.LABEL_NAME)),
						key, Attributes.LABEL_NAME));
	}
}