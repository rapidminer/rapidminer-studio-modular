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
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rapidminer.connection.gui.AbstractConnectionGUI;
import com.rapidminer.gui.look.icons.EmptyIcon;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.autocomplete.AutoCompleteComboBoxAddition;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterTypeAttributeValue;
import com.rapidminer.tools.I18N;


/**
 * This is the panel used in {@link AttributeValueDialog}.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
class AttributeValueDialogPanel extends JPanel {

	/**
	 * Max number of items added to an attribute value combobox (See {@link #fillAttributeValues(JComboBox, String)}).
	 */
	private static final int MAX_ATTRIBUTE_VALUE_ITEMS = 100;

	/**
	 * Helpful if the user adds many rows.
	 */
	private final JScrollPane scrollPane;

	/**
	 * The rows holding the attribute value pairs are added to this panel.
	 */
	private final JPanel rowOuterPanel;

	/**
	 * this is the root panel that holds together everything.
	 */
	private final JPanel rootPanel;

	/**
	 * These are all attributes that could be extracted from the given MetaData.
	 */
	private String[] attributes;

	/**
	 * This is the MetaData that was given via {@link ParameterTypeAttributeValue#getMetaDataProvider()}
	 */
	private ExampleSetMetaData metaData;

	/**
	 * The current list of attribute value pairs set by the user.
	 */
	private List<String[]> attributeValuePairs;

	/**
	 * Header for the attribute column in the dialog.
	 */
	private final String attributeHeader;

	/**
	 * Header for the attribute value column in the dialog.
	 */
	private final String attributeValueHeader;

	/**
	 * Creates a new panel showing a list of attribute value pairs to the user.
	 *
	 * @param type
	 * 		the parameter type holding a meta data provider
	 * @param initialAttributeValuePairs
	 * 		the list will initially be filled with these attribute value pairs
	 * @param i18nKey
	 * 		the i18nKey used to set the list headers
	 */
	AttributeValueDialogPanel(ParameterTypeAttributeValue type, List<String[]> initialAttributeValuePairs, String i18nKey) {
		super(new BorderLayout());

		attributeHeader = "<html><b>&nbsp;&nbsp;"
				+ I18N.getGUIMessage("gui.dialog." + i18nKey + ".attribute_header") + "</b></html>";
		attributeValueHeader = "<html><b>&nbsp;&nbsp;"
				+ I18N.getGUIMessage("gui.dialog." + i18nKey + ".attribute_value_header") + "</b></html>";
		metaData = castMetaData(type.getMetaDataProvider().getMetaData());
		setAttributes(metaData);
		attributeValuePairs = initialAttributeValuePairs != null ? new ArrayList<>(initialAttributeValuePairs) : new ArrayList<>();
		// add an empty row for the user to edit
		attributeValuePairs.add(new String[]{"", ""});

		rootPanel = new JPanel(new GridBagLayout());
		rootPanel.setBorder(AbstractConnectionGUI.DEFAULT_PANEL_BORDER);

		// row UI
		this.rowOuterPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.insets = new Insets(0, 0, 10, 0);
		gbc.fill = GridBagConstraints.BOTH;

		rootPanel.add(rowOuterPanel, gbc);

		scrollPane = new ExtendedJScrollPane(rootPanel);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);

		updateRows();
	}

	/**
	 * Creates and returns a new {@link ResourceAction} that adds a row to the list of attribute value pairs.
	 *
	 * @return the new ResourceAction
	 */
	ResourceAction createAddRowAction() {
		return new ResourceAction("attribute_value_list.add_row") {
			private static final long serialVersionUID = 6182717520989904651L;

			@Override
			protected void loggedActionPerformed(ActionEvent e) {
				attributeValuePairs.add(new String[]{"", ""});
				updateRows();
			}
		};
	}

	/**
	 * Returns the list of attribute value pairs currently set by the user.
	 */
	List<String[]> getAttributeValuePairs() {
		return attributeValuePairs;
	}

	/**
	 * Updates all rows in the UI, based on the current attribute value pairs.
	 */
	private void updateRows() {
		rowOuterPanel.removeAll();
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 5, 0);

		rowOuterPanel.add(createHeader(), gbc);
		gbc.gridy += 1;

		for (int i = 0; i < attributeValuePairs.size(); i++) {
			String[] pair = attributeValuePairs.get(i);
			rowOuterPanel.add(createRow(i, pair[0], pair[1]), gbc);
			gbc.gridy += 1;
		}

		// filler at bottom
		gbc.gridy += 1;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		rowOuterPanel.add(new JLabel(), gbc);

		rowOuterPanel.revalidate();
		rowOuterPanel.repaint();

		// arbitrary + 50 because Swing is stupid in height reporting. Even though all happens on the EDT, I do not have the correct height here all the time..
		scrollPane.getViewport().scrollRectToVisible(new Rectangle(1, rootPanel.getHeight() + 50, 1, 1));
	}

	/**
	 * Creates a new row holding the given attribute and attribute value.
	 *
	 * @param index
	 * 		the index of the row in the {@link #attributeValuePairs} list to use
	 * @param attribute
	 * 		The attribute name to set. Can be {@code null} - then the new row will be empty.
	 * @param attributeValue
	 * 		The attribute value to set. Can be {@code null} - then the new row will be empty.
	 */
	private JPanel createRow(int index, String attribute, String attributeValue) {

		JPanel rowPanel = new JPanel(new GridBagLayout());
		JPanel innerRowPanel = new JPanel(new GridLayout(1, 2, 10, 0));

		JComboBox<String> attributeValueCombo = new JComboBox<>();
		attributeValueCombo.setEditable(true);
		new AutoCompleteComboBoxAddition(attributeValueCombo);
		if (attribute != null) {
			fillAttributeValues(attributeValueCombo, attribute);
		}
		attributeValueCombo.setSelectedItem(attributeValue);
		attributeValueCombo.addItemListener(e -> {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}
			attributeValuePairs.get(index)[1] = (String) e.getItem();
		});

		JComboBox<String> attributeCombo = new JComboBox<>(attributes);
		attributeCombo.setEditable(true);
		new AutoCompleteComboBoxAddition(attributeCombo);
		attributeCombo.setSelectedItem(attribute);
		attributeCombo.addItemListener(e -> {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}

			String[] pair = attributeValuePairs.get(index);
			String oldAttributeName = pair[0];
			String newAttributeName = (String) e.getItem();
			if (!oldAttributeName.equals(newAttributeName)) {
				pair[0] = newAttributeName;
				fillAttributeValues(attributeValueCombo, newAttributeName);
			}
		});

		innerRowPanel.add(attributeCombo);
		innerRowPanel.add(attributeValueCombo);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		rowPanel.add(innerRowPanel, gbc);

		// delete button
		gbc.gridx += 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 10, 0, 0);
		JButton deleteButton = new JButton(new ResourceAction(false, "attribute_value_list.remove_row") {
			@Override
			protected void loggedActionPerformed(ActionEvent e) {
				attributeValuePairs.remove(index);
				updateRows();
			}
		});
		deleteButton.setContentAreaFilled(false);
		deleteButton.setBorderPainted(false);
		rowPanel.add(deleteButton, gbc);

		return rowPanel;
	}

	/**
	 * Creates the header for the list of attribute value pairs.
	 *
	 * @return the header
	 */
	private JPanel createHeader() {

		JPanel rowPanel = new JPanel(new GridBagLayout());
		JPanel innerRowPanel = new JPanel(new GridLayout(1, 2, 10, 0));

		JLabel attLabel = new JLabel(attributeHeader);
		JLabel attValLabel = new JLabel(attributeValueHeader);

		innerRowPanel.add(attLabel);
		innerRowPanel.add(attValLabel);
		attLabel.setPreferredSize(new Dimension(100, 30));
		attValLabel.setPreferredSize((new Dimension(100, 30)));
		attLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		attValLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		int intensity = 227;
		attLabel.setBackground(new Color(intensity, intensity, intensity));
		attValLabel.setBackground(new Color(intensity, intensity, intensity));
		attLabel.setOpaque(true);
		attValLabel.setOpaque(true);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		rowPanel.add(innerRowPanel, gbc);

		// fake delete button so that the allignment is right
		gbc.gridx += 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 10, 0, 0);
		JButton fakeButton = new JButton();
		fakeButton.setIcon(new EmptyIcon(24, 24));
		fakeButton.setContentAreaFilled(false);
		fakeButton.setBorderPainted(false);
		fakeButton.setEnabled(false);
		rowPanel.add(fakeButton, gbc);

		return rowPanel;
	}

	/**
	 * Used to initialize the {@link #attributes} array based on the given {@link ExampleSetMetaData}.
	 */
	private void setAttributes(ExampleSetMetaData metaData) {
		if (metaData != null) {
			attributes = metaData.getAllAttributes().stream().filter(AttributeMetaData::isNominal).
					map(AttributeMetaData::getName).toArray(String[]::new);
		} else {
			attributes = new String[0];
		}
	}

	/**
	 * Casts the given {@link MetaData} to {@link ExampleSetMetaData}. If the given metaData cannot be casted, {@code
	 * null} is returned instead.
	 */
	private ExampleSetMetaData castMetaData(MetaData metaData) {
		return metaData instanceof ExampleSetMetaData ? (ExampleSetMetaData) metaData : null;
	}

	/**
	 * Fills the given combobox with possible attribute values for the given attribute. The possible attribute values
	 * are determined via the metaData that came with {@link ParameterTypeAttributeValue#getMetaDataProvider()}. It
	 * never adds more than {@link #MAX_ATTRIBUTE_VALUE_ITEMS} to the combo box though.
	 */
	private void fillAttributeValues(JComboBox<String> combo, String attributeName) {
		Object selectedItem = combo.getSelectedItem();
		combo.removeAllItems();
		combo.setSelectedItem(selectedItem);
		if (metaData != null) {
			AttributeMetaData attribute = metaData.getAttributeByName(attributeName);
			if (attribute != null) {
				Set<String> valueSet = attribute.getValueSet();
				if (valueSet != null) {
					Iterator<String> it = valueSet.iterator();
					for (int numberOfItems = 0; it.hasNext() && numberOfItems < MAX_ATTRIBUTE_VALUE_ITEMS; numberOfItems++) {
						combo.addItem(it.next());
					}
				}
			}
		}
	}

}
