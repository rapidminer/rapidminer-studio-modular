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
package com.rapidminer.repository.versioned;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.AccessMode;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.SecurityTools;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.versioning.repository.exceptions.RepositoryFileException;
import com.rapidminer.versioning.repository.exceptions.RepositoryImmutableException;


/**
 * The superclass entry for {@link JsonStorableIOObject}s. Supports reading and writing a {@link JsonStorableIOObject} with
 * subclasses registered with the {@link JsonStorableIOObjectResolver} using an {@link ObjectMapper}. Subclasses only
 * need to implement a constructor matching {@link #JsonIOObjectEntry(String, BasicFolder, Class)}. This is necessary so
 * IOO suffixes and entry types can be distinct.
 *
 * @author Gisa Meier
 * @see JsonStorableIOObject
 * @since 9.10.0
 */
public abstract class JsonIOObjectEntry<T extends JsonStorableIOObject> extends AbstractIOObjectEntry<T> {

	/**
	 * First fields in ioobjects json.
	 */
	static final String TYPE = "type";
	static final String VERSION = "version";

	/**
	 * Reads the stopField among the first two fields and then stops via exception analog to {@link
	 * com.rapidminer.tools.io.ClassFromSerializationReader}
	 */
	private static class StoppingDeserializer extends StdDeserializer<JsonStorableIOObject> {

		private final String stopField;

		public StoppingDeserializer(String stopField) {
			super((Class<?>) null);
			this.stopField = stopField;
		}

