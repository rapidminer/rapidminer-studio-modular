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
package com.rapidminer.tools;

import java.util.Objects;

import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.util.ParameterUtility;


/**
 * Utility class to check for different argument's validity. Instead of throwing {@link NullPointerException NullPointerExceptions}
 * like the {@link Objects} class often does, throws more comprehensible {@link IllegalArgumentException IllegalArgumentExceptions},
 * indicating the arguments name if given.
 *
 * @author Jan Czogalla
 * @since 9.3
 * @deprecated since 9.9; use {@link ValidationUtilV2} instead
 */
@Deprecated
public final class ValidationUtil extends ValidationUtilV2 {

	/** Utility class; don't instantiate*/
	private ValidationUtil() {
		throw new UnsupportedOperationException("Instantiation of utility class not allowed");
	}

	/**
	 * Checks if the given {@link ConfigurationParameter} is set correctly.
	 *
	 * @param parameter
	 * 		the parameter, never {@code null}
	 * @return {@code true} if the parameter is injected or if it has a non-null and non-empty value; {@code false}
	 * otherwise
	 * @deprecated since 9.8.0; use {@link ParameterUtility#isValueSet(ConfigurationParameter)} instead. This method will be removed soon
	 */
	@Deprecated
	public static boolean isValueSet(ConfigurationParameter parameter) {
		return ParameterUtility.isValueSet(parameter);
	}

}
