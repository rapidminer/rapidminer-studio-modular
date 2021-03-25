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
package com.rapidminer.storage.hdf5;

import static com.rapidminer.storage.hdf5.Hdf5ColumnReader.MILLIS_PER_SECOND;
import static com.rapidminer.storage.hdf5.Hdf5ColumnReader.NANOS_PER_MILLI;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Optional;
import java.util.function.BiConsumer;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.DateTimeColumn;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.column.Statistics.Result;
import com.rapidminer.belt.column.Statistics.Statistic;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.gui.processeditor.results.DisplayContext;
import com.rapidminer.hdf5.file.ColumnDescriptor;
import com.rapidminer.hdf5.file.TableWriter;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.table.ColumnInfo;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;

import io.jhdf.api.Attribute;
import io.jhdf.api.Dataset;


/**
 * Utility class for statistics of {@link Table} writing and reading for hdf5 files. Writes and reads different
 * statistical hdf5-attributes.
 * <p>
 * {@link #STATISTICS_MISSING} is the number of missing values in a column,
 * {@link #STATISTICS_MIN} is the minimal value in the column (only for numeric, time or date-time columns), as double
 * value for numeric, long value for time, long value for date-time with only second-precision, otherwise array of long
 * values containing seconds and nanoseconds,
 * {@link #STATISTICS_MIN} is the maximal value in the column (only for numeric, time or date-time columns), as double
 * value for numeric, long value for time, long value for date-time with only second-precision, otherwise array of long
 * values containing seconds and nanoseconds
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
enum TableStatisticsHandler {

	;//No-instance enum, only static methods

	static final String STATISTICS_MISSING = "statistics:missing";
	static final String STATISTICS_MIN = "statistics:min";
	static final String STATISTICS_MAX = "statistics:max";


	/**
	 * Adds statistics from the {@link Table} to the {@link ColumnDescriptor} for the given label.
	 *
	 * @param descriptor
	 * 		the column descriptor to add to
	 * @param label
	 * 		the attribute for which to add the descriptor
	 * @param table
	 * 		the table with the column with the label
	 */
	static void addStatistics(ColumnDescriptor descriptor, String label, Table table) {
		Column column = table.column(label);
		final Result count = Statistics.compute(column, Statistic.COUNT, new DisplayContext());
		descriptor.addAdditionalAttribute(STATISTICS_MISSING, int.class, table.height() - (int) count.getNumeric());
		final ColumnType<?> type = column.type();
		if (type.category() == Column.Category.NUMERIC) {
			addNumericStatistics(descriptor, column);
		} else if (type.id() == Column.TypeId.DATE_TIME) {
			addDateTimeStatistics(descriptor, column);
		} else if (type.id() == Column.TypeId.TIME) {
			addTimeStatistics(descriptor, column);
		}
	}

	/**
	 * Adds statistics from the {@link TableMetaData} to the {@link ColumnDescriptor} for the given label.
	 *
	 * @param descriptor
	 * 		the column descriptor to add to
	 * @param label
	 * 		the attribute for which to add the descriptor
	 * @param table
	 * 		the table meta data with the column with the label
	 */
	static void addStatistics(ColumnDescriptor descriptor, String label, TableMetaData table) {
		ColumnInfo column = table.column(label);
		Integer unknownStatistics = column.getMissingValues().getNumber();
		if (unknownStatistics != null) {
			descriptor.addAdditionalAttribute(STATISTICS_MISSING, int.class, (int) unknownStatistics);
		}
		if (column.isNumeric() == MetaDataInfo.YES) {
			addNumericStatistics(descriptor, column);
		} else if (ColumnType.DATETIME.equals(column.getType().orElse(null))) {
			addDateTimeStatistics(descriptor, column);
		} else if (ColumnType.TIME.equals(column.getType().orElse(null))) {
			addTimeStatistics(descriptor, column);
		}
	}

	/**
	 * Reads statistics for the set into the builder.
	 *
	 * @param set
	 * 		the set with the statistics attributes to read
	 * @param builder
	 * 		the info builder to store the statistics in
	 */
	static void readStatistics(Dataset set, ColumnInfoBuilder builder) {
		MDInteger missingMD = readMissings(set);
		builder.setMissings(missingMD);
		if (builder.isNumeric()== MetaDataInfo.YES) {
			readNumericRange(set, builder.getValueSetRelation(), builder::setNumericRange);
		} else if (ColumnType.DATETIME.equals(builder.getType().orElse(null))) {
			readDateTimeStatistics(set, builder);
		} else if (ColumnType.TIME.equals(builder.getType().orElse(null))) {
			//handle the case that it is time stored as date-time in pre 9.9
			Attribute type = set.getAttribute(TableWriter.ATTRIBUTE_TYPE);
			if (type != null) {
				Object typeData = type.getData();
				if ((typeData instanceof String) &&
						typeData.equals(ColumnDescriptor.Hdf5ColumnType.DATE_TIME.toString())) {
					readTimeStatisticsFromDatetime(set, builder);
					return;
				}
			}
			readTimeStatistics(set, builder);
		}
	}

	static MDInteger readMissings(Dataset set) {
		Attribute missings = set.getAttribute(STATISTICS_MISSING);
		MDInteger missingMD = new MDInteger();
		missingMD.setUnkown();
		if (missings != null) {
			Object data = missings.getData();
			if (data instanceof Number) {
				missingMD = new MDInteger(((Number) data).intValue());
			}
		}
		return missingMD;
	}

	/**
	 * Adds the numerical statistics (min/max) for the {@link Column} to the descriptor.
	 */
	private static void addNumericStatistics(ColumnDescriptor descriptor, Column column) {
		final Result min =	Statistics.compute(column, Statistic.MIN, new DisplayContext());
		final Result max = Statistics.compute(column, Statistic.MAX, new DisplayContext());
		ExampleSetStatisticsHandler.addNumericStatistics(descriptor, min.getNumeric(), max.getNumeric(), Double.NaN);
	}

	/**
	 * Adds the numerical statistics (min/max/mean) for the column to the descriptor.
	 */
	private static void addNumericStatistics(ColumnDescriptor descriptor, ColumnInfo column) {
		double statisticsMin = Double.NaN;
		double statisticsMax = Double.NaN;
		if (column.getValueSetRelation() != SetRelation.UNKNOWN) {
			Optional<Range> valueRange = column.getNumericRange();
			if (valueRange.isPresent()) {
				statisticsMin = valueRange.get().getLower();
				statisticsMax = valueRange.get().getUpper();
			}
		}
		ExampleSetStatisticsHandler.addNumericStatistics(descriptor, statisticsMin, statisticsMax, Double.NaN);
	}

	/**
	 * Adds the date-time statistics (min/max) for the column to the descriptor.
	 */
	private static void addDateTimeStatistics(ColumnDescriptor descriptor, Column column) {
		final Result min = Statistics.compute(column, Statistic.MIN, new DisplayContext());
		final Result max = Statistics.compute(column, Statistic.MAX, new DisplayContext());
		final Instant minInstant = min.getObject(Instant.class);
		final Instant maxInstant = max.getObject(Instant.class);
		if (minInstant != null && maxInstant != null) {
			if (column instanceof DateTimeColumn && !((DateTimeColumn) column).hasSubSecondPrecision()) {
				descriptor.addAdditionalAttribute(STATISTICS_MIN, long.class, minInstant.getEpochSecond());
				descriptor.addAdditionalAttribute(STATISTICS_MAX, long.class, maxInstant.getEpochSecond());
			} else {
				descriptor.addAdditionalAttribute(STATISTICS_MIN, long[].class,
						new long[]{minInstant.getEpochSecond(), (long) minInstant.getNano()});
				descriptor.addAdditionalAttribute(STATISTICS_MAX, long[].class,
						new long[]{maxInstant.getEpochSecond(), (long) maxInstant.getNano()});
			}
		}
	}

	/**
	 * Adds the date-time statistics (min/max) for the column to the descriptor.
	 */
	private static void addDateTimeStatistics(ColumnDescriptor descriptor, ColumnInfo column) {
		if (column.getValueSetRelation() == SetRelation.UNKNOWN) {
			return;
		}
		final Optional<ObjectRange<Instant>> objectRange = column.getObjectRange(Instant.class);
		if (!objectRange.isPresent()) {
			return;
		}
		final Instant minInstant = objectRange.get().getLower();
		final Instant maxInstant = objectRange.get().getUpper();

		if (minInstant.getNano() == 0 && maxInstant.getNano() == 0) {
			descriptor.addAdditionalAttribute(STATISTICS_MIN, long.class, minInstant.getEpochSecond());
			descriptor.addAdditionalAttribute(STATISTICS_MAX, long.class, maxInstant.getEpochSecond());
		} else {
			descriptor.addAdditionalAttribute(STATISTICS_MIN, long[].class,
					new long[]{minInstant.getEpochSecond(), (long) minInstant.getNano()});
			descriptor.addAdditionalAttribute(STATISTICS_MAX, long[].class,
					new long[]{maxInstant.getEpochSecond(), (long) maxInstant.getNano()});
		}
	}

	/**
	 * Adds the time statistics (min/max) for the column to the descriptor.
	 */
	private static void addTimeStatistics(ColumnDescriptor descriptor, Column column) {
		final Result min = Statistics.compute(column, Statistic.MIN, new DisplayContext());
		final Result max = Statistics.compute(column, Statistic.MAX, new DisplayContext());
		final LocalTime minInstant = min.getObject(LocalTime.class);
		final LocalTime maxInstant = max.getObject(LocalTime.class);
		if (minInstant != null && maxInstant != null) {
			descriptor.addAdditionalAttribute(STATISTICS_MIN, long.class, minInstant.toNanoOfDay());
			descriptor.addAdditionalAttribute(STATISTICS_MAX, long.class, maxInstant.toNanoOfDay());
		}
	}

	/**
	 * Adds the time statistics (min/max) for the column to the descriptor.
	 */
	private static void addTimeStatistics(ColumnDescriptor descriptor, ColumnInfo column) {
		if (column.getValueSetRelation() != SetRelation.UNKNOWN) {
			final Optional<ObjectRange<LocalTime>> objectRange = column.getObjectRange(LocalTime.class);
			if (objectRange.isPresent()) {
				final LocalTime minInstant = objectRange.get().getLower();
				final LocalTime maxInstant = objectRange.get().getUpper();
				descriptor.addAdditionalAttribute(STATISTICS_MIN, long.class, minInstant.toNanoOfDay());
				descriptor.addAdditionalAttribute(STATISTICS_MAX, long.class, maxInstant.toNanoOfDay());
			}
		}
	}

	/**
	 * Reads the numeric statistics (min/max) from the set and adds them to the rangeConsumer.
	 */
	static void readNumericRange(Dataset set, SetRelation relation, BiConsumer<Range, SetRelation> rangeConsumer) {
		Attribute min = set.getAttribute(STATISTICS_MIN);
		Attribute max = set.getAttribute(STATISTICS_MAX);
		if (min != null && max != null) {
			Object minData = min.getData();
			Object maxData = max.getData();
			if (minData instanceof Number && maxData instanceof Number) {
				if (relation == SetRelation.UNKNOWN) {
					// if the set relation is unknown at this point, this might just be wrongly configured/missing
					// an attribute cannot have a range AND be an unknown relation
					relation = SetRelation.EQUAL;
				}
				rangeConsumer.accept(new Range(((Number) min.getData()).doubleValue(),
						((Number) max.getData()).doubleValue()), relation);
			}
		}
	}

	/**
	 * Reads the date-time statistics (min/max) from the attributes of the set and adds them to the meta data.
	 */
	private static void readDateTimeStatistics(Dataset set, ColumnInfoBuilder builder) {
		Attribute min = set.getAttribute(STATISTICS_MIN);
		Attribute max = set.getAttribute(STATISTICS_MAX);
		if (min != null && max != null) {
			Object minData = min.getData();
			Object maxData = max.getData();
			if (minData instanceof long[] && maxData instanceof long[]) {
				long[] mins = (long[]) minData;
				long[] maxs = (long[]) maxData;
				readSecondsAndNanos(builder, mins, maxs);
			} else if (minData instanceof Number && maxData instanceof Number) {
				try {
					Instant minValue = Instant.ofEpochSecond(((Number) minData).longValue());
					Instant maxValue = Instant.ofEpochSecond(((Number) maxData).longValue());
					builder.setObjectRange(new ObjectRange<>(minValue, maxValue, ColumnType.DATETIME.comparator())
							, SetRelation.EQUAL);
				} catch (ArithmeticException | DateTimeException e) {
					// ignore faulty values
				}
			}
		}
	}

	/**
	 * Reads the instant data consisting of seconds and nanos.
	 */
	private static void readSecondsAndNanos(ColumnInfoBuilder builder, long[] mins, long[] maxs) {
		if (mins.length == 2 && maxs.length == 2) {
			try {
				Instant minValue = Instant.ofEpochSecond(mins[0], mins[1]);
				Instant maxValue = Instant.ofEpochSecond(maxs[0], maxs[1]);
				builder.setObjectRange(new ObjectRange<>(minValue, maxValue, ColumnType.DATETIME.comparator())
						, SetRelation.EQUAL);
			} catch (ArithmeticException | DateTimeException e) {
				// ignore faulty values
			}
		}
	}

	/**
	 * Reads the time statistics (min/max) from the attributes of the set and adds them to the meta data.
	 */
	private static void readTimeStatistics(Dataset set, ColumnInfoBuilder builder) {
		Attribute min = set.getAttribute(STATISTICS_MIN);
		Attribute max = set.getAttribute(STATISTICS_MAX);
		if (min != null && max != null) {
			Object minData = min.getData();
			Object maxData = max.getData();
			if (minData instanceof Number && maxData instanceof Number) {
				try {
					LocalTime minValue = LocalTime.ofNanoOfDay(((Number) minData).longValue());
					LocalTime maxValue = LocalTime.ofNanoOfDay(((Number) maxData).longValue());
					builder.setObjectRange(new ObjectRange<>(minValue, maxValue, ColumnType.TIME.comparator())
							, SetRelation.EQUAL);
				} catch (ArithmeticException | DateTimeException e) {
					// ignore faulty values
				}
			}
		}
	}

	/**
	 * Reads the date-time statistics (min/max) from the attributes of the set and adds them to the meta data.
	 */
	private static void readTimeStatisticsFromDatetime(Dataset set, ColumnInfoBuilder builder) {
		Attribute min = set.getAttribute(STATISTICS_MIN);
		Attribute max = set.getAttribute(STATISTICS_MAX);
		if (min != null && max != null) {
			Object minData = min.getData();
			Object maxData = max.getData();
			if (minData instanceof long[] && maxData instanceof long[]) {
				long[] mins = (long[]) minData;
				long[] maxs = (long[]) maxData;
				readTimeSecondsAndNanos(builder, mins, maxs);
			} else if (minData instanceof Number && maxData instanceof Number) {
				try {
					Calendar calendar = Tools.getPreferredCalendar();
					LocalTime minValue = LocalTime.ofNanoOfDay(BeltConverter.legacyTimeDoubleToNanoOfDay(
							((Number) minData).longValue() * MILLIS_PER_SECOND, calendar));
					LocalTime maxValue = LocalTime.ofNanoOfDay(BeltConverter.legacyTimeDoubleToNanoOfDay(
							((Number) maxData).longValue() * MILLIS_PER_SECOND, calendar));
					builder.setObjectRange(new ObjectRange<>(minValue, maxValue, ColumnType.TIME.comparator())
							, SetRelation.EQUAL);
				} catch (ArithmeticException | DateTimeException e) {
					// ignore faulty values
				}
			}
		}
	}

	/**
	 * Reads the instant data consisting of seconds and nanos.
	 */
	private static void readTimeSecondsAndNanos(ColumnInfoBuilder builder, long[] mins, long[] maxs) {
		if (mins.length == 2 && maxs.length == 2) {
			try {
				Calendar calendar = Tools.getPreferredCalendar();
				LocalTime minValue = LocalTime.ofNanoOfDay(BeltConverter.legacyTimeDoubleToNanoOfDay(
						mins[0] * MILLIS_PER_SECOND + mins[1] / NANOS_PER_MILLI, calendar));
				LocalTime maxValue = LocalTime.ofNanoOfDay(BeltConverter.legacyTimeDoubleToNanoOfDay(
						maxs[0] * MILLIS_PER_SECOND + maxs[1] / NANOS_PER_MILLI, calendar));
				builder.setObjectRange(new ObjectRange<>(minValue, maxValue, ColumnType.TIME.comparator())
						, SetRelation.EQUAL);
			} catch (ArithmeticException | DateTimeException e) {
				// ignore faulty values
			}
		}
	}
}
