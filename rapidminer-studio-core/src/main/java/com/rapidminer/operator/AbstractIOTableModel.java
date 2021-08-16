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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.storage.hdf5.HeaderTableJsonDeserializer;
import com.rapidminer.storage.hdf5.HeaderTableJsonSerializer;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;


/**
 * The interface for all {@link GeneralModel}s operating on {@link IOTable}s.
 *
 * @author Gisa Meier
 * @since 9.10
 */
public abstract class AbstractIOTableModel extends ResultObjectAdapter implements IOTableModel {

	@JsonSerialize(using = HeaderTableJsonSerializer.class)
	@JsonDeserialize(using = HeaderTableJsonDeserializer.class)
	private IOTable headerTable;

	/**
	 * Creates a new model which was built on the given table. Please note that the given table is
	 * automatically stripped of the data which means that no reference to the data itself is
	 * kept but only to the header, i.e. to the column descriptions.
	 */
	protected AbstractIOTableModel(IOTable ioTable) {
		if (ioTable != null) {
			Table header = ioTable.getTable().stripData();
			this.headerTable = new IOTable(header);
		} else {
			headerTable = null;
		}
	}

	/**
	 * Default constructor for json deserialization.
	 */
	protected AbstractIOTableModel(){
	}

	@Override
	@JsonIgnore
	public IOTable getTrainingHeader() {
		return this.headerTable;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This default implementation returns false.
	 */
	@Override
	@JsonIgnore
	public boolean isUpdatable() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This default implementation throws an {@link UserError}. Subclasses overriding this method to update the model
	 * according to the given example set should also override the method {@link #isUpdatable()} by delivering true.
	 */
	@Override
	public void updateModel(IOTable updateObject, Operator operator) throws OperatorException {
		throw new UserError(operator, 135, getClass().getName());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Throws a UserError since most models should not allow additional parameters during application. However,
	 * subclasses may overwrite this method.
	 */
	@Override
	public void setParameter(String key, Object value) throws OperatorException {
		throw new UnsupportedApplicationParameterError(null, getName(), key);
	}

	@Override
	@JsonIgnore
	public String getName() {
		String result = super.getName();
		if (result.toLowerCase(Locale.ENGLISH).endsWith("model")) {
			result = result.substring(0, result.length() - "model".length());
		}
		return result;
	}

	/**
	 * This method allows storing in the legacy repository via Java serialization.
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		//IOTable gets serialized as ExampleSet, so invert this
		ObjectInputStream.GetField getField = in.readFields();
		ExampleSet headerES = (ExampleSet) getField.get("headerTable", headerTable);
		if (headerES != null) {
			IOTable convert = BeltConverter.convert(headerES, new SequentialConcurrencyContext());
			headerTable = new IOTable(convert.getTable().stripData());
		} else {
			headerTable = null;
		}
	}
}
