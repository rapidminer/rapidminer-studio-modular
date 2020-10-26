/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.versioned.datasummary;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.versioned.BasicIOCollectionEntry;
import com.rapidminer.repository.versioned.IOObjectFileTypeHandler;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.TempFileTools;
import com.rapidminer.versioning.repository.DataSummary;


/**
 * {@link DataSummarySerializer} for {@link com.rapidminer.operator.IOObjectCollection IOObjectCollections},
 *  i.e. for (de)serializing {@link CollectionMetaData}. Uses the new collection suffix
 *  {@value IOObjectFileTypeHandler#COLLECTION_SUFFIX}.
 *
 * @author Jan Czogalla
 * @since 9.8
 */
enum IOCollectionDataSummarySerializer implements DataSummarySerializer {
	INSTANCE;

	private static final String ELEMENT_REGEX = "^(\\d+)/element\\.(.+)$";
	private static final Pattern ELEMENT_PATTERN = Pattern.compile(ELEMENT_REGEX);

	private static final DataSummarySerializer DUMMY_SERIALIZER = new DataSummarySerializer() {
		@Override
		public String getSuffix() {
			return IOObjectFileTypeHandler.COLLECTION_SUFFIX;
		}

		@Override
		public Class<? extends DataSummary> getSummaryClass() {
			return DataSummary.class;
		}

		@Override
		public void serialize(Path path, DataSummary dataSummary) throws IOException {
			if (!Files.exists(path)) {
				Files.createFile(path);
			}
		}

		@Override
		public DataSummary deserialize(Path path) throws IOException {
			return new MetaData();
		}
	};

	@Override
	public String getSuffix() {
		return IOObjectFileTypeHandler.COLLECTION_SUFFIX;
	}

	@Override
	public Class<? extends DataSummary> getSummaryClass() {
		return CollectionMetaData.class;
	}

	@Override
	public boolean canHandle(DataSummary ds) {
		if (!(ds instanceof CollectionMetaData)) {
			return false;
		}
		MetaData elementMD = ((CollectionMetaData) ds).getElementMetaDataRecursive();
		return elementMD == null || elementMD.getClass() == MetaData.class && elementMD.getAnnotations().isEmpty()
				|| !DataSummarySerializerRegistry.getInstance().getCallbacks(elementMD).isEmpty();
	}


	@Override
	public void serialize(Path path, DataSummary dataSummary) throws IOException {
		if (!(dataSummary instanceof CollectionMetaData)) {
			// wrong data summary class
			return;
		}
		CollectionMetaData collectionSummary = (CollectionMetaData) dataSummary;
		MetaData elementMD = collectionSummary.getElementMetaDataRecursive();
		DataSummarySerializer serializer;
		if (elementMD == null || elementMD.getClass() == MetaData.class && elementMD.getAnnotations().isEmpty()) {
			serializer = DUMMY_SERIALIZER;
		} else {
			List<DataSummarySerializer> serializers = DataSummarySerializerRegistry.getInstance().getCallbacks(elementMD);
			if (serializers.isEmpty()) {
				// no serializer for element MD => do not serialize
				return;
			}
			serializer = serializers.get(0);
		}
		Path tempMD;
		try {
			// get temp file; should not exist to let the serializer take care of it
			tempMD = TempFileTools.createTempFile("col-ds-", ".tmpmd");
			Files.delete(tempMD);
			serializer.serialize(tempMD, elementMD);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING,
					I18N.getErrorMessage("com.rapidminer.repository.versioned.datasummary.IOCollectionDataSummarySerializer.md_write_error", path), e);
			throw e;
		}

		int depth = 0;
		MetaData md = collectionSummary;
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(path.toFile())))) {
			// write annotations recursively; prefix annotation names with current depth
			while (md instanceof CollectionMetaData) {
				if (!md.getAnnotations().isEmpty()) {
					ZipEntry annoEntry = new ZipEntry(depth + "/" + BasicIOCollectionEntry.ANNOTATIONS_FILE_NAME);
					zos.putNextEntry(annoEntry);
					zos.write(md.getAnnotations().asPropertyStyle().getBytes(StandardCharsets.UTF_8));
					zos.closeEntry();
				}
				md = ((CollectionMetaData) md).getElementMetaData();
				depth++;
			}
			// lastly, copy element metadata into zip, prefix with total depth
			ZipEntry elementEntry = new ZipEntry(depth +"/element." +  serializer.getSuffix());
			zos.putNextEntry(elementEntry);
			try (InputStream tempIS = Files.newInputStream(tempMD)) {
				IOUtils.copy(tempIS, zos);
			}
			zos.closeEntry();
		} catch (IOException e) {
			// get rid of possibly corrupted file
			FileUtils.deleteQuietly(path.toFile());
			throw e;
		}
	}

	@Override
	public DataSummary deserialize(Path path) throws IOException {
		try (ZipFile zipFile = new ZipFile(path.toFile())) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			ZipEntry elementEntry = null;
			// find actual inner metadata
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (!entry.isDirectory() && entry.getName().matches(ELEMENT_REGEX)) {
					elementEntry = entry;
					break;
				}
			}
			if (elementEntry == null) {
				// no inner metadata => no collection metadata
				return null;
			}
			String elementName = elementEntry.getName();
			Matcher matcher = ELEMENT_PATTERN.matcher(elementName);
			if (!matcher.matches()) {
				// should not happen at this point
				return null;
			}
			// extract depth and suffix from element metadata name
			int depth;
			try {
				depth = Integer.parseInt(matcher.group(1));
			} catch (NumberFormatException e) {
				return null;
			}
			String elementSuffix = matcher.group(2);
			DataSummarySerializer deserializer;
			if (elementSuffix.equals(IOObjectFileTypeHandler.COLLECTION_SUFFIX)) {
				deserializer = DUMMY_SERIALIZER;
			} else {
				deserializer = DataSummarySerializerRegistry.getInstance().getCallback(elementSuffix);
				if (deserializer == null) {
					// no deserializer for element metadata => do not deserialize
					return null;
				}
			}
			// extract element metadata and deserialize it
			Path tempMD = TempFileTools.createTempFile("col-ds-", ".tmpmd");
			try (FileOutputStream fos = new FileOutputStream(tempMD.toFile());
				 InputStream zis = zipFile.getInputStream(elementEntry);) {
				IOUtils.copy(zis, fos);
			}
			DataSummary elementSummary = deserializer.deserialize(tempMD);
			if (!(elementSummary instanceof MetaData)) {
				return null;
			}
			// wrap
			MetaData elementMD = (MetaData) elementSummary;
			CollectionMetaData result = new CollectionMetaData();
			for (int i = depth - 1; i >= 0; i--) {
				result = new CollectionMetaData(elementMD);
				ZipEntry annoEntry = zipFile.getEntry(i + "/" + BasicIOCollectionEntry.ANNOTATIONS_FILE_NAME);
				if (annoEntry != null) {
					try (InputStream annoStream = zipFile.getInputStream(annoEntry)) {
						result.getAnnotations().putAll(Annotations.fromPropertyStyle(annoStream));
					}
				}
				elementMD = result;
			}
			return result;
		}
	}
}
