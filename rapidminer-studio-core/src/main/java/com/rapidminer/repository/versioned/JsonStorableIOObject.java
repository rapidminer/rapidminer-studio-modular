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

package com.rapidminer.repository.versioned;

import static com.rapidminer.repository.versioned.JsonIOObjectEntry.TYPE;
import static com.rapidminer.repository.versioned.JsonIOObjectEntry.VERSION;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.rapidminer.operator.IOObject;


/**
 * Marker interface for {@link IOObject}s that are json storable in the filesystem repository using a {@link
 * JsonIOObjectEntry}. Also provides link to the json type id resolution class {@link JsonStorableIOObjectResolver}
 * where (non-abstract) subclasses need to be registered. The json storable ioobjects can use jackson json annotations
 * to define the format but are not allowed to use {@code @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)}, otherwise reading
 * again will not work.
 * <p>
 * To facilitate the storing as a {@link JsonIOObjectEntry} a subclass of
 * {@link com.rapidminer.repository.versioned.JsonStorableIOObjectHandler}
 * must be created for the json storable ioobject and {@link IOObjectFileTypeHandler#register() registered}, for example
 * in the {@link FilesystemRepositoryAdapter}.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = TYPE)
@JsonTypeIdResolver(JsonStorableIOObjectResolver.class)
@JsonAppend(
		prepend = true,
		attrs = {
				@JsonAppend.Attr(value = VERSION)
		}
)
public interface JsonStorableIOObject extends IOObject {
}