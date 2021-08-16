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
package com.rapidminer.operator.learner.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.TableCapability;
import com.rapidminer.operator.TableCapabilityCheck;
import com.rapidminer.operator.TableCapabilityProvider;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateTableModelTransformationRule;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.TableCapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.operator.ports.metadata.table.TablePassThroughRule;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeLong;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeTableExpression;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.tools.belt.BeltMetaDataTools;
import com.rapidminer.tools.belt.BeltTools;
import com.rapidminer.tools.belt.expression.DynamicResolver;
import com.rapidminer.tools.belt.expression.Expression;
import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionExceptionWrapper;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionParserBuilder;
import com.rapidminer.tools.belt.expression.ExpressionRegistry;
import com.rapidminer.tools.belt.expression.MacroResolver;
import com.rapidminer.tools.belt.expression.TableMetaDataResolver;
import com.rapidminer.tools.belt.expression.TableResolver;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;


/**
 * Operator that fits custom functions to data points. It uses the expression parser to extract and optimize the
 * function parameters.
 * <p>
 * Please note: This class is in a beta state and may change in future releases.
 *
 * @author Kevin Majchrzak
 * @since 9.10
 */
public class FunctionFitting extends Operator implements TableCapabilityProvider {

	/**
	 * Container for the optimization results.
	 */
	private static final class FittingResult {
		/**
		 * Table holding the optimized parameters and the corresponding error.
		 */
		private final Table parameters;

		/**
		 * Table holding the original input and an additional prediction column.
		 */
		private final Table prediction;

		private final FunctionFittingModel model;

		private FittingResult(Table parameters, Table prediction, FunctionFittingModel model) {
			this.parameters = parameters;
			this.prediction = prediction;
			this.model = model;
		}
	}

	// General parameters
	private static final String PARAMETER_EXPRESSION = "expression";
	private static final String PARAMETER_OPTIMIZATION_ALGORITHM = "optimization_algorithm";
	private static final String PARAMETER_MAX_ITERATIONS = "max_iterations";
	private static final String PARAMETER_MAX_EVALUATIONS = "max_evaluations";
	private static final String PARAMETER_USE_SEED = "use_local_random_seed";
	private static final String PARAMETER_SEED = "local_random_seed";
	private static final String PARAMETER_GUESS = "initial_parameter_values";
	private static final String PARAMETER_GUESS_NAME = "initial_parameter_name";
	private static final String PARAMETER_GUESS_VALUE = "initial_parameter_value";
	private static final String PARAMETER_BOUNDS = "parameter_bounds";
	private static final String PARAMETER_BOUNDS_NAME = "parameter_name";
	private static final String PARAMETER_BOUNDS_MIN = "min";
	private static final String PARAMETER_BOUNDS_MAX = "max";

	// CMAES parameters
	private static final String PARAMETER_CMAES_SET_POPULATION = "set_population_size";
	private static final String PARAMETER_CMAES_SIGMA = "sigma";
	private static final String PARAMETER_CMAES_POPULATION = "population_size";
	private static final String PARAM_CMAES_ACTIVE_CMA = "active_cma";
	private static final String PARAM_CMAES_DIAGONAL_ONLY = "diagonal_only";
	private static final String PARAM_CMAES_CHECK_FEASIBLE_COUNT = "feasible_count";
	private static final String PARAM_CMAES_STOP_IMPROVEMENT = "stop_improvement";
	private static final String PARAM_CMAES_STOP_ERROR = "stop_error";
	private static final String PARAM_CMAES_RANDOM_GENERATOR = "random_generator";

	// BOBYQA parameters
	private static final String PARAMETER_BOBYQA_SET_INTERPOLATION = "set_interpolation_points";
	private static final String PARAM_BOBYQA_INTERPOLATION = "interpolation_points";
	private static final String PARAM_BOBYQA_INITIAL_TRUST_REGION = "initial_trust";
	private static final String PARAM_BOBYQA_STOP_TRUST_REGION = "stop_trust";

	// available optimization algorithms
	private static final String CMA_ES = "CMA-ES";
	private static final String BOBYQA = "BOBYQA";

