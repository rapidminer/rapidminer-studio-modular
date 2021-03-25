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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.filter.ChangeAttributeRole;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;


/**
 * @author Sebastian Land
 * @deprecated since 9.9, use {@link QuickFixSupplier} instead.
 */
@Deprecated
public class ChangeAttributeRoleQuickFix extends OperatorInsertionQuickFix {

	private final InputPort inputPort;
	private final String role;

	public ChangeAttributeRoleQuickFix(InputPort inputPort, String role, String i18nKey, Object... i18nArgs) {
		super(i18nKey, i18nArgs, 10, inputPort);
		this.inputPort = inputPort;
		this.role = role;
	}

	@Override
	public Operator createOperator() throws OperatorCreationException {
		ExampleSetMetaData metaData = inputPort.getMetaDataAsOrNull(ExampleSetMetaData.class);
		if (metaData == null) {
			return null;
		}

		ChangeAttributeRole car = OperatorService.createOperator(ChangeAttributeRole.class);

		Object[] options = (metaData.getAttributeNamesByType(Ontology.VALUE_TYPE)).toArray();
		if (options.length > 0) {
			Object option = SwingTools.showInputDialog("quickfix.replace_by_dictionary", options, options[0], car
					.getParameters().getParameterType(ChangeAttributeRole.PARAMETER_NAME).getDescription());
			if (option != null) {
				car.setParameter(ChangeAttributeRole.PARAMETER_NAME, option.toString());
				car.setParameter(ChangeAttributeRole.PARAMETER_TARGET_ROLE, role);
				return car;
			} else {
				return null;
			}
		} else {
			return car;
		}
	}

}
