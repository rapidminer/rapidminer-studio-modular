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

import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_DAY;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_HOUR;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_MILLISECOND;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_MINUTE;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_MONTH;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_NANOSECOND;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_SECOND;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_WEEK;
import static com.rapidminer.tools.belt.expression.internal.ExpressionParserConstants.DATE_TIME_UNIT_YEAR;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.DateTimeBuffer;
import com.rapidminer.belt.buffer.NominalBuffer;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.buffer.ObjectBuffer;
import com.rapidminer.belt.buffer.TimeBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionParserBuilder;
import com.rapidminer.tools.belt.expression.ExpressionRegistry;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.FunctionInputExceptionWrapper;
import com.rapidminer.tools.belt.expression.MacroResolver;
import com.rapidminer.tools.belt.expression.TableResolver;


/**
 * Utils class providing some convenience methods that can be used with the expression parser.
 *
 * @author Kevin Majchrzak
 * @see ExpressionParserBuilder
 * @see ExpressionParser
 * @since 9.11
 */
public enum ExpressionParserUtils {

	; // no instance enum

	private static final String I18N_INVALID_UNIT = "expression_parser.invalid_argument.date_unit";


	/**
	 * Return the corresponding {@link ExpressionType} for the given {@link Column.TypeId}.
	 *
	 * @param id
	 * 		the column type id
	 * @return the corresponding expression type
	 */
	public static ExpressionType expressionTypeForColumnId(Column.TypeId id) {
		switch (id) {
			case REAL:
				return ExpressionType.DOUBLE;
			case INTEGER_53_BIT:
				return ExpressionType.INTEGER;
			case DATE_TIME:
				return ExpressionType.INSTANT;
			case TIME:
				return ExpressionType.LOCAL_TIME;
			case TEXT_SET:
				return ExpressionType.STRING_SET;
			case TEXT_LIST:
				return ExpressionType.STRING_LIST;
			case NOMINAL:
			case TEXT:
			default:
				return ExpressionType.STRING;
		}
	}

	/**
	 * Creates an {@link ExpressionParser} with all modules that are registered to the {@link ExpressionRegistry} and
	 * with the {@link SimpleConstantResolver}, {@link MacroResolver} and {@link TableResolver}. For full control over
	 * the used modules and resolvers use the {@link ExpressionParserBuilder} instead.
	 *
	 * @param op
	 * 		the operator to create the {@link ExpressionParser} for. May be {@code null}.
	 * @param table
	 * 		the table that should be used to resolve the variables
	 * @return the resulting expression parser
	 */
	public static ExpressionParser createDefaultParser(Operator op, Table table) {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		if (op != null && op.getProcess() != null) {
			builder.withProcess(op.getProcess());
			builder.withScope(new MacroResolver(op.getProcess().getMacroHandler(), op));
		}
		if (table != null) {
			TableResolver resolver = new TableResolver(table);
			builder.withDynamics(resolver);
		}
		builder.withModules(ExpressionRegistry.INSTANCE.getAll());
		return builder.build();
	}

	/**
	 * Creates a new column using the given expression parser and expression. Iterates over all rows and evaluates the
	 * expression for each row.
	 *
	 * @param numberOfRows
	 * 		the number of rows of the resulting column. The expression will be evaluated for each row.
	 * @param expression
	 * 		the expression used to create the column
	 * @param parser
	 * 		the parser used to parse the given expression
	 * @return the resulting column
	 * @throws ExpressionException
	 * 		if the expression parsing fails
	 */
	public static Column createColumn(int numberOfRows, String expression, ExpressionParser parser) throws ExpressionException {
		Expression parsedExpression = parser.parse(expression);
		ExpressionType expressionType = parsedExpression.getExpressionType();
		switch (expressionType) {
			case DOUBLE:
				return createNumericColumn(numberOfRows, parsedExpression, parser.getExpressionContext(), false);
			case INTEGER:
				return createNumericColumn(numberOfRows, parsedExpression, parser.getExpressionContext(), true);
			case INSTANT:
				return createDateTimeColumn(numberOfRows, parsedExpression, parser.getExpressionContext());
			case LOCAL_TIME:
				return createTimeColumn(numberOfRows, parsedExpression, parser.getExpressionContext());
			case STRING_SET:
				return createTextSetColumn(numberOfRows, parsedExpression, parser.getExpressionContext());
			case STRING_LIST:
				return createTextListColumn(numberOfRows, parsedExpression, parser.getExpressionContext());
			case BOOLEAN:
				return createStringColumn(numberOfRows, parsedExpression, parser.getExpressionContext(), true);
			case STRING:
				return createStringColumn(numberOfRows, parsedExpression, parser.getExpressionContext(), false);
			default:
				throw new IllegalStateException("Unknown expression type: " + expressionType.name());
		}
	}

