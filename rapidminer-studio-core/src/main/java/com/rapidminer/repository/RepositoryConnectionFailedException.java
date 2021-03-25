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
package com.rapidminer.repository;


/**
 * An Exception stating that the connection to a Repository failed, containing the information to be shown to the user
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class RepositoryConnectionFailedException extends RepositoryException {
	public RepositoryConnectionFailedException(String errorMessage) {
		super(errorMessage);
	}

	/**
	 * @since 9.7
	 */
	public RepositoryConnectionFailedException(Throwable cause) {
		super(cause);
	}

	/**
	 * @since 9.7
	 */
	public RepositoryConnectionFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