		@Override
		public JsonStorableIOObject deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
			String fieldName = jsonParser.nextFieldName();
			String value = jsonParser.nextTextValue();
			String foundValue = null;
			if (stopField.equals(fieldName)) {
				foundValue = value;
			} else {
				fieldName = jsonParser.nextFieldName();
				value = jsonParser.nextTextValue();
				if (stopField.equals(fieldName)) {
					foundValue = value;
				}
			}
			throw new MarkerIOException(foundValue);
		}
	}

	/**
	 * Exception to hold the read value.
	 */
	private static final class MarkerIOException extends IOException {

		private final String foundValue;

		private MarkerIOException(String foundValue) {
			this.foundValue = foundValue;
		}
	}

	/**
	 * MixIn for ignoring the version field on json read.
	 */
	@JsonIgnoreProperties(VERSION)
	private static class VersionMixin {
	}

	private static final ObjectWriter WRITER;
	private static final ObjectReader READER;
	private static final ObjectReader TYPE_READER;
	private static final ObjectReader VERSION_READER;

	static {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
		objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		objectMapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true);
		WRITER = objectMapper.writer();

		objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
		objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		try {
			// check if type validator class is present
			Class.forName("com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator");
			// this forbids all @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS), Id.NAME still allowed:
			// call JsonEntryTools.forbidJsonClassType(objectMapper) by reflection to be absolutely sure not to break
			// when new jackson library is not there at runtime
			Class<?> utilityClass = Class.forName("com.rapidminer.repository.versioned.JsonEntryTools");
			Method forbidMethod = utilityClass.getMethod("forbidJsonClassType", ObjectMapper.class);
			forbidMethod.invoke(null, objectMapper);
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			// class not found -> jackson library smaller 2.10
			LogService.getRoot().warning("Could not forbid json class type info, outdated jackson library");
		}

		READER = objectMapper.setMixInResolver(new ClassIntrospector.MixInResolver() {
					@Override
					public Class<?> findMixInClassFor(Class<?> cls) {
						return VersionMixin.class;
					}

					@Override
					public ClassIntrospector.MixInResolver copy() {
						return this;
					}
				})
				// leave deprecated calls here for backwards compatibility for Radoop
				// (needed for Spark 1.x/Hive 2.2, see RM-4081)
				.reader().withType(JsonStorableIOObject.class);

		objectMapper = new ObjectMapper();
		objectMapper.disable(MapperFeature.USE_ANNOTATIONS);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(JsonStorableIOObject.class, new StoppingDeserializer(TYPE));
		objectMapper.registerModule(module);
		TYPE_READER = objectMapper.reader()
				// leave deprecated calls here for backwards compatibility for Radoop
				// (needed for Spark 1.x/Hive 2.2, see RM-4081)
				.withType(JsonStorableIOObject.class);

		objectMapper = new ObjectMapper();
		objectMapper.disable(MapperFeature.USE_ANNOTATIONS);
		module = new SimpleModule();
		module.addDeserializer(JsonStorableIOObject.class, new StoppingDeserializer(VERSION));
		objectMapper.registerModule(module);
		VERSION_READER = objectMapper.reader()
				// leave deprecated calls here for backwards compatibility for Radoop
				// (needed for Spark 1.x/Hive 2.2, see RM-4081)
				.withType(JsonStorableIOObject.class);
	}

	/**
	 * Create a new instance using {@link com.rapidminer.repository.Repository#createIOObjectEntry(String, IOObject,
	 * Operator, ProgressListener)}
	 *
	 * @param name
	 * 		full filename of the file without a path: "foo.bar"
	 * @param parent
	 *        {@link BasicFolder} is required
	 * @param dataType
	 * 		class of the datatype this Entry contains
	 */
	protected JsonIOObjectEntry(String name, BasicFolder parent, Class<T> dataType) {
		super(name, parent, dataType);
	}

	@Override
	public synchronized Class<? extends IOObject> getObjectClass() {
		if (dataClass == null && getSize() > 0) {
			Class<? extends IOObject> readDataClass = readDataClass();
			if (readDataClass != null) {
				this.dataClass = readDataClass;
			}
		}
		return dataClass;
	}

	@Override
	protected void setIOObjectData(IOObject data) throws RepositoryFileException, RepositoryImmutableException,
			RepositoryException {
		T rightData = null;
		if (data != null) {
			if (!getDataType().isInstance(data)) {
				throw new RepositoryException("Data must be of type " + getDataType().getSimpleName());
			}
			//safe, we just checked
			rightData = (T) data;
			// prevent specialized IOOs from being written to wrong format
			Class<? extends IOObjectEntry> entryClass =
					IOObjectEntryTypeRegistry.getEntryClassForIOObjectClass(data.getClass());
			if (entryClass != getClass()) {
				throw new RepositoryException("Mismatched entry type " + entryClass.getSimpleName() + " instead of "
						+ getClass().getSimpleName() + "for data of type " + data.getClass().getSimpleName() + "!");
			}
		}
		setData(rightData);
	}

	@Override
	protected T read(InputStream load) throws IOException {
		VersionNumber readVersion = readVersion();

		T data;
		try {
			data = AccessController.doPrivileged((PrivilegedExceptionAction<T>) () -> {

				try {
					return READER.readValue(load);
				} catch (JsonResolverWithRegistry.NotRegisteredException e) {
					throw new IOException(e.getClassName() + " not registered for deserialization. Missing " +
							"extension?");
				} catch (IOException e) {
					return versionCheckedException(readVersion, e);
				}

			});
		} catch (PrivilegedActionException e) {
			return SecurityTools.handlePrivilegedExceptionToIO(e);
		}

		if (data != null) {
			dataClass = data.getClass();
			if (readVersion != null && readVersion.isAbove(new VersionNumber(getVersion(dataClass)))) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.versioned.JsonIOObjectEntry.newer_version",
						new Object[] {getLocation(), readVersion});
			}
		}
		return data;
	}

	@Override
	protected void write(T data) throws IOException, RepositoryImmutableException {
		dataClass = data.getClass();

		try {
			AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {

				try (OutputStream os = getOutputStream()) {
					WRITER.withAttribute(VERSION, getVersion(dataClass)).writeValue(os, data);
				} catch (JsonMappingException e) {
					if (e.getCause() instanceof JsonResolverWithRegistry.NotRegisteredException) {
						JsonResolverWithRegistry.NotRegisteredException cause =
								(JsonResolverWithRegistry.NotRegisteredException) e.getCause();
						throw new IOException(cause.getClassName() + " not registered for serialization.");
					} else {
						throw e;
					}
				}

				return null;
			});
		} catch (PrivilegedActionException e) {
			SecurityTools.handlePrivilegedExceptionToIO(e);
		}
	}

	/**
	 * Reads the version number from the file using the {@link #VERSION_READER}.
	 */
	private VersionNumber readVersion() {
		Path path = this.getRepositoryAdapter().getRealPath(this, AccessMode.READ);
		String versionString = AccessController.doPrivileged((PrivilegedAction<String>) () -> {

			try {
				VERSION_READER.readValue(path.toFile());
			} catch (MarkerIOException e) {
				return e.foundValue;
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Could not read from path " + path, e);
			}
			return null;

		});
		if (versionString != null) {
			try {
				return new VersionNumber(versionString);
			} catch (VersionNumber.VersionNumberException e) {
				LogService.getRoot().log(Level.WARNING, "Invalid version number for " + getLocation(), e);
			}
		}
		return null;
	}

	/**
	 * Reads the data class from the file using the {@link #TYPE_READER}.
	 */
	private Class<? extends IOObject> readDataClass() {
		Path path = this.getRepositoryAdapter().getRealPath(this, AccessMode.READ);

		return AccessController.doPrivileged((PrivilegedAction<Class<? extends IOObject>>) () -> {

			try {
				TYPE_READER.readValue(path.toFile());
			} catch (MarkerIOException e) {
				if (e.foundValue != null) {
					try {
						Class<?> typeClass =
								Class.forName(e.foundValue, false, Plugin.getMajorClassLoader());
						if (getDataType().isAssignableFrom(typeClass)) {
							return (Class<? extends IOObject>) typeClass;
						}
					} catch (ClassNotFoundException ce) {
						LogService.getRoot().log(Level.WARNING, "Could not find class for " + e.foundValue);
					}
				}
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Could not read from path " + path, e);
			}
			return null;

		});
	}

	/**
	 * Checks whether the cause of the failure might be a newer version and adjusts the exception accordingly.
	 */
	private T versionCheckedException(VersionNumber readVersion, IOException e) throws IOException {
		Class<? extends IOObject> dataClass = readDataClass();
		if (dataClass != null && readVersion != null && readVersion.isAbove(new VersionNumber(getVersion(dataClass)))) {
			throw new IOException("Failed to read " + dataClass + " written with newer version " +
					readVersion.getShortLongVersion() + ": " + e.getLocalizedMessage(), e);
		}
		throw e;
	}

	/**
	 * Gets the version associated with the dataClass, either the version of its plugin or the studio version.
	 */
	private static String getVersion(Class<? extends IOObject> dataClass) {
		Plugin pluginForClass = Plugin.getPluginForClass(dataClass);
		String version;
		if (pluginForClass == null) {
			version = RapidMiner.getVersion().getShortLongVersion();
		} else {
			version = pluginForClass.getVersion();
		}
		return version;
	}
}