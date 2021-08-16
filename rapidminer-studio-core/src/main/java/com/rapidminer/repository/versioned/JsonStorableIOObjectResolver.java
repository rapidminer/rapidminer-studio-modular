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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.operator.learner.functions.FunctionFittingModel;
import com.rapidminer.operator.performance.PerformanceVector;


/**
 * Used to resolve json type ids to (sub)classes for {@link JsonStorableIOObject}s stored as json with jackson in a
 * subclass of {@link com.rapidminer.repository.versioned.JsonIOObjectEntry JsonIOObjectEntry}. Every (non-abstract)
 * subclass of a class registered via its subclass of {@link com.rapidminer.repository.versioned.JsonStorableIOObjectHandler
 * JsonStorableIOObjectHandler} must be registered here.
 *
 * @author Gisa Meier
 * @see JsonStorableIOObject
 * @since 9.10.0
 */
public class JsonStorableIOObjectResolver extends JsonResolverWithRegistry<JsonStorableIOObject> {

	private static final Map<String, Class<? extends JsonStorableIOObject>> nameToClass = new ConcurrentHashMap<>();
	private static final Map<Class<? extends JsonStorableIOObject>, String> classToName = new ConcurrentHashMap<>();

	public static final JsonStorableIOObjectResolver INSTANCE = new JsonStorableIOObjectResolver();

	static {
		//register core classes here
		INSTANCE.register(PerformanceVector.class);
		INSTANCE.register(AttributeWeights.class);
		INSTANCE.register(FunctionFittingModel.class);
	}

	protected JsonStorableIOObjectResolver() {
		super(nameToClass, classToName);
	}
}