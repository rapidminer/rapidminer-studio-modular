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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.ports.InputPort;


/**
 * Super class for meta data of models operating on different {@link com.rapidminer.operator.IOObject}s. The generic
 * types {@code T} and {@code S} must match the model class, e.g. be the meta data for the type parameters of the
 * model.
 *
 * @author Gisa Meier
 * @see GeneralModel
 * @since 9.10
 */
public abstract class GeneralModelMetaData<T extends MetaData, S extends MetaData> extends MetaData {

	private static final long serialVersionUID = 1L;

	private T trainingSetMetaData;

	private Set<GeneralModel.ModelKind> modelKinds;

	/**
	 * Clone constructor
	 */
	protected GeneralModelMetaData() {
	}

	/**
	 * Constructs new model meta data for a {@link GeneralModel}. The types {@code T} and {@code S} must match the
	 * model class, e.g. be the meta data for the type parameters of the model.
	 *
	 * @param mclass
	 * 		the model class
	 * @param trainingMetaData
	 * 		the training meta data
	 * @param modelKinds
	 * 		the optional model kinds that this has
	 */
	protected GeneralModelMetaData(Class<? extends GeneralModel<?, ?>> mclass, T trainingMetaData,
								   GeneralModel.ModelKind... modelKinds) {
		super(mclass);
		this.trainingSetMetaData = trainingMetaData;
		this.modelKinds = new HashSet<>(Arrays.asList(modelKinds));
	}


	/**
	 * This method simulates the application of a model. First the compatibility of the model with the current example
	 * set is checked and then the effects are applied.
	 *
	 * @param metaData
	 * 		the input meta data
	 * @param inputPort
	 * 		the input port
	 * @return the transformed meta data
	 */
	public S apply(T metaData, InputPort inputPort) {
		checkCompatibility(metaData, inputPort);
		return applyEffects(metaData, inputPort);
	}

	/**
	 * Checks the compatibility of the metaData.
	 *
	 * @param metaData
	 * 		the input meta data
	 * @param inputPort
	 * 		the input port
	 */
	protected void checkCompatibility(T metaData, InputPort inputPort) {
	}

	/**
	 * This method must be implemented by subclasses in order to apply any changes on the meta data, that would
	 * occur on
	 * application of the real model.
	 *
	 * @param metaData
	 * 		the input meta data
	 * @param inputPort
	 * 		the input port
	 * @return the resulting meta data when applying the model
	 */
	protected abstract S applyEffects(T metaData, InputPort inputPort);

	@Override
	public GeneralModelMetaData clone() {
		GeneralModelMetaData md = (GeneralModelMetaData) super.clone();
		if (trainingSetMetaData != null) {
			md.trainingSetMetaData = trainingSetMetaData.clone();
		}
		return md;
	}

	public T getTrainingMetaData() {
		return trainingSetMetaData;
	}

	/**
	 * Checks if the model if of a given kind. For most models only returns {@code true} for one kind. Can return false
	 * for all kinds if the model kind is unknown.
	 *
	 * @param modelKind
	 * 		the kind of the model
	 * @return if this model is of this kind
	 */
	public boolean isModelKind(GeneralModel.ModelKind modelKind) {
		if (modelKinds != null) {
			return modelKinds.contains(modelKind);
		}
		return false;
	}

	/**
	 * Returns all model kinds where {@link #isModelKind} returns {@code true}.
	 *
	 * @param mmd
	 * 		the model meta data
	 * @return the array of all model kinds that the model is
	 */
	public static GeneralModel.ModelKind[] modelKindsAsArray(GeneralModelMetaData<?, ?> mmd) {
		return Arrays.stream(GeneralModel.ModelKind.values()).filter(mmd::isModelKind).toArray(GeneralModel.ModelKind[]::new);
	}

	/**
	 * Returns all model kinds where {@link #isModelKind} returns {@code true}.
	 *
	 * @param model
	 * 		the model meta data
	 * @return the array of all model kinds that the model is
	 */
	public static GeneralModel.ModelKind[] modelKindsAsArray(GeneralModel<?, ?> model) {
		return Arrays.stream(GeneralModel.ModelKind.values()).filter(model::isModelKind).toArray(GeneralModel.ModelKind[]::new);
	}
}