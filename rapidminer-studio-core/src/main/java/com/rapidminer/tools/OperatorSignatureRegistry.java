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
package com.rapidminer.tools;

import static com.rapidminer.tools.FunctionWithThrowable.suppress;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.TableCapability;
import com.rapidminer.operator.TableCapabilityProvider;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.metadata.CollectionMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeLinkButton;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.tools.plugin.Plugin;
import com.rapidminer.tools.signature.IOHolderProviderInfo;
import com.rapidminer.tools.signature.IOSignature;
import com.rapidminer.tools.signature.IOSignatureRegistry;
import com.rapidminer.tools.signature.IOType;
import com.rapidminer.tools.signature.ParameterSignature;
import com.rapidminer.tools.signature.SubNodeSignature;

/**
 * {@link IOSignatureRegistry} for {@link Operator Operators} based on their {@link OperatorDescription}.
 * {@link OperatorDescription OperatorDescriptions} can be {@link #register(OperatorDescription) registered}.
 * {@link VersionNumber Versions} of providers are looked up by using {@link RapidMiner#getVersion()} or
 * {@link Plugin#getVersion()}
 *
 * @author Jan Czogalla
 * @since 9.10
 */
public final class OperatorSignatureRegistry extends IOSignatureRegistry {

	/**
	 * The singleton instance
	 */
	public static final OperatorSignatureRegistry INSTANCE = new OperatorSignatureRegistry();

	private static final AtomicBoolean initialized = new AtomicBoolean();