	/**
	 * Default threshold used for CMA-ES convergence criteria.
	 */
	private static final double EPSILON = 1e-30;

	private final InputPort tableInput = getInputPorts().createPort("training set");
	private final OutputPort predictionOutput = getOutputPorts().createPort("prediction");
	private final OutputPort parameterOutput = getOutputPorts().createPort("parameters");
	private final OutputPort modelOutput = getOutputPorts().createPort("model");
	private final OutputPort originalOutput = getOutputPorts().createPort("original");

	public FunctionFitting(OperatorDescription description) {
		super(description);
		// we want table meta data as input
		tableInput.addPrecondition(new TableCapabilityPrecondition(this, tableInput));

		// pass through the original data
		getTransformer().addPassThroughRule(tableInput, originalOutput);
		// rename the attribute metadata
		getTransformer().addRule(new TablePassThroughRule(tableInput, predictionOutput, SetRelation.EQUAL) {
			@Override
			public TableMetaData modifyTableMetaData(TableMetaData metaData) {
				return FunctionFitting.this.createPredictionMetaData(metaData);
			}
		});

		getTransformer().addRule(new TablePassThroughRule(tableInput, parameterOutput, SetRelation.EQUAL) {
			@Override
			public TableMetaData modifyTableMetaData(TableMetaData metaData) {
				return FunctionFitting.this.createParameterMetaData(metaData);
			}
		});
		getTransformer().addRule(
				new GenerateTableModelTransformationRule(tableInput, modelOutput, FunctionFittingModel.class,
						GeneralModel.ModelKind.SUPERVISED));
	}

	@Override
	public void doWork() throws OperatorException {
		IOTable ioTable = tableInput.getData(IOTable.class);
		new TableCapabilityCheck(this).checkCapabilities(ioTable.getTable(), this);
		FittingResult result = fit(ioTable.getTable());
		originalOutput.deliver(ioTable);
		IOTable newIOTable = new IOTable(result.prediction);
		newIOTable.getAnnotations().addAll(ioTable.getAnnotations());
		predictionOutput.deliver(newIOTable);
		parameterOutput.deliver(new IOTable(result.parameters));
		modelOutput.deliver(result.model);
	}

	@Override
	public Set<TableCapability> supported() {
		return EnumSet.of(TableCapability.NOMINAL_COLUMNS, TableCapability.TWO_CLASS_COLUMNS,
				TableCapability.NUMERIC_COLUMNS, TableCapability.DATE_TIME_COLUMNS,
				TableCapability.TIME_COLUMNS, TableCapability.ADVANCED_COLUMNS,
				TableCapability.NUMERIC_LABEL);
	}

	@Override
	public Set<TableCapability> unsupported() {
		return EnumSet.of(TableCapability.MISSING_VALUES, TableCapability.NOMINAL_LABEL,
				TableCapability.ONE_CLASS_LABEL,
				TableCapability.TWO_CLASS_LABEL, TableCapability.NO_LABEL, TableCapability.MULTIPLE_LABELS,
				TableCapability.MISSINGS_IN_LABEL, TableCapability.UPDATABLE, TableCapability.WEIGHTED_ROWS);
	}

