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
package com.rapidminer.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


/**
 * Utility methods for manipulating input/output (Streams, ...).
 *
 * @author Marco Boeck
 * @since 9.8.0
 */
public final class IOTools {

	private static final int COPY_BUFFER_SIZE = 1024 * 20;

	/**
	 * Copies the contents read from the input stream to the output stream in the current thread. If desired, both
	 * streams will be closed, even in case of a failure.
	 *
	 * @param in                the input stream, must not be {@code null}
	 * @param out               the output stream, must not be {@code null}
	 * @param closeOutputStream if {@code true}, the streams will be closed in all cases at the end of this call; if
	 *                          {@code false} the streams will be left open
	 */
	public static void copyStreamSynchronously(InputStream in, OutputStream out, boolean closeOutputStream) throws IOException {
		byte[] buffer = new byte[1024 * 20];
		try {
			int length;
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			out.flush();
		} finally {
			if (closeOutputStream && out != null) {
				try {
					out.close();
				} catch (IOException ex) {
					// ignore
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		}
	}

	/**
	 * Writes the content stream to the fileLocation. Any conflicting files and folders inside the rootFolder are
	 * deleted.
	 *
	 * @param content      the file content stream, which is <b>not</b> closed by this method
	 * @param rootFolder   only files and folders in this path are deleted
	 * @param fileLocation the file location, must be inside the rootFolder
	 * @return the number of written bytes
	 * @throws IOException if anything goes wrong
	 * @since 9.8.0
	 */
	public static long forceWriteFile(InputStream content, Path rootFolder, Path fileLocation) throws IOException {
		rootFolder = rootFolder.normalize();
		fileLocation = rootFolder.resolve(fileLocation).normalize();
		if (!fileLocation.startsWith(rootFolder) || fileLocation.getNameCount() <= rootFolder.getNameCount()) {
			throw new IOException("fileLocation \"" + fileLocation + "\" is not inside the rootFolder \"" + rootFolder + '"');
		}
		// remove file -> folder conflicts
		if (Files.exists(rootFolder.resolve(fileLocation.getName(rootFolder.getNameCount())))) {
			for (Path p = fileLocation.getParent(); p.getNameCount() > rootFolder.getNameCount(); p = p.getParent()) {
				if (Files.exists(p) && !Files.isDirectory(p)) {
					Files.delete(p);
					// if it's a file the parent must be a folder
					break;
				}
			}
		}
		// remove folder -> file conflicts
		if (Files.isDirectory(fileLocation)) {
			FileUtils.forceDelete(fileLocation.toFile());
		}
		// create parent folders
		if (!Files.exists(fileLocation.getParent())) {
			Files.createDirectories(fileLocation.toAbsolutePath().getParent());
		}
		try (OutputStream out = Files.newOutputStream(fileLocation, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			return IOUtils.copy(content, out, IOTools.COPY_BUFFER_SIZE);
		}
	}
}
