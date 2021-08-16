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
package com.rapidminer.operator.preprocessing;

import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.operator.AbstractIOTableModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.belt.BeltTools;


/**
 * Super class for preprocessing models working on {@link IOTable}s. Analog to {@link PreprocessingModel}.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public abstract class IOTablePreprocessingModel extends AbstractIOTableModel {

	/**
	 * Constructs a preprocessing model trained with the given ioTable.
	 *
	 * @param ioTable
	 * 		the training table
	 */
	protected IOTablePreprocessingModel(IOTable ioTable) {
		super(ioTable);
	}

	/**
	 * Constructor for json deserialization
	 */
	protected IOTablePreprocessingModel() {
		super();
	}

	/**
	 * Applies the model.
	 *
	 * @param adjusted
	 * 		input adjusted to training header
	 * @param builder
	 * 		builder based on original input to use for changes
	 * @param operator
	 * 		calling operator, can be {@code null}
	 */
	public abstract void applyOnData(Table adjusted, TableBuilder builder, Operator operator) throws OperatorException;

	@Override
	public IOTable apply(IOTable ioTable, Operator operator) throws OperatorException {
		Table table = ioTable.getTable();
		// adapting table to contain only columns, which were present during learning time
		Table adapted = Tables.adapt(table, getTrainingHeader().getTable(),
				Tables.ColumnHandling.REORDER, needsRemapping() ? Tables.DictionaryHandling.CHANGE :
						Tables.DictionaryHandling.UNCHANGED);

		TableBuilder builder = Builders.newTableBuilder(table);
		applyOnData(adapted, builder, operator);

		IOTable result = new IOTable(builder.build(BeltTools.getContext(operator)));
		result.getAnnotations().addAll(ioTable.getAnnotations());
		return result;
	}

	@Override
	public boolean isModelKind(ModelKind modelKind) {
		return modelKind == ModelKind.PREPROCESSING;
	}


	@Override
	public String toResultString() {
		StringBuilder builder = new StringBuilder();
		List<String> trainingColumns = BeltTools.selectRegularColumns(getTrainingHeader().getTable()).labels();
		builder.append(getName()).append(Tools.getLineSeparators(2));
		builder.append("Model covering ").append(trainingColumns.size()).append(" attributes:").append(Tools.getLineSeparator());
		for (String name : trainingColumns) {
			builder.append(" - ").append(name).append(Tools.getLineSeparator());
		}
		return builder.toString();
	}

	/**
	 * Determines whether before the model application the nominal dictionaries should be remapped on the fly such that
	 * their returned indices match the indices of the training data. Subclasses should overwrite this to adjust to
	 * their needs. Note that remapped columns should not be returned.
	 *
	 * @return whether to remap nominal dictionaries
	 */
	protected abstract boolean needsRemapping();

}
