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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeAttributeValue;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.tools.FunctionWithThrowable;
import com.rapidminer.tools.I18N;


/**
 * Dialog used in {@link com.rapidminer.gui.properties.celleditors.value.AttributeValueValueCellEditor} to present a
 * list with attribute names and corresponding attribute values to the user.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
public abstract class AttributeValueDialog extends ButtonDialog {

	private static final long serialVersionUID = 3042032347432150498L;

	private final ParameterTypeAttributeValue type;
	private AttributeValueDialogPanel pairPanel;

	/**
	 * Creates a new AttributeValueDialog for the given parameter type. The operator is used to retrieve the initial
	 * attribute value pairs. You need to define values for the following i18n keys
	 * <p>
	 * gui.dialog.key.message
	 * <p>
	 * gui.dialog.key.title
	 * <p>
	 * gui.dialog.key.icon
	 * <p>
	 * gui.dialog.key.attribute_header
	 * <p>
	 * gui.dialog.key.attribute_value_header
	 * <p>
	 * where 'key' stands for the given i18n key. The i18n values will be used to set the dialog message, the dialog
	 * title, the dialog icon and the headers of the attribute name and attribute value columns.
	 *
	 * @param operator
	 * 		the operator used to retrieve the initial attribute value pairs
	 * @param type
	 * 		the parameter type
	 * @param key
	 * 		the i18n key used for gui messages, buttons, labels and so on
	 */
	public AttributeValueDialog(Operator operator, ParameterTypeAttributeValue type, String key) {
		super(ApplicationFrame.getApplicationFrame(), key, ModalityType.APPLICATION_MODAL, new Object[]{});
		this.type = type;
		initGUI(operator, key);
	}

	/**
	 * Initializes the GUI.
	 *
	 * @param operator the operator used to retrieve the initial attribute value pairs
	 * @param i18nKey  the i18nKey used for gui messages, buttons, labels and so on
	 */
	private void initGUI(Operator operator, String i18nKey) {
		List<String[]> initialAttributeValuePairs = Optional.ofNullable(operator).
				map(FunctionWithThrowable.suppress(op -> op.getParameterList(type.getKey()))).
				map(ArrayList::new).orElse(new ArrayList<>());
		pairPanel = new AttributeValueDialogPanel(type, initialAttributeValuePairs, i18nKey);
		layoutDefault(pairPanel, NORMAL, new JButton(pairPanel.createAddRowAction()), specialOkButton(), makeCancelButton());
	}

	/**
	 * Ok button that does not close the dialog when the user presses enter. It triggers {@link #okAction()} when
	 * pressed.
	 */
	private JButton specialOkButton() {
		JButton okButton = new JButton(new ResourceAction("ok") {

			private static final long serialVersionUID = 2265489760585034488L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				okAction();
			}
		});
		// deliberately not the normal ButtonDialog OK button -> it's very irritating to press Enter
		// to confirm your value
		// and have your dialog vanish. This behavior is ok for powerusers who love their keyboard
		// to death,
		// and know everything inside out, but the vast majority are NOT powerusers
		// and for them the default behavior is just annoying
		okButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.settings_ok.mne").charAt(0));
		return okButton;
	}

	/**
	 * Checks the user input for empty values and queries the user if he wants to discard them or to continue editing if
	 * necessary. If there are no empty values or the user confirms then the method calls {@link #receiveValue(String)}
	 * and closes the dialog.
	 */
	private void okAction() {
		List<String[]> attributeValuePairs = pairPanel.getAttributeValuePairs();
		if (!containsEmptyStrings(attributeValuePairs) || showConfirmDialog()) {
			receiveValue(ParameterTypeList.transformList2String(attributeValuePairs.stream().
					filter(pair -> pair[0] != null && !pair[0].isEmpty() && pair[1] != null && !pair[1].isEmpty()).
					collect(Collectors.toList())));
			dispose();
		}
	}

	/**
	 * Shows a confirm dialog to the user asking him if the really wants to discard incomplete rows or if he wants to
	 * continue editing. Returns true iff the user wants to discard incomplete rows.
	 */
	private boolean showConfirmDialog(){
		ConfirmDialog dialog = new ConfirmDialog(this, "undefined_values", ConfirmDialog.OK_CANCEL_OPTION, false) {

			private static final long serialVersionUID = 5066223709280617117L;

			@Override
			protected JButton makeOkButton() {
				JButton okButton = new JButton(new ResourceAction("undefined_values_continue") {

					private static final long serialVersionUID = -8187199234055845095L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						returnOption = OK_OPTION;
						ok();
					}
				});
				getRootPane().setDefaultButton(okButton);
				return okButton;
			}

			@Override
			protected JButton makeCancelButton() {
				ResourceAction cancelAction = new ResourceAction("undefined_values_dismiss") {

					private static final long serialVersionUID = -8387199234055845095L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						returnOption = CANCEL_OPTION;
						cancel();
					}
				};
				JButton cancelButton = new JButton(cancelAction);
				getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
						KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
				getRootPane().getActionMap().put("CANCEL", cancelAction);

				return cancelButton;
			}
		};
		dialog.setVisible(true);
		int answer = dialog.getReturnOption();
		return answer == ConfirmDialog.CANCEL_OPTION;
	}

	/**
	 * Returns true if the list contains pairs where exactly one of the strings is empty.
	 */
	private boolean containsEmptyStrings(List<String[]> attributeValuePairs) {
		for (String[] pair : attributeValuePairs) {
			boolean firstIsEmpty = pair[0] == null || pair[0].isEmpty();
			boolean secondIsEmpty = pair[1] == null || pair[1].isEmpty();
			if (firstIsEmpty && !secondIsEmpty || secondIsEmpty && !firstIsEmpty) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method will be called when the user closes the dialog and accepts the changes. The given String encodes the
	 * users selection. It can be transformed into a list of attribute value pairs via {@link
	 * ParameterTypeList#transformString2List(String)}.
	 *
	 * @param value
	 * 		String encoding a list of attribute value pairs
	 */
	protected abstract void receiveValue(String value);

}
