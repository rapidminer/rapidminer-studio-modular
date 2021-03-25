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
package com.rapidminer.operator.io;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;


/**
 * Super class of all operators requiring no input and creating an {@link IOTable}. Adjusted copy of {@link
 * AbstractExampleSource}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public abstract class AbstractTableSource extends AbstractReader<IOTable> {

	public AbstractTableSource(final OperatorDescription description) {
		super(description, IOTable.class);
	}

	@Override
	protected ExampleSetMetaData getDefaultMetaData() {
		return new ExampleSetMetaData();
	}

	/**
	 * Creates (or reads) the IOTable that will be returned by {@link #read()}.
	 */
	public abstract IOTable createTable() throws OperatorException;

	@Override
	public IOTable read() throws OperatorException {
		return createTable();
	}

}