	/**
	 * Converts a {@link ExpressionException} into a {@link UserError}.
	 *
	 * @param op
	 *            the calling operator
	 * @param function
	 *            the entered function
	 * @param e
	 *            the exception
	 * @return UserError
	 *             the converted {@link UserError}
	 */
	public static UserError convertToUserError(Operator op, String function, ExpressionException e) {
		// only show up to 15 characters of the function string
		String shortenedFunction = function;
		if (function.length() > 15) {
			shortenedFunction = function.substring(0, 15).concat(" (...)");
		}

		return new UserError(op, e, "expression_evaluation_failed", e.getShortMessage(), shortenedFunction);
	}

	/**
	 * Evaluates the given inputs and combines them to create a {@link ZonedDateTime}. If any of the inputs evaluates to
	 * {@code null} the result will be {@code null}.
	 *
	 * @param instant
	 * 		expression evaluator with an Instance callable
	 * @param timeZone
	 * 		expression evaluator with a String callable holding a time zone
	 * @throws Exception
	 * 		if the time zone is invalid or if an error occurs during evaluation
	 */
	public static ZonedDateTime getZonedDateTime(ExpressionEvaluator instant, ExpressionEvaluator timeZone) throws Exception {
		Instant instantValue = instant.getInstantFunction().call();
		String timeZoneValue = timeZone.getStringFunction().call();
		if (instantValue == null || timeZoneValue == null) {
			return null;
		}
		try {
			ZoneId zoneId = ZoneId.of(timeZoneValue);
			return instantValue.atZone(zoneId);
		} catch (DateTimeException e) {
			throw new FunctionInputExceptionWrapper(e.getMessage());
		}
	}

