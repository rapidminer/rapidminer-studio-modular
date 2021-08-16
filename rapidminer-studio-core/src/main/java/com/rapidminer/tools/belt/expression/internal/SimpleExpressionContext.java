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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.IntSupplier;

import com.rapidminer.belt.column.type.StringList;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.tools.belt.expression.ConstantResolver;
import com.rapidminer.tools.belt.expression.DoubleCallable;
import com.rapidminer.tools.belt.expression.DynamicResolver;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;
import com.rapidminer.tools.belt.expression.ExpressionType;
import com.rapidminer.tools.belt.expression.Function;
import com.rapidminer.tools.belt.expression.FunctionDescription;
import com.rapidminer.tools.belt.expression.FunctionInput;


/**
 * Simple {@link ExpressionContext} implementation. Please note that this implementation is not tread safe and needs
 * external synchronization if you want to access it from multiple threads.
 *
 * @author Gisa Meier, Kevin Majchrzak
 * @since 9.11
 */
public class SimpleExpressionContext implements ExpressionContext {

	private Map<String, Function> functionMap;

	private List<DynamicResolver> dynamicResolvers;
	private List<ConstantResolver> scopeResolvers;
	private List<ConstantResolver> constantResolvers;

	private final Callable<Void> stopChecker;

	/**
	 * The current index (row number). {@code -1} if no index has been set.
	 */
	private int currentIndex = -1;

	/**
	 * Creates an {@link ExpressionContext} that uses the given functions and resolvers.
	 *
	 * @param functions
	 * 		the functions to use in expressions
	 * @param scopeResolvers
	 * 		the scope resolvers to use
	 * @param dynamicResolvers
	 * 		the resolvers for dynamic variables to use
	 * @param constantResolvers
	 * 		the resolvers for constants
	 * @param stopChecker
	 * 		a callable that might throw an exception to stop a running evaluation. Can be {@code null}.
	 */
	public SimpleExpressionContext(List<Function> functions, List<ConstantResolver> scopeResolvers,
								   List<DynamicResolver> dynamicResolvers, List<ConstantResolver> constantResolvers,
								   Callable<Void> stopChecker) {
		this.scopeResolvers = scopeResolvers;
		this.dynamicResolvers = dynamicResolvers;
		this.constantResolvers = constantResolvers;
		this.stopChecker = stopChecker == null ? () -> null : stopChecker;

		this.functionMap = new LinkedHashMap<>();
		for (Function function : functions) {
			// take the first function with a certain function name if there is more than one
			if (!this.functionMap.containsKey(function.getFunctionName())) {
				this.functionMap.put(function.getFunctionName(), function);
			}
		}
	}

	@Override
	public Function getFunction(String functionName) {
		return functionMap.get(functionName);
	}

	@Override
	public ExpressionEvaluator getVariable(String variableName) {
		// A variable can either be a constant coming from a {@link Resolver} for constants or a
		// dynamic variable. This is done to keep compatibility with the old parser where
		// alpha-numeric strings could stand for constants or attribute values. Attribute values are
		// now a special case of dynamic variables.
		ExpressionEvaluator constant = getConstant(variableName);
		if (constant != null) {
			return constant;
		} else {
			return getDynamicVariable(variableName);
		}
	}

	@Override
	public ExpressionEvaluator getDynamicVariable(String variableName) {
		return getDynamicVariable(variableName, this::getIndex);
	}

	@Override
	public ExpressionEvaluator getDynamicVariable(String variableName, IntSupplier index) {
		DynamicResolver resolver = getDynamicResolverWithKnowledge(dynamicResolvers, variableName);
		if (resolver == null) {
			return null;
		}
		return getDynamicExpressionEvaluator(variableName, resolver, index);
	}

	@Override
	public void setIndex(int index) {
		currentIndex = index;
	}

	@Override
	public int getIndex() {
		return currentIndex;
	}

	@Override
	public ExpressionEvaluator getScopeConstant(String scopeName) {
		ConstantResolver resolver = getNonDynamicResolverWithKnowledge(scopeResolvers, scopeName);
		if (resolver == null) {
			return null;
		}
		return getConstantExpressionEvaluator(scopeName, resolver);
	}

	@Override
	public String getScopeString(String scopeName) {
		ConstantResolver resolver = getNonDynamicResolverWithKnowledge(scopeResolvers, scopeName);
		if (resolver == null || resolver.getVariableType(scopeName) != ExpressionType.STRING) {
			// ignore any non string scope constants
			return null;
		}
		return resolver.getStringValue(scopeName);
	}

	@Override
	public List<FunctionDescription> getFunctionDescriptions() {
		List<FunctionDescription> descriptions = new ArrayList<>(functionMap.size());
		for (Function function : functionMap.values()) {
			descriptions.add(function.getFunctionDescription());
		}
		return descriptions;
	}

