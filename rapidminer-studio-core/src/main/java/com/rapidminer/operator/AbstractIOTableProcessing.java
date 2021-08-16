/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.operator;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * Abstract superclass of all operators modifying a table, i.e. accepting an {@link IOTable} as input and delivering an
 * {@link IOTable} as output. The behavior is delegated from the {@link #doWork()} method to {@link #apply(IOTable)}.
 * Analog to {@link AbstractExampleSetProcessing}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public abstract class AbstractIOTableProcessing extends Operator {

	/**
	 * name of the table input port
	 */
	public static final String TABLE_INPUT_PORT_NAME = "example set input";

	/**
	 * name of the table output port
	 */
	public static final String TABLE_OUTPUT_PORT_NAME = "example set output";

	/**
	 * name of the original output port
	 */
	public static final String ORIGINAL_OUTPUT_PORT_NAME = "original";

	private final InputPort tableInput = getInputPorts().createPort(TABLE_INPUT_PORT_NAME);
	private final OutputPort tableOutput = getOutputPorts().createPort(TABLE_OUTPUT_PORT_NAME);
	private final OutputPort originalOutput = getOutputPorts().createPort(ORIGINAL_OUTPUT_PORT_NAME);

	public AbstractIOTableProcessing(OperatorDescription description) {
		super(description);
		tableInput.addPrecondition(new SimplePrecondition(tableInput, getRequiredMetaData()));
		getTransformer().addRule(new PassThroughRule(tableInput, tableOutput, false) {

			@Override
			public MetaData modifyMetaData(MetaData metaData) {
				TableMetaData tmd = BeltConversionTools.asTableMetaDataOrNull(metaData);
				if (tmd != null) {
					try {
						return AbstractIOTableProcessing.this.modifyMetaData(tmd);
					} catch (UndefinedParameterError e) {
						return metaData;
					}
				} else {
					return metaData;
				}
			}
		});
		getTransformer().addPassThroughRule(tableInput, originalOutput);
	}

	/**
	 * Subclasses might override this method to define the meta data transformation performed by this operator.
	 *
	 * @param metaData
	 * 		the input meta data
	 * @return the modified meta data
	 * @throws UndefinedParameterError
	 * 		if a parameter is not defined
	 */
	protected MetaData modifyMetaData(TableMetaData metaData) throws UndefinedParameterError {
		return metaData;
	}

	/**
	 * Returns the required meta data. Subclasses my override this method to define more precisely the meta data
	 * expected by this operator.
	 *
	 * @return the required meta data
	 */
	protected TableMetaData getRequiredMetaData() {
		return new TableMetaData();
	}

	@Override
	public final void doWork() throws OperatorException {
		IOTable inputTable = tableInput.getData(IOTable.class);

		IOTable result = apply(inputTable);
		originalOutput.deliver(inputTable);
		tableOutput.deliver(result);
	}

	/**
	 * The method specifying what the operator does on tables.
	 *
	 * @param ioTable
	 * 		the input table
	 * @return the result table
	 */
	public abstract IOTable apply(IOTable ioTable) throws OperatorException;

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == originalOutput) {
			return false;
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	/**
	 * Returns the table input port.
	 *
	 * @return the input port
	 */
	public InputPort getTableInputPort() {
		return tableInput;
	}

	/**
	 * Returns the table output port.
	 *
	 * @return the result output port
	 */
	public OutputPort getTableOutputPort() {
		return tableOutput;
	}

}
