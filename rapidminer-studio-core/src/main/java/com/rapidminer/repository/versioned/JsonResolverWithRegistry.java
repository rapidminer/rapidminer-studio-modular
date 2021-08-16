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

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;


/**
 * Used to resolve json type ids to (sub)classes of {@code T} stored as json with jackson. Subclasses should implement a
 * singleton approach and keep static concurrent maps that are provided in each creation of an instance.
 *
 * @param <T> the base class/interface this resolver is annotated at
 * @author Gisa Meier
 * @see JsonStorableIOObject
 * @since 9.10.0
 */
class JsonResolverWithRegistry<T> extends TypeIdResolverBase {

	/**
	 * Marker exception for not registered classes.
	 */
	static final class NotRegisteredException extends IllegalArgumentException{

		private final String className;

		private NotRegisteredException(String className){
			this.className = className;
		}

		String getClassName(){
			return className;
		}
	}

	private final Map<String, Class<? extends T>> nameToClass;
	private final Map<Class<? extends T>, String> classToName;

	private JavaType superType;

	JsonResolverWithRegistry(Map<String, Class<? extends T>> nameToClass, Map<Class<? extends T>, String> classToName) {
		this.nameToClass = nameToClass;
		this.classToName = classToName;
	}

	@Override
	public void init(JavaType baseType) {
		superType = baseType;
	}

	@Override
	public String idFromValue(Object obj) {
		return idFromValueAndType(obj, obj.getClass());
	}

	@Override
	public String idFromValueAndType(Object o, Class<?> aClass) {
		String id = classToName.get(aClass);
		if (id == null) {
			throw new NotRegisteredException(aClass.getName());
		}
		return id;
	}

	@Override
	public JavaType typeFromId(DatabindContext context, String id) throws IOException {
		Class<? extends T> aClass = nameToClass.get(id);
		if (aClass == null) {
			throw new NotRegisteredException(id);
		}
		return context.constructSpecializedType(superType, aClass);
	}

	@Override
	public JsonTypeInfo.Id getMechanism() {
		return JsonTypeInfo.Id.CUSTOM;
	}

	/**
	 * Registers a subclass of {@link JsonStorableIOObject} so that it can be written and read via an {@link
	 * JsonIOObjectEntry}.
	 *
	 * @param clazz
	 * 		the class to register
	 */
	public void register(Class<? extends T> clazz) {
		nameToClass.put(clazz.getName(), clazz);
		classToName.put(clazz, clazz.getName());
	}

	/**
	 * Unregisters the class. It cannot be written or read anymore.
	 *
	 * @param clazz
	 * 		the class to unregister
	 */
	public void unregister(Class<? extends T> clazz) {
		nameToClass.remove(clazz.getName());
		classToName.remove(clazz);
	}
}
