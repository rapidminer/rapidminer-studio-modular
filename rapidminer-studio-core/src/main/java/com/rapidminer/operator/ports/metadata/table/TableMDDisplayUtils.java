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
package com.rapidminer.operator.ports.metadata.table;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.rapidminer.RapidMiner;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.LegacyType;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Utility methods to display {@link TableMetaData} in the UI. Some of them are analogous to display methods for {@link
 * ExampleSetMetaData} in order to hide the difference.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public enum TableMDDisplayUtils {
	;//no instance enum

	/** Threshold for the number of nominal values shown by {@link #getLegacyRangeString(TableMetaData, String)} */
	private static final int MAX_DISPLAYED_NOMINAL_VALUES = 1000;

	private static final String DOTS = " \u2013 ";

	/**
	 * Gets the role of the column with the label as String analogously to {@link AttributeMetaData#getRole()}.
	 *
	 * @param tableMetaData
	 * 		the table meta data containing the column
	 * @param label
	 * 		the column name
	 * @return the role as String
	 */
	public static String getLegacyRoleString(TableMetaData tableMetaData, String label) {
		final ColumnRole role = tableMetaData.getFirstColumnMetaData(label, ColumnRole.class);
		return role == null ? null : role.toString().toLowerCase(Locale.ENGLISH);
	}

	/**
	 * Returns the legacy value type name associated with the column type of the info, analogously to {@link
	 * AttributeMetaData#getValueTypeName()}.
	 *
	 * @param info
	 * 		the column info to check for the type
	 * @return the legacy value type name
	 */
	public static String getLegacyValueTypeName(ColumnInfo info) {
		final Optional<ColumnType<?>> type = info.getType();
		if (!type.isPresent()) {
			return Ontology.VALUE_TYPE_NAMES[Ontology.ATTRIBUTE_VALUE];
		}
		String valueTypeString = type.get().id().toString().toLowerCase(Locale.ENGLISH).replace("-", "_");
		if (info.getDictionary().isBoolean()) {
			valueTypeString = Ontology.VALUE_TYPE_NAMES[Ontology.BINOMINAL];
		}
		return valueTypeString;
	}

	/**
	 * Returns either the value range or the value set, depending on the type of column, analogously to {@link
	 * AttributeMetaData#getRangeString()}.
	 *
	 * @param md
	 * 		the table meta data
	 * @param label
	 * 		the label of the column to display
	 * @return the value set or range as String
	 */
	public static String getLegacyRangeString(TableMetaData md, String label) {
		final ColumnInfo column = md.column(label);
		final SetRelation valueSetRelation = column.getValueSetRelation();
		if (ColumnType.DATETIME.equals(column.getType().orElse(null))) {
			if (column.getObjectRange(Instant.class).isPresent()) {
				return createLegacyDateTimeString(md, label, column, valueSetRelation);
			}
			return "Unbounded date range";
		} else if (ColumnType.TIME.equals(column.getType().orElse(null))) {
			if (column.getObjectRange(LocalTime.class).isPresent()) {
				return createTimeString(column);
			}
			return "Unbounded time range";
		} else if (column.isNumeric() == MetaDataInfo.YES && column.getNumericRange().isPresent()) {
			return valueSetRelation +
					(valueSetRelation != SetRelation.UNKNOWN ? column.getNumericRange().get().toString() : "");
		} else if (column.isNominal() == MetaDataInfo.YES) {
			return valueSetRelation +
					(valueSetRelation != SetRelation.UNKNOWN ? setToString(column.getDictionary().getValueSet()) : "");
		} else if (column.isObject() == MetaDataInfo.YES && column.getUncheckedObjectRange() != null &&
				valueSetRelation != SetRelation.UNKNOWN) {
			return valueSetRelation + "[" + column.getUncheckedObjectRange().getLower() + DOTS +
					column.getUncheckedObjectRange().getUpper() + "]";
		} else {
			return "unknown";
		}
	}

	/**
	 * Gets the column annotation for the label in the tableMetaData or {@code null} if none exists.
	 *
	 * @param tableMetaData
	 * 		the table meta data
	 * @param label
	 * 		the name of the column
	 * @return the column annotation
	 */
	public static String getColumnAnnotationAsComment(TableMetaData tableMetaData, String label) {
		final ColumnAnnotation columnAnnotation =
				tableMetaData.getFirstColumnMetaData(label, ColumnAnnotation.class);
		return columnAnnotation == null ? null : columnAnnotation.annotation();
	}

	/**
	 * Returns the description of the column information similarly to {@link AttributeMetaData#getDescription()} but
	 * not using legacy types.
	 *
	 * @param columnInfo
	 * 		the info to describe
	 * @return a string representation for the info
	 */
	static String getDescription(ColumnInfo columnInfo) {
		StringBuilder buf = new StringBuilder();
		buf.append(" (");
		final Optional<ColumnType<?>> type = columnInfo.getType();
		if (type.isPresent()) {
			buf.append(type.get().id().toString().toLowerCase(Locale.ENGLISH));
		} else {
			buf.append("unknown");
		}
		if (columnInfo.getValueSetRelation() != SetRelation.UNKNOWN) {
			buf.append(" in ");
			appendValueSetDescription(buf, columnInfo);
		} else {
			if (columnInfo.isNominal() == MetaDataInfo.YES) {
				buf.append(", values unkown");
			} else {
				buf.append(", range unknown");
			}
		}
		switch (columnInfo.hasMissingValues()) {
			case NO:
				buf.append("; no missing values");
				break;
			case YES:
				buf.append("; ");
				buf.append(columnInfo.getMissingValues().toString());
				buf.append(" missing values");
				break;
			case UNKNOWN:
				buf.append("; may contain missing values");
				break;
		}
		buf.append(")");
		return buf.toString();
	}

	/**
	 * Creates a short description of the table meta data to display in the tooltip analogously to {@link
	 * ExampleSetMetaData#getShortDescription()}.
	 *
	 * @param startDescription
	 * 		the description to start with
	 * @param md
	 * 		the table meta data to describe
	 * @return the short description
	 */
	static String getLegacyShortDescription(String startDescription, TableMetaData md) {
		StringBuilder buf = new StringBuilder(startDescription);
		buf.append("<br/>Number of examples ");
		buf.append(md.height().toString());
		if (!md.labels().isEmpty()) {
			buf.append("<br/>");
			switch (md.getColumnSetRelation()) {
				case SUBSET:
					buf.append("At most ");
					break;
				case SUPERSET:
					buf.append("At least ");
					break;
				default:
					// ignore, number of attributes will evaluate to "1 attribute" or "x attributes"
					break;
			}
			buf.append(md.labels().size());
			buf.append(" attribute").append(md.labels().size() != 1 ? "s" : "").append(": ");
		}
		if (md.labels().stream().anyMatch(l -> md.column(l).getDictionary().valueSetWasShrunk())) {
			buf.append(
					"<br/><small><strong>Note:</strong> Some of the nominal values in this set were discarded due to" +
							" performance reasons. You can change this behaviour in the preferences (<code>"
							+ RapidMiner.PROPERTY_RAPIDMINER_GENERAL_MAX_NOMINAL_VALUES + "</code>).</small>");
		}
		return buf.toString();
	}

	/**
	 * String representation of {@link TableMetaData} analogously to {@link ExampleSetMetaData#toString()} but without
	 * legacy names.
	 *
	 * @param md
	 * 		the table meta data to show
	 * @return a String representation für the input
	 */
	static String toString(TableMetaData md) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("TableMetaData: #rows: ").append(md.height()).append("; #columns: ")
				.append(md.labels().size()).append(Tools.getLineSeparator());
		for (String label : md.labels()) {
			buffer.append(getDescription(md, label)).append(Tools.getLineSeparator());
		}
		return buffer.toString();
	}

	/**
	 * Creates a description of the table meta data to display in the Result History analogously to {@link
	 * ExampleSetMetaData#getDescription()}.
	 *
	 * @param startDescription
	 * 		the description to start with
	 * @param md
	 * 		the table meta data to describe
	 * @return a html table description of the table meta data
	 */
	static String getLegacyDescription(String startDescription, TableMetaData md) {
		StringBuilder buf = new StringBuilder(startDescription);
		buf.append("<br/>Number of examples ");
		buf.append(md.height().toString());
		if (!md.labels().isEmpty()) {
			buf.append("<br/>");
			switch (md.getColumnSetRelation()) {
				case SUBSET:
					buf.append("At most ");
					break;
				case SUPERSET:
					buf.append("At least ");
					break;
				default:
					// ignore, number of attributes will evaluate to "1 attribute" or "x attributes"
					break;
			}
			buf.append(md.labels().size());
			buf.append(" attribute").append(md.labels().size() != 1 ? "s" : "").append(": ");
			buf.append(
					"<table><thead><tr><th>Role</th><th>Name</th><th>Type</th><th>Range</th><th>Missings</th><th" +
							">Comment</th></tr></thead><tbody>");
			for (String label : md.labels()) {
				buf.append(getLegacyDescriptionAsTableRow(md, label));
			}
			buf.append("</tbody></table>");
		}
		return buf.toString();
	}

	/**
	 * Creates a table row String for the meta data table in the Result History analogously to {@link
	 * AttributeMetaData#getDescriptionAsTableRow()}.
	 */
	private static String getLegacyDescriptionAsTableRow(TableMetaData tableMetaData, String label) {
		StringBuilder b = new StringBuilder();
		b.append("<tr><td>");
		String role = getLegacyRoleString(tableMetaData, label);
		String role2 = role == null ? "-" : role;
		b.append(role2).append("</td><td>");
		b.append(Tools.escapeHTML(label));
		b.append("</td><td>");
		ColumnInfo info = tableMetaData.column(label);
		b.append(getLegacyValueTypeName(info)).append("</td><td>");

		if (info.getValueSetRelation() != SetRelation.UNKNOWN) {
			LegacyType legacyType = tableMetaData.getFirstColumnMetaData(label, LegacyType.class);
			appendLegacyValueSetDescription(b, info, legacyType);
		} else {
			if (info.isNominal() == MetaDataInfo.YES) {
				b.append("values unkown");
			} else {
				b.append("range unknown");
			}
		}
		b.append("</td><td>");

		switch (info.hasMissingValues()) {
			case NO:
				b.append("no missing values");
				break;
			case YES:
				b.append(info.getMissingValues().toString());
				b.append(" missing values");
				break;
			case UNKNOWN:
				b.append("may contain missing values");
				break;
		}

		final String comment = getColumnAnnotationAsComment(tableMetaData, label);
		b.append("</td><td>").append(comment != null ? comment : "-").append("</tr></tr>");
		return b.toString();
	}

	/**
	 * Returns the description for the column associated with the label in the table meta data.
	 */
	private static String getDescription(TableMetaData tableMetaData, String label) {
		StringBuilder buf = new StringBuilder();
		final ColumnRole role = tableMetaData.getFirstColumnMetaData(label, ColumnRole.class);
		if (role != null) {
			buf.append("<em>");
			buf.append(role.toString().toLowerCase(Locale.ENGLISH));
			buf.append("</em>: ");
		}
		buf.append(label);
		buf.append(getDescription(tableMetaData.column(label)));
		return buf.toString();
	}

	/**
	 * Adds a value set description analogously to {@link AttributeMetaData#getValueSetDescription()} to the builder.
	 */
	private static void appendLegacyValueSetDescription(StringBuilder builder, ColumnInfo info, LegacyType legacyType) {
		if (info.isNominal() == MetaDataInfo.YES) {
			DictionaryInfo dictionary = info.getDictionary();
			appendDictionary(builder, info, dictionary);
		}
		if (info.isNumeric() == MetaDataInfo.YES) {
			appendNumericRange(builder, info);
		}
		if (ColumnType.DATETIME.equals(info.getType().orElse(null)) && info.getObjectRange(Instant.class).isPresent()) {
			int legacyOntology = Ontology.DATE_TIME;
			if (legacyType != null) {
				if (legacyType == LegacyType.DATE) {
					legacyOntology = Ontology.DATE;
				} else if (legacyType == LegacyType.TIME) {
					legacyOntology = Ontology.TIME;
				}
			}
			final ObjectRange<Instant> range = info.getObjectRange(Instant.class).get();
			builder.append(info.getValueSetRelation()).append(" [");
			switch (legacyOntology) {
				case Ontology.DATE:
					builder.append(Tools.formatDate(new Date((long) BeltConverter.toEpochMilli(range.getLower()))));
					builder.append("...");
					builder.append(Tools.formatDate(new Date((long) BeltConverter.toEpochMilli(range.getUpper()))));
					builder.append("]");
					break;
				case Ontology.TIME:
					builder.append(Tools.formatTimeWithMillis(new Date((long) BeltConverter.toEpochMilli(range.getLower()))));
					builder.append("...");
					builder.append(Tools.formatTimeWithMillis(new Date((long) BeltConverter.toEpochMilli(range.getUpper()))));
					builder.append("]");
					break;
				case Ontology.DATE_TIME:
				default:
					builder.append(Tools.formatDateTime(new Date((long) BeltConverter.toEpochMilli(range.getLower()))));
					builder.append("...");
					builder.append(Tools.formatDateTime(new Date((long) BeltConverter.toEpochMilli(range.getUpper()))));
					builder.append("]");
					break;
			}
		} else if (info.isObject() == MetaDataInfo.YES) {
			appendObjectRange(builder, info);
		}
	}

	/**
	 * Adds the object range to the builder if present.
	 */
	private static void appendObjectRange(StringBuilder builder, ColumnInfo info) {
		if (info.getUncheckedObjectRange() != null) {
			builder.append(info.getValueSetRelation()).append(" [");
			builder.append(info.getUncheckedObjectRange().getLower());
			builder.append("...");
			builder.append(info.getUncheckedObjectRange().getUpper());
			builder.append("]");
		}
	}

	/**
	 * Adds the numeric range to the builder if present.
	 */
	private static void appendNumericRange(StringBuilder builder, ColumnInfo info) {
		final Optional<Range> numericRange = info.getNumericRange();
		if (numericRange.isPresent()) {
			builder.append(info.getValueSetRelation()).append(" [");
			builder.append(Tools.formatNumber(numericRange.get().getLower(), 3));
			builder.append("...");
			builder.append(Tools.formatNumber(numericRange.get().getUpper(), 3));
			builder.append("]");
		}
	}

	/**
	 * Adds the dictionary to the builder.
	 */
	private static void appendDictionary(StringBuilder builder, ColumnInfo info, DictionaryInfo dictionary) {
		builder.append(info.getValueSetRelation()).append(" {");
		boolean first = true;
		int index = 0;
		for (String value : dictionary.getValueSet()) {
			index++;
			if (first) {
				first = false;
			} else {
				builder.append(", ");
			}

			if (index >= 10) {
				builder.append("...");
				break;
			}

			builder.append(Tools.escapeHTML(value));
		}
		builder.append("}");
	}

	/**
	 * Adds the value set description depending on the info type to the builder.
	 */
	private static void appendValueSetDescription(StringBuilder builder, ColumnInfo info) {
		if (info.isNominal() == MetaDataInfo.YES) {
			DictionaryInfo dictionary = info.getDictionary();
			if (dictionary.isBoolean()) {
				appendBooleanDictionary(builder, info, dictionary);
			} else {
				appendDictionary(builder, info, dictionary);
			}
		} else if (info.isNumeric() == MetaDataInfo.YES) {
			appendNumericRange(builder, info);
		} else if (info.isObject() == MetaDataInfo.YES) {
			appendObjectRange(builder, info);
		}
	}

	/**
	 * Adds the boolean dictionary info to the builder.
	 */
	private static void appendBooleanDictionary(StringBuilder builder, ColumnInfo info, DictionaryInfo dictionary) {
		builder.append(info.getValueSetRelation());
		if (dictionary.hasPositive() == MetaDataInfo.UNKNOWN) {
			builder.append(" unknown boolean");
		} else {
			builder.append(" {");
			final Optional<String> positiveValue = dictionary.getPositiveValue();
			if (positiveValue.isPresent()) {
				builder.append("positive: ");
				builder.append(Tools.escapeHTML(positiveValue.get()));
			}
			final Optional<String> negativeValue = dictionary.getNegativeValue();
			if (negativeValue.isPresent()) {
				if (positiveValue.isPresent()) {
					builder.append(", ");
				}
				builder.append("negative: ");
				builder.append(Tools.escapeHTML(negativeValue.get()));
			}
			builder.append("}");
		}
	}

	/**
	 * Creates a String for the data-time range in legacy format.
	 */
	private static String createLegacyDateTimeString(TableMetaData md, String label, ColumnInfo column,
													 SetRelation valueSetRelation) {
		StringBuilder buf = new StringBuilder();
		buf.append(valueSetRelation);
		if (valueSetRelation != SetRelation.UNKNOWN) {
			final LegacyType legacyType = md.getFirstColumnMetaData(label, LegacyType.class);
			int legacyOntology = Ontology.DATE_TIME;
			if (legacyType != null) {
				if (legacyType == LegacyType.DATE) {
					legacyOntology = Ontology.DATE;
				} else if (legacyType == LegacyType.TIME) {
					legacyOntology = Ontology.TIME;
				}
			}
			final Optional<ObjectRange<Instant>> objectRange = column.getObjectRange(Instant.class);
			if (objectRange.isPresent()) {
				final ObjectRange<Instant> range = objectRange.get();
				buf.append("[");
				switch (legacyOntology) {
					case Ontology.DATE:
						buf.append(Tools.formatDate(new Date((long) BeltConverter.toEpochMilli(range.getLower()))));
						buf.append(DOTS);
						buf.append(Tools.formatDate(new Date((long) BeltConverter.toEpochMilli(range.getUpper()))));
						break;
					case Ontology.TIME:
						buf.append(Tools.formatTimeWithMillis(new Date((long) BeltConverter.toEpochMilli(range.getLower()))));
						buf.append(DOTS);
						buf.append(Tools.formatTimeWithMillis(new Date((long) BeltConverter.toEpochMilli(range.getUpper()))));
						break;
					case Ontology.DATE_TIME:
					default:
						buf.append(Tools.formatDateTime(new Date((long) BeltConverter.toEpochMilli(range.getLower()))));
						buf.append(DOTS);
						buf.append(Tools.formatDateTime(new Date((long) BeltConverter.toEpochMilli(range.getUpper()))));
						break;
				}
				buf.append("]");
			}
			return buf.toString();
		} else {
			return "Unknown date range";
		}
	}

	/**
	 * Creates a String for the data-time range in legacy format.
	 */
	private static String createTimeString(ColumnInfo column) {
		StringBuilder buf = new StringBuilder();
		SetRelation valueSetRelation = column.getValueSetRelation();
		buf.append(valueSetRelation);
		if (valueSetRelation != SetRelation.UNKNOWN) {
			final Optional<ObjectRange<LocalTime>> objectRange = column.getObjectRange(LocalTime.class);
			if (objectRange.isPresent()) {
				final ObjectRange<LocalTime> range = objectRange.get();
				buf.append("[");
				buf.append(Tools.formatLocalTime(range.getLower()));
				buf.append(DOTS);
				buf.append(Tools.formatLocalTime(range.getUpper()));
				buf.append("]");
			}
			return buf.toString();
		} else {
			return "Unknown time range";
		}
	}

	/**
	 * Converts String set into String analogously to {@link AbstractCollection#toString()}, but with a maximum number
	 * of entries. This is necessary, because otherwise the UI can freeze when this String is calculated repeatedly.
	 * Note that the maximum number of entries cannot be determined by
	 * {@link AttributeMetaData#getMaximumNumberOfNominalValues()}
	 * because we do not want our UI to freeze if the user sets this too high.
	 *
	 * @param set
	 * 		the set which should be converted to a String
	 * @return a String with at most {@link #MAX_DISPLAYED_NOMINAL_VALUES} values of the set
	 */
	private static String setToString(Set<String> set) {
		if (set.isEmpty()) {
			return "[]";
		}

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		int size = set.size();
		int shownValues = Math.min(size, MAX_DISPLAYED_NOMINAL_VALUES);
		Iterator<String> it = set.iterator();
		for (int i = 0; i < shownValues - 1; i++) {
			String e = it.next();
			sb.append(e);
			sb.append(',').append(' ');
		}
		if (shownValues < size) {
			sb.append("...");
		} else {
			sb.append(it.next());
		}
		return sb.append(']').toString();
	}
}
