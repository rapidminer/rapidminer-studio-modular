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
package com.rapidminer.gui.flow;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMDDisplayUtils;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.tools.Ontology;


/**
 * This is a table model for the table meta data, analogous to {@link ExampleSetMetaDataTableModel}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class TableMetaDataTableModel implements TableModel {

	private final TableMetaData tmd;
	private final List<String> labelList;

	private static final String[] COLUMN_NAMES = {"Role", "Name", "Type", "Range", "Missings", "Comment"};
	private static final int ROLE_COLUMN = 0;
	private static final int NAME_COLUMN = 1;
	private static final int TYPE_COLUMN = 2;
	private static final int RANGE_COLUMN = 3;
	private static final int MISSINGS_COLUMN = 4;
	private static final int COMMENT_COLUMN = 5;

	public TableMetaDataTableModel(TableMetaData tmd) {
		super();
		this.tmd = tmd;
		this.labelList = new ArrayList<>(tmd.labels().size());
		this.labelList.addAll(tmd.labels());
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		//Table is immutable. We ignore all listeners.
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		//Table is immutable. We ignore all listeners.
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return COLUMN_NAMES[columnIndex];
	}

	@Override
	public int getRowCount() {
		return tmd.labels().size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final String label = labelList.get(rowIndex);
		switch (columnIndex) {
			case ROLE_COLUMN:
				return TableMDDisplayUtils.getLegacyRoleString(tmd, label);
			case NAME_COLUMN:
				return label;
			case TYPE_COLUMN:
				return TableMDDisplayUtils.getLegacyValueTypeName(tmd.column(label));
			case RANGE_COLUMN:
				return TableMDDisplayUtils.getLegacyRangeString(tmd, label);
			case MISSINGS_COLUMN:
				return tmd.column(label).getMissingValues();
			case COMMENT_COLUMN:
				final String comment = TableMDDisplayUtils.getColumnAnnotationAsComment(tmd, label);
				return comment == null ? "" : comment;
			default:
				return null;
		}

	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		throw new UnsupportedOperationException("Table is read only.");
	}

	/**
	 * Creates a meta data tooltip component analogously to
	 * {@link ExampleSetMetaDataTableModel#makeTableForToolTip(ExampleSetMetaData)}.
	 *
	 * @param tmd
	 * 		the table meta data to display
	 * @return a table for the tool tip
	 */
	public static Component makeTableForToolTip(TableMetaData tmd) {
		ExtendedJTable table = new ExtendedJTable(new TableMetaDataTableModel(tmd), true, true, true, false, false);
		table.getColumnModel().getColumn(TYPE_COLUMN).setCellRenderer(new DefaultTableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
														   boolean isSelected, boolean hasFocus,
														   int row, int column) {
				Component tableCellRendererComponent =
						super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (tableCellRendererComponent instanceof JLabel) {
					JLabel renderer = (JLabel) tableCellRendererComponent;
					int type = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(String.valueOf(value));

					if (type < 0) {
						type = 0;
					}
					Icon icon;
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NUMERICAL)) {
						icon = AttributeGuiTools.NUMERICAL_COLUMN_ICON;
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.NOMINAL)) {
						icon = AttributeGuiTools.NOMINAL_COLUMN_ICON;
					} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(type, Ontology.DATE_TIME)) {
						icon = AttributeGuiTools.DATE_COLUMN_ICON;
					} else {
						// attribute value type
						icon = AttributeGuiTools.UNKNOWN_COLUMN_ICON;
					}
					renderer.setIcon(icon);
				}

				return tableCellRendererComponent;
			}

		});
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setBorder(null);
		scrollPane.setPreferredSize(new Dimension(300, 200));
		scrollPane.setBackground(Colors.WHITE);
		scrollPane.getViewport().setBackground(Colors.WHITE);
		return scrollPane;
	}
}
