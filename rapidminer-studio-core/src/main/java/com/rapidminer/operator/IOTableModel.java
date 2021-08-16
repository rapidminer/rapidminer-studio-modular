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
package com.rapidminer.operator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.repository.versioned.JsonStorableIOObject;


/**
 * The interface for all {@link GeneralModel}s operating on {@link IOTable}s. All {@link IOTableModel} are storable as
 * json and must support a json (de)serialization as well as non-abstract classes must be registered with the {@link
 * com.rapidminer.repository.versioned.JsonStorableIOObjectResolver}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
@JsonIgnoreProperties({"source"})
public interface IOTableModel extends ResultObject, GeneralModel<IOTable, IOTable>, JsonStorableIOObject {

	@Override
	@JsonIgnore
	default Class<IOTable> getInputType() {
		return IOTable.class;
	}

	@Override
	@JsonIgnore
	default Class<IOTable> getOutputType() {
		return IOTable.class;
	}
}
