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
package com.rapidminer.connection.gui.components;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.gui.popup.PopupAction;

import javafx.beans.property.StringProperty;


/**
 * Multiple choice select panel with popup menu showing the available types, updating the ui if those are being changed
 * and selected, writes back to chosen types as a comma separated list.
 *
 * @author Andreas Timm
 * @since 9.8
 */
public class ConnectionMultipleChoicePanel extends JPanel {

	private JComponent typesPanel;
	private ConnectionParameterModel typesParam;

	/**
	 * Create a new instance of a multiple selection hidden under a {@link JToggleButton}
	 *
	 * @param actionKey           for label, tip and icon
	 * @param availableTypesParam {@link StringProperty} holding all the possible values as comma separated list, can be
	 *                            updated during runtime
	 * @param typesParam          current selection of chosen elements, will be updated by this UI to contain all named
	 *                            elements as a comma separated list
	 */
	public ConnectionMultipleChoicePanel(String actionKey, StringProperty availableTypesParam, ConnectionParameterModel typesParam) {
		super();
		this.typesParam = typesParam;

		typesPanel = new JPanel();
		typesPanel.setLayout(new BoxLayout(typesPanel, BoxLayout.PAGE_AXIS));

		Set<String> types = new TreeSet<>();
		types.addAll(splitStringList(availableTypesParam.getValue()));
		types.addAll(splitStringList(typesParam.getValue()));

		setAvailableTypes(types);

		availableTypesParam.addListener((obsValue, oldValue, newValue) -> setAvailableTypes(splitStringList(newValue)));

		JToggleButton tableTypesComp = new JToggleButton(new PopupAction(true, actionKey, typesPanel, PopupAction.PopupPosition.VERTICAL));
		tableTypesComp.setHorizontalTextPosition(SwingConstants.LEFT);
		setLayout(new BorderLayout());
		add(tableTypesComp, BorderLayout.CENTER);
	}

	/**
	 * Collects selected checkboxes and puts the result into the typesParam
	 */
	private void gatherSelectedTypes() {
		String collect = Arrays.stream(typesPanel.getComponents()).filter(comp -> comp instanceof JCheckBox && ((JCheckBox) comp).isSelected()).map(comp -> ((JCheckBox) comp).getText()).collect(Collectors.joining(","));
		typesParam.setValue(collect);
	}

	/**
	 * Show the available types from the given collection, keeps the selection from before
	 */
	private void setAvailableTypes(Collection<String> availableTypes) {
		List<String> selectedTypes = new ArrayList<>(splitStringList(typesParam.getValue()));
		ArrayList<String> list = new ArrayList<>(availableTypes);
		Collections.sort(list);
		typesPanel.removeAll();
		list.forEach(type -> {
			JCheckBox chkbox = new JCheckBox(type, selectedTypes.contains(type));
			chkbox.addActionListener(e -> gatherSelectedTypes());
			typesPanel.add(chkbox);
		});
	}

	/**
	 * Split the string by comma, will always return a {@link List}
	 */
	private List<String> splitStringList(String value) {
		if (value == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(value.split(","));
	}
}