	/**
	 * Iterates through the given evaluators. Returns {@code true} iff any given evaluator constantly evaluates to
	 * {@code null}.
	 *
	 * @param evaluators
	 * 		the evaluators to check
	 * @return {@code true}, if and only if, one of the evaluators constantly evaluates to {@code null}
	 * @throws Exception
	 * 		if the evaluation fails
	 */
	public static boolean containsConstantMissing(Iterable<ExpressionEvaluator> evaluators) throws Exception {
		for (ExpressionEvaluator evaluator : evaluators) {
			if (evaluator.isConstant() && evaluatesToNull(evaluator)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return {@code true} if and only if the given evaluator evaluates to missing ({@code null} or {@code NaN}).
	 * @throws Exception
	 * 		if the evaluation fails
	 */
	public static boolean evaluatesToNull(ExpressionEvaluator evaluator) throws Exception {
		switch (evaluator.getType()) {
			case STRING:
				return evaluator.getStringFunction().call() == null;
			case INTEGER:
			case DOUBLE:
				return Double.isNaN(evaluator.getDoubleFunction().call());
			case BOOLEAN:
				return evaluator.getBooleanFunction().call() == null;
			case INSTANT:
				return evaluator.getInstantFunction().call() == null;
			case LOCAL_TIME:
				return evaluator.getLocalTimeFunction().call() == null;
			case STRING_SET:
				return evaluator.getStringSetFunction().call() == null;
			case STRING_LIST:
				return evaluator.getStringListFunction().call() == null;
			default:
				throw new IllegalStateException("Unexpected value: " + evaluator.getType());
		}
	}

	/**
	 * Returns the appropriate exception for the given invalid unit.
	 */
	public static FunctionInputExceptionWrapper getInvalidTimeUnitException(String functionName, String unit) {
		switch (unit) {
			case DATE_TIME_UNIT_YEAR:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_YEAR);
			case DATE_TIME_UNIT_MONTH:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_MONTH);
			case DATE_TIME_UNIT_WEEK:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_WEEK);
			case DATE_TIME_UNIT_DAY:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_DAY);
			case DATE_TIME_UNIT_HOUR:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_HOUR);
			case DATE_TIME_UNIT_MINUTE:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_MINUTE);
			case DATE_TIME_UNIT_SECOND:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_SECOND);
			case DATE_TIME_UNIT_MILLISECOND:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_MILLISECOND);
			case DATE_TIME_UNIT_NANOSECOND:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, DATE_TIME_UNIT_NANOSECOND);
			default:
				return new FunctionInputExceptionWrapper(I18N_INVALID_UNIT, functionName, unit);
		}
	}

	private static Column createNumericColumn(int numberOfRows, Expression parsedExpression, ExpressionContext context,
											  boolean isInteger) throws ExpressionException {
		NumericBuffer buffer = isInteger ? Buffers.integer53BitBuffer(numberOfRows, false)
				: Buffers.realBuffer(numberOfRows, false);
		for (int i = 0; i < numberOfRows; i++) {
			context.setIndex(i);
			buffer.set(i, parsedExpression.evaluateNumerical());
		}
		return buffer.toColumn();
	}

	private static Column createStringColumn(int numberOfRows, Expression parsedExpression,
											 ExpressionContext context, boolean isBoolean) throws ExpressionException {
		NominalBuffer buffer = Buffers.nominalBuffer(numberOfRows);
		for (int i = 0; i < numberOfRows; i++) {
			context.setIndex(i);
			buffer.set(i, parsedExpression.evaluateNominal());
		}
		return isBoolean ? buffer.toBooleanColumn("true") : buffer.toColumn();
	}

	private static Column createDateTimeColumn(int numberOfRows, Expression parsedExpression,
											   ExpressionContext context) throws ExpressionException {
		DateTimeBuffer buffer = Buffers.dateTimeBuffer(numberOfRows, true, false);
		for (int i = 0; i < numberOfRows; i++) {
			context.setIndex(i);
			buffer.set(i, parsedExpression.evaluateInstant());
		}
		return buffer.toColumn();
	}

	private static Column createTimeColumn (int numberOfRows, Expression parsedExpression,
											   ExpressionContext context) throws ExpressionException {
		TimeBuffer buffer = Buffers.timeBuffer(numberOfRows,false);
		for (int i = 0; i < numberOfRows; i++) {
			context.setIndex(i);
			buffer.set(i, parsedExpression.evaluateLocalTime());
		}
		return buffer.toColumn();
	}

	private static Column createTextSetColumn(int numberOfRows, Expression parsedExpression,
											   ExpressionContext context) throws ExpressionException {
		ObjectBuffer<StringSet> buffer = Buffers.textsetBuffer(numberOfRows);
		for (int i = 0; i < numberOfRows; i++) {
			context.setIndex(i);
			buffer.set(i, parsedExpression.evaluateStringSet());
		}
		return buffer.toColumn();
	}

	private static Column createTextListColumn(int numberOfRows, Expression parsedExpression,
											   ExpressionContext context) throws ExpressionException {
		ObjectBuffer<StringList> buffer = Buffers.textlistBuffer(numberOfRows);
		for (int i = 0; i < numberOfRows; i++) {
			context.setIndex(i);
			buffer.set(i, parsedExpression.evaluateStringList());
		}
		return buffer.toColumn();
	}
}