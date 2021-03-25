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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Optional;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTable;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.AttributeValueDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeAttributeValue;
import com.rapidminer.tools.FunctionWithThrowable;

/**
 * {@link PropertyValueCellEditor} implementation for {@link ParameterTypeAttributeValue}.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
public class AttributeValueValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private transient Operator operator;
	private JButton button;
	private String attributeValue;
	private ParameterTypeAttributeValue type;

	/**
	 * The one argument constructor that is called via reflection (see {@link PropertyValueCellEditor}). You need to
	 * define values for the i18n keys
	 * <p>
	 * gui.action.type_key.attribute_value_dialog.open.label
	 * <p>
	 * gui.action.type_key.attribute_value_dialog.open.tip
	 * <p>
	 * where 'type_key' stands for the key of the given parameter type. The i18n values will be used as the label and
	 * tooltip texts for the button opening the {@link AttributeValueDialog}.
	 *
	 * @param type
	 * 		the type that will be rendered and edited by this PropertyValueCellEditor.
	 */
	public AttributeValueValueCellEditor(ParameterTypeAttributeValue type) {
		this.type = type;
		button = new JButton(new ResourceAction(true, type.getKey() + ".attribute_value_dialog.open") {

			private static final long serialVersionUID = 8274776396885048377L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				JDialog dialog = new AttributeValueDialog(operator, type, type.getKey() + ".attribute_value_dialog") {
					@Override
					protected void receiveValue(String value) {
						attributeValue = value;
					}
				};
				dialog.setVisible(true);
				// no dialog handling necessary, does everything itself
				fireEditingStopped();
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
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
		return button;
	}

	/**
	 * Returns the cell editor value (a list of attribute value pairs in the format defined by {@link
	 * com.rapidminer.parameter.ParameterTypeList}) or null if the value has not been set by the user yet.
	 */
	@Override
	public Object getCellEditorValue() {
		return attributeValue != null ? attributeValue :
				Optional.ofNullable(operator).map(FunctionWithThrowable.suppress(op -> op.getParameter(type.getKey()))).
						orElse(null);
	}


	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
												   int row, int column) {
		return button;
	}

	@Override
	public void activate() {
		button.doClick();
	}
}
