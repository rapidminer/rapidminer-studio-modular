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
package com.rapidminer.tools;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import com.github.markusbernhardt.proxy.ProxySearch;
import com.github.markusbernhardt.proxy.selector.pac.PacProxySelector;
import com.github.markusbernhardt.proxy.selector.pac.UrlPacScriptSource;
import com.github.markusbernhardt.proxy.util.Logger;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.properties.ProxyParameterSaver;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;


/**
 * This class applies the proxy settings on the JVM environment
 *
 * @author Jonas Wilms-Pfau
 * @since 7.3.0
 */
public class ProxySettings {

	/** Default value for Linux and Windows */
	private static final String DEFAULT = "";

	private static final SystemSettings SYSTEM_SETTINGS = new SystemSettings();

	public static final String PROXY_PREFIX = "rapidminer.proxy.";
	public static final String SYSTEM_PREFIX = "";

	private static final String HTTP_NON_PROXY_RULE = "http.nonProxyHosts";
	private static final String FTP_NON_PROXY_RULE = "ftp.nonProxyHosts";
	private static final String SOCKS_NON_PROXY_RULE = "socksNonProxyHosts";

	private static final List<Proxy> NO_PROXY_LIST = Collections.singletonList(Proxy.NO_PROXY);

	private static final ProxySelector DEFAULT_PROXY_SELECTOR = ProxySelector.getDefault();
	private static final ProxySelector NO_PROXY_SELECTOR = new ProxySelector() {

		@Override
		public List<Proxy> select(URI uri) {
			return NO_PROXY_LIST;
		}

		@Override
		public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
			DEFAULT_PROXY_SELECTOR.connectFailed(uri, sa, ioe);
		}
	};

	private static final SimpleProxySelectorWrapper MAIN_PROXY_SELECTOR = new SimpleProxySelectorWrapper(DEFAULT_PROXY_SELECTOR) {
		@Override
		public void setDelegate(ProxySelector delegate) {
			SecurityTools.requireInternalPermission();
			super.setDelegate(delegate);
		}
	};

	/* RapidMiner proxy settings */
	private static final String[] PROXY_HOSTS = { RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_HOST,
			RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_HOST, RapidMiner.PROPERTY_RAPIDMINER_FTP_PROXY_HOST,
			RapidMiner.PROPERTY_RAPIDMINER_SOCKS_PROXY_HOST };
	private static final String[] PROXY_PORTS = { RapidMiner.PROPERTY_RAPIDMINER_HTTP_PROXY_PORT,
			RapidMiner.PROPERTY_RAPIDMINER_HTTPS_PROXY_PORT, RapidMiner.PROPERTY_RAPIDMINER_FTP_PROXY_PORT,
			RapidMiner.PROPERTY_RAPIDMINER_SOCKS_PROXY_PORT };
	/* Real system settings */
	private static final String[] PROXY_RULES = { HTTP_NON_PROXY_RULE, FTP_NON_PROXY_RULE, SOCKS_NON_PROXY_RULE };

	/**
	 * To support OSX we have to store the System settings before the RapidMiner cfg file is loaded
	 */
	public static void storeSystemSettings() {
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
			Arrays.stream(toNative(PROXY_HOSTS)).forEach(SYSTEM_SETTINGS::store);
			Arrays.stream(toNative(PROXY_PORTS)).forEach(SYSTEM_SETTINGS::store);
			Arrays.stream(PROXY_RULES).forEach(SYSTEM_SETTINGS::store);
		} else {
			Arrays.stream(toNative(PROXY_HOSTS)).forEach(SYSTEM_SETTINGS::storeDefault);
			Arrays.stream(toNative(PROXY_PORTS)).forEach(SYSTEM_SETTINGS::storeDefault);
			Arrays.stream(PROXY_RULES).forEach(SYSTEM_SETTINGS::storeDefault);
		}

	}

	/**
	 * This Method
	 * <ul>
	 * <li>Migrates the old proxy settings if needed</li>
	 * <li>Applies the current proxy Settings</li>
	 * <li>Registers a ChangeListener to change the proxy settings on save</li>
	 * </ul>
	 *
	 */
	public static void init() {
		ProxySettings.storeSystemSettings();
		ProxyIntegrator.updateOldInstallation();
		ParameterService.registerParameterChangeListener(new ProxyParameterSaver());
		//pipe proxy vole logging through to our logger
		Logger.setBackend(new ProxyVoleLogger());
		ProxySelector.setDefault(MAIN_PROXY_SELECTOR);
	}

	/**
	 * Applies the RapidMiner proxy settings on the corresponding JVM System properties
	 */
	public static void apply() {

		switch (Objects.toString(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_MODE), RapidMiner.RAPIDMINER_PROXY_MODE_SYSTEM)) {
			case RapidMiner.RAPIDMINER_PROXY_MODE_DIRECT:
				// No Proxy
				MAIN_PROXY_SELECTOR.setDelegate(NO_PROXY_SELECTOR);
				break;
			case RapidMiner.RAPIDMINER_PROXY_MODE_MANUAL:
				MAIN_PROXY_SELECTOR.setDelegate(DEFAULT_PROXY_SELECTOR);
				// User Settings
				copyParameterToSystem(PROXY_HOSTS, toNative(PROXY_HOSTS));
				copyParameterToSystem(PROXY_PORTS, toNative(PROXY_PORTS));
				String exclusionRule =
						ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_EXCLUDE);
				setSystemValue(exclusionRule, PROXY_RULES);
				// Apply Socks Version
				int socksVersionOffset = Arrays.asList(RapidMiner.RAPIDMINER_SOCKS_VERSIONS)
						.indexOf(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_SOCKS_VERSION));
				int initialSocksVersion = 4;
				ParameterService.setParameterValue(toNative(RapidMiner.PROPERTY_RAPIDMINER_SOCKS_VERSION),
						String.valueOf(initialSocksVersion + socksVersionOffset));
				break;
			case RapidMiner.RAPIDMINER_PROXY_MODE_PAC:
				MAIN_PROXY_SELECTOR.setDelegate(DEFAULT_PROXY_SELECTOR);
				String pacType = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_PAC_TYPE);
				String pacUrl = null;
				if (RapidMiner.RAPIDMINER_PROXY_PAC_TYPE_PATH.equals(pacType)) {
					// convert path to url
					String pacPath = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_PAC_PATH);
					pacPath = StringUtils.trimToNull(pacPath);
					if (pacPath != null) {
						try {
							final URL url = Paths.get(pacPath).toUri().toURL();
							pacUrl = url.toString();
						} catch (MalformedURLException e) {
							LogService.getRoot().log(Level.WARNING,
									I18N.getMessage(LogService.getRoot().getResourceBundle(),
											"com.rapidminer.tools.ProxyService.pac_invalid_url", pacPath), e);
						}
					}
				} else {
					String url = ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_PAC_URL);
					pacUrl = StringUtils.trimToNull(url);
				}

				ProxySelector selector = DEFAULT_PROXY_SELECTOR;
				if (pacUrl != null) {
					final UrlPacScriptSource pacSource = new UrlPacScriptSource(pacUrl);
					if (pacSource.isScriptValid()) {
						selector = new PacProxySelector(pacSource);
					} else {
						LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.ProxyService.pac_script_invalid");
					}
				} else {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.ProxyService.pac_not_specified");
				}
				MAIN_PROXY_SELECTOR.setDelegate(selector);
				break;
			case RapidMiner.RAPIDMINER_PROXY_MODE_SYSTEM:
			default:
				//set to default before search so that old settings are not found
				MAIN_PROXY_SELECTOR.setDelegate(DEFAULT_PROXY_SELECTOR);
				// System Proxy
				SYSTEM_SETTINGS.apply();
				ProxySearch s = new ProxySearch();
				// to keep compatibility with older versions
				s.addStrategy(ProxySearch.Strategy.JAVA);
				// This needs switches for different OS
				s.addStrategy(ProxySearch.Strategy.OS_DEFAULT);
				// The fancy win 10 ui still sets the same Registry value as IE6
				if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.WINDOWS) {
					s.addStrategy(ProxySearch.Strategy.IE);
				}
				s.addStrategy(ProxySearch.Strategy.WPAD);
				// Invoke the proxy search. This will create a ProxySelector with the detected proxy settings.
				ProxySelector proxySelector = s.getProxySelector();

				// Install this ProxySelector as default ProxySelector for all connections.
				if (proxySelector == null) {
					proxySelector = DEFAULT_PROXY_SELECTOR;
				}
				MAIN_PROXY_SELECTOR.setDelegate(proxySelector);
				break;
		}
		GlobalAuthenticator.refreshProxyAuthenticators();
	}

	/**
	 * Set one value to all System Keys
	 *
	 * @param value the value
	 * @param systemKeys the keys
	 */
	private static void setSystemValue(String value, String[] systemKeys) {
		for (String parameterKey : systemKeys) {
			setSystemProperty(parameterKey, value);
		}
	}

	private static void setSystemProperty(String key, String value) {
		if (value != null && key != null) {
			System.setProperty(key, value);
		}

	}

	/**
	 * Copies the ParameterService values from the source keys to target System property keys
	 * <p>
	 * Warning: both arrays must have the same length.
	 * </p>
	 *
	 * @param sourceKeys
	 *            ParameterService keys
	 * @param targetKeys
	 *            System keys
	 */
	private static void copyParameterToSystem(String[] sourceKeys, String[] targetKeys) {
		for (int i = 0; i < sourceKeys.length; i++) {
			String sourceValue = ParameterService.getParameterValue(sourceKeys[i]);
			setSystemProperty(targetKeys[i], sourceValue);
		}
	}

	/**
	 * Converts the given keys to native System keys
	 *
	 * @param keys the keys
	 * @return the keys with the prefix
	 */
	private static String[] toNative(String[] keys) {
		return Arrays.stream(keys).map(ProxySettings::toNative).toArray(String[]::new);
	}

	/**
	 * Converts the given key to a native System key
	 *
	 * @param key the key
	 * @return the key without the prefix
	 */
	private static String toNative(String key) {
		return key.replace(PROXY_PREFIX, SYSTEM_PREFIX);
	}

	/**
	 * Pipe proxy vole logger to our log.
	 *
	 * @author Gisa Meier
	 * @since 9.8
	 */
	private static class ProxyVoleLogger implements Logger.LogBackEnd {

		@Override
		public void log(Class<?> aClass, Logger.LogLevel logLevel, String s, Object... objects) {
			Level rmLevel;
			switch (logLevel) {
				case ERROR:
					rmLevel = Level.WARNING;
					break;
				case WARNING:
					rmLevel = Level.INFO;
					break;
				case TRACE:
					rmLevel = Level.FINEST;
					break;
				case INFO:
				case DEBUG:
				default:
					rmLevel = Level.FINE;
					break;
			}
			LogService.getRoot().log(rmLevel, s, objects);
		}
	}

	/**
	 * This class migrates the old proxy settings into the new structure.
	 * <p>
	 * Use ProxyService.init() to check for updates
	 * </p>
	 *
	 * @author Jonas Wilms-Pfau
	 */
	private static class ProxyIntegrator {

		private static final String OLD_KEY = "http.proxyUsername";
		private static final String NEW_KEY = RapidMiner.PROPERTY_RAPIDMINER_SOCKS_VERSION;

		/**
		 * Update an old installation
		 * <p>
		 * Copies the old native properties into the new RapidMiner properties
		 * </p>
		 *
		 */
		private static void updateOldInstallation() {
			if (ParameterService.getParameterValue(OLD_KEY) != null && ParameterService.getParameterValue(NEW_KEY) == null) {
				LogService.getRoot().log(Level.INFO, "com.rapidminer.tools.ProxyService.migrate");
				// Copy from old System properties to new RapidMiner properties
				copyParameterValues(toNative(PROXY_HOSTS), PROXY_HOSTS);
				copyParameterValues(toNative(PROXY_PORTS), PROXY_PORTS);

				// merge exclusionRules together
				HashSet<String> rules = new LinkedHashSet<>();
				for (String ruleKey : PROXY_RULES) {
					String rule = System.getProperty(ruleKey);
					if (rule != null && !"".equals(rule)) {
						rules.addAll(Arrays.asList(rule.split("\\|")));
					}
				}
				String exclusionRule = String.join("|", rules);
				setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_PROXY_EXCLUDE, exclusionRule);
				setSystemValue(exclusionRule, PROXY_RULES);
			}
		}

		/**
		 * Copies the parameter values from source to target
		 * <p>
		 * Warning: both arrays must have the same length.
		 * </p>
		 *
		 * @param sourceKeys
		 *            ParameterService keys
		 * @param targetKeys
		 *            ParameterService keys
		 */
		private static void copyParameterValues(String[] sourceKeys, String[] targetKeys) {
			for (int i = 0; i < sourceKeys.length; i++) {
				String sourceValue = ParameterService.getParameterValue(sourceKeys[i]);
				setParameterValue(targetKeys[i], sourceValue);
			}
		}

		/**
		 * Set a value to the given ParameterService key
		 *
		 * @param key the key
		 * @param value the value
		 */
		private static void setParameterValue(String key, String value) {
			if (value != null && !value.isEmpty()) {
				ParameterService.setParameterValue(key, value);
			}
		}

	}

	/**
	 * Helper Class to support Mac OS X
	 */
	private static class SystemSettings {

		private final HashMap<String, String> settings = new HashMap<>();

		/**
		 * Stores the current System property or the default value for the given key
		 *
		 * @param key the key
		 */
		private void store(String key) {
			settings.putIfAbsent(key, System.getProperty(key, DEFAULT));
		}

		/**
		 * Stores the default value for the given key
		 *
		 * @param key the key
		 */
		private void storeDefault(String key) {
			settings.putIfAbsent(key, DEFAULT);
		}

		/**
		 * Applies all stored settings to the system
		 */
		private void apply() {
			settings.forEach(System::setProperty);
		}

	}

}
