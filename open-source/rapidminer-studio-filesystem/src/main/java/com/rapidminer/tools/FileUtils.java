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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;


/**
 * Utils to work with filenames and suffixes
 *
 * @author Andreas Timm
 * @since 9.8.0
 */
public enum FileUtils {
	;

	/**
	 * Get the suffix from a filename, will return "bar" for "foo.bar" and "gitignore" for ".gitignore".
	 *
	 * @param name the filename, not the path, to get the suffix from, must not be null or empty
	 * @return the suffix of the file, can be empty if no suffix exists, never {@code null}. Always lower case.
	 * @since 9.8.0
	 */
	public static String getSuffixFromFilename(String name) {
		if (StringUtils.stripToNull(name) == null) {
			throw new IllegalArgumentException("filename must not be null or empty!");
		}

		return FilenameUtils.getExtension(StringUtils.stripToNull(name)).toLowerCase(Locale.ENGLISH);
	}

	/**
	 * Tries to remove a file suffix from the given filename. The name can contain path separators. If no dot is found,
	 * nothing happens. Likewise, if the dot is the last character, nothing happens.
	 *
	 * @param filename the file name
	 * @return the filename without the suffix or the original filename
	 * @since 9.8.0
	 */
	public static String removeSuffix(String filename) {
		if (filename == null) {
			return null;
		}

		int lastDotIndex = filename.lastIndexOf('.');
		if (lastDotIndex > 0 && filename.length() > lastDotIndex + 1) {
			filename = filename.substring(0, lastDotIndex);
		}

		return filename;
	}

	/**
	 * Check if the given path contains a file. Will return false if it is a folder or does not exist or access failed.
	 *
	 * @return true only if the path is an existing file
	 * @since 9.8
	 */
	public static boolean isFile(String path) {
		try {
			File file = new File(path);
			return file.exists() && file.isFile();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Extract all content from the given zip stream to the provided location.
	 *
	 * @param extractToPath the path to extract to, i.e. use as root for extraction; must not be {@code null}
	 * @param zipStream 	the zip stream to extract from; must not be {@code null}
	 * @throws IOException    if an error occurs
	 * @since 9.8
	 */
	public static void extractZipStream(Path extractToPath, ZipInputStream zipStream) throws IOException {
		extractZipStream(extractToPath, zipStream, null);
	}

	/**
	 * Extract all matching content from the given zip stream to the provided location.
	 *
	 * @param extractToPath the path to extract to, i.e. use as root for extraction; must not be {@code null}
	 * @param zipStream     the zip stream to extract from; must not be {@code null}
	 * @param checkEntry    checks whether an entry should be extracted. If {@code null}, allows all entries.
	 * @throws IOException if an error occurs
	 * @since 9.8
	 */
	public static void extractZipStream(Path extractToPath, ZipInputStream zipStream, Predicate<ZipEntry> checkEntry) throws IOException {
		if (checkEntry == null) {
			checkEntry = entry -> true;
		}
		ZipEntry entry;
		while ((entry = zipStream.getNextEntry()) != null) {
			if (!checkEntry.test(entry)) {
				continue;
			}
			String name = entry.getName();
			Path target = extractToPath.resolve(name);
			if (entry.isDirectory()) {
				if (Files.exists(target) && !Files.isDirectory(target) && !org.apache.commons.io.FileUtils.deleteQuietly(target.toFile())) {
					throw new IOException("Cannot setup directory " + target);
				}
				Files.createDirectories(target);
			} else {
				Files.createDirectories(target.getParent());
				Files.copy(zipStream, target);
			}
		}
	}

	/**
	 * Packs all files from the given location to the provided zip stream.
	 *
	 * @param pathToPack	the root of what to pack; must not be {@code null}
	 * @param zipStream		the zip stream to pack into; must not be {@code null}
	 * @throws IOException	if an error occurs
	 * @since 9.8
	 */
	public static void pack(Path pathToPack, ZipOutputStream zipStream) throws IOException {
		pack(pathToPack, zipStream, null);
	}

	/**
	 * Packs all matching files from the given location to the provided zip stream. Entries are matched with the given
	 * {@link Predicate}. A {@code null} predicate will be the same as {@code file -> true}.
	 *
	 * @param pathToPack the root of what to pack; must not be {@code null}
	 * @param zipStream  the zip stream to pack into; must not be {@code null}
	 * @param checkPath  a predicate to indicate which files are allowed to be packed. If {@code null}, allows all files.
	 * @throws IOException if an error occurs
	 * @since 9.8
	 */
	public static void pack(Path pathToPack, ZipOutputStream zipStream, Predicate<Path> checkPath) throws IOException {
		if (checkPath == null) {
			checkPath = file -> true;
		}
		Predicate<Path> finalCheckPath = checkPath;
		Files.walkFileTree(pathToPack, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (pathToPack == dir) {
					return FileVisitResult.CONTINUE;
				}
				if (!finalCheckPath.test(dir)) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				zipStream.putNextEntry(new ZipEntry(pathToZipName(pathToPack, dir) + '/'));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (finalCheckPath.test(file)) {
					zipStream.putNextEntry(new ZipEntry(pathToZipName(pathToPack, file)));
					Files.copy(file, zipStream);
					zipStream.closeEntry();
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Converts the given sub {@link Path} to the actual zip entry name, relative to the root {@link Path}.
	 *
	 * @since 9.8
	 */
	private static String pathToZipName(Path root, Path sub) {
		return root.relativize(sub).toString().replace('\\', '/');
	}
}
