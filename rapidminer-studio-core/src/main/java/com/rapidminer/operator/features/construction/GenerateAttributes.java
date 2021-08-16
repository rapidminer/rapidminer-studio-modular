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
package com.rapidminer.operator.features.construction;

import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TablePassThroughRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeTableExpression;
import com.rapidminer.tools.belt.BeltTools;
import com.rapidminer.tools.belt.expression.DynamicResolver;
import com.rapidminer.tools.belt.expression.ExpressionException;
import com.rapidminer.tools.belt.expression.ExpressionParser;
import com.rapidminer.tools.belt.expression.ExpressionParserBuilder;
import com.rapidminer.tools.belt.expression.ExpressionRegistry;
import com.rapidminer.tools.belt.expression.MacroResolver;
import com.rapidminer.tools.belt.expression.TableResolver;
import com.rapidminer.tools.belt.expression.internal.ExpressionParserUtils;


/**
 * This is a prototype of the belt version of {@link AttributeConstruction})
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class GenerateAttributes extends Operator {

	/**
	 * The parameter name for &quot;List of functions to generate.&quot;
	 */
	public static final String PARAMETER_FUNCTIONS = "function_descriptions";

	/**
	 * The parameter name for &quot;If set to true, all the original attributes are kept, otherwise they are removed
	 * from the example set.&quot;
	 */
	public static final String PARAMETER_KEEP_ALL = "keep_all";

	private final InputPort tableInput = getInputPorts().createPort("example set input");
	private final OutputPort tableOutput = getOutputPorts().createPort("example set output");
	private final OutputPort originalOutput = getOutputPorts().createPort("original");

	public GenerateAttributes(OperatorDescription description) {
		super(description);
		// we want example set meta data as input
		tableInput.addPrecondition(new SimplePrecondition(tableInput, new ExampleSetMetaData()));
		// pass through the original data
		getTransformer().addPassThroughRule(tableInput, originalOutput);
		// rename the attribute metadata
		getTransformer().addRule(new TablePassThroughRule(tableInput, tableOutput, SetRelation.EQUAL) {
			@Override
			public TableMetaData modifyTableMetaData(TableMetaData metaData) {
				return GenerateAttributes.this.modifyMetaData(metaData);
			}
		});
	}

	private TableMetaData modifyMetaData(TableMetaData metaData) {
		// TODO implement
//		List<String> newAttributeNames = new LinkedList<>();
//
//		BeltResolverWithIndices resolver = new BeltResolverWithIndices(metadata);
//		ExpressionParser parser = ExpressionParserUtils.createAllModulesParserForMetaDataTransformation(this, resolver);
//
//		try {
//
//			List<String[]> parameterList = getParameterList(PARAMETER_FUNCTIONS);
//			for (String[] nameFunctionPair : parameterList) {
//				String name = nameFunctionPair[0];
//				String function = nameFunctionPair[1];
//
//				try {
//					AttributeMetaData amd = ExpressionParserUtils.generateAttributeMetaData(metaData, name,
//							parser.parse(function).getExpressionType());
//
//					newAttributeNames.add(name);
//					metaData.addAttribute(amd);
//
//					// update resolver meta data after meta data change
//					// in case more than one attribute is generated
//					if (parameterList.size() > 1) {
//						resolver.addAttributeMetaData(amd);
//					}
//
//				} catch (ExpressionException e) {
//					if (e.getCause() != null && e.getCause() instanceof UnknownResolverVariableException) {
//						// in case a resolver variable cannot be resolved, return a new attribute
//						// with nominal type
//						metaData.addAttribute(new AttributeMetaData(name, Ontology.NOMINAL));
//					} else {
//						// in all other cases abort meta data generation, add an error and return
//						// empty meta data
//						tableOutput.addError(new SimpleMetaDataError(Severity.ERROR,
//								this.tableOutput, "cannot_create_exampleset_metadata", e.getShortMessage()));
//						return new ExampleSetMetaData();
//					}
//				}
//			}
//
//			if (!getParameterAsBoolean(PARAMETER_KEEP_ALL)) {
//				for (AttributeMetaData attribute : originalAttributes) {
//					if (!newAttributeNames.contains(attribute.getName())) {
//						metaData.removeAttribute(attribute);
//					}
//				}
//			}
//		} catch (UndefinedParameterError e) {
//			// ignore
//		}

		return metaData;
	}

	@Override
	public void doWork() throws OperatorException {
		IOTable ioTable = tableInput.getData(IOTable.class);
		Table result = apply(ioTable.getTable());
		originalOutput.deliver(ioTable);
		IOTable newIOTable = new IOTable(result);
		newIOTable.getAnnotations().addAll(ioTable.getAnnotations());
		tableOutput.deliver(newIOTable);
	}

	private Table apply(Table table) throws OperatorException {
		// create resolver and parser
		TableResolver tableResolver = new TableResolver(table);
		ExpressionParser expParser = createExpressionParser(tableResolver);

		// initialize table builder
		boolean keepAll = getParameterAsBoolean(PARAMETER_KEEP_ALL);
		TableBuilder builder = keepAll ? Builders.newTableBuilder(table) : Builders.newTableBuilder(table.height());

		// generate the columns one by one
		for (String[] nameExpressionPair : getParameterList(PARAMETER_FUNCTIONS)) {
			String name = nameExpressionPair[0];
			String expression = nameExpressionPair[1];
			try {
				Column newColumn = ExpressionParserUtils.createColumn(table.height(), expression, expParser);
				// TODO: Do we want to keep the metadata if the new column has the same name as the old one?
				// (Does not really make sense since it is a newly generated column I would say but looks like
				// that is what used to happen (see ExpressionParserUtils#239))
				if (builder.contains(name)) {
					builder.remove(name);
				}
				builder.add(name, newColumn);
				// adding the newly generated column to the resolver on the fly
				tableResolver.addColumn(name, newColumn);
			} catch (ExpressionException e) {
				throw ExpressionParserUtils.convertToUserError(this, expression, e);
			}
			checkForStop();
		}

		return builder.build(BeltTools.getContext(this));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterTypeTableExpression expressionParameterType = new ParameterTypeTableExpression("function_expressions",
				"Function and arguments to use for generation.", tableInput);
		ParameterType type = new ParameterTypeList(PARAMETER_FUNCTIONS, "List of functions to generate.",
				new ParameterTypeString("attribute_name",
						"Specifies the name of the constructed attribute"), expressionParameterType);
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_ALL,
				"If set to true, all the original attributes are kept, otherwise they are removed from the example set.",
				true));

		return types;
	}

	/**
	 * Creates an expression parser using the given dynamic resolver.
	 *
	 * @param resolver
	 * 		the resolver used for resolving dynamic variables.
	 * @return newly created expression parser
	 */
	private ExpressionParser createExpressionParser(DynamicResolver resolver){
		ExpressionParserBuilder builder = new ExpressionParserBuilder();
		if (getProcess() != null) {
			builder.withProcess(getProcess());
			builder.withScope(new MacroResolver(getProcess().getMacroHandler(), this));
		}
		builder.withDynamics(resolver);
		builder.withModules(ExpressionRegistry.INSTANCE.getAll());
		return builder.build();
	}

}