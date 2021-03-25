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
package com.rapidminer.operator.io;

import java.util.EnumSet;
import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.Table;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.belt.BeltTools;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * This operator stores IOObjects at a location in a repository.
 * 
 * @author Simon Fischer
 * 
 */
public class RepositoryStorer extends AbstractWriter<IOObject> {

	public static final String PARAMETER_REPOSITORY_ENTRY = "repository_entry";

	public RepositoryStorer(OperatorDescription description) {
		super(description, IOObject.class);
	}

	@Override
	public IOObject write(IOObject ioobject) throws OperatorException {
		try {
			RepositoryLocation location =
					getParameterAsRepositoryLocationData(PARAMETER_REPOSITORY_ENTRY, IOObjectEntry.class);
			logConnection(ioobject);
			if (ioobject instanceof IOTable) {
				calculateStatistics(((IOTable) ioobject).getTable());
			}
			return RepositoryManager.getInstance(null).store(ioobject, location, this);
		} catch (RepositoryException e) {
			throw new UserError(this, e, 315, getParameterAsString(PARAMETER_REPOSITORY_ENTRY), e.getMessage());
		}
	}

	/**
	 * Calculate the statistics while there is access to the better context.
	 */
	private void calculateStatistics(Table table) {
		Context context = BeltTools.getContext(this);
		final EnumSet<Statistics.Statistic> minMax =
				EnumSet.of(Statistics.Statistic.COUNT, Statistics.Statistic.MIN, Statistics.Statistic.MAX);
		for (Column column : table.columnList()) {
			if (Statistics.supported(column, Statistics.Statistic.MAX)) {
				Statistics.compute(column, minMax, context);
			} else if (Statistics.supported(column, Statistics.Statistic.COUNT)) {
				Statistics.compute(column, Statistics.Statistic.COUNT, context);
			}
		}
	}

	/**
	 * Logs if the object is a connection.
	 */
	private void logConnection(IOObject ioObject) {
		if (ioObject instanceof ConnectionInformationContainerIOObject) {
			ActionStatisticsCollector.INSTANCE.logNewConnection(this,
					((ConnectionInformationContainerIOObject) ioObject).getConnectionInformation());
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeRepositoryLocation type = new ParameterTypeRepositoryLocation(PARAMETER_REPOSITORY_ENTRY,
				"Repository entry.", true, false, false, false, true, true);
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);
		return types;
	}
}
