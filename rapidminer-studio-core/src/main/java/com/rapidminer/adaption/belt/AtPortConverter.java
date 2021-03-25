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
package com.rapidminer.adaption.belt;

import java.util.Optional;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.metadata.ToTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.FromTableMetaDataConverter;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.belt.BeltTools;


/**
 * Utility methods to convert from belt {@link IOTable}s to {@link ExampleSet}s and vice versa at ports.
 *
 * Please note that this class is not part of any public API and might be modified or removed in future releases without
 * prior warning.
 *
 * @author Gisa Meier
 * @since 9.0.0
 */
public final class AtPortConverter {

	// Suppress default constructor for noninstantiability
	private AtPortConverter() {throw new AssertionError();}

	/**
	 * Checks if is is possible to convert the dataClass into the desired class. Only conversion from an {@link
	 * ExampleSet} to a {@link IOTable} and vice versa is possible.
	 *
	 * @param dataClass
	 * 		the actual class
	 * @param desiredClass
	 * 		the desired class
	 * @return whether conversion is possible
	 */
	public static boolean isConvertible(Class<? extends IOObject> dataClass, Class<? extends IOObject> desiredClass) {
		return (ExampleSet.class.equals(desiredClass) && IOTable.class.equals(dataClass))
				|| (IOTable.class.equals(desiredClass) && ExampleSet.class.isAssignableFrom(dataClass));
	}

	/**
	 * Checks if is is possible to convert the metadataClass into the desired class. Only conversion from an {@link
	 * ExampleSetMetaData} to a {@link TableMetaData} and vice versa is possible.
	 *
	 * @param metadataClass
	 * 		the actual class
	 * @param desiredClass
	 * 		the desired class
	 * @return whether conversion is possible
	 * @since 9.9
	 */
	public static boolean isMDConvertible(Class<? extends MetaData> metadataClass, Class<? extends MetaData> desiredClass) {
		return (ExampleSetMetaData.class.equals(desiredClass) && TableMetaData.class.equals(metadataClass))
				|| (TableMetaData.class.equals(desiredClass) && ExampleSetMetaData.class.isAssignableFrom(metadataClass));
	}

	/**
	 * Converts an {@link ExampleSet} into a {@link IOTable} or vice versa.
	 *
	 * @param data
	 * 		the data to convert
	 * @param port
	 * 		the port at which the conversion takes place
	 * @return the converted object
	 * @throws BeltConverter.ConversionException
	 * 		if the table cannot be converted because it contains advanced columns
	 */
	public static IOObject convert(IOObject data, Port port) {
		if (data instanceof ExampleSet) {
			ConcurrencyContext context = Resources.getConcurrencyContext(port.getPorts().getOwner().getOperator());
			return BeltConverter.convert((ExampleSet) data, context);
		} else if (data instanceof IOTable) {
			// convert as a view and throw on advanced columns
			return TableViewCreator.INSTANCE.convertOnWriteView((IOTable) data, true);
		} else {
			throw new UnsupportedOperationException("Conversion not supported");
		}
	}

	/**
	 * Converts an {@link ExampleSetMetaData} into a {@link TableMetaData} or vice versa.
	 *
	 * @param metadata
	 * 		the metadata to convert
	 * @param port
	 * 		the port at which the conversion takes place
	 * @return the converted object
	 * @since 9.9
	 */
	public static MetaData convert(MetaData metadata, Port port) {
		if (metadata instanceof ExampleSetMetaData) {
			return ToTableMetaDataConverter.convert((ExampleSetMetaData) metadata);
		} else if (metadata instanceof TableMetaData) {
			if (port != null && hasAdvanced((TableMetaData) metadata)) {
				port.addError(new SimpleMetaDataError(ProcessSetupError.Severity.WARNING, port, "metadata_conversion" +
						".advanced_columns"));
			}
			return FromTableMetaDataConverter.convert((TableMetaData) metadata);
		} else {
			throw new UnsupportedOperationException("Conversion not supported");
		}
	}

	/**
	 * Checks the table meta data for advanced types.
	 */
	private static boolean hasAdvanced(TableMetaData tmd) {
		for (ColumnInfo column : tmd.getColumns()) {
			final Optional<ColumnType<?>> type = column.getType();
			if (type.isPresent() && BeltTools.isAdvanced(type.get())) {
				return true;
			}
		}
		return false;
	}
}