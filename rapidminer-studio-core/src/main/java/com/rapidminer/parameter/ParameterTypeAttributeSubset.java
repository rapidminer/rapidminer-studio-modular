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

import org.w3c.dom.Element;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.XMLException;


/**
 * A parameter type for selecting several attributes. Same functionality as {@link ParameterTypeAttributes} but uses a
 * better separator that is less likely to be used in attribute names.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
public class ParameterTypeAttributeSubset extends ParameterTypeAttribute {

	private static final long serialVersionUID = -5451158185186447591L;

	public static final char ATTRIBUTE_SEPARATOR_CHARACTER = Parameters.RECORD_SEPARATOR;

	public ParameterTypeAttributeSubset(Element element) throws XMLException {
		super(element);
	}

	public ParameterTypeAttributeSubset(final String key, String description, InputPort inPort) {
		this(key, description, inPort, true, Ontology.ATTRIBUTE_VALUE);
	}

	public ParameterTypeAttributeSubset(final String key, String description, InputPort inPort, int... valueTypes) {
		this(key, description, inPort, true, valueTypes);
	}

	public ParameterTypeAttributeSubset(final String key, String description, InputPort inPort, boolean optional) {
		this(key, description, inPort, optional, Ontology.ATTRIBUTE_VALUE);
	}

	public ParameterTypeAttributeSubset(final String key, String description, InputPort inPort, boolean optional,
										int... valueTypes) {
		super(key, description, inPort, optional, valueTypes);
	}

	public ParameterTypeAttributeSubset(final String key, String description, MetaDataProvider metaDataProvider, boolean optional,
										int... valueTypes) {
		super(key, description, metaDataProvider, optional, valueTypes);
	}

	public ParameterTypeAttributeSubset(final String key, String description, InputPort inPort, boolean optional, boolean expert) {
		this(key, description, inPort, optional);
		setExpert(expert);
	}
}
