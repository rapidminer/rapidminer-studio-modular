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
package com.rapidminer.operator.nio.model;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.source.DataSource;


/**
 * Super interface for {@link AbstractDataResultSetReader} and {@link AbstractDataResultTableReader} to use in the data
 * import wizard.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public interface ConfigurationSupporter {

	/**
	 * Configures the operator with the specified {@link DataSource}. Will throw {@link UnsupportedOperationException}
	 * unless overwritten by subclasses.
	 *
	 * @param dataSource
	 * 		the datasource
	 * @throws DataSetException
	 * 		if something goes wrong during configuration
	 */
	void configure(DataSource dataSource) throws DataSetException;

	/**
	 * Returns the name for error messages.
	 *
	 * @return the name to use in error messages
	 */
	String getName();
}
