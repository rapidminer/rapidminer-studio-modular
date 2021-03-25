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

import com.rapidminer.operator.ports.InputPort;


/**
 * This ParameterType takes an {@link InputPort}, reads out its metadata and provides a key/value list with keys being
 * attributes names and values being attribute values for these attributes. If the user chooses an attribute name then
 * the corresponding attribute values are presented to him and he can select one of those.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
public class ParameterTypeAttributeValue extends ParameterTypeSingle {

	private transient MetaDataProvider metaDataProvider;

	/**
	 * Creates a new instance of this operator using the given {@link InputPort} as a metadata provider. (See {@link
	 * #getMetaDataProvider()})
	 *
	 * @param key
	 * 		the parameter key
	 * @param description
	 * 		the operator description
	 * @param inPort
	 * 		the InputPort holding ExampleSetMetaData
	 */
	public ParameterTypeAttributeValue(String key, String description, InputPort inPort) {
		super(key, description);
		this.metaDataProvider = new ParameterTypeAttribute.InputPortMetaDataProvider(inPort);
		setOptional(false);
	}

	/**
	 * Returns null.
	 */
	@Override
	public String getRange() {
		return null;
	}

	/**
	 * Returns null.
	 */
	@Override
	public Object getDefaultValue() {
		return null;
	}

	/**
	 * Does nothing. The default value is always null.
	 */
	@Override
	public void setDefaultValue(Object defaultValue) {
		// does nothing
	}

	/**
	 * Returns false
	 */
	@Override
	public boolean isNumerical() {
		return false;
	}

	/**
	 * Returns a {@link MetaDataProvider} holding example set metadata that is used to present the attribute names and
	 * their corresponding attribute values to the user so that he can select them.
	 */
	public MetaDataProvider getMetaDataProvider() {
		return metaDataProvider;
	}

}
