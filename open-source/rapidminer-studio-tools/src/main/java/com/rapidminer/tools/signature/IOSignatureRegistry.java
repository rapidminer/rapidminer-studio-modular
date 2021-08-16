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
package com.rapidminer.tools.signature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.gui.tools.VersionNumber.VersionNumberException;
import com.rapidminer.settings.Settings;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.SecurityTools;
import com.rapidminer.tools.ValidationUtilV2;

/**
 * An abstract registry for {@link IOHolderProviderInfo}/{@link IOSignature IOSignatures}. Signatures can be looked up
 * by using the provider key and holder key, separated by a semicolon, and also specifying the particular signature to
 * be looked up.
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public abstract class IOSignatureRegistry {

	public static final String CORE_PROVIDER = "core";
	public static final String PARAMETER_KEEP_CACHE = "rapidminer.system.file_cache.operator_signature.keep";

	protected static final String[] KEEP_CACHE_STRATEGIES = {"all", "production only"};

	private static final ObjectWriter SIGNATURE_WRITER;
	private static final ObjectReader SIGNATURE_READER;
	static {
		ObjectMapper mapper = new ObjectMapper()
				.setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
				.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE)
				.setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
				.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);
		SIGNATURE_WRITER = mapper.writerWithDefaultPrettyPrinter()
				.withType(mapper.getTypeFactory().constructCollectionType(Collection.class, IOSignature.class));
		SIGNATURE_READER = mapper.reader()
				.withType(mapper.getTypeFactory().constructCollectionType(List.class, IOSignature.class));
	}

	protected Map<String, IOHolderProviderInfo> signatures = new TreeMap<>();
	protected Class<?> defaultClass = Object.class;
	protected IOType defaultIOType;

	/**
	 * Creates a registry with the given default class.
	 */
	public IOSignatureRegistry(Class<?> defaultClass) {
		this.defaultClass = defaultClass;
		this.defaultIOType = defaultClass == null ? null : new IOType(defaultClass.getName(), false, false);
	}

	/**
	 * Looks up the signature for the given key (provider:holder) and specified signature point.
	 *
	 * @param operatorKey the key to look up
	 * @param portName    the signature point to look up
	 * @param input       whether the signature point is an input or output
	 * @return the signature; if nothing specifically found, returns {@link #defaultClass defaultClass.getName()} and
	 * indicates no collection
	 */
	public IOType lookup(String operatorKey, String portName, boolean input) {
		return lookupInternal(operatorKey, -1, portName, input);
	}

	/**
	 * Looks up the signature for the given key (provider:holder) and specified signature point in a sub node.
	 *
	 * @param operatorKey  the key to look up
	 * @param subProcessID index of the sub node; must be non-negative and should be a valid index
	 * @param portName     the signature point to look up
	 * @param input        whether the signature point is an input or output
	 * @return the signature; if nothing specifically found, returns {@link #defaultClass defaultClass.getName()} and
	 * indicates no collection
	 */
	public IOType lookup(String operatorKey, int subProcessID, String portName, boolean input) {
		if (subProcessID < 0) {
			throw new IllegalArgumentException("If you want to lookup the signature on an operator instead," +
					" use the method without the sub process ID");
		}
		return lookupInternal(operatorKey, subProcessID, portName, input);
	}

	/**
	 * Looks up all signatures for the given key (provider:holder) and specified signature point.
	 *
	 * @param operatorKey the key to look up
	 * @param input       whether the signature point is an input or output
	 * @return the signature map; if nothing specifically found, returns an empty map
	 */
	public Map<String, IOType> lookup(String operatorKey, boolean input) {
		return lookupInternal(operatorKey, -1, input)
				.<Map<String, IOType>>map(LinkedHashMap::new)
				.orElse(Collections.emptyMap());
	}

	/**
	 * Looks up all signatures for the given key (provider:holder) and specified signature point in a sub node.
	 *
	 * @param operatorKey  the key to look up
	 * @param subProcessID index of the sub node; must be non-negative and should be a valid index
	 * @param input        whether the signature point is an input or output
	 * @return the signature map; if nothing specifically found, returns an empty map
	 */
	public Map<String, IOType> lookup(String operatorKey, int subProcessID, boolean input) {
		if (subProcessID < 0) {
			throw new IllegalArgumentException("If you want to lookup the signature on an operator instead," +
					" use the method without the sub process ID");
		}
		return lookupInternal(operatorKey, subProcessID, input)
				.<Map<String, IOType>>map(LinkedHashMap::new)
				.orElse(Collections.emptyMap());
	}

	/**
	 * Looks up all parameters and their type classes for the given key (provider:holder);
	 *
	 * @param operatorKey  the key to look up
	 * @return the parameter map; if nothing specifically found, returns an empty map
	 */
	public Map<String, ParameterSignature> lookupParameters(String operatorKey) {
		return lookupInternal(operatorKey)
				.map(IOSignature::getParameters)
				.<Map<String, ParameterSignature>>map(LinkedHashMap::new)
				.orElse(Collections.emptyMap());
	}

	/**
	 * Looks up the list of supported capabilities for the given key (provider:holder);
	 *
	 * @param operatorKey  the key to look up
	 * @return the list of capabilities; if nothing specifically found, returns an empty list
	 */
	public List<String> lookupCapabilities(String operatorKey) {
		return lookupInternal(operatorKey)
				.map(IOSignature::getCapabilities)
				.<List<String>>map(ArrayList::new)
				.orElse(Collections.emptyList());
	}

	/**
	 * Writes all currently available signatures to the specified path.
	 *
	 * @param registryPath the path to write to
	 * @param clear        whether to clear existing files on the path
	 * @throws IOException if an error occurs
	 */
	public void write(Path registryPath, boolean clear) throws IOException {
		write(registryPath, clear, false);
	}

	/**
	 * Removes all signatures of all versions of the given provider. This can be used to unload a provider at runtime.
	 * The provider is the namespace of a plugin (without the rmx_ prefix).
	 *
	 * @param provider the provider to be removed
	 */
	public void removeProvider(String provider) {
		String prefix;
		if (!provider.endsWith(":")) {
			prefix = provider + ':';
		} else {
			prefix = provider;
		}
		this.signatures.keySet().removeIf(key -> key.startsWith(prefix));
	}

	/**
	 * Removes all signatures of all versions of the given provider. This can be used to unload a provider at runtime.
	 * This also removes all on-disk signatures for the given provider.
	 * The provider is the namespace of a plugin (without the rmx_ prefix).
	 *
	 * @param provider     the provider to be removed
	 * @param registryPath the registry path
	 */
	protected void removeProvider(String provider, Path registryPath) {
		ValidationUtilV2.requireNonNull(registryPath, "registryPath");
		removeProvider(provider);
		FileUtils.deleteQuietly(registryPath.resolve(provider).toFile());
	}

	/**
	 * Writes all currently available signatures to the specified path. Might omit {@link IOHolderProviderInfo} that
	 * are already present. Also might omit non-production versions if so specified in the setting
	 * {@value #PARAMETER_KEEP_CACHE}.
	 *
	 * @param registryPath the path to write to
	 * @param clear        whether to clear existing files on the path
	 * @param onlyWriteNew whether to only write entries that are new
	 * @throws IOException if an error occurs
	 */
	protected void write(Path registryPath, boolean clear, boolean onlyWriteNew) throws IOException {
		ValidationUtilV2.requireNonNull(registryPath, "registryPath");
		if (clear) {
			FileUtils.deleteQuietly(registryPath.toFile());
		}
		createDirectory(registryPath);

		String setting = Settings.getSetting(PARAMETER_KEEP_CACHE);
		if (setting == null) {
			setting = KEEP_CACHE_STRATEGIES[0];
		}
		boolean skipNonProduction = !setting.startsWith(KEEP_CACHE_STRATEGIES[0]);

		for (IOHolderProviderInfo providerInfo : signatures.values()) {
			if (providerInfo.getVersion().isDevelopmentBuild() && skipNonProduction) {
				continue;
			}
			Path providerPath = registryPath.resolve(providerInfo.getProviderID());
			createDirectory(providerPath);
			Path versionedFile = providerPath.resolve(providerInfo.getVersion().toString());
			if (onlyWriteNew && Files.exists(versionedFile) && !providerInfo.isNew()) {
				continue;
			}
			try {
				executePrivileged(() -> {
					SIGNATURE_WRITER.writeValue(versionedFile.toFile(), providerInfo.getSignatures().values());
					return null;
				});
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.signature.IOSignatureRegistry.cannot_write_signature", new Object[]{versionedFile, e.getMessage()});
				// skip file if writing is not possible
			}
		}
	}

	/**
	 * Prunes the in-memory signatures to only keep active provider IDs and their currently loaded versions in cache.
	 * On shutdown, only active signatures will be written.
	 */
	protected void cleanUpMemoryCache() {
		// find all currently loaded provider IDs
		List<String> providerIDs = this.signatures.keySet().stream()
				.map(key -> key.substring(0, key.indexOf(':')))
				.distinct().collect(Collectors.toList());

		// only keep valid/active versions in memory cache
		for (String providerID : providerIDs) {
			VersionNumber version = getVersion(providerID);
			String prefix = providerID + ':';
			// provider no longer in use? remove from registry
			Predicate<String> removalTest = key -> key.startsWith(prefix);
			if (version != null) {
				// remove unused versions from registry
				String onlyValidKey = prefix + version;
				removalTest = removalTest.and(key -> !key.equals(onlyValidKey));
			}
			this.signatures.keySet().removeIf(removalTest);
		}
	}

	/**
	 * Returns the version for the given provider ID if possible. Otherwise returns {@code null}
	 */
	protected abstract VersionNumber getVersion(String provider);

	/**
	 * Extracts a singular {@link IOType}.
	 */
	private IOType lookupInternal(String operatorKey, int subProcessID, String portName, boolean input) {
		ValidationUtilV2.requireNonNull(portName, "portName");
		return lookupInternal(operatorKey, subProcessID, input)
				.map(portMap -> portMap.get(portName))
				.orElse(defaultIOType);
	}

	/**
	 * Finds the full map of {@link IOType IOTypes} for the given key and subprocess
	 */
	private Optional<Map<String, IOType>> lookupInternal(String operatorKey, int subProcessID, boolean input) {
		boolean actualInput = input ^ subProcessID != -1;
		return lookupInternal(operatorKey)
				.map(sig -> getNodeSignature(sig, subProcessID))
				.map(actualInput ? NodeSignature::getInputs : NodeSignature::getOutputs);
	}

	/**
	 * Finds the full {@link IOSignature} for the given key
	 */
	private Optional<IOSignature> lookupInternal(String operatorKey) {
		ValidationUtilV2.requireNonNull(operatorKey, "operatorKey");
		String provider;

		int separatorIndex = operatorKey.indexOf(':');
		if (separatorIndex != -1) {
			provider = operatorKey.substring(0, separatorIndex);
		} else {
			provider = CORE_PROVIDER;
		}

		return Optional.of(provider).map(this::getVersion).map(v -> provider + ':' + v)
				.map(signatures::get).map(provInfo -> provInfo.getSignatures().get(operatorKey));
	}

	/**
	 * Creates a given directory if not yet existent. Throws an error if it already exists, but is not a directory.
	 */
	private void createDirectory(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectories(path);
		} else if (!Files.isDirectory(path)){
			throw new IOException("The given path is not a directory!");
		}
	}

	/**
	 * Reads all {@link IOSignature IOSignatures} from the specified path into the given registry. This will skip
	 * unreadable files.
	 *
	 * @param registryPath the path to read from
	 * @param registry     the registry to fill
	 * @param <T>          the generic type of the {@link IOSignatureRegistry}
	 * @throws IOException if an error occurs
	 */
	public static <T extends IOSignatureRegistry> void read(Path registryPath, T registry) throws IOException {
		if (registryPath == null || !Files.exists(registryPath) || !Files.isDirectory(registryPath)){
			throw new IOException("The given path does not exist or is not a directory!");
		}
		ValidationUtilV2.requireNonNull(registry, "registry");
		List<Path> providerPathList;
		try (Stream<Path> pathStream = Files.list(registryPath)) {
			providerPathList = pathStream.filter(Files::isDirectory).collect(Collectors.toList());
		}

		// find all provider folders
		for (Path providerPath : providerPathList) {
			List<Path> versionedFileList;
			try (Stream<Path> fileStream = Files.list(providerPath)) {
				versionedFileList = fileStream.filter(Files::isRegularFile).collect(Collectors.toList());
			}
			String providerID = providerPath.getFileName().toString();

			// find all signature files
			for (Path versionedFile : versionedFileList) {
				VersionNumber versionNumber;
				try {
					versionNumber = new VersionNumber(versionedFile.getFileName().toString());
				} catch (VersionNumberException e) {
					continue;
				}

				List<IOSignature> signatures;
				try {
					signatures = executePrivileged(() -> SIGNATURE_READER.readValue(versionedFile.toFile()));
				} catch (IOException e) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.signature.IOSignatureRegistry.cannot_read_signature", new Object[]{versionedFile, e.getMessage()});
					// skip unreadable files
					continue;
				}
				if (signatures == null || signatures.isEmpty()) {
					continue;
				}

				registry.signatures.put(providerID + ':' + versionNumber, IOHolderProviderInfo.builder()
						.providerID(providerID)
						.version(versionNumber)
						.signatures(signatures.stream().collect(Collectors
								.toMap(IOSignature::getIoHolderKey, sig -> sig, (a, b) -> a, LinkedHashMap::new)))
						.build());
			}
		}
	}

	/**
	 * Gets the node signature for the specidfied sub process ID; returns the given signature if the ID is -1.
	 */
	private static NodeSignature getNodeSignature(IOSignature sig, int subProcessID) {
		if (subProcessID == -1) {
			return sig;
		}
		List<SubNodeSignature> subProcesses = sig.getSubNodes();
		return subProcesses.size() > subProcessID ? subProcesses.get(subProcessID) : null;
	}

	/**
	 * Helper method to ensure proper execution
	 */
	private static <T> T executePrivileged(Callable<T> callable) throws IOException {
		try {
			return AccessController.doPrivileged((PrivilegedExceptionAction<T>) callable::call);
		} catch (PrivilegedActionException e) {
			return SecurityTools.handlePrivilegedExceptionToIO(e);
		}
	}
}
