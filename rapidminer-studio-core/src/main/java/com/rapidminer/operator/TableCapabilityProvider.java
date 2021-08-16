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
package com.rapidminer.operator;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import com.rapidminer.belt.table.Table;
import com.rapidminer.tools.LogService;


/**
 * Interface to define the capabilities for operators with respect to tables they can handle via specifying supported
 * and unsupported {@link TableCapability} sets. The capabilities automatically are displayed in the info screen and
 * global search, but to check them {@link TableCapabilityCheck#checkCapabilities(Table, Operator)} needs to be called
 * for table data and {@link com.rapidminer.operator.ports.metadata.TableCapabilityPrecondition} used for meta data
 * checks.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public interface TableCapabilityProvider {

	/**
	 * Returns the capabilities supported by this provider. Capabilities that are neither supported nor unsupported are
	 * unknown and not taken into account.
	 *
	 * @return the supported capabilities
	 */
	Set<TableCapability> supported();

	/**
	 * Returns the capabilities unsupported by this provider. Capabilities that are neither supported nor unsupported
	 * are unknown and not taken into account.
	 *
	 * @return the unsupported capabilities
	 */
	Set<TableCapability> unsupported();

	/**
	 * Whether the provider is a leaner and thus all {@link TableCapability capabilities} should be checked instead of
	 * only those which are also for non-learners. Returns {@code false} by default.
	 *
	 * @return whether the provider is a learner
	 */
	default boolean isLearner() {
		return false;
	}

	/**
	 * Checks if the supported and unsupported sets defined by the provider are consistent. Called by the {@link
	 * TableCapabilityCheck}.
	 *
	 * @param throwOnUnknown
	 * 		whether to throw an exception for unknown capabilities, otherwise they are logged
	 */
	default void checkCompatible(boolean throwOnUnknown) {
		Set<TableCapability> supported = supported();
		Set<TableCapability> unsupported = unsupported();
		if (supported == null || supported.isEmpty()) {
			supported = EnumSet.noneOf(TableCapability.class);
		}
		if (unsupported == null || unsupported.isEmpty()) {
			unsupported = EnumSet.noneOf(TableCapability.class);
		}
		Set<TableCapability> intersectionCheck = EnumSet.copyOf(supported);
		boolean intersected = intersectionCheck.removeAll(unsupported);
		if (intersected) {
			String message = "supported and unsupported are not disjoint";
			if (this instanceof Operator) {
				message += " for operator " + ((Operator) this).getName();
			}
			throw new IllegalStateException(message);
		}
		Set<TableCapability> all = EnumSet.copyOf(Arrays.asList(TableCapability.values(this)));
		all.removeAll(supported);
		all.removeAll(unsupported);
		if (!all.isEmpty()) {
			String message = "Capabilities " + all + " are unknown";
			if (this instanceof Operator) {
				message += " for operator " + ((Operator) this).getName();
			}
			if (throwOnUnknown) {
				throw new IllegalStateException(message);
			} else {
				LogService.getRoot().warning(message);
			}
		}
	}
}