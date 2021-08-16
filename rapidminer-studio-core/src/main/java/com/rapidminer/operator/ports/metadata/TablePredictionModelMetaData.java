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

import java.util.Set;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.math.container.Range;


/**
 * {@link MetaData} for {@link IOTablePredictionModel}s.
 *
 * @author Gisa Meier
 * @since 9.10.0
 */
public class TablePredictionModelMetaData extends TableModelMetaData {

	private static final long serialVersionUID = 1L;

	private Pair<String, ColumnInfo> predictedLabelMetaData;

	private TableMetaData generatedPredictionColumns;

	/**
	 * Clone constructor
	 */
	protected TablePredictionModelMetaData() {
	}

	/**
	 * Constructs model meta data without training meta data.
	 *
	 * @param modelClass
	 * 		the prediction model class
	 */
	public TablePredictionModelMetaData(Class<? extends IOTablePredictionModel> modelClass) {
		this(modelClass, null);
	}

	/**
	 * Constructs model meta data for the class and the training meta data.
	 *
	 * @param modelClass
	 * 		the prediction model class
	 * @param trainingMetaData
	 * 		the associated meta data to the data that the model was trained on
	 */
	public TablePredictionModelMetaData(Class<? extends IOTablePredictionModel> modelClass,
										TableMetaData trainingMetaData) {
		super(modelClass, trainingMetaData, GeneralModel.ModelKind.SUPERVISED);
		if (trainingMetaData != null) {
			Set<String> labels = trainingMetaData.selectByColumnMetaData(ColumnRole.LABEL);
			if (!labels.isEmpty()) {
				String firstLabel = labels.iterator().next();
				ColumnInfoBuilder builder = new ColumnInfoBuilder(trainingMetaData.column(firstLabel));
				if (builder.isNominal() == MetaDataInfo.YES && !builder.getDictionary().getValueSet().isEmpty()) {
					builder.setValueSetRelation(SetRelation.SUBSET);
				} else {
					builder.setValueSetRelation(SetRelation.SUPERSET);
				}
				builder.setMissings(MDInteger.newUnknown());
				ColumnInfo prediction = builder.build();
				String predictionName = "prediction(" + firstLabel + ")";
				predictedLabelMetaData = new Pair<>(predictionName, prediction);

				TableMetaDataBuilder predictionColumnBuilder = new TableMetaDataBuilder(0);
				predictionColumnBuilder.add(predictionName, prediction).addColumnMetaData(predictionName,
						ColumnRole.PREDICTION);

				// creating confidences
				if (prediction.isNominal() == MetaDataInfo.YES) {
					addConfidences(prediction, predictionName, predictionColumnBuilder);
				}

				generatedPredictionColumns = predictionColumnBuilder.build();
			}
		}
	}

	@Override
	public TableMetaData applyEffects(TableMetaData tmd, InputPort inputPort) {
		if (predictedLabelMetaData == null) {
			return tmd;
		}
		TableMetaData predictionColumns = getPredictionMetaData();
		if (predictionColumns != null) {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(tmd);
			for (String label : predictionColumns.labels()) {
				builder.add(label, predictionColumns.column(label)).addColumnMetaData(label,
						predictionColumns.getColumnMetaData(label));
			}
			builder.mergeColumnSetRelation(getPredictionColumnSetRelation());
			tmd = builder.build();
		}
		return tmd;
	}

	/**
	 * Returns the table meta data consisting of only the prediction and confidence column infos.
	 *
	 * @return the prediction meta data
	 */
	public TableMetaData getPredictionMetaData() {
		return generatedPredictionColumns;
	}

	/**
	 * Returns the prediction column name and its column info.
	 *
	 * @return a pair of column name and column info for the predicted label
	 */
	public Pair<String, ColumnInfo> getPredictedLabelMetaData() {
		return predictedLabelMetaData;
	}

	/**
	 * Returns the {@link SetRelation} for the prediction column.
	 *
	 * @return the set relation for the prediction column
	 */
	public SetRelation getPredictionColumnSetRelation() {
		if (predictedLabelMetaData != null) {
			return predictedLabelMetaData.getSecond().getValueSetRelation();
		} else {
			return SetRelation.UNKNOWN;
		}
	}

	@Override
	public String getDescription() {
		return super.getDescription() + "; generates: " + predictedLabelMetaData;
	}

	@Override
	public TablePredictionModelMetaData clone() {
		TablePredictionModelMetaData clone = (TablePredictionModelMetaData) super.clone();
		if (this.predictedLabelMetaData != null) {
			clone.predictedLabelMetaData = this.predictedLabelMetaData;
		}
		if (this.generatedPredictionColumns != null) {
			clone.generatedPredictionColumns = this.generatedPredictionColumns.clone();
		}
		return clone;
	}

	/**
	 * Adds confidence column infos to the builder.
	 */
	private void addConfidences(ColumnInfo prediction, String predictionName,
								TableMetaDataBuilder builder) {
		ColumnInfo confidenceInfo =
				new ColumnInfoBuilder(ColumnType.REAL).setNumericRange(new Range(0, 1),
						SetRelation.SUBSET).build();
		Set<String> valueSet = prediction.getDictionary().getValueSet();
		if (valueSet.isEmpty()) {
			String name = Attributes.CONFIDENCE_NAME + "(?)";
			builder.add(name, confidenceInfo)
					.addColumnMetaData(name, ColumnRole.SCORE)
					.addColumnMetaData(name, new ColumnReference(predictionName, "?"));
		} else {
			for (String value : valueSet) {
				String name = Attributes.CONFIDENCE_NAME + "(" + value + ")";
				builder.add(name, confidenceInfo)
						.addColumnMetaData(name, ColumnRole.SCORE)
						.addColumnMetaData(name, new ColumnReference(predictionName, value));
			}
		}
	}

}