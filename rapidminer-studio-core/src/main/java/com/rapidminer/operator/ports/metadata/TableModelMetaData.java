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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;


/**
 * {@link MetaData} for {@link IOTableModel}s.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public class TableModelMetaData extends GeneralModelMetaData<TableMetaData, TableMetaData> {

	/**
	 * Clone constructor
	 */
	protected TableModelMetaData() {
	}

	/**
	 * Constructs new meta data for {@link IOTableModel} with unknown model kinds.
	 *
	 * @param trainingMetaData
	 * 		the associated meta data to the data that the model was trained on
	 */
	public TableModelMetaData(TableMetaData trainingMetaData) {
		this(IOTableModel.class, trainingMetaData);
	}

	/**
	 * Constructs new meta data from the parameters.
	 *
	 * @param mclass
	 * 		the class of the model
	 * @param trainingMetaData
	 * 		the associated meta data to the data that the model was trained on
	 * @param modelKinds
	 * 		the optional model kinds of the model
	 */
	public TableModelMetaData(Class<? extends IOTableModel> mclass, TableMetaData trainingMetaData,
							  GeneralModel.ModelKind... modelKinds) {
		super(mclass, trainingMetaData, modelKinds);
	}

	/**
	 * Constructs the meta data directly from the model it describes.
	 *
	 * @param model
	 * 		the model
	 * @param ignoreStatistics
	 * 		whether the statistics of the training header should be ignored
	 */
	public TableModelMetaData(IOTableModel model, boolean ignoreStatistics) {
		super(model.getClass(), (TableMetaData) MetaData.forIOObject(model.getTrainingHeader(), ignoreStatistics),
				GeneralModelMetaData.modelKindsAsArray(model));
	}

	@Override
	protected TableMetaData applyEffects(TableMetaData tmd, InputPort inputPort) {
		return tmd;
	}

	@Override
	public TableModelMetaData clone() {
		return (TableModelMetaData) super.clone();
	}

}