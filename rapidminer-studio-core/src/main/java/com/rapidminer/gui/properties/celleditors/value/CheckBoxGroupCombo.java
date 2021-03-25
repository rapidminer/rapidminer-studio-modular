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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.popup.PopupAction;
import com.rapidminer.parameter.ParameterTypeCheckBoxGroup;
import com.rapidminer.parameter.Parameters;


/**
 * Fake combo box (actually a JToggleButton) that shows checkbox groups to the user. The user can select and deselect
 * multiple of these checkboxes from the popup.
 *
 * @author Kevin Majchrzak
 * @since 9.9
 */
final class CheckBoxGroupCombo extends JToggleButton {

	/**
	 * Is used for drawing the combo box arrow.
	 */
	private boolean isDown = false;

	private CheckBoxGroupPopup popup;

	/**
	 * Creates a new instance using the given {@link CheckBoxGroupPopup} as the combo box popup.
	 */
	private CheckBoxGroupCombo(CheckBoxGroupPopup popup){
		super(new PopupAction(false, "checkbox_group.type",
				popup, PopupAction.PopupPosition.VERTICAL));
		this.popup = popup;
		popup.setOwner(this);
		setBorderPainted(false);
		setContentAreaFilled(false);
		setHorizontalTextPosition(SwingConstants.RIGHT);
		setHorizontalAlignment(SwingConstants.LEFT);
		putClientProperty(RapidLookTools.PROPERTY_BUTTON_PAINT_AS_TOOLBAR_BUTTON, true);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				isDown = true;
				repaint();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				repaint();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				mouseReleased(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				isDown = false;
				repaint();
			}
		});
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		paintBox(g2);
		paintBorder(g2);
		paintCurrentSelection(g2);
		g2.dispose();
	}

	@Override
	protected void paintBorder(Graphics g) {
		// boarder is painted in paintComponent
	}

	@Override
	public String getText() {
		return popup == null ? "none" : popup.getValue().replace(String.valueOf(Parameters.RECORD_SEPARATOR), ", ");
	}

	/**
	 * Adds the given action listener to all checkboxes.
	 */
	void addActionListenerToCheckBoxes(ActionListener l) {
		popup.addActionListener(l);
	}

	/**
	 * Sets the given value as the current parameter type value. (Will update the checkboxes to represent the given
	 * value).
	 *
	 * @param parameterAsString the current string value of the represented {@link ParameterTypeCheckBoxGroup}
	 */
	void setValue(String parameterAsString) {
		popup.setValue(parameterAsString);
	}

	/**
	 * Returns the String representation of the current selection. (See {@link ParameterTypeCheckBoxGroup} for more
	 * information on the format).
	 */
	String getValue() {
		return popup.getValue();
	}

	/**
	 * Draws the combobox itself.
	 */
	private void paintBox(Graphics2D g2) {
		int w = getWidth();
		int h = getHeight() - 1;
		if (w <= 0 || h <= 0) {
			return;
		}

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (isEnabled()) {
			if (Boolean.parseBoolean(String.valueOf(getClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK)))) {
				g2.setColor(Colors.COMBOBOX_BACKGROUND_DARK);
			} else {
				g2.setColor(Colors.COMBOBOX_BACKGROUND);
			}
		} else {
			g2.setColor(Colors.COMBOBOX_BACKGROUND_DISABLED);
		}

		g2.fillRoundRect(0, 0, w - 1, h, RapidLookAndFeel.CORNER_DEFAULT_RADIUS, RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		// arrow
		int ny = getSize().height / 2 - 3;
		int nx = getWidth() - 15;

		if (isDown && isEnabled()) {
			nx++;
			ny++;
		}
		g2.translate(nx, ny);

		if (isEnabled()) {
			g2.setColor(Colors.COMBOBOX_ARROW);
		} else {
			g2.setColor(Colors.COMBOBOX_ARROW_DISABLED);
		}

		Polygon arrow = new Polygon(new int[]{0, 4, 8}, new int[]{0, 6, 0}, 3);
		g2.fillPolygon(arrow);

		g2.translate(-nx, -ny);
	}

	/**
	 * Draws the border of the combobox.
	 */
	private void paintBorder(Graphics2D g2) {
		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (isEnabled()) {
			if (hasFocus() || isSelected()) {
				g2.setColor(Colors.COMBOBOX_BORDER_FOCUS);
			} else {
				g2.setColor(Colors.COMBOBOX_BORDER);
			}
		} else {
			g2.setColor(Colors.COMBOBOX_BORDER_DISABLED);
		}

		g2.drawRoundRect(0, 0, w - 1, h - 1, RapidLookAndFeel.CORNER_DEFAULT_RADIUS, RapidLookAndFeel.CORNER_DEFAULT_RADIUS);
	}

	/**
	 * Draws the current selection.
	 */
	private void paintCurrentSelection(Graphics2D g2) {
		super.paintComponent(g2);
	}

	/**
	 * Creates a new instance for the given parameter type.
	 */
	static CheckBoxGroupCombo newInstance(ParameterTypeCheckBoxGroup type){
		return new CheckBoxGroupCombo(new CheckBoxGroupPopup(type));
	}
}
