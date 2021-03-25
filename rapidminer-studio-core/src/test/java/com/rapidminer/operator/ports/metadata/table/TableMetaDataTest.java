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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.type.StringSet;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDNumber;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.settings.Settings;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Tests the {@link TableMetaData}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
@RunWith(Enclosed.class)
public class TableMetaDataTest {

	private static class SalesExampleSetGeneratorTest {

		private static final int MAX_CUSTOMERS = 2000;

		private static final String[] STORES = {"Store 01", "Store 02", "Store 03", "Store 04", "Store 05",
				"Store 06", "Store 07", "Store 08", "Store 09", "Store 10", "Store 11", "Store 12", "Store 13",
				"Store 14", "Store 15"};

		private static final String[] PRODUCT_CATEGORIES = new String[]{"Books", "Movies", "Electronics", "Home" +
				"/Garden", "Health", "Toys", "Sports", "Clothing"};

		private static final TableMetaData DEFAULT_META_DATA;

		private static final int PRODUCT_ID_LOWER = 10_000;
		private static final int PRODUCT_ID_UPPER = 100_000;

		static {
			TableMetaDataBuilder emd = new TableMetaDataBuilder(MDInteger.newUnknown());
			emd.add("transaction_id", ColumnType.INTEGER_53_BIT, new MDInteger(0))
					.addColumnMetaData("transaction_id", ColumnRole.ID);
			emd.addNominal("store_id", Arrays.asList(STORES), SetRelation.EQUAL, new MDInteger(0));
			String[] customers = new String[MAX_CUSTOMERS];
			for (int i = 0; i < MAX_CUSTOMERS; i++) {
				customers[i] = "Customer " + (i + 1);
			}

			emd.addNominal("customer_id", Arrays.asList(customers), SetRelation.EQUAL, new MDInteger(0));
			emd.addInteger("product_id", new Range(PRODUCT_ID_LOWER, PRODUCT_ID_UPPER), SetRelation.EQUAL, new MDInteger(0));
			emd.addNominal("product_category", Arrays.asList(PRODUCT_CATEGORIES), SetRelation.EQUAL, new MDInteger(0));

			emd.addDateTime("date", new ObjectRange<>(Instant.parse("2005-02-01T00:00:00.000Z"),
							Instant.parse("2008-11-30T00:00:00.000Z"), ColumnType.DATETIME.comparator()),
					SetRelation.EQUAL, new MDInteger(0));
			emd.addInteger("amount", new Range(1, 10), SetRelation.EQUAL, new MDInteger(0));
			emd.addReal("single_price", new Range(10, 100), SetRelation.EQUAL, new MDInteger(0));
			DEFAULT_META_DATA = emd.build();
		}

		public TableMetaData getGeneratedMetaData(int numberOfRows) {
			TableMetaDataBuilder emd = new TableMetaDataBuilder(getDefaultMetaData());
			emd.updateHeightWithoutSideEffects(new MDInteger(numberOfRows));
			return emd.build();
		}

		protected TableMetaData getDefaultMetaData() {
			return DEFAULT_META_DATA;
		}
	}

	public static class SalesDataGeneratorTest {

		@Test
		public void testSalesExampleSetGeneratorSizes() {
			TableMetaData generatedMetaData = new SalesExampleSetGeneratorTest().getGeneratedMetaData(42);
			assertEquals(42, generatedMetaData.height().getNumber().intValue());
			assertEquals(MDNumber.Relation.EQUAL, generatedMetaData.height().getRelation());
			assertEquals(8, generatedMetaData.labels().size());
		}