	@Override
	public List<FunctionInput> getFunctionInputs() {
		List<FunctionInput> allFunctionInputs = new LinkedList<>();
		for (DynamicResolver resolver : dynamicResolvers) {
			allFunctionInputs.addAll(resolver.getAllVariables());
		}
		for (ConstantResolver resolver : constantResolvers) {
			allFunctionInputs.addAll(resolver.getAllVariables());
		}
		for (ConstantResolver resolver : scopeResolvers) {
			allFunctionInputs.addAll(resolver.getAllVariables());
		}
		return allFunctionInputs;
	}

	@Override
	public ExpressionEvaluator getConstant(String constantName) {
		ConstantResolver resolver = getNonDynamicResolverWithKnowledge(constantResolvers, constantName);
		if (resolver != null) {
			return getConstantExpressionEvaluator(constantName, resolver);
		} else {
			return null;
		}
	}

	@Override
	public ExpressionType getDynamicVariableType(String variableName) {
		DynamicResolver resolver = getDynamicResolverWithKnowledge(dynamicResolvers, variableName);
		if (resolver == null) {
			return null;
		}
		return resolver.getVariableType(variableName);
	}

	@Override
	public Callable<Void> getStopChecker() {
		return stopChecker;
	}

	/**
	 * Creates an non-constant {@link ExpressionEvaluator} for the variableName using the resolver.
	 */
	private ExpressionEvaluator getDynamicExpressionEvaluator(final String variableName, final DynamicResolver resolver,
															  IntSupplier index) {
		ExpressionType type = resolver.getVariableType(variableName);
		switch (type) {
			case DOUBLE:
			case INTEGER:
				DoubleCallable doubleCallable = () -> resolver.getDoubleValue(variableName, index.getAsInt());
				return ExpressionEvaluatorFactory.ofDouble(doubleCallable, false, type);
			case INSTANT:
				Callable<Instant> instantCallable = () -> resolver.getInstantValue(variableName, index.getAsInt());
				return ExpressionEvaluatorFactory.ofInstant(instantCallable, false);
			case BOOLEAN:
			case STRING:
				Callable<String> stringCallable = () -> resolver.getStringValue(variableName, index.getAsInt());
				return ExpressionEvaluatorFactory.ofString(stringCallable, false);
			case LOCAL_TIME:
				Callable<LocalTime> localTimeCallable = () -> resolver.getLocalTimeValue(variableName, index.getAsInt());
				return ExpressionEvaluatorFactory.ofLocalTime(localTimeCallable, false);
			case STRING_SET:
				Callable<StringSet> stringSetCallable = () -> resolver.getStringSetValue(variableName, index.getAsInt());
				return ExpressionEvaluatorFactory.ofStringSet(stringSetCallable, false);
			case STRING_LIST:
				Callable<StringList> stringListCallable = () -> resolver.getStringListValue(variableName, index.getAsInt());
				return ExpressionEvaluatorFactory.ofStringList(stringListCallable, false);
			default:
				return null;
		}
	}

	/**
	 * Looks for the first dynamic resolver in the resolvers list that knows the variableName.
	 *
	 * @param variableName
	 *            the name to look for
	 */
	private DynamicResolver getDynamicResolverWithKnowledge(List<DynamicResolver> resolvers, String variableName) {
		for (DynamicResolver resolver : resolvers) {
			if (resolver.getVariableType(variableName) != null) {
				return resolver;
			}
		}
		return null;
	}

	/**
	 * Looks for the first constant resolver in the resolvers list that knows the variableName.
	 *
	 * @param variableName
	 *            the name to look for
	 */
	private ConstantResolver getNonDynamicResolverWithKnowledge(List<ConstantResolver> resolvers, String variableName) {
		for (ConstantResolver resolver : resolvers) {
			if (resolver.getVariableType(variableName) != null) {
				return resolver;
			}
		}
		return null;
	}

	/**
	 * Creates a constant {@link ExpressionEvaluator} for the variableName using a constant or scope resolver.
	 */
	private ExpressionEvaluator getConstantExpressionEvaluator(String variableName, ConstantResolver resolver) {
		// the index will be ignored by the resolvers since the variable is constant
		ExpressionType type = resolver.getVariableType(variableName);
		switch (type) {
			case DOUBLE:
			case INTEGER:
				return ExpressionEvaluatorFactory.ofDouble(resolver.getDoubleValue(variableName), type);
			case INSTANT:
				return ExpressionEvaluatorFactory.ofInstant(resolver.getInstantValue(variableName));
			case STRING:
				return ExpressionEvaluatorFactory.ofString(resolver.getStringValue(variableName));
			case BOOLEAN:
				return ExpressionEvaluatorFactory.ofBoolean(resolver.getBooleanValue(variableName));
			case LOCAL_TIME:
				return ExpressionEvaluatorFactory.ofLocalTime(resolver.getLocalTimeValue(variableName));
			case STRING_SET:
				return ExpressionEvaluatorFactory.ofStringSet(resolver.getStringSetValue(variableName));
			case STRING_LIST:
				return ExpressionEvaluatorFactory.ofStringList(resolver.getStringListValue(variableName));
			default:
				return null;
		}
	}
}
