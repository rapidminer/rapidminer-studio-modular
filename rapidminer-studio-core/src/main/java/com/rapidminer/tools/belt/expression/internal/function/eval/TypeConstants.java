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
package com.rapidminer.tools.belt.expression.internal.function.eval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.gui.properties.ExpressionPropertyDialog;
import com.rapidminer.tools.belt.expression.Constant;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.internal.SimpleConstant;


/**
 * Type constants that can be used as a second argument in the eval and attribute eval function.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public enum TypeConstants {

	INSTANCE;

	private static final String USED_IN_EVAL = "used in eval and attribute eval";
	private final List<Constant> constants = new ArrayList<>(5);
	private final Map<String, ExpressionType> conversionMap = new HashMap<>(5);

	TypeConstants() {
		conversionMap.put("NOMINAL", ExpressionType.STRING);
		conversionMap.put("BINOMINAL", ExpressionType.BOOLEAN);
		conversionMap.put("DATE_TIME", ExpressionType.INSTANT);
		conversionMap.put("REAL", ExpressionType.DOUBLE);
		conversionMap.put("INTEGER", ExpressionType.INTEGER);
		conversionMap.put("TIME", ExpressionType.LOCAL_TIME);
		conversionMap.put("TEXT_SET", ExpressionType.STRING_SET);
		conversionMap.put("TEXT_LIST", ExpressionType.STRING_LIST);

		for (String constantName : conversionMap.keySet()) {
			constants.add(new SimpleConstant(constantName, constantName, USED_IN_EVAL));
		}
	}

	/**
	 * Returns the key associated to the constants contained. The constants are shown in the
	 * {@link ExpressionPropertyDialog} under the category defined by
	 * "gui.dialog.function_input.key.constant_category".
	 *
	 * @return the key for the constant category
	 */
	public String getKey() {
		return "core.type_constants";
	}

	/**
	 * @return all type constants
	 */
	public List<Constant> getConstants() {
		return constants;
	}

	/**
	 * Returns the {@link ExpressionType} that is associated with the constant with the given name.
	 *
	 * @param name
	 *            the name of the constant
	 * @return the associated expression type
	 */
	public ExpressionType getTypeForName(String name) {
		return conversionMap.get(name);
	}

	/**
	 * Returns the name of the constant that is associated with the type.
	 *
	 * @param type
	 *            the expression type
	 * @return the constant name
	 */
	public String getNameForType(ExpressionType type) {
		for (Map.Entry<String, ExpressionType> entry : conversionMap.entrySet()) {
			String key = entry.getKey();
			ExpressionType value = entry.getValue();
			if (value == type) {
				return key;
			}
		}
		// cannot happen since all enum entries are present
		return null;
	}

	/**
	 * @return a string containing the names of all type constants
	 */
	public String getValidConstantsString() {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String name : conversionMap.keySet()) {
			if (first) {
				first = false;
			} else {
				builder.append(", ");
			}
			builder.append("'");
			builder.append(name);
			builder.append("'");
		}
		return builder.toString();
	}

}