		@Test
		public void testSalesExampleSetGeneratorLabels() {
			TableMetaData generatedMetaData = new SalesExampleSetGeneratorTest().getGeneratedMetaData(42);
			final List<String> expectedLabels = Arrays.asList("transaction_id", "store_id", "customer_id",
					"product_id", "product_category", "date", "amount", "single_price");
			assertEquals(expectedLabels, new ArrayList<>(generatedMetaData.labels()));
			final ColumnRole[] columnRoles =
					expectedLabels.stream().map(l -> generatedMetaData.getFirstColumnMetaData(l, ColumnRole.class)).toArray(ColumnRole[]::new);
			ColumnRole[] expectedColumnRoles =
					new ColumnRole[]{ColumnRole.ID, null, null, null, null, null, null, null};
			assertArrayEquals(expectedColumnRoles, columnRoles);
		}

		@Test
		public void testSalesExampleSetGeneratorStatistics() {
			TableMetaData generatedMetaData = new SalesExampleSetGeneratorTest().getGeneratedMetaData(42);
			final List<String> expectedLabels = Arrays.asList("transaction_id", "store_id", "customer_id",
					"product_id",
					"product_category", "date", "amount", "single_price");
			final MDInteger[] missings =
					expectedLabels.stream().map(l -> generatedMetaData.column(l).getMissingValues()).toArray(MDInteger[]::new);
			final MDInteger noMissings = new MDInteger(0);
			MDInteger[] expectedMissings = new MDInteger[missings.length];
			Arrays.fill(expectedMissings, noMissings);
			//need to compare MDInteger by hand since it has no sensible equals
			assertArrayEquals(Arrays.stream(expectedMissings).map(MDNumber::getNumber).toArray(),
					Arrays.stream(missings).map(MDNumber::getNumber).toArray());
			assertArrayEquals(Arrays.stream(expectedMissings).map(MDNumber::getRelation).toArray(),
					Arrays.stream(missings).map(MDNumber::getRelation).toArray());
		}
	}

	public static class TableTest {

		@Test
		public void testTable() {
			Table table = Builders.newTableBuilder(10)
					.addNominal("nominal", i -> i == 0 ? null : "value")
					.addBoolean("boolean", i -> "value", "value")
					.addInt53Bit("int", i -> 2)
					.addReal("real", i -> i == 2 ? Double.NaN : 2)
					.addDateTime("date_time", i -> Instant.EPOCH)
					.addTime("time", i -> LocalTime.NOON)
					.addTextset("set", i -> new StringSet(Collections.singletonList("")))
					.addTextlist("list", i -> null).build(Belt.defaultContext());
			final TableMetaData tableMetaData = new TableMetaData(new IOTable(table), false);

			TableMetaData controlMD = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON, ColumnType.TIME.comparator()),
							SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("list", ColumnType.TEXTLIST, new MDInteger(10)).build();

			assertEquals(controlMD.getFullColumns(), tableMetaData.getFullColumns());
			assertEquals(controlMD.getMetaDataMap(), tableMetaData.getMetaDataMap());
			assertEquals(controlMD.height().getRelation(), tableMetaData.height().getRelation());
			assertEquals(controlMD.height().getNumber(), tableMetaData.height().getNumber());
			assertEquals(controlMD.getColumnSetRelation(), tableMetaData.getColumnSetRelation());
		}

		@Test
		public void testTableWithoutStats() {
			Table table = Builders.newTableBuilder(10)
					.addNominal("nominal", i -> i == 0 ? null : "value")
					.addBoolean("boolean", i -> "value", "value")
					.addInt53Bit("int", i -> 2)
					.addReal("real", i -> i == 2 ? Double.NaN : 2)
					.addDateTime("date_time", i -> Instant.EPOCH)
					.addTime("time", i -> LocalTime.NOON)
					.addTextset("set", i -> new StringSet(Collections.singletonList("")))
					.addTextlist("list", i -> null).build(Belt.defaultContext());
			final TableMetaData tableMetaData = new TableMetaData(new IOTable(table), true);

			TableMetaData controlMD = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, MDInteger.newPossible())
					.addBoolean("boolean", "value", null, MDInteger.newPossible())
					.addInteger("int", null, SetRelation.EQUAL, MDInteger.newPossible())
					.addReal("real", null, SetRelation.EQUAL, MDInteger.newPossible())
					.addDateTime("date_time", null, SetRelation.EQUAL, MDInteger.newPossible())
					.addTime("time", null, SetRelation.EQUAL, MDInteger.newPossible())
					.add("set", ColumnType.TEXTSET, MDInteger.newPossible())
					.add("list", ColumnType.TEXTLIST, MDInteger.newPossible()).build();

