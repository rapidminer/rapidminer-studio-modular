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
package com.rapidminer.tools.belt;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ToTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.FromTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;


/**
 * Helper methods to convert between {@link IOTable} and {@link ExampleSet} or {@link TableMetaData} and {@link
 * ExampleSetMetaData}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public enum BeltConversionTools {
	;//no instance enum

	/**
	 * Casts or converts the ioobject to a {@link IOTable} or returns {@code null} if that is not possible. Conversion
	 * is only possible if the ioobject is an {@link ExampleSet}.
	 *
	 * @param ioObject
	 * 		the object to change to a table
	 * @param context
	 * 		the context to use for converting in parallel, can be {@code null}, then sequential conversion is used
	 * @return a {@link IOTable} or {@code null}
	 */
	public static IOTable asIOTableOrNull(IOObject ioObject, ConcurrencyContext context) {
		if (ioObject instanceof IOTable) {
			return (IOTable) ioObject;
		} else if (ioObject instanceof ExampleSet) {
			if (context == null) {
				context = new SequentialConcurrencyContext();
			}
			return BeltConverter.convert((ExampleSet) ioObject, context);
		}
		return null;
	}

	/**
	 * Casts or converts the ioobject to an {@link ExampleSet} or returns {@code null} if that is not possible.
	 * Conversion is only possible if the ioobject is a {@link IOTable} and will be a wrapping into an {@link
	 * ExampleSet} without copying all the data.
	 *
	 * @param ioObject
	 * 		the object to change to an example set
	 * @return an {@link ExampleSet} or {@code null}
	 */
	public static ExampleSet asExampleSetOrNull(IOObject ioObject) {
		if (ioObject instanceof ExampleSet) {
			return (ExampleSet) ioObject;
		} else if (ioObject instanceof IOTable) {
			return TableViewCreator.INSTANCE.convertOnWriteView((IOTable) ioObject, false);
		}
		return null;
	}

	/**
	 * Casts or converts the metaData to a {@link TableMetaData} or returns {@code null} if that is not possible.
	 * Conversion is only possible if the metaData is an {@link ExampleSetMetaData}.
	 *
	 * @param metaData
	 * 		the object to change to a table
	 * @return a {@link TableMetaData} or {@code null}
	 */
	public static TableMetaData asTableMetaDataOrNull(MetaData metaData) {
		if (metaData instanceof TableMetaData) {
			return (TableMetaData) metaData;
		} else if (metaData instanceof ExampleSetMetaData) {
			return ToTableMetaDataConverter.convert((ExampleSetMetaData) metaData);
		}
		return null;
	}

	/**
	 * Casts or converts the metaData to an {@link ExampleSetMetaData} or returns {@code null} if that is not possible.
	 * Conversion is only possible if the metaData is a {@link TableMetaData}.
	 *
	 * @param metaData
	 * 		the object to change to a table
	 * @return an {@link ExampleSetMetaData} or {@code null}
	 */
	public static ExampleSetMetaData asExampleSetMetaDataOrNull(MetaData metaData) {
		if (metaData instanceof TableMetaData) {
			return FromTableMetaDataConverter.convert((TableMetaData) metaData);
		} else if (metaData instanceof ExampleSetMetaData) {
			return (ExampleSetMetaData) metaData;
		}
		return null;
	}
}
