/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.io.FileUtils;

import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.SystemInfoUtilities;


/**
 * Tools for Filesystem based Repositories to find the operating systems default documents folder. Contains methods for
 * keeping a path relative to the documents folder.
 *
 * @author Andreas Timm
 * @since 9.8
 */
public enum FilesystemRepositoriesTools {
	;

	public static final String RAPIDMINER_FOLDER = "RapidMiner";
	public static final String SEPARATOR;
	private static final Path DOCUMENTS_DIR;
	private static final String DOCUMENTS_PLACEHOLDER = "%{DOCUMENTS}";

	static {
		switch (SystemInfoUtilities.getOperatingSystem()) {
			case WINDOWS:
				DOCUMENTS_DIR = FileSystemView.getFileSystemView().getDefaultDirectory().toPath();
				break;
			case OSX:
			case UNIX:
				DOCUMENTS_DIR = Paths.get(System.getProperty("user.home"), "Documents");
				break;
			case SOLARIS:
			case OTHER:
			default:
				File dir = FileSystemService.getUserConfigFile("repositories");
				dir.mkdir();
				DOCUMENTS_DIR = dir.toPath();
		}
		SEPARATOR = DOCUMENTS_DIR.getFileSystem().getSeparator();
	}

	/**
	 * Returns the default folder in which a repository with this alias would be stored. The directory will not be
	 * created by this method.
	 *
	 * @return the default folder for a given repository alias
	 */
	public static File getDefaultRepositoryFolder(String forAlias) {
		return new File(getDefaultRepositoryContainerFolder(), forAlias);
	}

	/**
	 * Returns the default folder in which a repository with this alias would be stored. The directory will not be
	 * created by this method.
	 *
	 * @return the default folder for a given repository alias
	 */
	public static File getDefaultRepositoryFolder(String... pathElements) {
		return FileUtils.getFile(getDefaultRepositoryContainerFolder(), pathElements);
	}

	/**
	 * When storing the repository path, it should identify the documents folder automatically to be independent of
	 * external changes to this folder. The returned String will include a placeholder instead of the system default
	 * documents folder.
	 *
	 * @param dir a path that may be a child of the users documents folder and needs to be tracked
	 * @return a String containing a placeholder if the path was a subpath of the users documents folder
	 */
	public static String getShortenedDocsPath(Path dir) {
		Path myPath = dir.toAbsolutePath();
		if (myPath.startsWith(DOCUMENTS_DIR)) {
			return DOCUMENTS_PLACEHOLDER + SEPARATOR + DOCUMENTS_DIR.relativize(myPath);
		} else {
			return myPath.toString();
		}
	}

	/**
	 * This method reverts {@link #getShortenedDocsPath(Path)} and inserts the users document folder where the
	 * placeholder was found.
	 *
	 * @param dirpath a filesystem path string with a placeholder
	 * @return a String with documents folder instead of the potential placeholder
	 */
	public static String getUnshortenedDocsPath(String dirpath) {
		if (dirpath.startsWith(DOCUMENTS_PLACEHOLDER)) {
			return dirpath.replace(DOCUMENTS_PLACEHOLDER, DOCUMENTS_DIR.toString());
		} else {
			return dirpath;
		}
	}

	/**
	 * Returns the folder which, by default, contains RM repositories, e.g. "C:\Users\Administrator\Documents\RapidMiner"
	 */
	private static File getDefaultRepositoryContainerFolder() {
		File repoDir = DOCUMENTS_DIR.resolve(RAPIDMINER_FOLDER).toFile();
		if (!repoDir.exists()) {
			repoDir.mkdir();
		}
		return repoDir;
	}

}