	@Override
	public boolean isLearner() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeTableExpression(PARAMETER_EXPRESSION,
				"Function and parameters used for fitting.", tableInput);
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_OPTIMIZATION_ALGORITHM,
				"Choose optimization algorithm used for fitting.",
				new String[]{BOBYQA, CMA_ES}, 0, false);
		types.add(type);

		type = new ParameterTypeList(PARAMETER_GUESS, "Click to initial parameter values.",
				new ParameterTypeString(PARAMETER_GUESS_NAME, "Parameter name"),
				new ParameterTypeDouble(PARAMETER_GUESS_VALUE, "The initial parameter value.",
						Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false));
		types.add(type);

		type = new ParameterTypeList(PARAMETER_BOUNDS, "Click to set parameter bounds.",
				new ParameterTypeString(PARAMETER_BOUNDS_NAME, "Parameter name"),
				new ParameterTypeTupel("values (Min, Max)", "(Min,Max)",
						new ParameterTypeDouble(PARAMETER_BOUNDS_MIN, "Lower bound for the paramter value",
								Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, false),
						new ParameterTypeDouble(PARAMETER_BOUNDS_MAX, "Upper bound for the paramter value",
								Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, false)));
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MAX_ITERATIONS, "Maximum number of iterations.",
				1, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MAX_EVALUATIONS, "Maximum number of evaluations.",
				1, Integer.MAX_VALUE, Integer.MAX_VALUE, true);
		types.add(type);

		types.addAll(createCMAESParameterTypes());
		types.addAll(createBOBYQAParameterTypes());

		return types;
	}

	/**
	 * Creates the prediction column from the parameters.
	 *
	 * @param table
	 * 		the table to predict for
	 * @param parameterNames
	 * 		the parameter names
	 * @param parameterValues
	 * 		the values for the parameter names
	 * @param expressionString
	 * 		the expression
	 * @param operator
	 * 		the calling operator
	 * @return the prediction column
	 * @throws UserError
	 * 		if the expression evaluation fails
	 */
	static Column createPredictionColumn(Table table, String[] parameterNames, double[] parameterValues,
										 String expressionString, Operator operator) throws UserError {
		DynamicResolver variableResolver = new TableResolver(table);
		ExpressionParser parser =
				createParserWithParameters(variableResolver, parameterNames, parameterValues, operator);
		ExpressionContext context = parser.getExpressionContext();
		Expression expression;
		NumericBuffer numericBuffer = Buffers.realBuffer(table.height(), false);
		try {
			expression = parser.parse(expressionString);
			for (int i = 0; i < table.height(); i++) {
				context.setIndex(i);
				numericBuffer.set(i, expression.evaluateNumerical());
			}
		} catch (ExpressionException e) {
			throw ExpressionParserUtils.convertToUserError(operator, expressionString, e);
		}
		return numericBuffer.toColumn();
	}

	/**
	 * @return the bobyqa specific parameter types.
	 */
	private List<ParameterType> createBOBYQAParameterTypes() {
		List<ParameterType> types = new ArrayList<>();

		ParameterType type = new ParameterTypeBoolean(PARAMETER_BOBYQA_SET_INTERPOLATION,
				"Manually specify the number of interpolation points.",
				false, true);
		types.add(type);

		type = new ParameterTypeInt(PARAM_BOBYQA_INTERPOLATION,
				"Number of interpolation points. Must be in the interval [n+2, (n+1)(n+2)/2] for n-dimensional problems.",
				4, Integer.MAX_VALUE, 10, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_BOBYQA_SET_INTERPOLATION, true, true));
		types.add(type);

		type = new ParameterTypeDouble(PARAM_BOBYQA_INITIAL_TRUST_REGION,
				"The initial trust region radius.",
				0, Double.MAX_VALUE, BOBYQAOptimizer.DEFAULT_INITIAL_RADIUS, true);
		types.add(type);

		type = new ParameterTypeDouble(PARAM_BOBYQA_STOP_TRUST_REGION,
				"Stopping criterion. Algorithm stops if the trust region radius drops below this threshold.",
				0, Double.MAX_VALUE, BOBYQAOptimizer.DEFAULT_STOPPING_RADIUS, true);
		types.add(type);

		// show bobyqa parameters only if bobyqa is the chosen optimization algorithm
		EqualStringCondition bobyqaCondition = new EqualStringCondition(this,
				PARAMETER_OPTIMIZATION_ALGORITHM, false, BOBYQA);
		for (ParameterType cmaESType : types) {
			cmaESType.registerDependencyCondition(bobyqaCondition);
		}

		return types;
	}

	/**
	 * @return the cma-es specific parameter types.
	 */
	private List<ParameterType> createCMAESParameterTypes() {
		List<ParameterType> types = new ArrayList<>();

		ParameterType type = new ParameterTypeDouble(PARAMETER_CMAES_SIGMA, "Initial step size.", 0,
				Double.POSITIVE_INFINITY, 5, true);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_CMAES_SET_POPULATION,
				"Manually specify population size.", false, true);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_CMAES_POPULATION, "Population size.", 1,
				Integer.MAX_VALUE, 5, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_CMAES_SET_POPULATION, true, true));
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_USE_SEED, "Choose a seed for the internal source of randomness.",
				false, true);
		types.add(type);

		type = new ParameterTypeLong(PARAMETER_SEED, "Choose a seed for the internal source of randomness.",
				Long.MIN_VALUE, Long.MAX_VALUE, 64193543937302973L, true);
		type.registerDependencyCondition(new BooleanParameterCondition(this,
				PARAMETER_USE_SEED, true, true));
		types.add(type);

		type = new ParameterTypeBoolean(PARAM_CMAES_ACTIVE_CMA,
				"Uses active Covariance Matrix Adaptation if this parameter is checked.",
				true, true);
		types.add(type);

		type = new ParameterTypeInt(PARAM_CMAES_DIAGONAL_ONLY,
				"Number of initial iterations with diagonal covariance matrix.",
				0, Integer.MAX_VALUE, 0, true);
		types.add(type);

		type = new ParameterTypeInt(PARAM_CMAES_CHECK_FEASIBLE_COUNT,
				"Number of times new random offspring is generated in case it is outside of the defined bounds.",
				0, Integer.MAX_VALUE, 0, true);
		types.add(type);

		type = new ParameterTypeDouble(PARAM_CMAES_STOP_IMPROVEMENT,
				"Stopping criterion. Algorithm stops if the improvement is below this threshold.",
				0, Double.POSITIVE_INFINITY, EPSILON, true);
		types.add(type);

		type = new ParameterTypeDouble(PARAM_CMAES_STOP_ERROR,
				"Stopping criterion. Algorithm stops if the error is below this threshold.",
				0, Double.POSITIVE_INFINITY, EPSILON, true);
		types.add(type);

		// TODO add if time is left (or remove for now): PARAM_CMAES_RANDOM_GENERATOR

		// show cma-es parameters only if cma-es is the chosen optimization algorithm
		EqualStringCondition cmaESCondition = new EqualStringCondition(this,
				PARAMETER_OPTIMIZATION_ALGORITHM, false, CMA_ES);
		for (ParameterType cmaESType : types) {
			cmaESType.registerDependencyCondition(cmaESCondition);
		}

		return types;
	}

	/**
	 * @return the meta data for the parameters output
	 */
	private TableMetaData createParameterMetaData(TableMetaData metaData) {
		if (isParameterSet(PARAMETER_EXPRESSION)) {
			try {
				String expressionString = getParameterAsString(PARAMETER_EXPRESSION);
				if (expressionString == null || expressionString.isEmpty()) {
					parameterOutput.addError(new SimpleMetaDataError(ProcessSetupError.Severity.ERROR, parameterOutput,
							Collections.singletonList(new ParameterSettingQuickFix(this, PARAMETER_EXPRESSION)),
							"empty_expression", PARAMETER_EXPRESSION));
				}
				String[] parameterNames = extractParameterNames(new TableMetaDataResolver(
						BeltMetaDataTools.regularSubtable(metaData)), expressionString);
				TableMetaDataBuilder builder = new TableMetaDataBuilder(1);
				for (String parameterName : parameterNames) {
					builder.addReal(parameterName, null, null, new MDInteger(0));
				}
				builder.addReal("error", null, null, new MDInteger(0));
				return builder.build();
			} catch (UndefinedParameterError e) {
				// cannot happen since we checked that the parameter is set
			} catch (ExpressionException e) {
				parameterOutput.addError(new SimpleMetaDataError(ProcessSetupError.Severity.ERROR,
						parameterOutput, "cannot_create_exampleset_metadata", e.getShortMessage()));
			}
		}
		return new TableMetaData();
	}

	/**
	 * @return the meta data for the prediction output
	 */
	private TableMetaData createPredictionMetaData(TableMetaData metaData) {
		Set<String> labels = metaData.selectByColumnMetaData(ColumnRole.LABEL);
		if (!labels.isEmpty()) {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(metaData);
			String predictionColumnName = "prediction(" + labels.iterator().next() + ")";
			builder.addReal(predictionColumnName, null, null, null);
			builder.addColumnMetaData(predictionColumnName, ColumnRole.PREDICTION);
			return builder.build();
		}
		return metaData;
	}

	/**
	 * Fits the parameters to the data points. Returns the optimized parameters and error, the original table with an
	 * additional prediction column and the model.
	 */
	private FittingResult fit(Table table) throws OperatorException {

		String expressionString = getParameterAsString(PARAMETER_EXPRESSION);
		if (expressionString == null || expressionString.isEmpty()) {
			throw new UserError(this, "empty_expression", PARAMETER_EXPRESSION);
		}

		// ignore special attributes in function
		Table regularTable = BeltTools.regularSubtable(table);
		TableResolver tableResolver = new TableResolver(regularTable);

		String[] parameterNames;
		try {
			parameterNames = extractParameterNames(tableResolver, expressionString);
		} catch (ExpressionException e) {
			throw ExpressionParserUtils.convertToUserError(this, expressionString, e);
		}
		Map<String, Integer> nameToIndex = new HashMap<>(parameterNames.length);
		for (int index = 0; index < parameterNames.length; index++) {
			nameToIndex.put(parameterNames[index], index);
		}
		SimpleBounds bounds = createBounds(nameToIndex);
		InitialGuess guess = createInitialGuess(nameToIndex);

		MultivariateFunction function = createObjectiveFunction(table, expressionString,
				tableResolver, parameterNames);

		PointValuePair result;
		try {
			if (BOBYQA.equals(getParameterAsString(PARAMETER_OPTIMIZATION_ALGORITHM))) {
				result = optimizeBOBYQA(parameterNames.length, function, guess, bounds);
			} else {
				result = optimizeCMAES(parameterNames.length, function, guess, bounds);
			}
		} catch (ExpressionExceptionWrapper e) {
			throw ExpressionParserUtils.convertToUserError(this, expressionString, e.unwrap());
		}

		double[] optimalParams = result.getPoint();
		TableBuilder builder = Builders.newTableBuilder(1);
		for (int i = 0; i < parameterNames.length; i++) {
			double optimalParameter = optimalParams[i];
			builder.addReal(parameterNames[i], k -> optimalParameter);
		}
		builder.addReal("error", i -> result.getValue());
		Table parameterTable = builder.build(BeltTools.getContext(this));
		Table prediction = createPrediction(table, parameterNames, optimalParams, expressionString);
		FunctionFittingModel model = new FunctionFittingModel(table, expressionString, optimalParams, parameterNames);
		return new FittingResult(parameterTable, prediction, model);
	}

	/**
	 * Creates the objective function (sum of the squared errors). The objective function may throw
	 * ExpressionExceptionWrapper exceptions if the expression evaluation fails.
	 *
	 * @param table
	 * 		the table, used to read the label values
	 * @param expressionString
	 * 		the parametrized function
	 * @param tableResolver
	 * 		the table resolver, used to resolve dynamic variabes
	 * @param parameterNames
	 * 		the names of the parameters to be optimized
	 * @return the multivariate objective function
	 * @throws UserError
	 * 		if the
	 */
	private MultivariateFunction createObjectiveFunction(Table table, String expressionString,
														 TableResolver tableResolver, String[] parameterNames) throws UserError {
		List<Column> columns = table.select().withMetaData(ColumnRole.LABEL).columns();
		if (columns.size() != 1) {
			throw new UserError(this, "one_label_expected");
		}
		NumericReader labelReader = Readers.numericReader(columns.get(0));

		return parameterValues -> {
			ExpressionParser parser = createParserWithParameters(tableResolver, parameterNames, parameterValues, FunctionFitting.this);
			ExpressionContext context = parser.getExpressionContext();
			labelReader.setPosition(-1);
			// calculate the sum of the squared errors
			double objective = 0;
			try {
				Expression expression = parser.parse(expressionString);
				for (int i = 0; i < table.height(); i++) {
					context.setIndex(i);
					double error = labelReader.read() - expression.evaluateNumerical();
					objective += error * error;
				}
			} catch (ExpressionException e) {
				// wrap it so that we can extract it later
				throw new ExpressionExceptionWrapper(e);
			}
			return objective;
		};
	}

	/**
	 * Constructs the initial guess from the user input or falls back to the default ({@code 0.5}).
	 */
	private InitialGuess createInitialGuess(Map<String, Integer> nameToIndex) throws UserError {
		double[] result = new double[nameToIndex.size()];
		Arrays.fill(result, 0.5);
		if (!isParameterSet(PARAMETER_GUESS)) {
			return new InitialGuess(result);
		}

		List<String[]> nameValueList = getParameterList(PARAMETER_GUESS);
		for (String[] nameValuePair : nameValueList) {
			if (nameValuePair != null) {
				String parameterName = nameValuePair[0];
				double parameterValue = Double.parseDouble(nameValuePair[1]);
				if (nameToIndex.containsKey(parameterName)) {
					int index = nameToIndex.get(parameterName);
					result[index] = parameterValue;
				} else {
					throw new UserError(this, "unknown_parameter", parameterName,
							PARAMETER_GUESS.replace('_', ' '), nameToIndex.keySet());
				}
			}
		}
		return new InitialGuess(result);
	}

	/**
	 * Constructs the bounds from the user input or falls back to the default (unbounded).
	 */
	private SimpleBounds createBounds(Map<String, Integer> nameToIndex) throws UserError {
		if (!isParameterSet(PARAMETER_BOUNDS)) {
			return SimpleBounds.unbounded(nameToIndex.size());
		}
		double[] lowerBounds = new double[nameToIndex.size()];
		double[] upperBounds = new double[nameToIndex.size()];

		Arrays.fill(lowerBounds, Double.NEGATIVE_INFINITY);
		Arrays.fill(upperBounds, Double.POSITIVE_INFINITY);

		List<String[]> boundsList = getParameterList(PARAMETER_BOUNDS);
		for (String[] bounds : boundsList) {
			if (bounds != null) {
				String parameterName = bounds[0];
				if (nameToIndex.containsKey(parameterName)) {
					int index = nameToIndex.get(parameterName);
					String[] tupel = ParameterTypeTupel.transformString2Tupel(bounds[1]);
					lowerBounds[index] = Double.parseDouble(tupel[0]);
					upperBounds[index] = Double.parseDouble(tupel[1]);
				} else {
					throw new UserError(this, "unknown_parameter", parameterName,
							PARAMETER_BOUNDS.replace('_', ' '), nameToIndex.keySet());
				}
			}
		}

		return new SimpleBounds(lowerBounds, upperBounds);
	}

	/**
	 * Minimizes the given function via BOBYQA.
	 */
	private PointValuePair optimizeBOBYQA(int numberOfParameters, MultivariateFunction function,
										  InitialGuess initialGuess, SimpleBounds bounds) throws UndefinedParameterError {
		PointValuePair result;
		int interpolationPoints = getParameterAsBoolean(PARAMETER_BOBYQA_SET_INTERPOLATION) ?
				getParameterAsInt(PARAM_BOBYQA_INTERPOLATION) : 2 * numberOfParameters + 1;
		BOBYQAOptimizer optimizer = new BOBYQAOptimizer(interpolationPoints,
				getParameterAsDouble(PARAM_BOBYQA_INITIAL_TRUST_REGION),
				getParameterAsDouble(PARAM_BOBYQA_STOP_TRUST_REGION));
		result = optimizer.optimize(new ObjectiveFunction(function), GoalType.MINIMIZE,
				initialGuess, bounds, new MaxEval(getParameterAsInt(PARAMETER_MAX_EVALUATIONS)),
				new MaxIter(getParameterAsInt(PARAMETER_MAX_ITERATIONS)));
		return result;
	}

	/**
	 * Minimizes the given function via CMA-ES.
	 */
	private PointValuePair optimizeCMAES(int numberOfParameters, MultivariateFunction function,
										 InitialGuess initialGuess, SimpleBounds bounds) throws UndefinedParameterError {
		double[] sigma = new double[numberOfParameters];
		Arrays.fill(sigma, getParameterAsDouble(PARAMETER_CMAES_SIGMA));

		int populationSize = getParameterAsBoolean(PARAMETER_CMAES_SET_POPULATION) ?
				getParameterAsInt(PARAMETER_CMAES_POPULATION)
				: (int) Math.ceil(4 + 3 * Math.log(numberOfParameters));

		RandomGenerator random = getParameterAsBoolean(PARAMETER_USE_SEED) ?
				new MersenneTwister(getParameterAsLong(PARAMETER_SEED)) : new MersenneTwister();

		CMAESOptimizer optimizer = new CMAESOptimizer(getParameterAsInt(PARAMETER_MAX_ITERATIONS),
				getParameterAsDouble(PARAM_CMAES_STOP_ERROR), getParameterAsBoolean(PARAM_CMAES_ACTIVE_CMA),
				getParameterAsInt(PARAM_CMAES_DIAGONAL_ONLY), getParameterAsInt(PARAM_CMAES_CHECK_FEASIBLE_COUNT),
				random, false, new SimpleValueChecker(getParameterAsDouble(PARAM_CMAES_STOP_IMPROVEMENT),
				getParameterAsDouble(PARAM_CMAES_STOP_ERROR), getParameterAsInt(PARAMETER_MAX_ITERATIONS)));

		return optimizer.optimize(new ObjectiveFunction(function), GoalType.MINIMIZE,
				initialGuess, bounds, new CMAESOptimizer.Sigma(sigma), new CMAESOptimizer.PopulationSize(populationSize),
				new MaxEval(getParameterAsInt(PARAMETER_MAX_EVALUATIONS)));
	}

	/**
	 * Checks for variables that are not specified via the given variable resolver and extracts their names so that they
	 * can be used as optimization parameters.
	 */
	private String[] extractParameterNames(DynamicResolver variableResolver, String expression) throws ExpressionException {
		NumericParameterExtractor extractor = new NumericParameterExtractor();
		ExpressionParser fakeParser = createFakeParser(variableResolver, extractor);
		fakeParser.parse(expression);
		return extractor.getParameterNames();
	}

	/**
	 * Helper method used in {@link #extractParameterNames(DynamicResolver, String)} to create the parser used to extract
	 * the parameter names.
	 */
	private ExpressionParser createFakeParser(DynamicResolver variableResolver, DynamicResolver fakeResolver) {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		if (getProcess() != null) {
			builder.withProcess(getProcess());
			builder.withScope(new MacroResolver(getProcess().getMacroHandler(), this));
		}
		builder.withDynamics(variableResolver);
		builder.withDynamics(fakeResolver);
		builder.withModules(ExpressionRegistry.INSTANCE.getAll());
		return builder.build();
	}

	/**
	 * Creates a parameterized expression parser. (Sets the given parameter names and values as constants for this
	 * expression parser instance.)
	 */
	private static ExpressionParser createParserWithParameters(DynamicResolver variableResolver,
															   String[] parameterNames,
															   double[] parameterValues, Operator operator) {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		if (operator != null && operator.getProcess() != null) {
			builder.withProcess(operator.getProcess());
			builder.withScope(new MacroResolver(operator.getProcess().getMacroHandler(), operator));
		}
		builder.withDynamics(variableResolver);
		builder.withModules(ExpressionRegistry.INSTANCE.getAll());
		builder.withModules(Collections.singletonList(new FunctionFittingParameters(parameterNames, parameterValues)));
		return builder.build();
	}

	/**
	 * Creates a parametrized expression parser and applies it to the given expression to create a prediction column.
	 */
	private Table createPrediction(Table table, String[] parameterNames,
								   double[] parameterValues, String expressionString) throws UserError {
		Column predictionColumn =
				createPredictionColumn(table, parameterNames, parameterValues, expressionString, this);
		String labelColumnName = table.select().withMetaData(ColumnRole.LABEL).labels().get(0);
		String predictionColumnName = "prediction(" + labelColumnName + ")";
		return Builders.newTableBuilder(table)
				.add(predictionColumnName, predictionColumn)
				.addMetaData(predictionColumnName, ColumnRole.PREDICTION)
				.addMetaData(predictionColumnName, new ColumnReference(labelColumnName))
				.build(BeltTools.getContext(this));
	}

}