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
package com.rapidminer.tools.belt.expression.internal.antlr;

import java.time.Instant;
import java.time.LocalTime;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FatalExpressionException;


/**
 * A basic {@link Expression}. Not thread safe.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 *
 */
class SimpleExpression implements Expression {

	private ExpressionEvaluator evaluator;

	/**
	 * Creates a basic expression based on the evaluator.
	 *
	 * @param evaluator
	 *            the evaluator to use for evaluating the expression
	 */
	SimpleExpression(ExpressionEvaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public ExpressionType getExpressionType() {
		return evaluator.getType();
	}

	@Override
	public String evaluateNominal() throws ExpressionException {
		try {
			switch (evaluator.getType()) {
				case BOOLEAN:
					Boolean result = evaluator.getBooleanFunction().call();
					return result == null ? null : result.toString();
				case STRING:
					return evaluator.getStringFunction().call();
				default:
					throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
							+ " as nominal");
			}
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionExceptionWrapper e) {
			throw e.unwrap();
		} catch (Exception e) {
			throw new FatalExpressionException(e);
		}
	}

	@Override
	public double evaluateNumerical() throws ExpressionException {
		try {
			switch (evaluator.getType()) {
				case DOUBLE:
				case INTEGER:
					return evaluator.getDoubleFunction().call();
				default:
					throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
							+ " as numerical");
			}
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionExceptionWrapper e) {
			throw e.unwrap();
		} catch (Exception e) {
			throw new FatalExpressionException(e);
		}
	}

	@Override
	public Instant evaluateInstant() throws ExpressionException {
		try {
			if (evaluator.getType() == ExpressionType.INSTANT) {
				return evaluator.getInstantFunction().call();
			}
			throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
					+ " as date-time");
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionExceptionWrapper e) {
			throw e.unwrap();
		} catch (Exception e) {
			throw new FatalExpressionException(e);
		}
	}

	@Override
	public LocalTime evaluateLocalTime() throws ExpressionException {
		try {
			if (evaluator.getType() == ExpressionType.LOCAL_TIME) {
				return evaluator.getLocalTimeFunction().call();
			}
			throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
					+ " as time");
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionExceptionWrapper e) {
			throw e.unwrap();
		} catch (Exception e) {
			throw new FatalExpressionException(e);
		}
	}

	@Override
	public StringSet evaluateStringSet() throws ExpressionException {
		try {
			if (evaluator.getType() == ExpressionType.STRING_SET) {
				return evaluator.getStringSetFunction().call();
			}
			throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
					+ " as text set");
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionExceptionWrapper e) {
			throw e.unwrap();
		} catch (Exception e) {
			throw new FatalExpressionException(e);
		}
	}

	@Override
	public StringList evaluateStringList() throws ExpressionException {
		try {
			if (evaluator.getType() == ExpressionType.STRING_LIST) {
				return evaluator.getStringListFunction().call();
			}
			throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
					+ " as text list");
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionExceptionWrapper e) {
			throw e.unwrap();
		} catch (Exception e) {
			throw new FatalExpressionException(e);
		}
	}

	@Override
	public Boolean evaluateBoolean() throws ExpressionException {
		try {
			if (evaluator.getType() == ExpressionType.BOOLEAN) {
				return evaluator.getBooleanFunction().call();
			}
			throw new IllegalArgumentException("Cannot evaluate expression of type " + getExpressionType()
					+ " as boolean");
		} catch (ExpressionException e) {
			throw e;
		} catch (ExpressionExceptionWrapper e) {
			throw e.unwrap();
		} catch (Exception e) {
			throw new FatalExpressionException(e);
		}
	}

}
