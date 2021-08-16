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
package com.rapidminer.operator;

import java.util.Arrays;

import com.rapidminer.tools.I18N;


/**
 * The possible capabilities for operators. Some of the capabilities are only for learners.
 * Used by {@link TableCapabilityProvider}.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public enum TableCapability {

	//general capabilities
	/**
	 * Nominal columns, either boolean or not, supported
	 */
	NOMINAL_COLUMNS("nominal_columns", false),

	/**
	 * Nominal columns with two values supported
	 */
	TWO_CLASS_COLUMNS("two_class_columns", false),

	/**
	 * Real and integer columns supported
	 */
	NUMERIC_COLUMNS("numeric_columns", false),

	/**
	 * date-time columns (not numeric-readable) supported
	 */
	DATE_TIME_COLUMNS("date_time_columns", false),

	/**
	 * time columns (numeric readable) supported
	 */
	TIME_COLUMNS("time_columns", false),

	/**
	 * Special columns that are not the ones above, e.g. text, text-set,..., supported
	 */
	ADVANCED_COLUMNS("advanced_columns", false),

	/**
	 * Columns with missing values supported
	 */
	MISSING_VALUES("missing_values", false),


	//learner capabilities

	/**
	 * All nominal labels allowed, in particular also boolean label
	 */
	NOMINAL_LABEL("nominal_label", true),

	/**
	 * Real and integer label allowed
	 */
	NUMERIC_LABEL("numeric_label", true),

	/**
	 * Nominal label with only one dictionary value allowed
	 */
	ONE_CLASS_LABEL("one_class_label", true),

	/**
	 * Nominal label with exactly two dictionary value allowed
	 */
	TWO_CLASS_LABEL("two_class_label", true),

	/**
	 * No label column required
	 */
	NO_LABEL("unlabeled", true),

	/**
	 * multiple label columns allowed
	 */
	MULTIPLE_LABELS("multiple_labels", true),

	/**
	 * missing values in the label column(s) allowed
	 */
	MISSINGS_IN_LABEL("label_missings", true),

	//unchecked learner capabilities
	/**
	 * The model that this learner constructs is updatable
	 */
	UPDATABLE("updatable", true),

	/**
	 * The learner can use weights to improve the result
	 */
	WEIGHTED_ROWS("weighted_rows", true);

	private final String descriptionI18N;
	private final boolean onlyLearner;

	TableCapability(String descriptionI18N, boolean onlyLearner) {
		this.descriptionI18N = descriptionI18N;
		this.onlyLearner = onlyLearner;
	}

	/**
	 * The internationalized description of the value.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return I18N.getGUIMessage("gui.capability.description." + descriptionI18N);
	}

	@Override
	public String toString() {
		return getDescription();
	}

	/**
	 * Returns all operator capabilities for the given capability provider. If it is a learner, then all values are
	 * returned, otherwise only those that make sense for non-learners.
	 *
	 * @param provider
	 * 		the capability provider for which to return the values
	 * @return all enum values for the provider
	 */
	public static TableCapability[] values(TableCapabilityProvider provider) {
		return provider.isLearner() ? values() : nonLeanerValues();
	}

	/**
	 * Returns the enum values without the onlyLearner property.
	 */
	private static TableCapability[] nonLeanerValues() {
		return Arrays.stream(TableCapability.values()).filter(p -> !p.onlyLearner).toArray(TableCapability[]::new);
	}
}
