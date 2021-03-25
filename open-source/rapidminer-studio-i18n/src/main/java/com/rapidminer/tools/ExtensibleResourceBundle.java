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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;


/**
 * This class represents an extensible resource bundle, where other resource bundles might be added. These are searched
 * if the key isn't contained in the previously added bundles.
 *
 * @author Sebastian Land
 */
public class ExtensibleResourceBundle extends ResourceBundle {

	private static boolean logMissingTranslations = false;

	/**
	 * Allows to enumerate seamlessly over multiple resource bundles
	 *
	 * @author Sebastian Land
	 */
	private static class KeyEnumeration implements Enumeration<String> {

		private Iterator<ResourceBundle> bundleIterator;
		private Enumeration<String> keyIterator;

		KeyEnumeration(Iterable<ResourceBundle> iterable) {
			bundleIterator = iterable.iterator();
		}

		@Override
		public boolean hasMoreElements() {
			if (keyIterator == null || !keyIterator.hasMoreElements()) {
				while (bundleIterator.hasNext() && (keyIterator == null || !keyIterator.hasMoreElements())) {
					keyIterator = bundleIterator.next().getKeys();
				}
			}
			if (keyIterator == null || !keyIterator.hasMoreElements()) {
				return false;
			}
			return true;
		}

		@Override
		public String nextElement() {
			if (keyIterator == null) {
				keyIterator = bundleIterator.next().getKeys();
			}
			while (!keyIterator.hasMoreElements()) {
				keyIterator = bundleIterator.next().getKeys();
			}
			return keyIterator.nextElement();
		}
	}

	/**
	 * Set of keys with missing translations
	 */
	private static final Set<String> MISSING_TRANSLATION_KEYS = Collections.newSetFromMap(new ConcurrentHashMap<>());
	/**
	 * Used to separate bundle from key
	 */
	private static final String SEPARATOR = "|";
	/**
	 * Sorts by "bundle is in selected language" descending
	 */
	private static final Comparator<ResourceBundle> CURRENT_LANG_DESC = Comparator.comparing(ExtensibleResourceBundle::weightResourceBundle, Integer::compareTo);

	private List<ResourceBundle> bundles = new CopyOnWriteArrayList<>();
	private final List<ResourceBundle> coreBundles;


	public ExtensibleResourceBundle(ResourceBundle parent) {
		coreBundles = flattenResourceBundle(parent);
		bundles.addAll(coreBundles);
	}

	/**
	 * This method extends this resource bundle with the properties set by the given bundle. If those properties are
	 * already contained, they will be ignored.
	 */
	public void addResourceBundle(ResourceBundle bundle) {
		bundles.addAll(flattenResourceBundle(bundle));
		bundles.sort(CURRENT_LANG_DESC);
	}

	/**
	 * This method extends this resource bundle with the properties set by the given bundle. If those properties were
	 * already contained, they are overwritten by the new bundle's settings.
	 * <p>
	 * Internal API, do not use!
	 * </p>
	 */
	public void addResourceBundleAndOverwrite(ResourceBundle bundle) {
		try {
			SecurityTools.requireInternalPermission();
		} catch (UnsupportedOperationException u) {
			I18N.LOGGER.log(Level.FINEST, u.getMessage(), u.getCause());
			addResourceBundle(bundle);
			return;
		}
		bundles.addAll(0, flattenResourceBundle(bundle));
		bundles.sort(CURRENT_LANG_DESC);
	}

	@Override
	public String getBaseBundleName() {
		// this is overridden because of java.util.logging.Logger not taking a bundle with a null basename
		return "extensibleResourceBundleBase";
	}

	@Override
	public Enumeration<String> getKeys() {
		return new KeyEnumeration(bundles);
	}

