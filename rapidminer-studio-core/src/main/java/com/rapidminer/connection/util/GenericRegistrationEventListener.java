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
package com.rapidminer.connection.util;

import java.util.EventListener;

/**
 * A generic {@link EventListener} for registrations, uses {@link RegistrationEvent RegistrationEvents}
 *
 * @author Jan Czogalla
 * @see GenericHandlerRegistry
 * @since 9.3
 */
public interface GenericRegistrationEventListener<H extends GenericHandler> extends EventListener {

	/**
	 * Process the changed object
	 *
	 * @param event
	 * 		the event; should not be {@code null}
	 * @param changedObject
	 * 		the object that changed; should not be {@code null}
	 */
	void registrationChanged(RegistrationEvent event, H changedObject);
}