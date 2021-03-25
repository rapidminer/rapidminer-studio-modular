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
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;


/**
 * Delivers the metadata for the union of tables sets to an output port. If a prefix is specified, duplicate names will
 * be renamed. Otherwise, duplicates are skipped.
 *
 * Analog to {@link com.rapidminer.operator.ports.metadata.ExampleSetUnionRule}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class TableUnionRule implements MDTransformationRule {

	private final InputPort inputPort1;
	private final InputPort inputPort2;
	private final OutputPort outputPort;
	private final String postfixForDuplicates;

	public TableUnionRule(InputPort inputPort1, InputPort inputPort2, OutputPort outputPort,
						  String postfixForDuplicates) {
		this.inputPort1 = inputPort1;
		this.inputPort2 = inputPort2;
		this.outputPort = outputPort;
		this.postfixForDuplicates = postfixForDuplicates;
	}

	/**
	 * Gets the postfix to use for duplicate column names from the second input, can be {@code null} if they should be
	 * ignored.
	 *
	 * @return the postfix for duplicate names
	 */
	protected String getPostfix() {
		return postfixForDuplicates;
	}

	@Override
	public void transformMD() {
		MetaData md1 = inputPort1.getRawMetaData();
		MetaData md2 = inputPort2.getRawMetaData();
		if ((md1 != null) && (md2 != null)) {
			TableMetaData tmd1 = inputPort1.getMetaDataAsOrNull(TableMetaData.class);
			TableMetaData tmd2 = inputPort2.getMetaDataAsOrNull(TableMetaData.class);
			if (tmd1 != null && tmd2 != null) {
				TableMetaData joinedTmd = modifyMetaData(tmd1, tmd2);
				outputPort.deliverMD(joinedTmd);
			} else {
				outputPort.deliverMD(new TableMetaData());
			}
		} else {
			outputPort.deliverMD(null);
		}
	}

	/**
	 * Allows subclasses to transform the column from the right before it is added. Does nothing by default.
	 *
	 * @param leftTmd
	 * 		the left table
	 * @param rightToAdd
	 * 		the column from the right to add
	 * @return the adjusted column from the right
	 */
	protected ColumnInfo transformAddedColumnInfo(TableMetaData leftTmd, ColumnInfo rightToAdd) {
		return rightToAdd;
	}

	/**
	 * Merges the right table to the left. If a column name from the right already exists, the postfix is added or the
	 * column ignored if the postfix is {@code null}.
	 */
	protected TableMetaData modifyMetaData(TableMetaData leftTmd, TableMetaData rightTmd) {
		TableMetaDataBuilder builder = new TableMetaDataBuilder(leftTmd);
		for (String label : rightTmd.labels()) {
			if (leftTmd.contains(label) == MetaDataInfo.YES) {
				if (postfixForDuplicates != null) {
					final ColumnInfo oldColumn = rightTmd.column(label);
					String newLabel = label + postfixForDuplicates;
					while (builder.contains(newLabel) == MetaDataInfo.YES) {
						//handle the rare case that the label plus postfix is already contained
						newLabel += postfixForDuplicates;
					}
					builder.add(newLabel, transformAddedColumnInfo(leftTmd, oldColumn));
					builder.addColumnMetaData(newLabel, rightTmd.getColumnMetaData(label));
				}
				//otherwise ignore duplicates
			} else {
				final ColumnInfo oldColumn = rightTmd.column(label);
				builder.add(label, transformAddedColumnInfo(leftTmd, oldColumn));
				builder.addColumnMetaData(label, rightTmd.getColumnMetaData(label));
			}
		}
		return builder.build();
	}

}
