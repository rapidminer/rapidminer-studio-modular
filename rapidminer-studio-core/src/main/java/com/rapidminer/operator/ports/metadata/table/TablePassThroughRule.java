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
package com.rapidminer.operator.ports.metadata.table;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.belt.BeltConversionTools;


/**
 * Pass through rule for {@link com.rapidminer.adaption.belt.IOTable}s, analog to {@link ExampleSetPassThroughRule}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class TablePassThroughRule extends PassThroughRule {

	private SetRelation relation;

	/**
	 * Creates a new instance.
	 *
	 * @param inputPort
	 * 		the input port receiving the IOTable.
	 * @param outputPort
	 * 		the output port the IOTable will be delivered to.
	 * @param columnSetRelation
	 * 		the given relation will be merged with the column set relation of the incoming meta data.
	 */
	public TablePassThroughRule(InputPort inputPort, OutputPort outputPort, SetRelation columnSetRelation) {
		super(inputPort, outputPort, false);
		this.relation = columnSetRelation;
	}

	@Override
	public MetaData modifyMetaData(MetaData metaData) {
		TableMetaData tmd = BeltConversionTools.asTableMetaDataOrNull(metaData);
		if (tmd != null) {
			if (relation != null && relation != SetRelation.EQUAL) {
				TableMetaDataBuilder builder = new TableMetaDataBuilder(tmd);
				builder.mergeColumnSetRelation(relation);
				tmd = builder.build();
			}
			try {
				return modifyTableMetaData(tmd);
			} catch (UndefinedParameterError e) {
				//ignore
				return tmd;
			}
		} else {
			return metaData;
		}
	}

	/**
	 * This method might be used for convenience for slight modifications of the table like adding a column. Subclasses
	 * might override this method.
	 *
	 * @param metaData
	 * 		the metaData to change
	 * @return the changed metaData
	 * @throws UndefinedParameterError
	 * 		if a required parameter is not set
	 */
	public TableMetaData modifyTableMetaData(TableMetaData metaData) throws UndefinedParameterError {
		// does nothing by default
		return metaData;
	}

}
