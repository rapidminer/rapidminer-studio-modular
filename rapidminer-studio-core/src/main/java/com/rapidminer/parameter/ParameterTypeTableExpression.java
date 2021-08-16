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
package com.rapidminer.parameter;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.belt.expression.internal.function.process.ParameterValue;


/**
 * Copy of {@link ParameterTypeExpression} that is used for the new expression parser.
 * <p>
 * This attribute type supports the user by letting him define an expression with a user interface known from
 * calculators.
 * <p>
 * For knowing attribute names before process execution a valid meta data transformation must be performed. Otherwise
 * the user might type in the name, instead of choosing.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class ParameterTypeTableExpression extends ParameterTypeString {

	private static final long serialVersionUID = -1938925853519339382L;

	private static final String ATTRIBUTE_INPUT_PORT = "port-name";

	private static final String PARAMETER_VALUE_FUNCTION_NAME = new ParameterValue(null).getFunctionName();

	private static final String RENAMING_PATTERN = PARAMETER_VALUE_FUNCTION_NAME + " *\\( *\"(%s)\" *,.*?\\)";

	private transient InputPort inPort;

	public ParameterTypeTableExpression(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeTableExpression(final String key, String description, InputPort inPort) {
		this(key, description, inPort, false);
	}

	public ParameterTypeTableExpression(final String key, String description, InputPort inPort, boolean optional, boolean expert) {
		this(key, description, inPort, optional);
		setExpert(expert);
	}

	public ParameterTypeTableExpression(final String key, String description, final InputPort inPort, boolean optional) {
		super(key, description, optional);
		this.inPort = inPort;
		if (inPort == null) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.parameter.ParameterTypeExpression.no_input_port_provided");
		}
	}

	@Override
	public Object getDefaultValue() {
		return "";
	}

	/**
	 * Returns the input port associated with this ParameterType. This might be null!
	 */
	public InputPort getInputPort() {
		return inPort;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		super.writeDefinitionToXML(typeElement);

		typeElement.setAttribute(ATTRIBUTE_INPUT_PORT, inPort.getName());
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) {
		// replacement is done later via the grammar
		return parameterValue;
	}

	@Override
	public String substitutePredefinedMacros(String parameterValue, Operator operator) {
		// replacement is done later via the grammar
		return parameterValue;
	}

	@Override
	public String notifyOperatorRenaming(String oldName, String newName, String value) {
		if (!value.contains(PARAMETER_VALUE_FUNCTION_NAME) || !value.contains(oldName)) {
			return value;
		}
		Pattern pattern = Pattern.compile(String.format(RENAMING_PATTERN, Pattern.quote(oldName)));
		Matcher matcher = pattern.matcher(value);
		int pos = 0;
		StringBuilder newValue = new StringBuilder();
		while (matcher.find(pos)) {
			int start = matcher.start(1);
			// constant part before the first match or after previous match
			if (start > pos) {
				newValue.append(value.substring(pos, start));
			}
			newValue.append(newName);
			pos = matcher.end(1);
		}
		if (pos > 0 && pos < value.length()) {
			newValue.append(value.substring(pos));
		}
		return newValue.toString();
	}

}