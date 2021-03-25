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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.FunctionWithThrowable;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.TempFileTools;
import com.rapidminer.tools.ValidationUtilV2;
import com.rapidminer.tools.io.EmptyDirCleaner;


/**
 * Maps arbitrary String content to repository locations and/or hashes. The store is persistent, i.e. once something is
 * added here, it is persisted on disk in the .RapidMiner/{@value FileSystemService#RAPIDMINER_INTERNAL_CACHE_CONTENT_MAPPER_STORE_FULL}
 * folder. The content can then be retrieved at any point in time later, even in later Studio sessions. Also works for
 * things that do not have a repository location by letting the user specify (either only or in addition to a repository
 * location) a hash. <br/>
 * <p>
 * To store and later retrieve something, follow these steps:
 * <ol>
 * <li>Store the data by calling one of: {@link #store(String, String, RepositoryLocation)}, {@link #store(String,
 * String, String)}, or {@link #store(String, String, RepositoryLocation, String)}</li>
 * <li>Retrieve the data again by calling one of: {@link #retrieve(String, RepositoryLocation)}, {@link
 * #retrieve(String, String)}, or {@link #retrieve(String, RepositoryLocation, String)}</li>
 * </ol>
 * To for example store an ExampleSet without a repository location, use the hash provider already registered by default
 * by calling {@link #createHash(Object)} to create a hash based on the attribute names and types, and then provide the
 * hash to the store/retrieve calls without a repository location.
 * </p>
 * <br/>
 * <p>
 * If a repository location is specified, renaming the repository, an intermediate folder, or the actual entry will
 * result in this mapper being updated as well. Stored information can still be retrieved after such operations. Copying
 * and moving of the file into different folders however is not supported. The mapper will then simply not find the
 * previously stored information and it will remain available under the previous location.
 * </p>
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public enum PersistentContentMapperStore {

	INSTANCE;

	/**
	 * Interface indicating a content serializer that can write an object of type {@code T} into the store.
	 *
	 * @param <T>
	 * 		the type to be serialized
	 * @author Jan Czogalla
	 * @see PersistentContentMapperStore#store(String, Object, ContentSerializer, RepositoryLocation, String)
	 * @since 9.7
	 */
	public interface ContentSerializer<T> {
		/** Serializes the given object {@code t} to the specified path */
		void serialize(Path path, T t) throws IOException;

	}

	/**
	 * Interface indicating a content deserializer that can read an object of type {@code T} from the store.
	 *
	 * @param <T>
	 * @author Jan Czogalla
	 * @see PersistentContentMapperStore#retrieve(String, ContentDeserializer, RepositoryLocation, String)
	 * @since 9.7
	 */
	public interface ContentDeserializer<T> extends FunctionWithThrowable<Path, T, IOException> {
		/** Deserializes the object from the specified path */
		T deserialize(Path path) throws IOException;

		@Override
		default T applyWithException(Path path) throws IOException {
			return deserialize(path);
		}
	}

	/**
	 * Default String serializer
	 * @since 9.7
	 */
	public static final ContentSerializer<String> STRING_CONTENT_SERIALIZER = (Path p, String s) ->
			Files.write(p, s.getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	/**
	 * Default String deserializer
	 * @since 9.7
	 */
	public static final ContentDeserializer<String> STRING_CONTENT_DESERIALIZER = p ->
			FileUtils.readFileToString(p.toFile(), StandardCharsets.UTF_8);

	private static final String UNSAVED_PATH = ".unsaved";

	private Path storeRepoRootPath;
	private Path storeNoLocationPath;

	private RepositoryListener repositoryListener;
	private RepositoryManagerListener repositoryManagerListener;

	private Map<Class, Function<?, String>> hashProviderRegistry = new HashMap<>();


	PersistentContentMapperStore() {
		storeRepoRootPath = FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_CONTENT_MAPPER_STORE_FULL);
		storeNoLocationPath = FileSystemService.getUserRapidMinerDir().toPath().resolve(FileSystemService.RAPIDMINER_INTERNAL_CACHE_CONTENT_MAPPER_STORE_FULL).resolve(UNSAVED_PATH);
		repositoryListener = new RepositoryListener() {
			@Override
			public void entryAdded(Entry newEntry, Folder parent) {
				// ignored
			}

			@Override
			public void entryChanged(Entry entry) {
				// ignored
			}

			@Override
			public void entryMoved(Entry newEntry, Folder formerParent, String formerName) {
				if (newEntry instanceof Repository) {
					repositoryRenamed(formerName, newEntry.getName());
				} else {
					repositoryEntryMoved(newEntry, formerParent, formerName);
				}
			}

			@Override
			public void entryRemoved(Entry removedEntry, Folder parent, int oldIndex) {
				// ignored
			}

			@Override
			public void folderRefreshed(Folder folder) {
				// ignored
			}
		};
		repositoryManagerListener = new RepositoryManagerListener() {
			@Override
			public void repositoryWasAdded(Repository repository) {
				repository.addRepositoryListener(repositoryListener);
			}

			@Override
			public void repositoryWasRemoved(Repository repository) {
				repository.removeRepositoryListener(repositoryListener);
				try {
					Path repoRoot = storeRepoRootPath.resolve(createPath(repository.getLocation(), null));
					FileUtils.deleteQuietly(repoRoot.toFile());
				} catch (Exception e) {
					// ignore
				}
			}
		};

		// register a hash provider for ExampleSets
		registerHashGenerator(ExampleSet.class, exampleSet -> {
			int hashCount = exampleSet.getAttributes().allSize() * 2;
			Object[] hashTargets = new Object[hashCount];

			Iterator<Attribute> attributeIterator = exampleSet.getAttributes().allAttributes();
			int i = 0;
			while (attributeIterator.hasNext()) {
				Attribute att = attributeIterator.next();
				hashTargets[i++] = att.getValueType();
				hashTargets[i++] = att.getName();
			}

			return String.valueOf(Objects.hash(hashTargets));
		});
	}

	/**
	 * Stores the given string content for the given {@link RepositoryLocation} under the given key.
	 *
	 * @param key
	 * 		the key, used to later retrieve the content again. Storing again for the same location and the same key will
	 * 		overwrite any existing content already stored!
	 * @param content
	 * 		the content to be persisted. If {@code null}, will simply delete the stored content file
	 * @param location
	 * 		the repository location for which the content should be persisted
	 * @throws IOException
	 * 		if something goes wrong during writing to disk
	 */
	public void store(String key, String content, RepositoryLocation location) throws IOException {
		store(key, content, location, null);
	}

	/**
	 * Stores the given string content for the given hash string under the given key.
	 *
	 * @param key
	 * 		the key, used to later retrieve the content again. Storing again for the same location and the same key will
	 * 		overwrite any existing content already stored!
	 * @param content
	 * 		the content to be persisted. If {@code null}, will simply delete the stored content file
	 * @param additionalHash
	 * 		arbitrary hash to identify content, e.g. when you have no repository location. See {@link
	 * 		#createHash(Object)}.
	 * @throws IOException
	 * 		if something goes wrong during writing to disk
	 */
	public void store(String key, String content, String additionalHash) throws IOException {
		store(key, content, null, additionalHash);
	}

	/**
	 * Stores the given string content for the given {@link RepositoryLocation} under the given key.
	 *
	 * @param key
	 * 		the key, used to later retrieve the content again. Storing again for the same location and the same key will
	 * 		overwrite any existing content already stored!
	 * @param content
	 * 		the content to be persisted. If {@code null}, will simply delete the stored content file
	 * @param location
	 * 		the repository location for which the content should be persisted
	 * @param additionalHash
	 * 		optional, another identifier if repository location alone is not sufficient, e.g. when you also need to take
	 * 		the data content into account. Ignored if {@code null}
	 * @throws IOException
	 * 		if something goes wrong during writing to disk
	 */
	public void store(String key, String content, RepositoryLocation location, String additionalHash) throws IOException {
		store(key, content, STRING_CONTENT_SERIALIZER, location, additionalHash);
	}

	/**
	 * Stores the given (generic) content for the given {@link RepositoryLocation} under the given key with the provided
	 * {@link ContentSerializer}.
	 *
	 * @param <T>
	 *		the content type
	 * @param key
	 * 		the key, used to later retrieve the content again. Storing again for the same location and the same key will
	 * 		overwrite any existing content already stored!
	 * @param content
	 * 		the content to be persisted. If {@code null}, will simply delete the stored content file
	 * @param serializer
	 * 		the content serializer to actually write to disk
	 * @param location
	 * 		the repository location for which the content should be persisted
	 * @throws IOException
	 * 		if something goes wrong during writing to disk
	 * @since 9.7
	 */
	public <T> void store(String key, T content, ContentSerializer<T> serializer, RepositoryLocation location) throws IOException {
		store(key, content, serializer, location, null);
	}

	/**
	 * Stores the given (generic) content for the given {@link RepositoryLocation} under the given key with the provided
	 * {@link ContentSerializer}.
	 *
	 * @param <T>
	 *		the content type
	 * @param key
	 * 		the key, used to later retrieve the content again. Storing again for the same location and the same key will
	 * 		overwrite any existing content already stored!
	 * @param content
	 * 		the content to be persisted. If {@code null}, will simply delete the stored content file
	 * @param serializer
	 * 		the content serializer to actually write to disk
	 * @param location
	 * 		the repository location for which the content should be persisted
	 * @param additionalHash
	 * 		optional, another identifier if repository location alone is not sufficient, e.g. when you also need to take
	 * 		the data content into account. Ignored if {@code null}
	 * @throws IOException
	 * 		if something goes wrong during writing to disk
	 * @since 9.7
	 */
	public <T> void store(String key, T content, ContentSerializer<T> serializer, RepositoryLocation location, String additionalHash) throws IOException {
		key = ValidationUtilV2.requireNonEmptyString(key, "key");
		if (content != null) {
			ValidationUtilV2.requireNonNull(serializer, "serializer");
		}
		if (location == null && additionalHash == null) {
			throw new IllegalArgumentException("location and additionalHash must not be null at the same time!");
		}

		String pathString = createPath(location, additionalHash);
		Path filePath = location != null ? storeRepoRootPath : storeNoLocationPath;
		filePath = filePath.resolve(pathString).resolve(key);
		if (content != null) {
			if (!Files.exists(filePath.getParent())) {
				Files.createDirectories(filePath.getParent());
			}
			serializer.serialize(filePath, content);
		} else {
			if (!FileUtils.deleteQuietly(filePath.toFile())) {
				TempFileTools.registerCleanup(filePath);
			}
		}
	}

	/**
	 * Retrieves the string content for the given {@link RepositoryLocation} under the given key. Can return {@code
	 * null} if nothing was persisted yet.
	 *
	 * @param key
	 * 		the key which was used to earlier store the content.
	 * @param location
	 * 		the repository location for which the content should be read
	 * @return the persisted content or {@code null} if nothing was persisted yet
	 * @throws IOException
	 * 		if something goes wrong during reading from disk
	 */
	public String retrieve(String key, RepositoryLocation location) throws IOException {
		return retrieve(key, location, null);
	}

	/**
	 * Retrieves the string content for the given hash String under the given key. Can return {@code null} if nothing
	 * was persisted yet.
	 *
	 * @param key
	 * 		the key which was used to earlier store the content.
	 * @param additionalHash
	 * 		arbitrary hash to identify content, e.g. when you have no repository location. Ignored if {@code null}. See
	 * 		{@link #createHash(Object)}, but you can also provide your own hash
	 * @return the persisted content or {@code null} if nothing was persisted yet
	 * @throws IOException
	 * 		if something goes wrong during reading from disk
	 */
	public String retrieve(String key, String additionalHash) throws IOException {
		return retrieve(key, null, additionalHash);
	}

	/**
	 * Retrieves the string content for the given {@link RepositoryLocation} under the given key. Can return {@code
	 * null} if nothing was persisted yet.
	 *
	 * @param key
	 * 		the key which was used to earlier store the content.
	 * @param location
	 * 		the repository location for which the content should be read
	 * @param additionalHash
	 * 		another identifier if repository location alone is not sufficient, e.g. when you also need to take the data
	 * 		content into account. Ignored if {@code null}. See {@link #createHash(Object)}, but you can also provide your
	 * 		own hash
	 * @return the persisted content or {@code null} if nothing was persisted yet
	 * @throws IOException
	 * 		if something goes wrong during reading from disk
	 */
	public String retrieve(String key, RepositoryLocation location, String additionalHash) throws IOException {
		return retrieve(key, STRING_CONTENT_DESERIALIZER, location, additionalHash);
	}

	/**
	 * Retrieves the(generic) content for the given {@link RepositoryLocation} under the given key using the provided
	 * {@link ContentDeserializer}. Can return {@code null} if nothing was persisted yet.
	 *
	 * @param <T>
	 * 		the content type
	 * @param key
	 * 		the key which was used to earlier store the content.
	 * @param deserializer
	 * 		the deserializer for the given content key
	 * @param location
	 * 		the repository location for which the content should be read
	 * @return the persisted content or {@code null} if nothing was persisted yet
	 * @throws IOException
	 * 		if something goes wrong during reading from disk
	 * @since 9.7
	 */
	public <T> T retrieve(String key, ContentDeserializer<T> deserializer, RepositoryLocation location) throws IOException {
		return retrieve(key, deserializer, location, null);
	}

	/**
	 * Retrieves the(generic) content for the given {@link RepositoryLocation} under the given key using the provided
	 * {@link ContentDeserializer}. Can return {@code null} if nothing was persisted yet.
	 *
	 * @param <T>
	 * 		the content type
	 * @param key
	 * 		the key which was used to earlier store the content.
	 * @param deserializer
	 * 		the deserializer for the given content key
	 * @param location
	 * 		the repository location for which the content should be read
	 * @param additionalHash
	 * 		another identifier if repository location alone is not sufficient, e.g. when you also need to take the data
	 * 		content into account. Ignored if {@code null}. See {@link #createHash(Object)}, but you can also provide your
	 * 		own hash
	 * @return the persisted content or {@code null} if nothing was persisted yet
	 * @throws IOException
	 * 		if something goes wrong during reading from disk
	 * @since 9.7
	 */
	public <T> T retrieve(String key, ContentDeserializer<T> deserializer, RepositoryLocation location, String additionalHash) throws IOException {
		key = ValidationUtilV2.requireNonEmptyString(key, "key");
		ValidationUtilV2.requireNonNull(deserializer, "deserializer");
		if (location == null && additionalHash == null) {
			return null;
		}

		String pathString = createPath(location, additionalHash);
		Path filePath = location != null ? storeRepoRootPath : storeNoLocationPath;
		filePath = filePath.resolve(pathString).resolve(key);
		if (!Files.exists(filePath)) {
			return null;
		}
		return deserializer.deserialize(filePath);
	}

	/**
	 * Copies all content keys from the old location to the new location. Can be used when duplicating single
	 * {@link Entry entries} or a {@link Folder}.
	 *
	 * @param oldLocation
	 * 		the old location to copy from
	 * @param newLocation
	 * 		the new location to copy to
	 * @since 9.7
	 */
	public void copyContent(RepositoryLocation oldLocation, RepositoryLocation newLocation) {
		repositoryEntryMovedOrCopied(oldLocation, newLocation, false);
	}

	/**
	 * Clears the list of given content keys from the specified {@link RepositoryLocation} and cleans up empty folders afterwards
	 *
	 * @param keys
	 * 		the content keys to remove
	 * @param location
	 * 		the location to remove the keys from
	 * @since 9.7
	 */
	public void clearKeys(List<String> keys, RepositoryLocation location) {
		String pathString = createPath(location, null);
		Path rootPath = storeRepoRootPath.resolve(pathString);
		if (!Files.exists(rootPath)) {
			return;
		}
		Predicate<Path> deleteFile = path -> keys != null && keys.contains(path.getFileName().toString());

		try {
			Files.walkFileTree(rootPath, new EmptyDirCleaner(deleteFile));
		} catch (IOException e) {
			// ignore
		}
	}

	/**
	 * Initializes the mapper store. Call during init of the repository manager.
	 */
	void init() {
		RepositoryManager.getInstance(null).addRepositoryManagerListener(repositoryManagerListener);
		RepositoryManager.getInstance(null).getRepositories().forEach(repo -> repo.addRepositoryListener(repositoryListener));
	}


	/**
	 * Registers a hash generator for the given object class. This can be used to later create hashes via {@link
	 * #createHash(Object)}. The hash in turn can be used to identify objects that have no repository location path.
	 *
	 * @param objectClass
	 * 		the class of the objects for which to register the hash generator.
	 * @param generator
	 * 		the hash generator, never {@code null}
	 */
	public <T> void registerHashGenerator(Class<T> objectClass, Function<T, String> generator) {
		ValidationUtilV2.requireNonNull(objectClass, "object class");
		ValidationUtilV2.requireNonNull(generator, "generator");
		hashProviderRegistry.putIfAbsent(objectClass, generator);
	}

	/**
	 * Checks if the factory has an adapter registered for the given IOObject class.
	 *
	 * @param t
	 * 		the object, never {@code null}
	 * @return {@code true} if there is an adapter registered; {@code false} if there is no adapter
	 */
	public <T> boolean hasHashGeneratorFor(T t) {
		return createHash(t) != null;
	}

	/**
	 * Tries to create a hash for the given object. Only works if a hash provider has been registered for the object
	 * class before via {@link #registerHashGenerator(Class, Function)}.
	 *
	 * @param t
	 * 		the object for which to create the hash
	 * @return the hash or {@code null} if no hash generator is registered for the given object class
	 */
	@SuppressWarnings("unchecked")
	public <T> String createHash(T t) {
		if (t == null) {
			return null;
		}

		Class objectClass = t.getClass();
		Function<T, String> generator = null;
		outerLoop: while (objectClass != null) {
			if (hashProviderRegistry.containsKey(objectClass)) {
				generator = (Function<T, String>) hashProviderRegistry.get(objectClass);
				break;
			}

			for (Class interFace : objectClass.getInterfaces()) {
				if (hashProviderRegistry.containsKey(interFace)) {
					generator = (Function<T, String>) hashProviderRegistry.get(interFace);
					break outerLoop;
				}
			}

			objectClass = objectClass.getSuperclass();
		}

		if (generator == null) {
			return null;
		}

		return generator.apply(t);
	}

	/**
	 * Called when a repository has been renamed. This renames the repository folder in the cache, so that the cache
	 * does not suddenly only produces misses for the renamed repository.
	 *
	 * @param formerName
	 * 		the previous name of the repository
	 * @param newName
	 * 		the new name of the repository
	 */
	private void repositoryRenamed(String formerName, String newName) {
		try {
			Path oldRepoDir = storeRepoRootPath.resolve(formerName);
			Path newRepoDir = storeRepoRootPath.resolve(newName);
			if (Files.exists(oldRepoDir)) {
				FileUtils.moveDirectory(oldRepoDir.toFile(), newRepoDir.toFile());
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.PersistentContentMapperStore.repo_rename_update_failed", e);
		}
	}

	/**
	 * Called when a repository entry has been moved or renamed. This copies the cache content for the old entry
	 * location to the new entry location, so that the cache does not suddenly only produces misses for the
	 * moved/renamed entry.
	 *
	 * @param newEntry
	 * 		the new repository entry
	 * @param formerParent
	 * 		the name of the previous parent folder. If it's the same as the new entry parent, then the entry itself was
	 * 		renamed
	 * @param formerName
	 * 		the previous name of the entry. May be unchanged if the entry was moved
	 */
	private void repositoryEntryMoved(Entry newEntry, Folder formerParent, String formerName) {
		try {
			String locString = formerParent.getLocation().getAbsoluteLocation() + RepositoryLocation.SEPARATOR + formerName;
			RepositoryLocation newEntryLocation = newEntry.getLocation();
			RepositoryLocation oldFullLoc = new RepositoryLocationBuilder().withLocationType(newEntryLocation.getLocationType())
					.withExpectedDataEntryType(newEntryLocation.getExpectedDataEntryType())
					.withFailIfDuplicateIOObjectExists(newEntryLocation.isFailIfDuplicateIOObjectExists())
					.buildFromAbsoluteLocation(locString);
			repositoryEntryMovedOrCopied(oldFullLoc, newEntryLocation, true);
		} catch (MalformedRepositoryLocationException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.PersistentContentMapperStore.entry_rename_update_failed", e);
		}
	}

	/**
	 * Moves or copies all content keys from the old location to the new location
	 *
	 * @since 9.7
	 */
	private void repositoryEntryMovedOrCopied(RepositoryLocation oldLocation, RepositoryLocation newLocation, boolean move) {
		try {
			Path oldFolder = storeRepoRootPath.resolve(createPath(oldLocation, null));
			if (Files.exists(oldFolder)) {
				Path newFolder = storeRepoRootPath.resolve(createPath(newLocation, null));
				if (move) {
					FileUtils.moveDirectory(oldFolder.toFile(), newFolder.toFile());
				} else {
					FileUtils.copyDirectory(oldFolder.toFile(), newFolder.toFile());
				}
			}
		} catch (IOException e) {
			String messageKey = "entry_rename_update_failed";
			if (!move) {
				messageKey = "entry_copy_update_failed";
			}
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.PersistentContentMapperStore." + messageKey, e);
		}
	}

	/**
	 * Creates a path structure mimicking the absolute repository location if a location is given; or just returns a
	 * UUID using the given string as the random seed.
	 *
	 * @param location
	 * 		the absolute repository location, can be {@code null} if no location is defined. If {@code null}, hash must not
	 * 		be {@code null} at the same time
	 * @param hash
	 * 		a hash describing the object for which to create the path, can be {@code null} if a location is defined. If
	 * 		{@code null}, location must not be {@code null} at the same time
	 * @return the UUID for an absolute repository location. The UUID will always be the same for the same repository
	 * location
	 */
	private String createPath(RepositoryLocation location, String hash) {
		if (location == null && hash == null) {
			throw new IllegalArgumentException("location and hash must not be null at the same time!");
		}

		if (location != null) {
			return location.getRepositoryName() + "/" + RepositoryTools.getPathWithSuffix(location);
		} else {
			return UUID.nameUUIDFromBytes(hash.getBytes()).toString();
		}
	}

}
