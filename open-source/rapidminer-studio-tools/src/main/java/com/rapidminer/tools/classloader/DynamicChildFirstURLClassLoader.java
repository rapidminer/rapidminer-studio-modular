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
package com.rapidminer.tools.classloader;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import com.rapidminer.tools.FunctionWithThrowable;
import com.rapidminer.tools.IteratorEnumerationAdapter;


/**
 * A child first {@link URLClassLoader} mainly used for JDBC Drivers to make it possible to have more dynamically loaded drivers.
 * Adding another {@link URLClassLoader} as a build up class loader on creation allows for dynamic growing of the class loader
 * if necessary; this can also be stopped using {@link #seal()}. Internally, when a URL is added, the corresponding jar
 * is looped through the {@link #getOrCreateCacheFiles(JarFile) file caching mechanism}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public abstract class DynamicChildFirstURLClassLoader extends URLClassLoader {

	private URLClassLoader buildupLoader;

	/** Simple constructor, using {@link #getSystemClassLoader()} as a parent. */
	public DynamicChildFirstURLClassLoader(URL[] classpath) {
		this(classpath, getSystemClassLoader());
	}

	/** Constructor to explicitly set the parent. */
	public DynamicChildFirstURLClassLoader(URL[] classpath, ClassLoader parent) {
		this(classpath, parent, null);
	}

	/**
	 * Constructor that allows for a build up/base class loader to find and add jar files for missing classes on the fly.
	 *
	 * @see #updateUCP(String)
	 * @see #seal()
	 */
	public DynamicChildFirstURLClassLoader(URL[] classpath, ClassLoader parent, URLClassLoader buildupLoader) {
		super(removeDuplicates(classpath), parent);
		this.buildupLoader = buildupLoader;
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		ClassNotFoundException[] lastError = {new ClassNotFoundException(name)};
		Consumer<Throwable> handler = t -> {if (t instanceof ClassNotFoundException) lastError[0] = (ClassNotFoundException) t;};
		return Stream.of(FunctionWithThrowable.suppress(this::findLoadedClass, handler), FunctionWithThrowable.suppress(this::findClass, handler),
				FunctionWithThrowable.suppress((String n) -> super.loadClass(n, resolve), handler), FunctionWithThrowable.suppress(this::updateUCP, handler))
				.map(f -> f.apply(name)).filter(Objects::nonNull)
				.peek(c -> {if(resolve) resolveClass(c);})
				.findFirst().orElseThrow(() -> lastError[0]);
	}

	/** Check if the given class can be found in the build up class loader. If so, update URLs and try again */
	private synchronized Class<?> updateUCP(String name) throws ClassNotFoundException {
		if (buildupLoader == null) {
			throw new ClassNotFoundException(name, new NullPointerException("No build up class loader available"));
		}
		Class<?> aClass = Class.forName(name, false, buildupLoader);
		if (preventUCPUpdate(name, buildupLoader)) {
			return aClass;
		}
		URL resource = aClass.getResource('/' + aClass.getName().replace('.', '/') + ".class");
		if (resource == null) {
			throw new ClassNotFoundException(name, new NullPointerException("Resource for class not found"));
		}
		URLConnection urlConnection;
		try {
			urlConnection = resource.openConnection();
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		}
		if (!(urlConnection instanceof JarURLConnection)) {
			throw new ClassNotFoundException(name, new IllegalArgumentException("Class resource " + resource + " is not from jar file"));
		}
		try {
			JarFile jarFile = ((JarURLConnection) urlConnection).getJarFile();
			List<Path> cachedFile = getOrCreateCacheFiles(jarFile);
			if (cachedFile.isEmpty()) {
				// should not happen, but better safe then sorry
				throw new ClassNotFoundException(name, new NullPointerException("Could not get or create cache file."));
			}
			URL url = cachedFile.get(0).toUri().toURL();
			addURL(url);
			return findClass(name);
		} catch (IOException | RuntimeException e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	/**
	 * Check whether a class should not trigger its containing jar file be added to the classpath.
	 *
	 * @param name
	 * 		the name of the class
	 * @param cl
	 * 		the buildup classloader
	 * @return {@code false} by default
	 */
	protected boolean preventUCPUpdate(String name, ClassLoader cl) {
		return false;
	}

	@Override
	public URL getResource(String name) {
		URL url = findResource(name);
		if (url == null) {
			// This call to getResource may eventually call findResource again, in case the parent doesn't find anything.
			url = super.getResource(name);
		}
		return url;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		 // Similar to super, but local resources are enumerated before parent resources
		Enumeration<URL> localUrls = findResources(name);
		Enumeration<URL> parentUrls = null;
		if (getParent() != null) {
			parentUrls = getParent().getResources(name);
		}
		List<URL> urls = new ArrayList<>();
		if (localUrls != null) {
			while (localUrls.hasMoreElements()) {
				urls.add(localUrls.nextElement());
			}
		}
		if (parentUrls != null) {
			while (parentUrls.hasMoreElements()) {
				urls.add(parentUrls.nextElement());
			}
		}
		return IteratorEnumerationAdapter.from(urls.iterator());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Synchronized to avoid calls from different threads
	 * @see #updateUCP(String)
	 * @see #seal()
	 */
	@Override
	@SuppressWarnings("squid:S1185")
	protected synchronized void addURL(URL url) {
		super.addURL(url);
	}

	/** Seals this class loader, i.e. no more jars will be added to its class path */
	public synchronized void seal() {
		buildupLoader = null;
	}

	/**
	 * If the jar file already exists in a cached version, will return the cached file, otherwise will cache the file
	 * and return it. This usually returns a singleton list.
	 *
	 * @param jarFile the jar file to cache
	 * @return the cached jar file
	 * @throws IOException if an error occurs
	 * @since 9.8.0
	 */
	protected abstract List<Path> getOrCreateCacheFiles(JarFile jarFile) throws IOException;

	/**
	 * Removes duplicates from classpath array
	 * <p>
	 * <strong>Note:</strong> Should be replaced with ValidationUtil#merge call in the future
	 *
	 * @since 9.8.0
	 */
	private static URL[] removeDuplicates(URL[] classpath) {
		return Arrays.stream(classpath).distinct().toArray(URL[]::new);
	}
}