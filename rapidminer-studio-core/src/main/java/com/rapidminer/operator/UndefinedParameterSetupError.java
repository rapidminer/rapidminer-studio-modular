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

import java.util.Collections;

import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;

/**
 * A setup error that indicates a parameter was not properly set/defined.
 * Comes with a {@link ParameterSettingQuickFix} for the specified key.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class UndefinedParameterSetupError extends SimpleProcessSetupError {

	private final String key;

	/**
	 * Creates an error that comes with a quickfix to set the parameter with the given key
	 *
	 * @param operator
	 * 		the operator this error refers to
	 * @param key
	 * 		the key of the parameter that is missing
	 */
	public UndefinedParameterSetupError(Operator operator, String key) {
		super(Severity.ERROR, operator.getPortOwner(),
				Collections.singletonList(new ParameterSettingQuickFix(operator, key)),
				"undefined_parameter", key.replace('_', ' '));
		this.key = key;
	}

	/** @return the offending parameter key */
	public String getKey() {
		return key;
	}
}