	private static final String KEY_CLEAR_CACHE_NOW = "operator_signature.clear_cache_now";
	private static final ResourceAction CLEAR_CACHE_NOW_ACTION = new ResourceAction(KEY_CLEAR_CACHE_NOW) {
		@Override
		protected void loggedActionPerformed(ActionEvent e) {
			ProgressThread cleanUpThread = new ProgressThread(KEY_CLEAR_CACHE_NOW, false) {

				@Override
				public void run() {
					try {
						INSTANCE.cleanUp(FileSystemService.getUserRapidMinerDir().toPath()
								.resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_SIGNATURE_FULL));
					} catch (IOException |
							IllegalArgumentException ex) {
						// ignore?
					}
				}
			};
			cleanUpThread.setIndeterminate(true);
			cleanUpThread.start();
		}
	};

	/**
	 * Creates the registry with {@link IOObject} as default class.
	 */
	private OperatorSignatureRegistry() {
		super(IOObject.class);
	}

	/**
	 * Registers the given {@link OperatorDescription} with this registry. If an entry already exists, will skip time-
	 * extensive {@link Operator} instantiation and meta data transformation and not create a new entry.
	 *
	 * @param description the description to register
	 * @return whether the registration was successful
	 */
	public boolean register(OperatorDescription description) {
		Plugin provider = ValidationUtilV2.requireNonNull(description, "description").getProvider();
		String providerID;
		String version;
		if (provider == null) {
			providerID = CORE_PROVIDER;
			version = RapidMiner.getVersion().toString();
		} else {
			providerID = provider.getPrefix();
			version = provider.getVersion();
		}

		VersionNumber versionNumber = new VersionNumber(version);
		String providerKey = providerID + ':' + versionNumber;
		signatures.putIfAbsent(providerKey,
				IOHolderProviderInfo.builder()
						.providerID(providerID)
						.version(versionNumber)
						.isNew(true)
						.build());
		IOHolderProviderInfo providerInfo = signatures.get(providerKey);
		// if operator already in registry, ignore
		if (providerInfo.getSignatures().containsKey(description.getKey())) {
			return false;
		}

		Operator operator;
		try {
			operator = description.createOperatorInstance();
		} catch (OperatorCreationException e) {
			LogService.getRoot().log(Level.WARNING, String.format("Could not create operator from description for key %s;"
					+ "skipping registration for signature.", description.getKey()), e);
			return false;
		}
		IOSignature signature = IOSignature.builder()
				.ioHolderKey(description.getKey())
				.ioHolderClass(description.getOperatorClass().getName())
				.parameters(operator.getParameterTypes().stream()
						.collect(Collectors.toMap(ParameterType::getKey, OperatorSignatureRegistry::createParameterSignature,
								(a, b) -> a, LinkedHashMap::new)))
				.build();

		try {
			operator.assumePreconditionsSatisfied();
			operator.transformMetaData();
		} catch (Exception e) {
			// ignore
		}
		registerInputs(operator.getInputPorts(), signature.getInputs());
		registerOutputs(operator.getOutputPorts(), signature.getOutputs());

		if (operator instanceof CapabilityProvider) {
			CapabilityProvider capProv = (CapabilityProvider) operator;
			List<String> capabilities = signature.getCapabilities();
			for (OperatorCapability capability : OperatorCapability.values()) {
				if (capProv.supportsCapability(capability)) {
					capabilities.add(capability.getDescription());
				}
			}
		}
		if (operator instanceof TableCapabilityProvider) {
			TableCapabilityProvider capProv = (TableCapabilityProvider) operator;
			List<String> capabilities = signature.getCapabilities();
			Set<TableCapability> supported = capProv.supported();
			if (supported != null){
				for (TableCapability capability : supported) {
					capabilities.add(capability.getDescription());
				}
			}
		}

		if (operator instanceof OperatorChain) {
			AtomicInteger index = new AtomicInteger();
			((OperatorChain) operator).getSubprocesses().forEach(unit -> {
				SubNodeSignature subSignature = SubNodeSignature.builder().index(index.getAndIncrement()).build();
				registerInputs(unit.getInnerSinks(), subSignature.getOutputs());
				registerOutputs(unit.getInnerSources(), subSignature.getInputs());
				signature.getSubNodes().add(subSignature);
			});
		}

		providerInfo.getSignatures().put(description.getKey(), signature);
		return true;
	}

	/**
	 * Removes all signatures of all versions of the given provider. This can be used to unload a provider at runtime.
	 * This also removes all on-disk signatures for the given provider if so specified.
	 * The provider is the namespace of a plugin (without the rmx_ prefix).
	 *
	 * @param provider    the provider to be removed
	 * @param clearOnDisk the registry path
	 */
	public void removeProvider(String provider, boolean clearOnDisk) {
		if (!clearOnDisk) {
			removeProvider(provider);
		} else {
			removeProvider(provider, FileSystemService.getUserRapidMinerDir().toPath()
					.resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_SIGNATURE_FULL));
		}
	}

	/**
	 * Looks up {@link VersionNumber} by using either {@link RapidMiner#getVersion()} or {@link Plugin#getVersion()}.
	 */
	@Override
	protected VersionNumber getVersion(String provider) {
		if (StringUtils.isEmpty(provider)) {
			return null;
		}
		if (CORE_PROVIDER.equals(provider)) {
			return RapidMiner.getVersion();
		}
		Plugin plugin = Plugin.getPluginByExtensionId("rmx_" + provider);
		if (plugin == null) {
			return null;
		}
		return new VersionNumber(plugin.getVersion());
	}

	/**
	 * Initializes the registry. Will read in existing signatures from
	 * {@link FileSystemService#RAPIDMINER_INTERNAL_CACHE_SIGNATURE_FULL} and registers a
	 * {@link ShutdownHooks#addShutdownHook(Runnable) shutdown hook} to write back new entries.
	 */
	public static void init() {
		if (!initialized.compareAndSet(false, true)) {
			return;
		}
		Path internalRegistryPath = FileSystemService.getUserRapidMinerDir().toPath()
				.resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_SIGNATURE_FULL);
		try {
			read(internalRegistryPath, INSTANCE);
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "Could not read operator signatures; will generate on the fly", e);
		}
		ShutdownHooks.addShutdownHook(() -> {
			try {
				INSTANCE.write(internalRegistryPath, false, true);
			} catch (IOException e) {
				LogService.getRoot().log(Level.WARNING, "Could not write operator signatures", e);
			}
		});

		// init settings
		ParameterType cacheType = new ParameterTypeTupel(PARAMETER_KEEP_CACHE, "",
				new ParameterTypeCategory("keep_strategy", "", KEEP_CACHE_STRATEGIES, 0),
				new ParameterTypeLinkButton("clean_up", "", CLEAR_CACHE_NOW_ACTION));
		RapidMiner.registerParameter(cacheType, "system");
	}

	/**
	 * Registers all {@link IOObject} subclasses that can be found in the signatures with the {@link OperatorService}
	 */
	public static void registerAllIOObjects() {
		Set<String> iooSimpleNames = OperatorService.getIOObjectsNames();
		List<Class<? extends IOObject>> loadedIOOClasses = INSTANCE.signatures.values().stream()
				// get all signatures
				.map(IOHolderProviderInfo::getSignatures)
				.flatMap(sigMap -> sigMap.values().stream())
				// collect signatures and sub process signatures
				.flatMap(sig -> Stream.concat(Stream.of(sig), sig.getSubNodes().stream()))
				// collect inputs & outputs
				.flatMap(node -> Stream.concat(node.getInputs().values().stream(), node.getOutputs().values().stream()))
				// get the IOO classes and make sure we have no duplicates
				.map(IOType::getClassName).distinct()
				// do not register classes that are already in
				.filter(cName -> !iooSimpleNames.contains(cName.substring(cName.lastIndexOf('.'))))
				// resolve classes and filter out unloadable classes
				.map(suppress(Plugin.getMajorClassLoader()::loadClass)).filter(Objects::nonNull)
				// make sure only IOO classes are actually collected
				.filter(IOObject.class::isAssignableFrom).map(c -> (Class<? extends IOObject>) c)
				.collect(Collectors.toList());
		OperatorService.registerIOObjects(loadedIOOClasses);
	}

	/**
	 * Finalize loading by removing all unused signatures
	 */
	public static void finalizeLoading() {
		try {
			SecurityTools.requireInternalPermission();
		} catch (Exception e) {
			// only allow internal access
			return;
		}
		INSTANCE.cleanUpMemoryCache();
	}

	/**
	 * Registers all input ports; uses {@link com.rapidminer.operator.ports.InputPort#getAllPreconditions()
	 * preconditions} to determine the expected object class. Will use {@link IOObject} as default otherwise.
	 */
	private static void registerInputs(InputPorts ports, Map<String, IOType> portMap) {
		ports.getAllPorts().forEach(ip -> {
			IOType portType = Optional.of(ip.getAllPreconditions())
					.filter(pc -> !pc.isEmpty()).map(pc -> pc.iterator().next())
					.map(Precondition::getExpectedMetaData)
					.map(OperatorSignatureRegistry::toCollectionAwareType)
					.orElse(INSTANCE.defaultIOType);
			portMap.put(ip.getName(), portType);
		});
	}

	/**
	 * Registers all output ports after the operator's meta data transformation was performed.
	 */
	private static void registerOutputs(OutputPorts ports, Map<String, IOType> portMap) {
		ports.getAllPorts().forEach(op -> {
			IOType portType = Optional.ofNullable(op.getRawMetaData())
					.map(OperatorSignatureRegistry::toCollectionAwareType)
					.orElse(INSTANCE.defaultIOType);
			portMap.put(op.getName(), portType);
		});
	}

	/**
	 * Converts the given {@link MetaData} object to an {@link IOType}, indicating either a simple {@link IOObject}
	 * class or a collection of objects with the element MD as the class.
	 */
	private static IOType toCollectionAwareType(MetaData md) {
		if (md instanceof CollectionMetaData) {
			String name = Optional.ofNullable(((CollectionMetaData) md).getElementMetaDataRecursive())
					.map(MetaData::getObjectClass).map(Class::getName)
					.orElse(INSTANCE.defaultIOType.getClassName());
			return new IOType(name, true, true);
		}
		return new IOType(md.getObjectClass().getName(), false, true);
	}

	/**
	 * Creates a {@link ParameterSignature} from a given {@link ParameterType} instance
	 */
	private static ParameterSignature createParameterSignature(ParameterType pt) {
		return new ParameterSignature(pt.getDescription(), pt.getClass().getName(), pt.isOptional());
	}

	/**
	 * Clears the cache completely on disk and also prunes the in-memory signatures to only keep active
	 * provider IDs and their currently loaded versions in cache. On shutdown, only active signatures will be written.
	 */
	private void cleanUp(Path registryPath) throws IOException {
		if (registryPath == null || !Files.exists(registryPath) || !Files.isDirectory(registryPath)){
			throw new IOException("The given path does not exist or is not a directory!");
		}
		// delete current cache
		FileUtils.deleteQuietly(registryPath.toFile());

		cleanUpMemoryCache();
	}
}
