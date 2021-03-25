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


import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;

import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.tools.TempFileTools;


/**
 * Test resolving the documents folder and finding it using a placeholder
 *
 * @author Andreas Timm
 * @since 9.8.0
 */
public class FilesystemRepositoriesToolsTest {

	@Test
	public void testGetReusableDocumentsEscapedPath() {
		File xyzDir = FilesystemRepositoriesTools.getDefaultRepositoryFolder("Repositories", "XYZ");
		String separator = FileSystems.getDefault().getSeparator();
		// matches with any-OS .RapidMiner/repositories/XYZ and Windows C:\User\Any\Documents\RapidMiner Repositories
		Assert.assertTrue(xyzDir.getPath().endsWith("epositories" + separator + "XYZ"));
		Assert.assertEquals("%{DOCUMENTS}" + separator + "RapidMiner" + separator + "Repositories" + separator + "XYZ",
				FilesystemRepositoriesTools.getShortenedDocsPath(xyzDir.toPath()));
	}

	@Test
	public void checkConversion() throws IOException {
		File projectDir = FilesystemRepositoriesTools.getDefaultRepositoryFolder("Projects", "new project");
		String projectPath = projectDir.getAbsolutePath();
		String shortenedDocsPath = FilesystemRepositoriesTools.getShortenedDocsPath(projectDir.toPath());
		String unshortenedDocsPath = FilesystemRepositoriesTools.getUnshortenedDocsPath(shortenedDocsPath);
		Assert.assertEquals(projectPath, unshortenedDocsPath);

		File otherDir = TempFileTools.createTempFile("foo", "bar").getParent().toFile();
		String otherPath = otherDir.getAbsolutePath();
		shortenedDocsPath = FilesystemRepositoriesTools.getShortenedDocsPath(otherDir.toPath());
		unshortenedDocsPath = FilesystemRepositoriesTools.getUnshortenedDocsPath(shortenedDocsPath);
		Assert.assertEquals(otherPath, unshortenedDocsPath);
	}

}
