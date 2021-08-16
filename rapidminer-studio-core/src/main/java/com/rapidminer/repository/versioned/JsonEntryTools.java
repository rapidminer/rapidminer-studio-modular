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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;


/**
 * Method to set polymorphic type validator. Extracted from {@link JsonIOObjectEntry} so that it still works with a
 * jackson 2.8 runtime which might be used for Spark pushdown.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
enum JsonEntryTools {

	; //no instance enum

	/**
	 * Set type validator to the objectMapper that forbids all {@code @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)},
	 * {@code Id.NAME} still allowed.
	 *
	 * @param objectMapper
	 * 		the mapper that should not allow class type info
	 */
	public static void forbidJsonClassType(ObjectMapper objectMapper) {

		objectMapper.setPolymorphicTypeValidator(new PolymorphicTypeValidator() {
			@Override
			public Validity validateBaseType(MapperConfig<?> config, JavaType baseType) {
				//setting this to indeterminate allows null fields
				return Validity.INDETERMINATE;
			}

			@Override
			public Validity validateSubClassName(MapperConfig<?> config, JavaType baseType, String subClassName) {
				return Validity.DENIED;
			}

			@Override
			public Validity validateSubType(MapperConfig<?> config, JavaType baseType, JavaType subType) {
				return Validity.DENIED;
			}
		});

	}

}