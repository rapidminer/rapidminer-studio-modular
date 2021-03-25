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
package com.rapidminer.operator.ports.quickfix;

import java.security.AccessControlException;
import java.security.AccessController;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.security.PluginSandboxPolicy;


/**
 * Used to register quickfix suppliers and to create quickfixes using the registered suppliers.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
public class QuickFixSupplier {

	/**
	 * Quickfix supplier that returns a quickfix for the case that a mandatory role has not been set by the user.
	 */
	public interface SetRoleQuickFixSupplier {

		/**
		 * Returns a quickfix for the case that a mandatory role has not been set by the user.
		 *
		 * @param inputPort
		 * 		input port of the operator that is missing the mandatory role
		 * @param role
		 * 		the missing mandatory role
		 * @param i18nKey
		 * 		used for the user message
		 * @param i18nArgs
		 * 		used for the user message
		 * @return new instance of Quickfix
		 */
		QuickFix get(InputPort inputPort, String role, String i18nKey, Object... i18nArgs);
	}

	/**
	 * The set role operator is part of the blending extension. Therefore, we use a supplier pattern and fall back to
	 * the old operator if the pattern fails.
	 */
	private static SetRoleQuickFixSupplier quickFixSupplier = ChangeAttributeRoleQuickFix::new;

	/**
	 * Returns a quickfix for the case that a mandatory role has not been set by the user.
	 *
	 * @param inputPort
	 * 		input port of the operator that is missing the mandatory role
	 * @param role
	 * 		the missing mandatory role
	 * @param i18nKey
	 * 		used for the user message
	 * @param i18nArgs
	 * 		used for the user message
	 * @return new instance of Quickfix
	 */
	public static QuickFix getSetRoleQuickFix(InputPort inputPort, String role, String i18nKey, Object... i18nArgs) {
		return quickFixSupplier.get(inputPort, role, i18nKey, i18nArgs);
	}

	/**
	 * Sets the {@link SetRoleQuickFixSupplier} used to get a quickfix if a mandatory attribute role has not been set by
	 * the user.
	 *
	 * @param supplier
	 * 		will be used to get a set role quickfix that will be used if a mandatory attribute role has not been set by the
	 * 		user.
	 */
	public static void registerQuickFixSupplier(SetRoleQuickFixSupplier supplier) {
		try {
			if (System.getSecurityManager() != null) {
				AccessController
						.checkPermission(new RuntimePermission(PluginSandboxPolicy.RAPIDMINER_INTERNAL_PERMISSION));
			}
		} catch (AccessControlException e) {
			return;
		}
		quickFixSupplier = supplier;
	}
}
