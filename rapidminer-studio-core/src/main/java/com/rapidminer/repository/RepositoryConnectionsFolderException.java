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
 * Exception in case the Connections Folder may not be created on an AI Hub because it is too old
 *
 * @author Andreas Timm
 * @since 9.5.0
 */
public class RepositoryConnectionsFolderException extends RepositoryException {

    /**
     * Create a new {@link RepositoryConnectionsFolderException} which is a {@link RepositoryException} but can be separated from that
     *
     * @param messageConnectionFolderError the error to be displayed to the user
     */
    public RepositoryConnectionsFolderException(String messageConnectionFolderError) {
        super(messageConnectionFolderError);
    }
}