	@Override
	protected Object handleGetObject(String key) {
		if (key.endsWith(I18N.ICON_SUFFIX)) {
			for (ResourceBundle bundle : coreBundles) {
				if (bundle.containsKey(key)) {
					return bundle.getObject(key);
				}
			}
		}

		for (ResourceBundle bundle : bundles) {
			if (bundle.containsKey(key)) {
				logIfMissing(bundle, key);
				return bundle.getObject(key);
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(String key) {
		if (super.containsKey(key)) {
			return true;
		}

		for (ResourceBundle subbundle : bundles) {
			if (subbundle.containsKey(key)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Configure if missing translations should be logged in this class to be available using #getMissingKeys() later
	 *
	 * @param shouldLog true to keep keys with no translation available
	 */
	public static void setLogMissingTranslations(boolean shouldLog) {
		logMissingTranslations = shouldLog;
	}

	/**
	 * See if logging of missing translations is requested. Defaults to false.
	 *
	 * @return the configuration if keys without translation should be logged
	 */
	public static boolean isLogMissingTranslations() {
		return logMissingTranslations;
	}

	/**
	 * If missing translations are being logged, defined by setting in {@link #setLogMissingTranslations(boolean)}, this
	 * Set will contain the keys without a translation
	 *
	 * @return unmodifiable Set of missing translation key Strings
	 */
	public static Set<String> getMissingTranslationKeys() {
		return Collections.unmodifiableSet(MISSING_TRANSLATION_KEYS);
	}

	/**
	 * Tries to flatten the resource bundle
	 *
	 * <p>This is needed to support partial translations from multiple extensions.</p>
	 *
	 * @param bundle the bundle hierarchy
	 * @return a list containing the bundle and it's parents
	 */
	private static List<ResourceBundle> flattenResourceBundle(ResourceBundle bundle) {
		if (bundle instanceof ExtensibleResourceBundle) {
			return ((ExtensibleResourceBundle) bundle).bundles;
		}
		Field parentField;
		List<ResourceBundle> resourceBundles = new ArrayList<>();
		try {
			parentField = ResourceBundle.class.getDeclaredField("parent");
			parentField.setAccessible(true);
			ResourceBundle currentBundle = bundle;
			while (currentBundle != null) {
				ResourceBundle parentBundle = (ResourceBundle) parentField.get(currentBundle);
				parentField.set(currentBundle, null);
				resourceBundles.add(currentBundle);
				currentBundle = parentBundle;
			}
		} catch (Exception e) {
			I18N.LOGGER.log(Level.SEVERE, "Flattening of ResourceBundle failed. I18N might be broken.", e);
			return Collections.singletonList(bundle);
		}
		return resourceBundles;
	}

	/**
	 * Logs the bundle name and key in case of a missing translation
	 *
	 * @param bundle the bundle
	 * @param key    the key in the bundle
	 */
	private static void logIfMissing(ResourceBundle bundle, String key) {
		if (logMissingTranslations && bundle.getLocale().getLanguage().isEmpty() && !key.endsWith(I18N.ICON_SUFFIX)) {
			String[] bundlePath = Objects.toString(bundle.getBaseBundleName(), "").split("[/.]");
			MISSING_TRANSLATION_KEYS.add(bundlePath[bundlePath.length - 1] + SEPARATOR + key);
		}
	}

	/**
	 * Compares the given bundle locale against the default locale.
	 *
	 * <h3>Meaning of the rating</h5>
	 * <ol>
	 *     <li value=0>Root Bundle</li>
	 *     <li value=-1>Language equal</li>
	 *     <li value=-2>Language and Country equal</li>
	 *     <li value=-3>Language, Country and Variant equal</li>
	 * </ol>
	 * Negative values are used to sort descending, while keeping the previous order.
	 *
	 * @param bundle the bundle to weight
	 * @return likeliness to the default locale [-3,0]
	 */
	private static int weightResourceBundle(ResourceBundle bundle) {
		return weightLocale(bundle.getLocale());
	}

	/**
	 * Compares the given locale against the default locale.
	 *
	 * <h3>Meaning of the rating</h5>
	 * <ol>
	 *     <li value=0>Root Bundle</li>
	 *     <li value=-1>Language equal</li>
	 *     <li value=-2>Language and Country equal</li>
	 *     <li value=-3>Language, Country and Variant equal</li>
	 * </ol>
	 * Negative values are used to sort descending, while keeping the previous order.
	 *
	 * @param locale the locale to weight
	 * @return likeliness to the default locale [-3,0]
	 */
	private static int weightLocale(Locale locale) {
		//between ~50% (non en, no country) and 100% (en) of the loaded bundles are ROOT bundles
		if (locale.getLanguage().isEmpty()) {
			// root bundle
			return 0;
		}
		// The locale bundle mechanism only loads files with the right language and root bundles
		// For de-AT only de_AT, de and ROOT is loaded

		// Check country
		if (!Locale.getDefault().getCountry().equalsIgnoreCase(locale.getCountry())) {
			// language matched
			return -1;
		}
		// Check variant
		if (!Locale.getDefault().getVariant().equalsIgnoreCase(locale.getVariant())) {
			// language and country
			return -2;
		}
		// language country and variant matched
		return -3;
	}

}
