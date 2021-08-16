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
package com.rapidminer.tools.belt.expression.internal.function.logical;

import java.time.Instant;
import java.util.concurrent.Callable;

import com.rapidminer.tools.Tools;
import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FatalExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.FunctionDescription;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.internal.ExpressionEvaluatorFactory;
import com.rapidminer.tools.belt.expression.internal.function.AbstractFunction;


/**
 * Class for the IF function that has one logical (numerical, true or false) input and two arbitrary inputs
 *
 * @author Sabrina Kirstein
 * @since 9.11
 */
public class If extends AbstractFunction {

	// TODO add new types

	/**
	 * Constructs an IF Function with 3 parameters with {@link FunctionDescription}
	 */
	public If() {
		super("logical.if", 3, null);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context,
									   ExpressionEvaluator... inputEvaluators) {

		if (inputEvaluators.length != 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), 3,
					inputEvaluators.length);
		}

		ExpressionEvaluator condition = inputEvaluators[0];
		ExpressionEvaluator ifCase = inputEvaluators[1];
		ExpressionEvaluator elseCase = inputEvaluators[2];

		ExpressionType type = null;

		// if we know that the condition is constant, check the type based on the condition
		if (condition.isConstant()) {

			if (condition.getType() != ExpressionType.INTEGER && condition.getType() != ExpressionType.DOUBLE
					&& condition.getType() != ExpressionType.BOOLEAN) {
				throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type.argument_two", 1, getFunctionName(),
						"boolean", "numerical");
			}

			Boolean cond = getCondition(condition);
			if (cond != null) {
				type = computeType(cond, ifCase.getType(), elseCase.getType());
			}
		}

		// if the condition is not constant, make the best guess given the types of the second and
		// their ExpressionEvaluator
		if (type == null) {
			type = getResultType(inputEvaluators);
		}

		// return the callables based on the return type that was computed before
		switch (type) {
			// TODO add new types
			case DOUBLE:
			case INTEGER:
				DoubleCallable doubleCallable = makeDoubleCallable(condition, ifCase, elseCase);
				return ExpressionEvaluatorFactory.ofDouble(doubleCallable, isResultConstant(inputEvaluators), type);
			case BOOLEAN:
				Callable<Boolean> booleanCallable = makeBooleanCallable(condition, ifCase, elseCase);
				return ExpressionEvaluatorFactory.ofBoolean(booleanCallable, isResultConstant(inputEvaluators));
			case INSTANT:
				Callable<Instant> dateCallable = makeDateCallable(condition, ifCase, elseCase);
				return ExpressionEvaluatorFactory.ofInstant(dateCallable, isResultConstant(inputEvaluators));
			case STRING:
			default:
				Callable<String> stringCallable = makeStringCallable(condition, ifCase, elseCase);
				return ExpressionEvaluatorFactory.ofString(stringCallable, isResultConstant(inputEvaluators));
		}
	}

	/**
	 * Builds a boolean callable from the given evaluators
	 *
	 * @param condition
	 *            evaluator
	 * @param ifBlock
	 *            evaluator
	 * @param elseBlock
	 *            evaluator
	 * @return the resulting boolean callable
	 */
	protected Callable<Boolean> makeBooleanCallable(final ExpressionEvaluator condition, final ExpressionEvaluator ifBlock,
			final ExpressionEvaluator elseBlock) {

		// if we know that the condition is constant, make the callable based on the selected case
		if (condition.isConstant()) {
			// check the condition value
			Boolean cond = getCondition(condition);
			if (cond != null) {
				// check the condition value
				if (cond) {
					// return Callable<Boolean> from the ifCase
					// if this block is constant, just return the value
					if (ifBlock.isConstant()) {
						try {
							final Boolean value = ifBlock.getBooleanFunction().call();
							return () -> value;
						} catch (ExpressionExceptionWrapper e) {
							throw e;
						} catch (Exception e) {
							throw new FatalExpressionExceptionWrapper(e);
						}
					} else {
						// if it isnt constant, return the function result
						return () -> ifBlock.getBooleanFunction().call();
					}
				} else {
					// return Callable<Boolean> from the elseCase
					// if this block is constant, just return the value
					if (elseBlock.isConstant()) {
						try {
							final Boolean value = elseBlock.getBooleanFunction().call();
							return () -> value;
						} catch (ExpressionExceptionWrapper e) {
							throw e;
						} catch (Exception e) {
							throw new FatalExpressionExceptionWrapper(e);
						}
					} else {
						// if it isnt constant, return the function result
						return () -> elseBlock.getBooleanFunction().call();
					}
				}
			} else {
				return () -> null;
			}
		} else {
			// create a Callable<Boolean> that checks whether the condition is given and calls
			// the if or else part
			return () -> {
				Boolean cond = getCondition(condition);
				if (cond == null) {
					return null;
				}
				if (cond) {
					return ifBlock.getBooleanFunction().call();
				} else {
					return elseBlock.getBooleanFunction().call();
				}
			};
		}

	}

	/**
	 * Builds a String callable from the given evaluators
	 *
	 * @param condition
	 *            evaluator
	 * @param ifBlock
	 *            evaluator
	 * @param elseBlock
	 *            evaluator
	 * @return the resulting String callable
	 */
	protected Callable<String> makeStringCallable(final ExpressionEvaluator condition, final ExpressionEvaluator ifBlock,
			final ExpressionEvaluator elseBlock) {
		// if we know that the condition is constant, make the callable based on the selected case
		if (condition.isConstant()) {
			// check the condition value
			Boolean cond = getCondition(condition);
			if (cond != null) {
				if (cond) {
					// return Callable<String> from the ifCase
					// if this block is constant, just return the value
					if (ifBlock.isConstant()) {
						try {
							final String value = ifBlock.getStringFunction().call();
							return () -> value;
						} catch (ExpressionExceptionWrapper e) {
							throw e;
						} catch (Exception e) {
							throw new FatalExpressionExceptionWrapper(e);
						}
					} else {
						// if it isnt constant, return the function result
						return () -> ifBlock.getStringFunction().call();
					}
				} else {
					// return Callable<String> from the elseCase
					// if this block is constant, just return the value
					if (elseBlock.isConstant()) {
						try {
							final String value = elseBlock.getStringFunction().call();
							return () -> value;
						} catch (ExpressionExceptionWrapper e) {
							throw e;
						} catch (Exception e) {
							throw new FatalExpressionExceptionWrapper(e);
						}
					} else {
						// if it isnt constant, return the function result
						return () -> elseBlock.getStringFunction().call();
					}
				}
			} else {
				return () -> null;
			}
		} else {
			// create a Callable<String> that checks whether the condition is given and calls the
			// if or else part
			return () -> {
				Boolean cond = getCondition(condition);
				if (cond == null) {
					return null;
				}
				if (cond) {
					switch (ifBlock.getType()) {
						// TODO add new types here and elsewhere
						case BOOLEAN:
							return convertToString(ifBlock.getBooleanFunction().call());
						case INTEGER:
							return convertToString(ifBlock.getDoubleFunction().call(), true);
						case DOUBLE:
							return convertToString(ifBlock.getDoubleFunction().call(), false);
						case INSTANT:
							return convertToString(ifBlock.getInstantFunction().call());
						case STRING:
						default:
							return ifBlock.getStringFunction().call();
					}
				} else {
					switch (elseBlock.getType()) {
						case BOOLEAN:
							return convertToString(elseBlock.getBooleanFunction().call());
						case INTEGER:
							return convertToString(elseBlock.getDoubleFunction().call(), true);
						case DOUBLE:
							return convertToString(elseBlock.getDoubleFunction().call(), false);
						case INSTANT:
							return convertToString(elseBlock.getInstantFunction().call());
						case STRING:
						default:
							return elseBlock.getStringFunction().call();
					}
				}
			};
		}
	}

	/**
	 * Converts the object into a String, return {@code null} if the object is {@code null}.
	 */
	private String convertToString(Object object) {
		if (object == null) {
			return null;
		}
		return object.toString();
	}

	/**
	 * Converts the value into a string, returning {@code null} if the value is Double.NaN,
	 * formatting infinity via a symbol and casting to int if possible and the value is supposed to
	 * represent an integer.
	 *
	 * @param value
	 * @param isInteger
	 *            whether the double value represents an integer
	 * @return the value converted to an integer
	 */
	private String convertToString(double value, boolean isInteger) {
		if (Double.isNaN(value)) {
			return null;
		} else if (Double.isInfinite(value)) {
			return Tools.formatNumber(value);
		} else if (isInteger && value == (int) value) {
			return Integer.toString((int) value);
		} else {
			return Double.toString(value);
		}
	}

	/**
	 * Builds a Date callable from the given evaluators
	 *
	 * @param condition
	 *            evaluator
	 * @param ifBlock
	 *            evaluator
	 * @param elseBlock
	 *            evaluator
	 * @return the resulting Date callable
	 */
	protected Callable<Instant> makeDateCallable(final ExpressionEvaluator condition, final ExpressionEvaluator ifBlock,
			final ExpressionEvaluator elseBlock) {

		// if we know that the condition is constant, make the callable based on the selected case
		if (condition.isConstant()) {
			// check the condition value
			Boolean cond = getCondition(condition);
			if (cond != null) {
				if (cond) {
					// return Callable<Date> from the ifCase
					// if this block is constant, just return the value
					if (ifBlock.isConstant()) {
						try {
							final Instant value = ifBlock.getInstantFunction().call();
							return () -> value;
						} catch (ExpressionExceptionWrapper e) {
							throw e;
						} catch (Exception e) {
							throw new FatalExpressionExceptionWrapper(e);
						}
					} else {
						// if it isnt constant, return the function result
						return () -> ifBlock.getInstantFunction().call();
					}
				} else {
					// return Callable<Date> from the elseCase
					// if this block is constant, just return the value
					if (elseBlock.isConstant()) {
						try {
							final Instant value = elseBlock.getInstantFunction().call();
							return () -> value;
						} catch (ExpressionExceptionWrapper e) {
							throw e;
						} catch (Exception e) {
							throw new FatalExpressionExceptionWrapper(e);
						}
					} else {
						// if it isnt constant, return the function result
						return () -> elseBlock.getInstantFunction().call();
					}
				}
			} else {
				return () -> null;
			}
		} else {
			// create a Callable<Date> that checks whether the condition is given and calls the if
			// or else part
			return () -> {
				Boolean cond = getCondition(condition);
				if (cond == null) {
					return null;
				}
				if (cond) {
					return ifBlock.getInstantFunction().call();
				} else {
					return elseBlock.getInstantFunction().call();
				}
			};
		}
	}

	/**
	 * Builds a Double callable from the given evaluators
	 *
	 * @param condition
	 *            evaluator
	 * @param ifBlock
	 *            evaluator
	 * @param elseBlock
	 *            evaluator
	 * @return the resulting Double callable
	 */
	protected DoubleCallable makeDoubleCallable(final ExpressionEvaluator condition, final ExpressionEvaluator ifBlock,
			final ExpressionEvaluator elseBlock) {
		// if we know that the condition is constant, make the callable based on the selected case
		if (condition.isConstant()) {
			// check the condition value
			Boolean cond = getCondition(condition);
			if (cond != null) {
				if (cond) {
					// return DoubleCallable from the ifCase
					// if this block is constant, just return the value
					if (ifBlock.isConstant()) {
						try {
							final double value = ifBlock.getDoubleFunction().call();
							return () -> value;
						} catch (ExpressionExceptionWrapper e) {
							throw e;
						} catch (Exception e) {
							throw new FatalExpressionExceptionWrapper(e);
						}
					} else {
						// if it isnt constant, return the function result
						return () -> ifBlock.getDoubleFunction().call();
					}
				} else {
					// return DoubleCallable from the elseCase
					// if this block is constant, just return the value
					if (elseBlock.isConstant()) {
						try {
							final double value = elseBlock.getDoubleFunction().call();
							return () -> value;
						} catch (ExpressionExceptionWrapper e) {
							throw e;
						} catch (Exception e) {
							throw new FatalExpressionExceptionWrapper(e);
						}
					} else {
						// if it isnt constant, return the function result
						return () -> elseBlock.getDoubleFunction().call();
					}
				}
			} else {
				return () -> Double.NaN;
			}
		} else {
			// create a DoubleCallable that checks whether the condition is given and calls the
			// if or else part
			return () -> {
				Boolean cond = getCondition(condition);
				if (cond == null) {
					return Double.NaN;
				}
				if (cond) {
					return ifBlock.getDoubleFunction().call();
				} else {
					return elseBlock.getDoubleFunction().call();
				}
			};
		}

	}

	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {

		// check whether there are 3 arguments
		if (inputTypes.length != 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input", getFunctionName(), "3",
					inputTypes.length);
		}
		// check that the first input is logical (boolean or double)
		if (inputTypes[0] != ExpressionType.INTEGER && inputTypes[0] != ExpressionType.DOUBLE
				&& inputTypes[0] != ExpressionType.BOOLEAN) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_type.argument_two", 1, getFunctionName(),
					"boolean", "numerical");
		}

		ExpressionType ifCase = inputTypes[1];
		ExpressionType elseCase = inputTypes[2];

		// if both cases have the same type, the type is clear
		if (ifCase.equals(elseCase)) {
			return ifCase;
		} else if (ifCase.equals(ExpressionType.INTEGER) && elseCase.equals(ExpressionType.DOUBLE)) {
			return ExpressionType.DOUBLE;
		} else if (ifCase.equals(ExpressionType.DOUBLE) && elseCase.equals(ExpressionType.INTEGER)) {
			return ExpressionType.DOUBLE;
		} else {
			return ExpressionType.STRING;
		}
	}

	/**
	 * Returns the {@link ExpressionType} based on the condition value
	 *
	 * @param conditionTrue
	 * @param ifcase
	 *            {@link ExpressionType} of the if case
	 * @param elsecase
	 *            {@link ExpressionType} of the else case
	 * @return the {@link ExpressionType} of the case, which is given by the parameter conditionTrue
	 */
	private ExpressionType computeType(Boolean conditionTrue, ExpressionType ifcase, ExpressionType elsecase) {
		if (conditionTrue) {
			return ifcase;
		} else {
			return elsecase;
		}
	}

	/**
	 * Returns the condition of the given {@link ExpressionEvaluator} if this one is constant
	 *
	 * @param condition
	 *            {@link ExpressionEvaluator}
	 * @return the condition of the {@link ExpressionEvaluator}
	 */
	private Boolean getCondition(ExpressionEvaluator condition) {

		Boolean cond = false;

		if (condition.getType() == ExpressionType.BOOLEAN) {
			final Callable<Boolean> funcCond = condition.getBooleanFunction();
			try {
				cond = funcCond.call();
			} catch (ExpressionExceptionWrapper e) {
				throw e;
			} catch (Exception e) {
				throw new FatalExpressionExceptionWrapper(e);
			}
		} else if (condition.getType() == ExpressionType.DOUBLE || condition.getType() == ExpressionType.INTEGER) {

			final DoubleCallable funcCond = condition.getDoubleFunction();
			try {
				final double condValue = funcCond.call();
				if (Double.isNaN(condValue)) {
					return null;
				}
				cond = Math.abs(condValue) >= Double.MIN_VALUE * 2;

			} catch (ExpressionExceptionWrapper e) {
				throw e;
			} catch (Exception e) {
				throw new FatalExpressionExceptionWrapper(e);
			}
		}
		return cond;
	}

}
