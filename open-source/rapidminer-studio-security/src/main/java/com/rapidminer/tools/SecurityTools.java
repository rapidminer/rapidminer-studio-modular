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
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedActionException;


/**
 * Tools around the security subsystem of Studio.
 *
 * @author Marco Boeck
 * @since 9.8.0
 */
public class SecurityTools {

	public static final String RAPIDMINER_INTERNAL_PERMISSION = "accessClassInPackage.rapidminer.internal";

	/**
	 * Checks if the caller has the {@link SecurityTools#RAPIDMINER_INTERNAL_PERMISSION}.
	 *
	 * @throws UnsupportedOperationException
	 * 		if the caller is not signed
	 */
	public static void requireInternalPermission() {
		try {
			if (System.getSecurityManager() != null) {
				AccessController.checkPermission(new RuntimePermission(RAPIDMINER_INTERNAL_PERMISSION));
			}
		} catch (AccessControlException e) {
			throw new UnsupportedOperationException("Internal API, cannot be called by unauthorized sources.", e);
		}
	}

	/**
	 * Transforms the given {@link PrivilegedActionException} to an {@link IOException} or {@link RuntimeException} and
	 * throws it. Never returns, but always throws. This method is generic so it can also be used in methods that expect
	 * a return value.
	 *
	 * @param pException the privileged exception to transform
	 * @param <T>        a generic parameter to allow usage in cases where a return value is expected
	 * @return nothing; always throws
	 * @throws IOException always throws an appropriate {@link IOException} or {@link RuntimeException}
	 * @since 9.8
	 */
	public static <T> T handlePrivilegedExceptionToIO(PrivilegedActionException pException) throws IOException {
		Exception innerException = pException.getException();
		if (innerException instanceof IOException) {
			throw (IOException) innerException;
		} else if (innerException instanceof RuntimeException) {
			throw (RuntimeException) innerException;
		} else {
			throw new IOException(innerException);
		}
	}
}
