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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeCheckBoxGroup;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * ValueCellEditor for {@link ParameterTypeCheckBoxGroup}. Renders the labeled groups of checkboxes defined by the given
 * ParameterType. The user can select any combination of these checkboxes. The selected checkboxes are encoded via a
 * String holding their names, separated via {@link ParameterTypeCheckBoxGroup#CHECKBOX_SEPARATOR}.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
public class CheckBoxGroupValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private ParameterTypeCheckBoxGroup type;
	private CheckBoxGroupCombo combo;

	/**
	 * The one argument constructor that is called via reflection (see {@link PropertyValueCellEditor}).
	 *
	 * @param type the type that will be rendered and edited by this PropertyValueCellEditor.
	 */
	public CheckBoxGroupValueCellEditor(ParameterTypeCheckBoxGroup type) {
		this.type = type;
		combo = CheckBoxGroupCombo.newInstance(type);
		combo.addActionListenerToCheckBoxes(e -> fireEditingStopped());
	}

	@Override
	public void setOperator(Operator operator) {
		if (operator != null && operator.isParameterSet(type.getKey())) {
			try {
				combo.setValue(operator.getParameterAsString(type.getKey()));
			} catch (UndefinedParameterError undefinedParameterError) {
				combo.setValue(null);
			}
		} else {
			combo.setValue(null);
		}
	}

	/**
	 * Returns true.
	 */
	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	/**
	 * Returns false.
	 */
	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return combo;
	}

	/**
	 * Returns the names of the currently selected checkboxes separated via {@link ParameterTypeCheckBoxGroup#CHECKBOX_SEPARATOR}.
	 * Can be converted to an array of names via {@link ParameterTypeCheckBoxGroup#stringToSelection(String)}.
	 *
	 * @return the current selection encoded as String
	 */
	@Override
	public Object getCellEditorValue() {
		return combo.getValue();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
												   int row, int column) {
		return combo;
	}
}