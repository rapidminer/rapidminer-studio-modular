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
package com.rapidminer.repository.versioned;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.ProgressListener;


/**
 * Specific {@link JsonIOObjectEntry} subclass for {@link IOTableModel}.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public class JsonIOTableModelEntry extends JsonIOObjectEntry<IOTableModel> {

	/**
	 * Create a new instance using {@link com.rapidminer.repository.Repository#createIOObjectEntry(String, IOObject,
	 * Operator, ProgressListener)}
	 *
	 * @param name
	 * 		full filename of the file without a path: "foo.bar"
	 * @param parent
	 *        {@link BasicFolder} is required
	 * @param dataType
	 * 		class of the datatype this Entry contains
	 */
	protected JsonIOTableModelEntry(String name, BasicFolder parent, Class<IOTableModel> dataType) {
		super(name, parent, dataType);
	}
}