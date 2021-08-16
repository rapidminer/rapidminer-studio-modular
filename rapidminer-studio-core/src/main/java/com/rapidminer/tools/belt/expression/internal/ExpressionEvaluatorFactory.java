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
package com.rapidminer.tools.belt.expression.internal;

import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.Callable;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionType;


/**
 * Factory that can be used to create ExpressionEvaluator instances holding exactly one non-{@code null} callable.
 *
 * @author Kevin Majchrzak
 * @see ExpressionEvaluator
 * @since 9.11
 */
public enum ExpressionEvaluatorFactory {

	; // no instance enum

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are {@code null}. type must
	 * be ExpressionType.INTEGER or ExpressionType.DOUBLE.
	 *
	 * @param doubleCallable
	 * 		the callable to store
	 * @param isConstant
	 * 		whether the result of the callable is constant
	 * @param type
	 * 		the type of the result of the callable, must be ExpressionType.INTEGER or ExpressionType.DOUBLE
	 */
	public static ExpressionEvaluator ofDouble(DoubleCallable doubleCallable, boolean isConstant, ExpressionType type) {
		if (type != ExpressionType.DOUBLE && type != ExpressionType.INTEGER) {
			throw new IllegalArgumentException("Invalid type " + type + "for double callable");
		}
		return new AbstractExpressionEvaluator(type, isConstant) {
			@Override
			public DoubleCallable getDoubleFunction() {
				return doubleCallable;
			}
		};
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are {@code null}.
	 *
	 * @param stringCallable
	 * 		the callable to store
	 * @param isConstant
	 * 		whether the result of the callable is constant
	 */
	public static ExpressionEvaluator ofString(Callable<String> stringCallable, boolean isConstant) {
		return new AbstractExpressionEvaluator(ExpressionType.STRING, isConstant) {
			@Override
			public Callable<String> getStringFunction() {
				return stringCallable;
			}
		};
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are {@code null}.
	 *
	 * @param booleanCallable
	 * 		the callable to store
	 * @param isConstant
	 * 		whether the result of the callable is constant
	 */
	public static ExpressionEvaluator ofBoolean(Callable<Boolean> booleanCallable, boolean isConstant) {
		return new AbstractExpressionEvaluator(ExpressionType.BOOLEAN, isConstant) {
			@Override
			public Callable<Boolean> getBooleanFunction() {
				return booleanCallable;
			}
		};
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are {@code null}.
	 *
	 * @param instantCallable
	 * 		the callable to store
	 * @param isConstant
	 * 		whether the result of the callable is constant
	 */
	public static ExpressionEvaluator ofInstant(Callable<Instant> instantCallable, boolean isConstant) {
		return new AbstractExpressionEvaluator(ExpressionType.INSTANT, isConstant) {
			@Override
			public Callable<Instant> getInstantFunction() {
				return instantCallable;
			}
		};
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are {@code null}.
	 *
	 * @param localTimeCallable
	 * 		the callable to store
	 * @param isConstant
	 * 		whether the result of the callable is constant
	 */
	public static ExpressionEvaluator ofLocalTime(Callable<LocalTime> localTimeCallable, boolean isConstant) {
		return new AbstractExpressionEvaluator(ExpressionType.LOCAL_TIME, isConstant) {
			@Override
			public Callable<LocalTime> getLocalTimeFunction() {
				return localTimeCallable;
			}
		};
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are {@code null}.
	 *
	 * @param stringSetCallable
	 * 		the callable to store
	 * @param isConstant
	 * 		whether the result of the callable is constant
	 */
	public static ExpressionEvaluator ofStringSet(Callable<StringSet> stringSetCallable, boolean isConstant) {
		return new AbstractExpressionEvaluator(ExpressionType.STRING_SET, isConstant) {
			@Override
			public Callable<StringSet> getStringSetFunction() {
				return stringSetCallable;
			}
		};
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with the given data where the other callables are {@code null}.
	 *
	 * @param stringListCallable
	 * 		the callable to store
	 * @param isConstant
	 * 		whether the result of the callable is constant
	 */
	public static ExpressionEvaluator ofStringList(Callable<StringList> stringListCallable, boolean isConstant) {
		return new AbstractExpressionEvaluator(ExpressionType.STRING_LIST, isConstant) {
			@Override
			public Callable<StringList> getStringListFunction() {
				return stringListCallable;
			}
		};
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link DoubleCallable} constantly returning the given double value.
	 * type must be ExpressionType.INTEGER or ExpressionType.DOUBLE.
	 *
	 * @param doubleValue
	 * 		the constant double return value
	 * @param type
	 * 		the type of the result of the callable, must be ExpressionType.INTEGER or ExpressionType.DOUBLE
	 */
	public static ExpressionEvaluator ofDouble(double doubleValue, ExpressionType type) {
		return ExpressionEvaluatorFactory.ofDouble(makeConstantCallable(doubleValue), true, type);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<String>} constantly returning the given string
	 * value.
	 *
	 * @param stringValue
	 * 		the constant String return value
	 */
	public static ExpressionEvaluator ofString(String stringValue) {
		return ExpressionEvaluatorFactory.ofString(makeConstantCallable(stringValue), true);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<Boolean>} constantly returning the given boolean
	 * value.
	 *
	 * @param booleanValue
	 * 		the constant Boolean return value
	 */
	public static ExpressionEvaluator ofBoolean(Boolean booleanValue) {
		return ExpressionEvaluatorFactory.ofBoolean(makeConstantCallable(booleanValue), true);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<Instant>} constantly returning the given Instant
	 * value.
	 *
	 * @param instantValue
	 * 		the constant Instant return value
	 */
	public static ExpressionEvaluator ofInstant(Instant instantValue) {
		return ExpressionEvaluatorFactory.ofInstant(makeConstantCallable(instantValue), true);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<LocalTime>} constantly returning the given
	 * LocalTime value.
	 *
	 * @param timeValue
	 * 		the constant LocalTime return value
	 */
	public static ExpressionEvaluator ofLocalTime(LocalTime timeValue) {
		return ExpressionEvaluatorFactory.ofLocalTime(makeConstantCallable(timeValue), true);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<StringSet>} constantly returning the given
	 * StringSet value.
	 *
	 * @param stringSetValue
	 * 		the constant StringSet return value
	 */
	public static ExpressionEvaluator ofStringSet(StringSet stringSetValue) {
		return ExpressionEvaluatorFactory.ofStringSet(makeConstantCallable(stringSetValue), true);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a {@link Callable<StringList>} constantly returning the given
	 * StringList value.
	 *
	 * @param stringListValue
	 * 		the constant StringList return value
	 */
	public static ExpressionEvaluator ofStringList(StringList stringListValue) {
		return ExpressionEvaluatorFactory.ofStringList(makeConstantCallable(stringListValue), true);
	}

	private static DoubleCallable makeConstantCallable(final double doubleValue) {
		return () -> doubleValue;
	}

	private static <V> Callable<V> makeConstantCallable(V value) {
		return () -> value;
	}

}
