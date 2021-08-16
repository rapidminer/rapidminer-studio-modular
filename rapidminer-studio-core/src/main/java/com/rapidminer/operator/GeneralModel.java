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

/**
 * Super interface for models operating on different {@link IOObject}s.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public interface GeneralModel<T extends IOObject, S extends IOObject> extends ResultObject {

	/**
	 * The kind of the model.
	 */
	enum ModelKind {
		PREPROCESSING, POSTPROCESSING, SUPERVISED, UNSUPERVISED, FORECASTING;
	}

	/**
	 * Returns the type of {@link IOObject} that the model works on as input.
	 *
	 * @return the type the model can be applied to
	 */
	Class<T> getInputType();

	/**
	 * Returns the type of {@link IOObject} that the model generates.
	 *
	 * @return the type the model puts out
	 */
	Class<S> getOutputType();

	/**
	 * This method has to return the object containing the signature of the data during training time. This is
	 * important
	 * for checking the compatibility of the object on application time.
	 *
	 * @return the training header {@link IOObject}
	 */
	T getTrainingHeader();

	/**
	 * Applies the model on the given {@link IOObject}. Please note that the delivered {@link IOObject} might be the
	 * same as the input {@link IOObject}. This is, however, not always the case. The operator can be {@code null}.
	 *
	 * @param testObject
	 * 		the object to apply the model to
	 * @param operator
	 * 		the operator in which the apply happens
	 * @return the result of the application
	 */
	S apply(T testObject, Operator operator) throws OperatorException;

	/**
	 * This method can be used to allow additional parameters. Most models do not support parameters during
	 * application.
	 *
	 * @param key
	 * 		the key of the parameter to set
	 * @param value
	 * 		the value to set for the key
	 */
	void setParameter(String key, Object value) throws OperatorException;

	/**
	 * Returns true if this model is updatable. Please note that only models which return true here must implement the
	 * method {@link #updateModel(T, Operator)}.
	 *
	 * @return {@code true} iff the model is updatable
	 */
	boolean isUpdatable();

	/**
	 * Updates the model according to the given object. This method might throw an {@link UserError} if the model is
	 * not updatable. In that case the method {@link #isUpdatable()} should deliver false. The operator can be {@code
	 * null}.
	 *
	 * @param updateObject
	 * 		the object to use for updating the model
	 * @param operator
	 * 		the operator in which this happens
	 */
	void updateModel(T updateObject, Operator operator) throws OperatorException;

	/**
	 * Checks if the model if of a given kind. For most models only returns {@code true} for one kind. Can return false
	 * for all kinds if the model kind is unknown.
	 *
	 * @param modelKind
	 * 		the kind of the model
	 * @return if this model is of this kind
	 */
	boolean isModelKind(ModelKind modelKind);
}