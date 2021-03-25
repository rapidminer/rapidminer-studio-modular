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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.parameter.ParameterTypeCheckBoxGroup;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;


/**
 * Popup for {@link CheckBoxGroupCombo}.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
class CheckBoxGroupPopup extends JPanel {

	private static final Font OPEN_SANS_SEMI_BOLD_12 = FontTools.getFont("Open Sans Semibold", Font.BOLD, 12);
	private JComponent owner = null;
	private List<JCheckBox> checkBoxes;

	CheckBoxGroupPopup(ParameterTypeCheckBoxGroup type) {
		super();
		initGUI(type);
	}

	@Override
	public Dimension getPreferredSize() {
		return owner != null ? new Dimension(owner.getWidth() - 12, super.getPreferredSize().height)
				: super.getPreferredSize();
	}

	/**
	 * The owner used to set the size of the popup.
	 */
	void setOwner(JComponent owner) {
		this.owner = owner;
	}

	/**
	 * See {@link CheckBoxGroupCombo#addActionListenerToCheckBoxes(ActionListener)}.
	 */
	void addActionListener(ActionListener a) {
		for (JCheckBox checkBox : checkBoxes) {
			checkBox.addActionListener(a);
		}
	}

	/**
	 * See {@link CheckBoxGroupCombo#getValue()}.
	 */
	void setValue(String value) {
		if (value != null && !value.isEmpty()) {
			Set<String> selection = new HashSet<>(Arrays.asList(value.split(String.valueOf(Parameters.RECORD_SEPARATOR))));
			checkBoxes.forEach(checkBox -> checkBox.setSelected(selection.contains(checkBox.getText())));
		} else {
			checkBoxes.forEach(checkBox -> checkBox.setSelected(false));
		}
		if (owner != null) {
			owner.repaint();
		}
	}

	/**
	 * Returns a String holding the names of the currently selected checkboxes separated by {@link
	 * ParameterTypeCheckBoxGroup#CHECKBOX_SEPARATOR}.
	 */
	String getValue() {
		StringBuilder builder = new StringBuilder();
		for (JCheckBox checkBox : checkBoxes) {
			if (checkBox.isSelected()) {
				builder.append(checkBox.getText());
				builder.append(ParameterTypeCheckBoxGroup.CHECKBOX_SEPARATOR);
			}
		}
		if (builder.length() > 0) {
			// need to remove the last separator
			return builder.substring(0, builder.length() - 1);
		} else {
			return "none";
		}
	}

	/**
	 * Initialize the GUI.
	 */
	private void initGUI(ParameterTypeCheckBoxGroup type) {
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 0, 0);

		setBackground(Color.WHITE);

		checkBoxes = new ArrayList<>();
		for (String group : type.getGroups()) {
			if (group != null) {
				JLabel groupLabel = new JLabel(group);
				groupLabel.setFont(OPEN_SANS_SEMI_BOLD_12);
				gbc.gridy += 1;
				add(groupLabel, gbc);
			}
			List<String> checkBoxNames = type.getCheckBoxNames(group);
			if (checkBoxNames != null) {
				for (String checkBoxName : checkBoxNames) {
					JCheckBox checkBox = new JCheckBox(checkBoxName);
					checkBox.setToolTipText(I18N.getGUILabel("check_box_group.click.tip"));
					checkBox.addActionListener(e -> {
						if (owner != null) {
							owner.repaint();
						}
					});
					gbc.gridy += 1;
					add(checkBox, gbc);
					checkBoxes.add(checkBox);
				}
			}
			gbc.gridy += 1;
			JComponent separator = new JLabel();
			separator.setPreferredSize(new Dimension(0, 8));
			add(separator, gbc);
		}
	}
}
