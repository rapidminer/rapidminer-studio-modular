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
package com.rapidminer.tools.belt.expression;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import com.rapidminer.Process;
import com.rapidminer.tools.belt.expression.internal.SimpleConstantResolver;
import com.rapidminer.tools.belt.expression.internal.SimpleExpressionContext;
import com.rapidminer.tools.belt.expression.internal.antlr.AntlrParser;
import com.rapidminer.tools.belt.expression.internal.function.eval.Evaluation;
import com.rapidminer.tools.belt.expression.internal.function.eval.TypeConstants;
import com.rapidminer.tools.belt.expression.internal.function.process.ParameterValue;
import com.rapidminer.tools.belt.expression.internal.function.statistical.Random;


/**
 * Builder for an {@link ExpressionParser}.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class ExpressionParserBuilder {

	private Process process;

	private List<Function> functions = new LinkedList<>();

	private List<ConstantResolver> scopeResolvers = new LinkedList<>();
	private List<DynamicResolver> dynamicsResolvers = new LinkedList<>();
	private List<ConstantResolver> constantResolvers = new LinkedList<>();

	/**
	 * Builds an {@link ExpressionParser} with the given data.
	 *
	 * @return an expression parser
	 */
	public ExpressionParser build() {
		// add functions with process information
		if (process != null) {
			functions.add(new Random(process));
			functions.add(new ParameterValue(process));
		}

		// add eval function
		Evaluation evalFunction = new Evaluation();
		functions.add(evalFunction);

		// add eval constants
		constantResolvers.add(new SimpleConstantResolver(TypeConstants.INSTANCE.getKey(), TypeConstants.INSTANCE.getConstants()));

		ExpressionContext context = new SimpleExpressionContext(functions, scopeResolvers, dynamicsResolvers,
				constantResolvers, getStopChecker());
		AntlrParser parser = new AntlrParser(context);

		evalFunction.setParser(parser);

		return parser;
	}

	/**
	 * Adds the process which enables process dependent functions.
	 *
	 * @param process
	 *            the process to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withProcess(Process process) {
		this.process = process;
		return this;
	}

	/**
	 * Adds the resolver as a resolver for scope constants (%{scope_constant} in the expression).
	 *
	 * @param resolver
	 *            the resolver to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withScope(ConstantResolver resolver) {
		scopeResolvers.add(resolver);
		return this;
	}

	/**
	 * Adds the resolver as a resolver for dynamic variables ([variable_name] or variable_name in
	 * the expression).
	 *
	 * @param resolver
	 *            the resolver to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withDynamics(DynamicResolver resolver) {
		dynamicsResolvers.add(resolver);
		return this;
	}

	/**
	 * Adds the given module that supplies functions and constant values.
	 *
	 * @param module
	 *            the module to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withModule(ExpressionParserModule module) {
		addModule(module);
		return this;
	}

	/**
	 * Adds all functions of the module to the list of functions and adds a {@link SimpleConstantResolver}
	 * knowing all constants of the module to the list of constant resolver.
	 *
	 * @param module
	 */
	private void addModule(ExpressionParserModule module) {
		List<Constant> moduleConstants = module.getConstants();
		if (moduleConstants != null && !moduleConstants.isEmpty()) {
			constantResolvers.add(new SimpleConstantResolver(module.getKey(), moduleConstants));
		}
		List<Function> moduleFunctions = module.getFunctions();
		if (moduleFunctions != null) {
			functions.addAll(moduleFunctions);
		}
	}

	/**
	 * Adds the given modules that supplies functions and constant values.
	 *
	 * @param modules
	 *            the modules to add
	 * @return the builder
	 */
	public ExpressionParserBuilder withModules(List<ExpressionParserModule> modules) {
		for (ExpressionParserModule module : modules) {
			addModule(module);
		}
		return this;
	}

	/** @since 9.6.0 */
	private Callable<Void> getStopChecker() {
		if (process != null) {
			return  () -> {process.getRootOperator().checkForStop(); return null;};
		}
		return () -> null;
	}

}