			assertEquals(controlMD.getFullColumns(), tableMetaData.getFullColumns());
			assertEquals(controlMD.getMetaDataMap(), tableMetaData.getMetaDataMap());
			assertEquals(controlMD.height().getRelation(), tableMetaData.height().getRelation());
			assertEquals(controlMD.height().getNumber(), tableMetaData.height().getNumber());
			assertEquals(controlMD.getColumnSetRelation(), tableMetaData.getColumnSetRelation());
		}

		@Test
		public void testMethods() {
			Table table = Builders.newTableBuilder(10)
					.addNominal("nominal", i -> i == 0 ? null : "value")
					.addBoolean("boolean", i -> "value", "value")
					.addInt53Bit("int", i -> 2)
					.addReal("real", i -> i == 2 ? Double.NaN : 2)
					.addDateTime("date_time", i -> Instant.EPOCH)
					.addTime("time", i -> LocalTime.NOON)
					.addTextset("set", i -> new StringSet(Collections.singletonList("")))
					.addTextlist("list", i -> null)
					.addMetaData("nominal", ColumnRole.LABEL)
					.addMetaData("real", ColumnRole.METADATA)
					.addMetaData("list", ColumnRole.METADATA)
					.build(Belt.defaultContext());
			final TableMetaData tableMetaData = new TableMetaData(new IOTable(table), false);
			assertSame(MetaDataInfo.YES, tableMetaData.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.YES, tableMetaData.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.NO, tableMetaData.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.YES, tableMetaData.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.YES, tableMetaData.contains("real"));
			assertSame(MetaDataInfo.NO, tableMetaData.contains("integer"));
			assertSame(MetaDataInfo.YES, tableMetaData.containsType(ColumnType.NOMINAL, true));
			assertSame(MetaDataInfo.NO, tableMetaData.containsType(ColumnType.REAL, false));
			assertSame(MetaDataInfo.NO, tableMetaData.containsType(null, true));
			assertEquals(Collections.singleton("nominal"), tableMetaData.selectByColumnMetaData(ColumnRole.LABEL));
			assertEquals(new HashSet<>(Arrays.asList("real", "list")), tableMetaData.selectByColumnMetaData(ColumnRole.METADATA));
			assertEquals(new HashSet<>(Arrays.asList("nominal", "real", "list")), tableMetaData.selectByColumnMetaData(ColumnRole.class));
			assertEquals(new HashSet<>(Arrays.asList("nominal", "boolean")), tableMetaData.selectByType(ColumnType.NOMINAL));
			assertEquals(Collections.singleton("list"), tableMetaData.selectByType(ColumnType.TEXTLIST));
			assertEquals(new HashSet<>(Arrays.asList("nominal", "boolean")), tableMetaData.selectByCategory(Column.Category.CATEGORICAL));
			assertEquals(new HashSet<>(Arrays.asList("int", "real")), tableMetaData.selectByCategory(Column.Category.NUMERIC));
		}

		@Test
		public void testColumnMD() {
			Table table = Builders.newTableBuilder(10)
					.addNominal("nominal", i -> i == 0 ? null : "value")
					.addBoolean("boolean", i -> "value", "value")
					.addInt53Bit("int", i -> 2)
					.addReal("real", i -> i == 2 ? Double.NaN : 2)
					.addDateTime("date_time", i -> Instant.EPOCH)
					.addTime("time", i -> LocalTime.NOON)
					.addTextset("set", i -> new StringSet(Collections.singletonList("")))
					.addTextlist("list", i -> null)
					.addMetaData("nominal", ColumnRole.LABEL)
					.addMetaData("real", ColumnRole.METADATA)
					.addMetaData("list", ColumnRole.METADATA)
					.addMetaData("set", ColumnRole.CLUSTER)
					.addMetaData("time", new ColumnAnnotation("bla"))
					.build(Belt.defaultContext());
			final TableMetaData tableMetaData = new TableMetaData(new IOTable(table), false);

			final List<List<ColumnMetaData>> expectedMD =
					table.labels().stream().map(table::getMetaData).collect(Collectors.toList());
			final List<List<ColumnMetaData>> realMD =
					tableMetaData.labels().stream().map(tableMetaData::getColumnMetaData).collect(Collectors.toList());
			assertEquals(expectedMD, realMD);

			final List<ColumnRole> expectedFMD =
					table.labels().stream().map(l -> table.getFirstMetaData(l, ColumnRole.class)).collect(Collectors.toList());
			final List<ColumnRole> realFMD =
					tableMetaData.labels().stream().map(l -> tableMetaData.getFirstColumnMetaData(l, ColumnRole.class)).collect(Collectors.toList());
			assertEquals(expectedFMD, realFMD);

			final List<List<ColumnRole>> expectedTMD =
					table.labels().stream().map(l -> table.getMetaData(l, ColumnRole.class)).collect(Collectors.toList());
			final List<List<ColumnRole>> realTMD =
					tableMetaData.labels().stream().map(l -> tableMetaData.getColumnMetaData(l, ColumnRole.class)).collect(Collectors.toList());
			assertEquals(expectedTMD, realTMD);
		}

		@Test
		public void testManyColumns() {
			Settings.setSetting(TableMetaData.SETTING_METADATA_COLUMN_LIMIT, "200");
			final TableBuilder tableBuilder = Builders.newTableBuilder(1);
			final int columns = TableMetaData.getMaximumNumberOfMdColumns() + 3;
			for (int i = 0; i < columns; i++) {
				tableBuilder.addReal("col" + i, j -> j);
			}
			final Table table = tableBuilder.build(Belt.defaultContext());
			final TableMetaData tableMetaData = new TableMetaData(new IOTable(table), false);
			assertSame(SetRelation.SUPERSET, tableMetaData.getColumnSetRelation());
			Settings.setSetting(TableMetaData.SETTING_METADATA_COLUMN_LIMIT, null);
		}

		@Test
		public void testAnnotations() {
			final Table table = Builders.newTableBuilder(1).build(Belt.defaultContext());
			final IOTable ioTable = new IOTable(table);
			ioTable.getAnnotations().put("bla", "blup");
			ioTable.getAnnotations().put("bla2", "blup");
			final TableMetaData tableMetaData = new TableMetaData(ioTable, false);
			assertEquals(ioTable.getAnnotations(), tableMetaData.getAnnotations());
		}

		@Test
		public void testColumns() {
			Table table = Builders.newTableBuilder(10)
					.addNominal("nominal", i -> i == 0 ? null : "value")
					.addBoolean("boolean", i -> "value", "value")
					.addInt53Bit("int", i -> 2)
					.addReal("real", i -> i == 2 ? Double.NaN : 2)
					.addDateTime("date_time", i -> Instant.EPOCH)
					.addTime("time", i -> LocalTime.NOON)
					.addTextset("set", i -> new StringSet(Collections.singletonList("")))
					.addTextlist("list", i -> null)
					.addMetaData("nominal", ColumnRole.LABEL)
					.addMetaData("real", ColumnRole.METADATA)
					.addMetaData("list", ColumnRole.METADATA)
					.build(Belt.defaultContext());
			TableMetaData tableMetaData = new TableMetaData(new IOTable(table), false);

			List<String> columnSelection = Arrays.asList("int", "list", "date_time", "nominal");

			final Table subTable = table.columns(columnSelection);
			final TableMetaData controlMD = new TableMetaData(new IOTable(subTable), false);

			tableMetaData = tableMetaData.columns(columnSelection);

			assertEquals(controlMD.getFullColumns(), tableMetaData.getFullColumns());
			assertEquals(controlMD.getMetaDataMap(), tableMetaData.getMetaDataMap());
			assertEquals(controlMD.height().getRelation(), tableMetaData.height().getRelation());
			assertEquals(controlMD.height().getNumber(), tableMetaData.height().getNumber());
			assertEquals(controlMD.getColumnSetRelation(), tableMetaData.getColumnSetRelation());
		}
	}

	public static class SpecialTableTest {

		@Test
		public void testTableSuperset() {
			TableMetaData tableMetaData = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
							ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("text", ColumnType.TEXT, new MDInteger(5))
					.add("unknown", null, null)
					.add("list", ColumnType.TEXTLIST, new MDInteger(10))
					.addColumnMetaData("nominal", ColumnRole.LABEL)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("list", ColumnRole.METADATA)
					.addColumnMetaData("set", ColumnRole.CLUSTER)
					.addColumnMetaData("time", new ColumnAnnotation("bla"))
					.addColumnMetaData("unknown", ColumnRole.PREDICTION)
					.columnsAreSuperset().build();
			assertSame(MetaDataInfo.YES, tableMetaData.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.YES, tableMetaData.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.NO, tableMetaData.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasUniqueColumnMetaData(ColumnRole.ID));
			assertSame(MetaDataInfo.YES, tableMetaData.contains("real"));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.contains("integer"));
			assertSame(MetaDataInfo.YES, tableMetaData.containsType(ColumnType.NOMINAL, true));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.containsType(ColumnType.REAL, false));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.containsType(null, false));
			assertSame(MetaDataInfo.YES, tableMetaData.containsType(null, true));
		}

		@Test
		public void testTableSubset() {
			TableMetaData tableMetaData = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
							ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("text", ColumnType.TEXT, new MDInteger(5))
					.add("unknown", null, null)
					.add("list", ColumnType.TEXTLIST, new MDInteger(10))
					.addColumnMetaData("nominal", ColumnRole.LABEL)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("list", ColumnRole.METADATA)
					.addColumnMetaData("set", ColumnRole.CLUSTER)
					.addColumnMetaData("unknown", ColumnRole.PREDICTION)
					.addColumnMetaData("time", new ColumnAnnotation("bla"))
					.columnsAreSubset().build();
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.NO, tableMetaData.hasColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.NO, tableMetaData.hasUniqueColumnMetaData(ColumnRole.ID));
			assertSame(MetaDataInfo.NO, tableMetaData.hasUniqueColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.contains("real"));
			assertSame(MetaDataInfo.NO, tableMetaData.contains("integer"));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.containsType(ColumnType.NOMINAL, true));
			assertSame(MetaDataInfo.NO, tableMetaData.containsType(ColumnType.REAL, false));
			assertSame(MetaDataInfo.NO, tableMetaData.containsType(null, false));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.containsType(null, true));
		}

		@Test
		public void testTableUnknown() {
			TableMetaData tableMetaData = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
							ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("text", ColumnType.TEXT, new MDInteger(5))
					.add("unknown", null, null)
					.add("list", ColumnType.TEXTLIST, new MDInteger(10))
					.addColumnMetaData("nominal", ColumnRole.LABEL)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("list", ColumnRole.METADATA)
					.addColumnMetaData("set", ColumnRole.CLUSTER)
					.addColumnMetaData("unknown", ColumnRole.PREDICTION)
					.addColumnMetaData("time", new ColumnAnnotation("bla"))
					.columnsAreSubset().mergeColumnSetRelation(SetRelation.SUPERSET).build();
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasUniqueColumnMetaData(ColumnRole.ID));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.hasUniqueColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.contains("real"));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.contains("integer"));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.containsType(ColumnType.NOMINAL, true));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.containsType(ColumnType.REAL, false));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.containsType(null, false));
			assertSame(MetaDataInfo.UNKNOWN, tableMetaData.containsType(null, true));
		}

		@Test
		public void testTableEqual() {
			TableMetaData tableMetaData = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
							ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("text", ColumnType.TEXT, new MDInteger(5))
					.add("unknown", null, null)
					.add("list", ColumnType.TEXTLIST, new MDInteger(10))
					.addColumnMetaData("nominal", ColumnRole.LABEL)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("list", ColumnRole.METADATA)
					.addColumnMetaData("set", ColumnRole.CLUSTER)
					.addColumnMetaData("unknown", ColumnRole.PREDICTION)
					.addColumnMetaData("time", new ColumnAnnotation("bla"))
					.columnsAreKnown().build();
			assertSame(MetaDataInfo.YES, tableMetaData.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.YES, tableMetaData.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.NO, tableMetaData.hasColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.NO, tableMetaData.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(MetaDataInfo.YES, tableMetaData.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(MetaDataInfo.NO, tableMetaData.hasUniqueColumnMetaData(ColumnRole.ID));
			assertSame(MetaDataInfo.NO, tableMetaData.hasUniqueColumnMetaData(ColumnRole.SCORE));
			assertSame(MetaDataInfo.YES, tableMetaData.contains("real"));
			assertSame(MetaDataInfo.NO, tableMetaData.contains("integer"));
			assertSame(MetaDataInfo.YES, tableMetaData.containsType(ColumnType.NOMINAL, true));
			assertSame(MetaDataInfo.NO, tableMetaData.containsType(ColumnType.REAL, false));
			assertSame(MetaDataInfo.NO, tableMetaData.containsType(null, false));
			assertSame(MetaDataInfo.YES, tableMetaData.containsType(null, true));
		}
	}

	public static class More {
		@Test
		public void testClone() {
			TableMetaData tableMetaData = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
							ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("text", ColumnType.TEXT, new MDInteger(5))
					.add("unknown", null, null)
					.add("list", ColumnType.TEXTLIST, new MDInteger(10))
					.addColumnMetaData("nominal", ColumnRole.LABEL)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("list", ColumnRole.METADATA)
					.addColumnMetaData("set", ColumnRole.CLUSTER)
					.addColumnMetaData("unknown", ColumnRole.PREDICTION)
					.addColumnMetaData("time", new ColumnAnnotation("bla"))
					.build();
			tableMetaData.getAnnotations().put("ann", "test");
			final TableMetaData clone = tableMetaData.clone();
			assertEquals(tableMetaData.getAnnotations(), clone.getAnnotations());
			assertEquals(tableMetaData.getFullColumns(), clone.getFullColumns());
			assertEquals(tableMetaData.getMetaDataMap(), clone.getMetaDataMap());
			assertEquals(tableMetaData.height().getNumber(), clone.height().getNumber());
			assertEquals(tableMetaData.height().getRelation(), clone.height().getRelation());
			assertEquals(tableMetaData.getColumnSetRelation(), clone.getColumnSetRelation());
		}

		@Test
		public void testShortDescription() {
			Table table = Builders.newTableBuilder(10).addReal("real", i -> Math.PI *
					i).addInt53Bit("id", i -> i).addMetaData("id", ColumnRole.ID).addDateTime("dt",
					i -> Instant.EPOCH).build(Belt.defaultContext());
			final IOTable ioTable = new IOTable(table);
			final ExampleSet exampleSet = BeltConverter.convert(ioTable, new SequentialConcurrencyContext());
			final TableMetaData tableMetaData = new TableMetaData(ioTable, false);
			final ExampleSetMetaData exampleSetMetaData = new ExampleSetMetaData(exampleSet, false);
			assertEquals(exampleSetMetaData.getShortDescription().replace("ExampleSet", "IOTable"),
					tableMetaData.getShortDescription());
		}

		@Test
		public void testShortDescriptionShortened() {
			Table table = Builders.newTableBuilder(120).addReal("real", i -> Math.PI *
					i).addInt53Bit("id", i -> i).addMetaData("id", ColumnRole.ID).addDateTime("dt",
					i -> Instant.EPOCH).addNominal("nominal", i -> "val" + i).build(Belt.defaultContext());
			final IOTable ioTable = new IOTable(table);
			final ExampleSet exampleSet = BeltConverter.convert(ioTable, new SequentialConcurrencyContext());
			final TableMetaData tableMetaData = new TableMetaData(ioTable, false);
			final ExampleSetMetaData exampleSetMetaData = new ExampleSetMetaData(exampleSet, false);
			assertEquals(exampleSetMetaData.getShortDescription().replace("ExampleSet", "IOTable"),
					tableMetaData.getShortDescription());
		}

		@Test
		public void testRangeString() {
			Table table = Builders.newTableBuilder(12).addReal("real", i -> Math.PI *
					i).addInt53Bit("id", i -> i).addMetaData("id", ColumnRole.ID).addDateTime("dt",
					i -> Instant.EPOCH).addNominal("nominal", i -> "val" + i).build(Belt.defaultContext());
			final IOTable ioTable = new IOTable(table);
			final ExampleSet exampleSet = BeltConverter.convert(ioTable, new SequentialConcurrencyContext());
			final TableMetaData tableMetaData = new TableMetaData(ioTable, false);
			final ExampleSetMetaData exampleSetMetaData = new ExampleSetMetaData(exampleSet, false);
			for (String label : tableMetaData.labels()) {
				assertEquals(exampleSetMetaData.getAttributeByName(label).getRangeString(),
						TableMDDisplayUtils.getLegacyRangeString(tableMetaData, label));
			}

		}

		@Test
		public void testValueTypeName() {
			Table table = Builders.newTableBuilder(12).addReal("real", i -> Math.PI *
					i).addInt53Bit("id", i -> i).addMetaData("id", ColumnRole.ID).addDateTime("dt",
					i -> Instant.EPOCH).addBoolean("boolean", i -> "val" + (i%2),"val1").build(Belt.defaultContext());
			final IOTable ioTable = new IOTable(table);
			final ExampleSet exampleSet = BeltConverter.convert(ioTable, new SequentialConcurrencyContext());
			final TableMetaData tableMetaData = new TableMetaData(ioTable, false);
			final ExampleSetMetaData exampleSetMetaData = new ExampleSetMetaData(exampleSet, false);
			for (String label : tableMetaData.labels()) {
				assertEquals(exampleSetMetaData.getAttributeByName(label).getValueTypeName(),
						TableMDDisplayUtils.getLegacyValueTypeName(tableMetaData.column(label)));
			}

		}

		@Test
		public void testRoleString() {
			Table table = Builders.newTableBuilder(12).addReal("real", i -> Math.PI *
					i).addInt53Bit("id", i -> i).addMetaData("id", ColumnRole.ID).addDateTime("dt",
					i -> Instant.EPOCH).addNominal("nominal", i -> "val" + i).addMetaData("nominal", ColumnRole.LABEL).build(Belt.defaultContext());
			final IOTable ioTable = new IOTable(table);
			final ExampleSet exampleSet = BeltConverter.convert(ioTable, new SequentialConcurrencyContext());
			final TableMetaData tableMetaData = new TableMetaData(ioTable, false);
			final ExampleSetMetaData exampleSetMetaData = new ExampleSetMetaData(exampleSet, false);
			for (String label : tableMetaData.labels()) {
				assertEquals(exampleSetMetaData.getAttributeByName(label).getRole(),
						TableMDDisplayUtils.getLegacyRoleString(tableMetaData, label));
			}

		}


	}

}
