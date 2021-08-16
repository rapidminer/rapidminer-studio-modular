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
package com.rapidminer.tools.belt.expression.internal.function.eval;

import java.time.Instant;
import java.util.concurrent.Callable;

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
 * TODO fix docs
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class Lead extends AbstractFunction {

	// TODO add new types

	public Lead() {
		// TODO change the i18n key
		super("process.lead", FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, null);
	}

	protected Lead(String i18nKey) {
		super(i18nKey, FunctionDescription.UNFIXED_NUMBER_OF_ARGUMENTS, null);
	}

	@Override
	public ExpressionEvaluator compute(Callable<Void> stopChecker, ExpressionContext context, ExpressionEvaluator... inputEvaluators) {
		validateInput(context, inputEvaluators);
		if (inputEvaluators[0].isConstant() && inputEvaluators[1].isConstant()) {
			// all inputs are constant, therefore we can calculate the result
			if (inputEvaluators.length == 2) {
				return resolve(inputEvaluators[0], inputEvaluators[1], context);
			} else {
				return computeAndCheckType(inputEvaluators[0], inputEvaluators[1],
						getExpectedReturnType(inputEvaluators[2]), context);
			}
		} else {
			// create callable because the attribute name and / or the index is non-constant
			final ExpressionType expectedType;
			if (inputEvaluators.length == 3) {
				expectedType = getExpectedReturnType(inputEvaluators[2]);
			} else {
				// in this case the name is constant (checked in #validateInput)
				expectedType = resolveVariableType(inputEvaluators[0], context);
			}
			switch (expectedType) {
				// TODO add new types
				case INSTANT:
					return makeInstantEvaluator(expectedType, inputEvaluators[0], inputEvaluators[1], context);
				case DOUBLE:
				case INTEGER:
					return makeDoubleEvaluator(expectedType, inputEvaluators[0], inputEvaluators[1], context);
				case BOOLEAN:
					return makeBooleanEvaluator(expectedType, inputEvaluators[0], inputEvaluators[1], context);
				case STRING:
				default:
					return makeStringEvaluator(expectedType, inputEvaluators[0], inputEvaluators[1], context);
			}
		}
	}

	/**
	 * Is not used and returns always {@code null}.
	 */
	@Override
	protected ExpressionType computeType(ExpressionType... inputTypes) {
		return null;
	}

	/**
	 * TODO this is where the final variable expression is built and resolved
	 */
	protected ExpressionEvaluator resolve(String attributeName, int offset, ExpressionContext context) {
		ExpressionEvaluator evaluator = context.getDynamicVariable(attributeName, () -> context.getIndex() + offset);
		if (evaluator == null) {
			throw new AttributeEvaluationExceptionWrapper(getFunctionName(), attributeName);
		}
		return evaluator;
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with an instant callable that calls {@link
	 * #computeAndCheckType(ExpressionEvaluator, ExpressionEvaluator, ExpressionType, ExpressionContext)}.
	 */
	private ExpressionEvaluator makeInstantEvaluator(final ExpressionType expectedType,
													 final ExpressionEvaluator attributeNameEvaluator,
													 final ExpressionEvaluator indexEvaluator,
													 final ExpressionContext context) {
		Callable<Instant> instantCallable = () -> {
			ExpressionEvaluator subExpressionEvaluator = computeAndCheckType(attributeNameEvaluator, indexEvaluator,
					expectedType, context);
			return subExpressionEvaluator.getInstantFunction().call();
		};
		return ExpressionEvaluatorFactory.ofInstant(instantCallable, false);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a boolean callable that calls {@link
	 * #computeAndCheckType(ExpressionEvaluator, ExpressionEvaluator, ExpressionType, ExpressionContext)}.
	 */
	private ExpressionEvaluator makeBooleanEvaluator(final ExpressionType expectedType,
													 final ExpressionEvaluator attributeNameEvaluator,
													 final ExpressionEvaluator indexEvaluator,
													 final ExpressionContext context) {
		Callable<Boolean> booleanCallable = () -> {
			ExpressionEvaluator subExpressionEvaluator = computeAndCheckType(attributeNameEvaluator, indexEvaluator,
					expectedType, context);
			return subExpressionEvaluator.getBooleanFunction().call();
		};
		return ExpressionEvaluatorFactory.ofBoolean(booleanCallable, false);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a double callable that calls {@link
	 * #computeAndCheckType(ExpressionEvaluator, ExpressionEvaluator, ExpressionType, ExpressionContext)}.
	 */
	private ExpressionEvaluator makeDoubleEvaluator(final ExpressionType expectedType,
													final ExpressionEvaluator attributeNameEvaluator,
													final ExpressionEvaluator indexEvaluator,
													final ExpressionContext context) {
		DoubleCallable doubleCallable = () -> {
			// TODO does compute and check type really make sense here?
			// TODO why do we have to convert to the expected type? Can we not simply use the actual type?
			// TODO reiterate the whole lead function and see if it can be optimized
			ExpressionEvaluator subExpressionEvaluator = computeAndCheckType(attributeNameEvaluator, indexEvaluator,
					expectedType, context);
			return subExpressionEvaluator.getDoubleFunction().call();
		};
		return ExpressionEvaluatorFactory.ofDouble(doubleCallable, false, expectedType);
	}

	/**
	 * Creates an {@link ExpressionEvaluator} with a string callable that calls {@link
	 * #computeAndCheckType(ExpressionEvaluator, ExpressionEvaluator, ExpressionType, ExpressionContext)}.
	 */
	private ExpressionEvaluator makeStringEvaluator(final ExpressionType expectedType,
													final ExpressionEvaluator attributeNameEvaluator,
													final ExpressionEvaluator indexEvaluator,
													final ExpressionContext context) {
		Callable<String> stringCallable = () -> {
			ExpressionEvaluator subExpressionEvaluator = computeAndCheckType(attributeNameEvaluator, indexEvaluator,
					expectedType, context);
			return subExpressionEvaluator.getStringFunction().call();
		};
		return ExpressionEvaluatorFactory.ofString(stringCallable, false);
	}

	/**
	 * Converts the type constant passed by the user to an {@link ExpressionType}.
	 *
	 * @param expressionEvaluator the evaluator holding the type constant.
	 * @return the generated ExpressionType
	 */
	private ExpressionType getExpectedReturnType(ExpressionEvaluator expressionEvaluator) { // TODO move duplicate code into tools class?
		if (!expressionEvaluator.isConstant()) {
			String validTypeArguments = TypeConstants.INSTANCE.getValidConstantsString();
			throw new FunctionInputExceptionWrapper("expression_parser.row.type_not_constant", getFunctionName(),
					validTypeArguments);
		}
		String typeString;
		try {
			typeString = expressionEvaluator.getStringFunction().call();
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}

		ExpressionType expectedType = TypeConstants.INSTANCE.getTypeForName(typeString);
		if (expectedType == null) {
			String validTypeArguments = TypeConstants.INSTANCE.getValidConstantsString();
			throw new FunctionInputExceptionWrapper("expression_parser.row.invalid_type", typeString, getFunctionName(),
					validTypeArguments);
		}
		return expectedType;
	}

	/**
	 * Evaluates the expression into a String, feeds it to the parser and checks if the resulting type is the expected
	 * type. If the resulting type is not as expected and the expected type is String then converts the result to a
	 * string evaluator. If the expected type is double and the result type is integer the result type is changed to
	 * double.
	 *
	 * @param attributeNameEvaluator the evaluator whose string function call provides the attribute name
	 * @param indexEvaluator         the evaluator whose double function call provides the row index TODO update
	 *                               javadoc
	 * @param expectedType           the expected type of the result
	 * @return the result of parsing the expression string
	 */
	private ExpressionEvaluator computeAndCheckType(ExpressionEvaluator attributeNameEvaluator, ExpressionEvaluator indexEvaluator, // TODO move duplicate code to tools class?
													ExpressionType expectedType, ExpressionContext context) {
		ExpressionEvaluator outEvaluator = resolve(attributeNameEvaluator, indexEvaluator, context);
		if (outEvaluator.getType() == expectedType) {
			return outEvaluator;
		} else if (expectedType == ExpressionType.DOUBLE && outEvaluator.getType() == ExpressionType.INTEGER) {
			// use same resulting evaluator but with different type
			return ExpressionEvaluatorFactory.ofDouble(outEvaluator.getDoubleFunction(), outEvaluator.isConstant(),
					expectedType);
		} else if (expectedType == ExpressionType.STRING) {
			return convertToStringEvaluator(outEvaluator);
		} else {
			throw new FunctionInputExceptionWrapper("expression_parser.row.type_not_matching", getFunctionName(),
					getConstantName(outEvaluator.getType()), getConstantName(expectedType));
		}
	}

	/**
	 * Converts the outEvaluator into an {@link ExpressionEvaluator} of type string. If outEvaluator is constant the
	 * result is also constant.
	 *
	 * @param outEvaluator a evaluator which is not of type String
	 * @return an {@link ExpressionEvaluator} of type String
	 */
	private ExpressionEvaluator convertToStringEvaluator(final ExpressionEvaluator outEvaluator) { // TODO move duplicate code to tools class?
		if (outEvaluator.isConstant()) {
			try {
				String stringValue = getStringValue(outEvaluator);
				return ExpressionEvaluatorFactory.ofString(stringValue);
			} catch (ExpressionExceptionWrapper e) {
				throw e;
			} catch (Exception e) {
				throw new FatalExpressionExceptionWrapper(e);
			}
		} else {
			Callable<String> stringCallable = () -> getStringValue(outEvaluator);
			return ExpressionEvaluatorFactory.ofString(stringCallable, false);
		}
	}

	/**
	 * Calculates the String value that the outEvaluator should return.
	 */
	private String getStringValue(ExpressionEvaluator outEvaluator) throws Exception { // TODO move duplicate code to tools class?
		switch (outEvaluator.getType()) {
			case DOUBLE:
			case INTEGER:
				return doubleToString(outEvaluator.getDoubleFunction().call(),
						outEvaluator.getType() == ExpressionType.INTEGER);
			case BOOLEAN:
				return booleanToString(outEvaluator.getBooleanFunction().call());
			case INSTANT:
				return instantToString(outEvaluator.getInstantFunction().call());
				// TODO add new types
			default:
				// cannot happen
				return null;
		}
	}

	/**
	 * Converts the input to a string with special missing value handling
	 */
	private String instantToString(Instant input) { // TODO move duplicate code to tools class?
		if (input == null) {
			return null;
		} else {
			// TODO this is probably not what we want?
			return input.toString();
		}
	}

	/**
	 * Converts the input to a string with special missing value handling.
	 */
	private String booleanToString(Boolean input) { // TODO move duplicate code to tools class?
		if (input == null) {
			return null;
		} else {
			return input.toString();
		}

	}

	/**
	 * Converts the input to a string with special missing value handling and integers represented as integers if
	 * possible.
	 */
	private String doubleToString(double input, boolean isInteger) { // TODO move duplicate code to tools class?
		if (Double.isNaN(input)) {
			return null;
		}
		if (isInteger && input == (int) input) {
			return "" + (int) input;
		} else {
			return "" + input;
		}
	}

	/**
	 * Converts the ExpressionType to the name of the constant that the user should use to mark this type.
	 *
	 * @param type an {@link ExpressionType}
	 * @return the string name of the constant associated to this type
	 */
	// TODO update the type constants
	private String getConstantName(ExpressionType type) { // TODO move duplicate code to tools class?
		return TypeConstants.INSTANCE.getNameForType(type);
	}

	/**
	 * Resolves the type of the variable with the given name.
	 *
	 * @param variableNameInput
	 *        {@link ExpressionEvaluator} input representing the variable name.
	 * @return the type of the variable
	 */
	private ExpressionType resolveVariableType(ExpressionEvaluator variableNameInput, ExpressionContext context) {
		try {
			// TODO handle null pointer in case the variableName is null
			return context.getDynamicVariableType(variableNameInput.getStringFunction().call());
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
	}

	/**
	 * Evaluates the expression into a String and feeds it to the parser.
	 *
	 * @param attributeNameEvaluator the evaluator whose string function call provides the attribute name
	 * @param indexEvaluator         the evaluator whose double function call provides the row index TODO update
	 *                               javadoc
	 * @return the result of parsing the expression string
	 */
	private ExpressionEvaluator resolve(ExpressionEvaluator attributeNameEvaluator, ExpressionEvaluator indexEvaluator,
										ExpressionContext context) { // TODO does the method name make sense?
		String attributeName;
		int offset;
		try {
			attributeName = attributeNameEvaluator.getStringFunction().call();
			offset = (int) indexEvaluator.getDoubleFunction().call();
		} catch (ExpressionExceptionWrapper e) {
			throw e;
		} catch (Exception e) {
			throw new FatalExpressionExceptionWrapper(e);
		}
		return resolve(attributeName, offset, context);
	}

	/**
	 * TODO
	 */
	private void validateInput(ExpressionContext context, ExpressionEvaluator... inputEvaluators) {
		// check if context has been set
		if (context == null) {
			throw new IllegalStateException("context must be set in order to evaluate");
		}
		// check if the number of inputs is valid
		if (inputEvaluators.length < 2 || inputEvaluators.length > 3) {
			throw new FunctionInputExceptionWrapper("expression_parser.function_wrong_input_two", getFunctionName(), 2, 3,
					inputEvaluators.length);
		}
		// check if the type of inputs is valid
		String wrongTypeI18nKey = "expression_parser.function_wrong_type.argument";
		if (inputEvaluators[0].getType() != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(wrongTypeI18nKey, 1, getFunctionName(),
					"nominal");
		}
		if (inputEvaluators[1].getType() != ExpressionType.INTEGER && inputEvaluators[1].getType() != ExpressionType.DOUBLE) {
			throw new FunctionInputExceptionWrapper(wrongTypeI18nKey, 2, getFunctionName(),
					"integer");
		}
		if (inputEvaluators.length == 3 && inputEvaluators[2].getType() != ExpressionType.STRING) {
			throw new FunctionInputExceptionWrapper(wrongTypeI18nKey, 3, getFunctionName(),
					"nominal");
		}
		// check if the name is constant or the type is given
		if (inputEvaluators.length == 2 && !inputEvaluators[0].isConstant()) {
			throw new FunctionInputExceptionWrapper("expression_parser.row.missing_type_argument",
					getFunctionName());
		}
	}
}
