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
package com.rapidminer.storage.hdf5;

import static com.rapidminer.storage.hdf5.IOTableHdf5Writer.ATTRIBUTE_LEGACY_ROLE;
import static com.rapidminer.storage.hdf5.IOTableHdf5Writer.ATTRIBUTE_LEGACY_TYPE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.rapidminer.belt.table.LegacyRole;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.tools.FunctionWithThrowable;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.container.Triple;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Registry for storing custom {@link ColumnMetaData} created in extensions.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public enum ColumnMetaDataStorageRegistry {

	;//no instance enum

	/**
	 * Interface for serialization functions used in {@link #register}. It converts the meta data to the storage class.
	 */
	public interface Hdf5Serializer extends Function<ColumnMetaData, Object> {
	}

	/**
	 * Exception to throw from a {@link Hdf5Deserializer} if deserialization cannot happen because some constraints are
	 * not satisfied.
	 */
	public static class IllegalHdf5FormatException extends Exception {

		public IllegalHdf5FormatException(String message) {
			super(message);
		}
	}

	/**
	 * Interface for deserialization functions used in {@link #register}. It converts the data of the storage class to
	 * the subclass of {@link ColumnMetaData}. If the format does not fit the constraints it can throw a {@link
	 * IllegalHdf5FormatException}.
	 */
	public interface Hdf5Deserializer extends FunctionWithThrowable<Object, ColumnMetaData,
			IllegalHdf5FormatException> {
	}

	/**
	 * The length of the identifier must be restricted as the utf8-length of it must fit into an hdf5 attribute
	 * message. The threshold could be higher, but this should be enough.
	 */
	private static final int IDENTIFIER_MAX_LENGTH = 1000;

	private static final Set<Class<?>> ALLOWED_CLASSES =
			new HashSet<>(Arrays.asList(String.class, byte.class, short.class, int.class, double.class, long.class));

	private static Map<Class<? extends ColumnMetaData>, Triple<String, Class<?>, Hdf5Serializer>>
			serializers = new ConcurrentHashMap<>();

	private static Map<String, Pair<Class<?>, Hdf5Deserializer>> deserializers =
			new ConcurrentHashMap<>();

	static {
		register(ATTRIBUTE_LEGACY_TYPE, LegacyType.class, byte.class, m -> (byte) ((LegacyType) m).ontology(),
				b -> {
					int ontologyIndex = (byte) b;
					if (ontologyIndex == 0 || ontologyIndex > Ontology.VALUE_TYPE_NAMES.length - 1) {
						throw new IllegalHdf5FormatException("Illegal legacy type with ontology " + ontologyIndex);
					}
					return LegacyType.forOntology(ontologyIndex);
				});
		register(ATTRIBUTE_LEGACY_ROLE, LegacyRole.class, String.class, m -> ((LegacyRole) m).role(),
				s -> new LegacyRole((String) s));
		register("annotation", ColumnAnnotation.class, String.class, m -> ((ColumnAnnotation) m).annotation(),
				s -> new ColumnAnnotation((String) s));
	}

	/**
	 * Registers a subclass of {@link ColumnMetaData} for storage as hdf5 attribute. The meta data can be stored as
	 * {@link String}, {@link double}, {@link int}, {@link long} or {@link byte}. If the meta data does not easily fit
	 * into those classes, serialization to json can be used.
	 *
	 * @param plugin
	 * 		the plugin that defines the {@link ColumnMetaData}, will be used to prefix the identifier with the
	 * 		extension id to make it unique and for warnings if the extension is not loaded
	 * @param identifier
	 * 		the identifier for the hdf5 attribute, max length of 500, will be automatically prefixed with the
	 * 		extension id
	 * @param metadataType
	 * 		the subclass of {@link ColumnMetaData} that should be stored
	 * @param storageClass
	 * 		the class as which it should be stored in hdf5, can be one of {@link String}, {@link double}, {@link int},
	 *        {@link long}, {@link byte}
	 * @param serializer
	 * 		function to convert the meta data subclass to the storage class
	 * @param deserializer
	 * 		function to convert the storage class back to the meta data subclass, can throw {@link
	 *        IllegalHdf5FormatException} if the data does not match the constraints
	 * @return whether registration was successful, {@code false} if the plugin-identifier combination or the metadata
	 * type has already been registered
	 * @throws IllegalArgumentException
	 * 		if any of the inputs are {@code null} or empty or the identifier is too long
	 */
	public static boolean register(Plugin plugin, String identifier, Class<? extends ColumnMetaData> metadataType,
								   Class<?> storageClass, Hdf5Serializer serializer, Hdf5Deserializer deserializer) {
		ValidationUtilV2.requireNonEmptyString(identifier, "identifier");
		return register(
				plugin.getExtensionId() + ":" + identifier, metadataType, storageClass, serializer, deserializer);
	}

	/**
	 * Registers a subclass of {@link ColumnMetaData} for storage as hdf5 attribute. The meta data can be stored as
	 * {@link String}, {@link double}, {@link int}, {@link long}, {@link short} or {@link byte}. If the meta data does
	 * not easily fit into those classes, serialization to json can be used.
	 *
	 * @param identifier
	 * 		unique identifier, should contain extension id, max length of 1000
	 * @param metadataType
	 * 		the subclass of {@link ColumnMetaData} that should be stored
	 * @param storageClass
	 * 		the class as which it should be stored in hdf5, can be one of {@link String}, {@link double}, {@link int},
	 *        {@link long}, {@link byte}, {@link short}
	 * @param serializer
	 * 		function to convert the meta data subclass to the storage class
	 * @param deserializer
	 * 		function to convert the storage class back to the meta data subclass, can throw {@link
	 *        IllegalHdf5FormatException} if the data does not match the constraints
	 * @return whether registration was successful
	 */
	private static synchronized boolean register(String identifier, Class<? extends ColumnMetaData> metadataType,
												 Class<?> storageClass,
												 Hdf5Serializer serializer,
												 Hdf5Deserializer deserializer) {
		ValidationUtilV2.requireNonNull(metadataType, "metadata type");
		ValidationUtilV2.requireNonNull(serializer, "serializer");
		ValidationUtilV2.requireNonNull(deserializer, "deserializer");
		if (!ALLOWED_CLASSES.contains(storageClass)) {
			throw new IllegalArgumentException(
					"illegal storage class " + storageClass + " for identifier " + identifier);
		}
		if (identifier.length() > IDENTIFIER_MAX_LENGTH) {
			// extension id should not succeed length 500...
			throw new IllegalArgumentException("identifier " + identifier + " too long");
		}
		if (deserializers.containsKey(identifier) || serializers.containsKey(metadataType)) {
			return false;
		}
		serializers.put(metadataType, new Triple<>(identifier, storageClass, serializer));
		deserializers.put(identifier, new Pair<>(storageClass, deserializer));
		return true;
	}

	/**
	 * Gets the serializer for the given type.
	 *
	 * @param metadataType
	 * 		the class to serialize
	 * @return the serializer
	 */
	static Triple<String, Class<?>, Hdf5Serializer> getSerializer(Class<? extends ColumnMetaData> metadataType) {
		return serializers.get(metadataType);
	}

	/**
	 * Gets the deserializer for the given hdf5 attribute identifier.
	 *
	 * @param identifier
	 * 		the identifier for the meta data to deserialize
	 * @return the deserializer
	 */
	static Pair<Class<?>, Hdf5Deserializer> getDeserializer(String identifier) {
		return deserializers.get(identifier);
	}

	/**
	 * Gets all registered identifiers.
	 *
	 * @return the identifiers
	 */
	static Set<String> getIdentifiers() {
		return deserializers.keySet();
	}
}
