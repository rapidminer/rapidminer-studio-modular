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

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.tools.belt.expression.Constant;
import com.rapidminer.tools.belt.expression.ExpressionType;


/**
 * A {@link Constant} that supplies constructors for all admissible combinations of its fields.
 *
 * @author Gisa Meier
 * @since 9.11
 */
public class SimpleConstant implements Constant {

	private static final String NAME_NULL_ERROR_MESSAGE = "name must not be null";
	private final ExpressionType type;
	private final String name;
	private final String stringValue;
	private final double doubleValue;
	private final boolean booleanValue;
	private final Instant instantValue;
	private final LocalTime localTimeValue;
	private final StringSet stringSetValue;
	private final StringList stringListValue;
	private String annotation;
	private boolean invisible = false;

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringValue
	 *            the string value
	 */
	public SimpleConstant(String name, String stringValue) {
		if (name == null) {
			throw new IllegalArgumentException(NAME_NULL_ERROR_MESSAGE);
		}
		this.type = ExpressionType.STRING;
		this.name = name;
		this.stringValue = stringValue;
		this.doubleValue = 0;
		this.booleanValue = false;
		this.instantValue = null;
		localTimeValue = null;
		stringSetValue = null;
		stringListValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringValue
	 *            the string value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, String stringValue, String annotation) {
		this(name, stringValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringValue
	 *            the string value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, String stringValue, String annotation, boolean invisible) {
		this(name, stringValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param doubleValue
	 *            the double value
	 */
	public SimpleConstant(String name, double doubleValue) {
		if (name == null) {
			throw new IllegalArgumentException(NAME_NULL_ERROR_MESSAGE);
		}
		this.type = ExpressionType.DOUBLE;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = doubleValue;
		this.booleanValue = false;
		this.instantValue = null;
		localTimeValue = null;
		stringSetValue = null;
		stringListValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param doubleValue
	 *            the double value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, double doubleValue, String annotation) {
		this(name, doubleValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param doubleValue
	 *            the double value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, double doubleValue, String annotation, boolean invisible) {
		this(name, doubleValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param booleanValue
	 *            the boolean value
	 */
	public SimpleConstant(String name, boolean booleanValue) {
		if (name == null) {
			throw new IllegalArgumentException(NAME_NULL_ERROR_MESSAGE);
		}
		this.type = ExpressionType.BOOLEAN;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = 0;
		this.booleanValue = booleanValue;
		this.instantValue = null;
		localTimeValue = null;
		stringSetValue = null;
		stringListValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param booleanValue
	 *            the boolean value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, boolean booleanValue, String annotation) {
		this(name, booleanValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param booleanValue
	 *            the boolean value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, boolean booleanValue, String annotation, boolean invisible) {
		this(name, booleanValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param instantValue
	 *            the instant value
	 */
	public SimpleConstant(String name, Instant instantValue) {
		if (name == null) {
			throw new IllegalArgumentException(NAME_NULL_ERROR_MESSAGE);
		}
		this.type = ExpressionType.INSTANT;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = 0;
		this.booleanValue = false;
		this.instantValue = instantValue;
		localTimeValue = null;
		stringSetValue = null;
		stringListValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param instantValue
	 *            the instant value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, Instant instantValue, String annotation) {
		this(name, instantValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param instantValue
	 *            the instant value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, Instant instantValue, String annotation, boolean invisible) {
		this(name, instantValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param localTimeValue
	 *            the local time value
	 */
	public SimpleConstant(String name, LocalTime localTimeValue) {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		this.type = ExpressionType.LOCAL_TIME;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = 0;
		this.booleanValue = false;
		this.localTimeValue = localTimeValue;
		instantValue = null;
		stringSetValue = null;
		stringListValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param localTimeValue
	 *            the local time value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, LocalTime localTimeValue, String annotation) {
		this(name, localTimeValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param localTimeValue
	 *            the local time value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, LocalTime localTimeValue, String annotation, boolean invisible) {
		this(name, localTimeValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringSetValue
	 *            the string set value
	 */
	public SimpleConstant(String name, StringSet stringSetValue) {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		this.type = ExpressionType.STRING_SET;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = 0;
		this.booleanValue = false;
		this.stringSetValue = stringSetValue;
		instantValue = null;
		localTimeValue = null;
		stringListValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringSetValue
	 *            the string set value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, StringSet stringSetValue, String annotation) {
		this(name, stringSetValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringSetValue
	 *            the string set value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, StringSet stringSetValue, String annotation, boolean invisible) {
		this(name, stringSetValue, annotation);
		this.invisible = invisible;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringListValue
	 *            the string list value
	 */
	public SimpleConstant(String name, StringList stringListValue) {
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		this.type = ExpressionType.STRING_LIST;
		this.name = name;
		this.stringValue = null;
		this.doubleValue = 0;
		this.booleanValue = false;
		this.stringListValue = stringListValue;
		instantValue = null;
		localTimeValue = null;
		stringSetValue = null;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringListValue
	 *            the string list value
	 * @param annotation
	 *            an optional annotation
	 */
	public SimpleConstant(String name, StringList stringListValue, String annotation) {
		this(name, stringListValue);
		this.annotation = annotation;
	}

	/**
	 * Creates a Constant with the given characteristics.
	 *
	 * @param name
	 *            the name of the constant, cannot be {@code null}
	 * @param stringListValue
	 *            the string list value
	 * @param annotation
	 *            an optional annotation
	 * @param invisible
	 *            option to hide a constant in the UI but recognize it in the parser
	 */
	public SimpleConstant(String name, StringList stringListValue, String annotation, boolean invisible) {
		this(name, stringListValue, annotation);
		this.invisible = invisible;
	}

	@Override
	public ExpressionType getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getStringValue() {
		if (type == ExpressionType.STRING) {
			return stringValue;
		} else {
			throw new IllegalStateException("element is not of type String");
		}
	}

	@Override
	public double getDoubleValue() {
		if (type == ExpressionType.DOUBLE) {
			return doubleValue;
		} else {
			throw new IllegalStateException("element is not of type double");
		}
	}

	@Override
	public boolean getBooleanValue() {
		if (type == ExpressionType.BOOLEAN) {
			return booleanValue;
		} else {
			throw new IllegalStateException("element is not of type boolean");
		}
	}

	@Override
	public Instant getInstantValue() {
		if (type == ExpressionType.INSTANT) {
			return instantValue;
		} else {
			throw new IllegalStateException("element is not of type instant");
		}
	}

	@Override
	public LocalTime getLocalTimeValue() {
		if (type == ExpressionType.LOCAL_TIME) {
			return localTimeValue;
		} else {
			throw new IllegalStateException("element is not of type local time");
		}
	}

	@Override
	public StringSet getStringSetValue() {
		if (type == ExpressionType.STRING_SET) {
			return stringSetValue;
		} else {
			throw new IllegalStateException("element is not of type string set");
		}
	}

	@Override
	public StringList getStringListValue() {
		if (type == ExpressionType.STRING_LIST) {
			return stringListValue;
		} else {
			throw new IllegalStateException("element is not of type string list");
		}
	}

	@Override
	public String getAnnotation() {
		return annotation;
	}

	/**
	 * Sets the annotation of this constant.
	 *
	 * @param annotation
	 *            the annotation to set
	 */
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	@Override
	public boolean isInvisible() {
		return invisible;
	}

}
