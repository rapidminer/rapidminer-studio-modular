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

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;


/**
 * A simple {@link ProxySelector} decorator/wrapper.
 *
 * @author Jan Czogalla
 * @since 9.8
 */
public class SimpleProxySelectorWrapper extends ProxySelector {

	private ProxySelector delegate;

	public SimpleProxySelectorWrapper(ProxySelector delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<Proxy> select(URI uri) {
		return delegate.select(uri);
	}

	@Override
	public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
		delegate.connectFailed(uri, sa, ioe);
	}

	public ProxySelector getDelegate() {
		return delegate;
	}

	public void setDelegate(ProxySelector delegate) {
		this.delegate = delegate;
	}
}
