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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnAnnotation;
import com.rapidminer.belt.util.ColumnMetaData;
import com.rapidminer.belt.util.ColumnReference;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDNumber;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.settings.Settings;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;

/**
 * Tests the {@link TableMetaDataBuilder}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
@RunWith(Enclosed.class)
public class TableMetaDataBuilderTest {

	public static class TableComparison {

		@Test
		public void testData() {
			TableMetaDataBuilder tableMetaDataBuilder = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, null)
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON, ColumnType.TIME.comparator()),
							SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("list", ColumnType.TEXTLIST, new MDInteger(10));
			TableMetaData controlMD = tableMetaDataBuilder.build();

			assertEquals(controlMD.labels(), tableMetaDataBuilder.labels());
			assertEquals(new ArrayList<>(controlMD.getColumns()), new ArrayList<>(tableMetaDataBuilder.getColumns()));
			assertEquals(controlMD.height().getRelation(),
					tableMetaDataBuilder.height().getRelation());
			assertEquals(controlMD.height().getNumber(), tableMetaDataBuilder.height().getNumber());
			assertEquals(controlMD.getColumnSetRelation(), tableMetaDataBuilder.getColumnSetRelation());
			assertEquals(controlMD.labels().stream().map(controlMD::column).collect(Collectors.toList()),
					tableMetaDataBuilder.labels().stream().map(tableMetaDataBuilder::column).collect(Collectors.toList()));

			tableMetaDataBuilder = new TableMetaDataBuilder(controlMD);
			assertEquals(controlMD.labels(), tableMetaDataBuilder.labels());
			assertEquals(controlMD.height().getRelation(),
					tableMetaDataBuilder.height().getRelation());
			assertEquals(controlMD.height().getNumber(), tableMetaDataBuilder.height().getNumber());
			assertEquals(controlMD.getColumnSetRelation(), tableMetaDataBuilder.getColumnSetRelation());
			assertEquals(controlMD.labels().stream().map(controlMD::column).collect(Collectors.toList()),
					tableMetaDataBuilder.labels().stream().map(tableMetaDataBuilder::column).collect(Collectors.toList()));
		}

		@Test
		public void testMethods() {
			TableMetaDataBuilder tableMetaDataBuilder = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, -2), SetRelation.EQUAL, null)
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON, ColumnType.TIME.comparator()),
							SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("list", ColumnType.TEXTLIST, new MDInteger(10))
					.addColumnMetaData("nominal", ColumnRole.LABEL)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("list", ColumnRole.METADATA)
					.addColumnMetaData("set", ColumnRole.CLUSTER)
					.addColumnMetaData("time", new ColumnAnnotation("bla"));
			TableMetaData controlMD = tableMetaDataBuilder.build();

			assertSame(controlMD.hasColumnMetaData(ColumnRole.LABEL),
					tableMetaDataBuilder.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(controlMD.hasColumnMetaData(ColumnRole.METADATA),
					tableMetaDataBuilder.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(controlMD.hasUniqueColumnMetaData(ColumnRole.METADATA),
					tableMetaDataBuilder.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(controlMD.hasUniqueColumnMetaData(ColumnRole.LABEL),
					tableMetaDataBuilder.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(controlMD.contains("real"), tableMetaDataBuilder.contains("real"));
			assertSame(controlMD.contains("integer"), tableMetaDataBuilder.contains("integer"));
			assertSame(controlMD.containsType(ColumnType.NOMINAL, true),
					tableMetaDataBuilder.containsType(ColumnType.NOMINAL, true));
			assertSame(controlMD.containsType(ColumnType.REAL, false),
					tableMetaDataBuilder.containsType(ColumnType.REAL, false));
			assertSame(controlMD.containsType(null, true), tableMetaDataBuilder.containsType(null, true));
			assertEquals(controlMD.selectByColumnMetaData(ColumnRole.LABEL),
					tableMetaDataBuilder.selectByColumnMetaData(ColumnRole.LABEL));
			assertEquals(controlMD.selectByColumnMetaData(ColumnRole.METADATA),
					tableMetaDataBuilder.selectByColumnMetaData(ColumnRole.METADATA));
			assertEquals(controlMD.selectByColumnMetaData(ColumnRole.class),
					tableMetaDataBuilder.selectByColumnMetaData(ColumnRole.class));
			assertEquals(controlMD.selectByType(ColumnType.NOMINAL),
					tableMetaDataBuilder.selectByType(ColumnType.NOMINAL));
			assertEquals(controlMD.selectByType(ColumnType.TEXTLIST),
					tableMetaDataBuilder.selectByType(ColumnType.TEXTLIST));
			assertEquals(controlMD.selectByCategory(Column.Category.NUMERIC),
					tableMetaDataBuilder.selectByCategory(Column.Category.NUMERIC));
			assertEquals(controlMD.selectByCategory(Column.Category.CATEGORICAL),
					tableMetaDataBuilder.selectByCategory(Column.Category.CATEGORICAL));

			tableMetaDataBuilder = new TableMetaDataBuilder(controlMD);
			assertSame(controlMD.hasColumnMetaData(ColumnRole.LABEL),
					tableMetaDataBuilder.hasColumnMetaData(ColumnRole.LABEL));
			assertSame(controlMD.hasColumnMetaData(ColumnRole.METADATA),
					tableMetaDataBuilder.hasColumnMetaData(ColumnRole.METADATA));
			assertSame(controlMD.hasUniqueColumnMetaData(ColumnRole.METADATA),
					tableMetaDataBuilder.hasUniqueColumnMetaData(ColumnRole.METADATA));
			assertSame(controlMD.hasUniqueColumnMetaData(ColumnRole.LABEL),
					tableMetaDataBuilder.hasUniqueColumnMetaData(ColumnRole.LABEL));
			assertSame(controlMD.contains("real"), tableMetaDataBuilder.contains("real"));
			assertSame(controlMD.contains("integer"), tableMetaDataBuilder.contains("integer"));
			assertSame(controlMD.containsType(ColumnType.NOMINAL, true),
					tableMetaDataBuilder.containsType(ColumnType.NOMINAL, true));
			assertSame(controlMD.containsType(ColumnType.REAL, false),
					tableMetaDataBuilder.containsType(ColumnType.REAL, false));
			assertSame(controlMD.containsType(null, true), tableMetaDataBuilder.containsType(null, true));
			assertEquals(controlMD.selectByColumnMetaData(ColumnRole.LABEL),
					tableMetaDataBuilder.selectByColumnMetaData(ColumnRole.LABEL));
			assertEquals(controlMD.selectByColumnMetaData(ColumnRole.METADATA),
					tableMetaDataBuilder.selectByColumnMetaData(ColumnRole.METADATA));
			assertEquals(controlMD.selectByColumnMetaData(ColumnRole.class),
					tableMetaDataBuilder.selectByColumnMetaData(ColumnRole.class));
			assertEquals(controlMD.selectByType(ColumnType.NOMINAL),
					tableMetaDataBuilder.selectByType(ColumnType.NOMINAL));
			assertEquals(controlMD.selectByType(ColumnType.TEXTLIST),
					tableMetaDataBuilder.selectByType(ColumnType.TEXTLIST));
			assertEquals(controlMD.selectByCategory(Column.Category.NUMERIC),
					tableMetaDataBuilder.selectByCategory(Column.Category.NUMERIC));
			assertEquals(controlMD.selectByCategory(Column.Category.CATEGORICAL),
					tableMetaDataBuilder.selectByCategory(Column.Category.CATEGORICAL));
		}

		@Test
		public void testColumnMD() {
			final TableMetaDataBuilder tableMetaDataBuilder = new TableMetaDataBuilder(10)
					.addNominal("nominal", Collections.singleton("value"), SetRelation.EQUAL, new MDInteger(1))
					.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON, ColumnType.TIME.comparator()),
							SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("list", ColumnType.TEXTLIST, new MDInteger(10))
					.addColumnMetaData("nominal", ColumnRole.LABEL)
					.addColumnMetaData("real", ColumnRole.METADATA)
					.addColumnMetaData("list", ColumnRole.METADATA)
					.addColumnMetaData("set", ColumnRole.CLUSTER)
					.addColumnMetaData("time", new ColumnAnnotation("bla"));
			TableMetaData controlMD = tableMetaDataBuilder.build();

			final List<List<ColumnMetaData>> expectedMD =
					controlMD.labels().stream().map(controlMD::getColumnMetaData).collect(Collectors.toList());
			final List<List<ColumnMetaData>> realMD =
					tableMetaDataBuilder.labels().stream().map(tableMetaDataBuilder::getColumnMetaData).collect(Collectors.toList());
			assertEquals(expectedMD, realMD);

			final List<ColumnRole> expectedFMD =
					controlMD.labels().stream().map(l -> controlMD.getFirstColumnMetaData(l, ColumnRole.class)).collect(Collectors.toList());
			final List<ColumnRole> realFMD =
					tableMetaDataBuilder.labels().stream().map(l -> tableMetaDataBuilder.getFirstColumnMetaData(l,
							ColumnRole.class)).collect(Collectors.toList());
			assertEquals(expectedFMD, realFMD);

			final List<List<ColumnRole>> expectedTMD =
					controlMD.labels().stream().map(l -> controlMD.getColumnMetaData(l, ColumnRole.class)).collect(Collectors.toList());
			final List<List<ColumnRole>> realTMD =
					tableMetaDataBuilder.labels().stream().map(l -> tableMetaDataBuilder.getColumnMetaData(l,
							ColumnRole.class)).collect(Collectors.toList());
			assertEquals(expectedTMD, realTMD);
		}

		@Test
		public void testAnnotations() {
			TableMetaData tableMetaData = new TableMetaData();
			tableMetaData.getAnnotations().put("bla", "blup");
			tableMetaData.getAnnotations().put("bla2", "blup");
			final TableMetaDataBuilder builder = new TableMetaDataBuilder(tableMetaData);
			assertEquals(tableMetaData.getAnnotations(), builder.build().getAnnotations());
		}

		private static class NonUnique implements ColumnMetaData {

			private String data;

			private NonUnique(String data) {
				this.data = data;
			}

			@Override
			public String type() {
				return "new.type";
			}

			@Override
			public Uniqueness uniqueness() {
				return Uniqueness.NONE;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}
				NonUnique nonUnique = (NonUnique) o;
				return Objects.equals(data, nonUnique.data);
			}

			@Override
			public int hashCode() {
				return Objects.hash(data);
			}
		}

		@Test
		public void testRemoveReplace() {
			final TableMetaDataBuilder builder =
					new TableMetaDataBuilder(10).addNominal("nominal", Collections.singleton("value"),
							SetRelation.EQUAL, new MDInteger(1))
							.addBoolean("boolean", "value", null, new MDInteger(0))
							.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
							.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
							.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
									ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
							.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
											ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
							.add("set", ColumnType.TEXTSET, new MDInteger(0))
							.add("list", ColumnType.TEXTLIST, new MDInteger(10))
							.addColumnMetaData("boolean", new NonUnique("bah"))
							.addColumnMetaData("boolean", new NonUnique("bah"))
							.addColumnMetaData("boolean", new NonUnique("buh"))
							.addColumnMetaData("nominal", ColumnRole.LABEL)
							.addColumnMetaData("real", ColumnRole.METADATA)
							.addColumnMetaData("list", ColumnRole.METADATA)
							.addColumnMetaData("set", ColumnRole.CLUSTER)
							.addColumnMetaData("int", ColumnRole.ID)
							.addColumnMetaData("time", new ColumnAnnotation("bla"))
							.addColumnMetaData("date_time", new ColumnAnnotation("blaah"));

			TableMetaData tableMetaData = builder.build();

			builder.removeColumnMetaData("int", ColumnRole.ID)
					.remove("int")
					.rename("date_time", "date-time")
					.replace("boolean", new ColumnInfo(ColumnType.NOMINAL, null, "value", new MDInteger(11)))
					.add("bla", null, null)
					.removeColumnMetaData("list", ColumnRole.class)
					.removeColumnMetaData("time", ColumnRole.METADATA)
					.removeColumnMetaData("nominal", ColumnAnnotation.class)
					.clearColumnMetadata("real")
					.addColumnMetaData("boolean", Arrays.asList(ColumnRole.PREDICTION, new ColumnAnnotation("blup")));

			assertEquals(new HashSet<>(Arrays.asList("nominal", "boolean", "real", "time", "set", "list", "date-time",
					"bla")), builder.labels());
			final List<List<ColumnMetaData>> metaData =
					builder.labels().stream().map(builder::getColumnMetaData).collect(Collectors.toList());
			assertEquals(Arrays.asList(Collections.singletonList(ColumnRole.LABEL),
					Arrays.asList(new NonUnique("bah"), new NonUnique("buh"), ColumnRole.PREDICTION, new ColumnAnnotation("blup")), Collections.emptyList(),
					Collections.singletonList(new ColumnAnnotation("bla")),
					Collections.singletonList(ColumnRole.CLUSTER), Collections.emptyList(), Collections.singletonList(new ColumnAnnotation("blaah")),
					Collections.emptyList()), metaData);

			TableMetaDataBuilder builder2 = new TableMetaDataBuilder(tableMetaData);

			builder2.removeColumnMetaData("int", ColumnRole.ID)
					.remove("int")
					.rename("date_time", "date-time")
					.replace("boolean", new ColumnInfo(ColumnType.NOMINAL, null, "value", new MDInteger(11)))
					.add("bla", null, null)
					.removeColumnMetaData("list", ColumnRole.class)
					.removeColumnMetaData("time", ColumnRole.METADATA)
					.removeColumnMetaData("nominal", ColumnAnnotation.class)
					.clearColumnMetadata("real")
					.addColumnMetaData("boolean", Arrays.asList(ColumnRole.PREDICTION, new ColumnAnnotation("blup")));

			assertEquals(builder.build().getFullColumns(), builder2.build().getFullColumns());
			assertEquals(builder.build().getMetaDataMap(), builder2.build().getMetaDataMap());

		}

		@Test
		public void testReplace() {
			final TableMetaDataBuilder builder =
					new TableMetaDataBuilder(10).addNominal("nominal", Collections.singleton("value"),
							SetRelation.EQUAL, new MDInteger(1))
							.addBoolean("boolean", "value", null, new MDInteger(0))
							.addBoolean("boolean2", "value", null, new MDInteger(0))
							.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
							.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
							.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
									ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
							.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
									ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
							.add("set", ColumnType.TEXTSET, new MDInteger(0))
							.add("list", ColumnType.TEXTLIST, new MDInteger(10))
							.add("null", null, new MDInteger(1))
							.addColumnMetaData("null", new ColumnReference("boolean", "more info"))
							.addColumnMetaData("boolean", new NonUnique("bah"))
							.addColumnMetaData("boolean", new NonUnique("bah"))
							.addColumnMetaData("boolean", new NonUnique("buh"))
							.addColumnMetaData("boolean2", new NonUnique("bah"))
							.addColumnMetaData("boolean2", new NonUnique("bah"))
							.addColumnMetaData("boolean2", new NonUnique("buh"))
							.addColumnMetaData("nominal", ColumnRole.LABEL)
							.addColumnMetaData("real", ColumnRole.METADATA)
							.addColumnMetaData("list", ColumnRole.METADATA)
							.addColumnMetaData("set", ColumnRole.CLUSTER)
							.addColumnMetaData("int", ColumnRole.ID)
							.addColumnMetaData("time", new ColumnAnnotation("bla"))
							.addColumnMetaData("date_time", new ColumnAnnotation("blaah"));

			TableMetaData tableMetaData = builder.build();

			TableMetaDataBuilder builder2 = new TableMetaDataBuilder(tableMetaData);
			builder2.replaceBoolean("boolean", "positive value", "negative value",
					new MDInteger(1))
					.replaceBoolean("boolean2", new MDInteger(1))
					.replaceNominal("nominal", Collections.emptyList(), SetRelation.UNKNOWN, new MDInteger(1))
					.replaceInteger("int", new Range(0, 1), SetRelation.SUPERSET, new MDInteger(0))
					.replaceReal("real", new Range(-2, 2), SetRelation.EQUAL, new MDInteger(100))
					.replaceDateTime("date_time", null, SetRelation.SUPERSET, new MDInteger(0))
					.replaceTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
							ColumnType.TIME.comparator()), SetRelation.SUBSET, new MDInteger(0))
					.replace("set", ColumnType.TEXTLIST, new MDInteger(0))
					.replace("list", ColumnType.TEXTSET, new MDInteger(10))
					.replace("null", null, new MDInteger(0));

			TableMetaData tableMetaData2 = builder2.build();

			// check that the column infos have been replaced
			assertEquals("value", tableMetaData.column("boolean")
					.getDictionary().getPositiveValue().get());
			assertEquals(MetaDataInfo.YES, tableMetaData.column("boolean2").getDictionary().hasPositive());
			assertEquals(new Range(2, 2), tableMetaData.column("int").getNumericRange().get());
			assertEquals(1, (int) tableMetaData.column("real").getMissingValues().getNumber());
			assertTrue(tableMetaData.column("date_time").getObjectRange(Instant.class).isPresent());
			assertEquals(SetRelation.EQUAL, tableMetaData.column("time").getValueSetRelation());
			assertEquals(ColumnType.TEXTLIST, tableMetaData.column("list").getType().get());
			assertEquals(ColumnType.TEXTSET, tableMetaData.column("set").getType().get());
			assertEquals(1, (int) tableMetaData.column("null").getMissingValues().getNumber());

			assertEquals("positive value", tableMetaData2.column("boolean")
					.getDictionary().getPositiveValue().get());
			assertEquals(MetaDataInfo.UNKNOWN, tableMetaData2.column("boolean2").getDictionary().hasPositive());
			assertEquals(new Range(0, 1), tableMetaData2.column("int").getNumericRange().get());
			assertEquals(10, (int) tableMetaData2.column("real").getMissingValues().getNumber());
			assertFalse(tableMetaData2.column("date_time").getObjectRange(Instant.class).isPresent());
			assertEquals(SetRelation.SUBSET, tableMetaData2.column("time").getValueSetRelation());
			assertEquals(ColumnType.TEXTLIST, tableMetaData2.column("set").getType().get());
			assertEquals(ColumnType.TEXTSET, tableMetaData2.column("list").getType().get());
			assertEquals(0, (int) tableMetaData2.column("null").getMissingValues().getNumber());

			// check that the column meta data remains the same
			assertEquals(tableMetaData.getMetaDataMap(), tableMetaData2.getMetaDataMap());

		}

	}

	public static class SanityChecks {

		@Test
		public void testMissings() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			builder.addNominal("nominal", null, SetRelation.EQUAL, new MDInteger(100));
			final MDInteger missings = new MDInteger(20);
			missings.reduceByUnknownAmount();
			builder.addBoolean("bool1", "true", "false", missings);
			final MDInteger missings2 = new MDInteger(20);
			missings2.increaseByUnknownAmount();
			builder.addBoolean("bool2", missings2);
			builder.addReal("real", null, SetRelation.EQUAL, MDInteger.newUnknown());
			builder.addInteger("int", null, SetRelation.EQUAL, MDInteger.newPossible());
			builder.addDateTime("date-time", null, SetRelation.EQUAL, new MDInteger(6));
			final MDInteger mdInteger = new MDInteger(6);
			mdInteger.reduceByUnknownAmount();
			builder.addTime("time", null, SetRelation.EQUAL, mdInteger);
			final MDInteger mdInteger2 = new MDInteger(6);
			mdInteger2.increaseByUnknownAmount();
			builder.add("further", null, mdInteger2);
			final MDInteger mdInteger3 = new MDInteger(60);
			mdInteger3.reduceByUnknownAmount();
			mdInteger3.increaseByUnknownAmount();
			builder.add("text", ColumnType.TEXT, mdInteger3);
			builder.add("list", ColumnType.TEXTLIST, new MDInteger(-1));
			builder.add("info", new ColumnInfo(ColumnType.TEXT, new MDInteger(100)));
			final MDInteger mdInteger1 = new MDInteger(12);
			mdInteger.increaseByUnknownAmount();
			builder.replace("real", ColumnType.INTEGER_53_BIT, mdInteger1);
			final TableMetaData tableMetaData = builder.build();

			final Object[] missingsAtMost =
					tableMetaData.labels().stream().map(l ->
							tableMetaData.column(l).getMissingValues().getNumber() <= 10 ||
									tableMetaData.column(l).getMissingValues().getRelation() ==
											MDNumber.Relation.AT_MOST ||
									tableMetaData.column(l).getMissingValues().getRelation() ==
											MDNumber.Relation.UNKNOWN).toArray();
			Object[] control = new Object[missingsAtMost.length];
			Arrays.fill(control, true);
			assertArrayEquals(control, missingsAtMost);
		}

		@Test
		public void testMissingsAtMost() {
			MDInteger rows = new MDInteger(10);
			rows.reduceByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(rows);
			builder.addNominal("nominal", null, SetRelation.EQUAL, new MDInteger(100));
			final MDInteger missings = new MDInteger(20);
			missings.reduceByUnknownAmount();
			builder.addBoolean("bool1", "true", "false", missings);
			final MDInteger missings2 = new MDInteger(20);
			missings2.increaseByUnknownAmount();
			builder.addBoolean("bool2", missings2);
			builder.addReal("real", null, SetRelation.EQUAL, MDInteger.newUnknown());
			builder.addInteger("int", null, SetRelation.EQUAL, MDInteger.newPossible());
			builder.addDateTime("date-time", null, SetRelation.EQUAL, new MDInteger(6));
			final MDInteger mdInteger = new MDInteger(6);
			mdInteger.reduceByUnknownAmount();
			builder.addTime("time", null, SetRelation.EQUAL, mdInteger);
			final MDInteger mdInteger2 = new MDInteger(6);
			mdInteger2.increaseByUnknownAmount();
			builder.add("further", null, mdInteger2);
			final MDInteger mdInteger3 = new MDInteger(60);
			mdInteger3.reduceByUnknownAmount();
			mdInteger3.increaseByUnknownAmount();
			builder.add("text", ColumnType.TEXT, mdInteger3);
			builder.add("list", ColumnType.TEXTLIST, new MDInteger(-1));
			builder.add("info", new ColumnInfo(ColumnType.TEXT, new MDInteger(100)));
			final MDInteger mdInteger1 = new MDInteger(12);
			mdInteger1.increaseByUnknownAmount();
			builder.replace("real", ColumnType.INTEGER_53_BIT, mdInteger1);
			builder.addReal("real2", null, SetRelation.EQUAL, null);
			final TableMetaData tableMetaData = builder.build();

			final Object[] missingsAtMost =
					tableMetaData.labels().stream().map(l ->
							tableMetaData.column(l).getMissingValues().getNumber() <= 10 ||
									tableMetaData.column(l).getMissingValues().getRelation() ==
											MDNumber.Relation.AT_MOST ||
									tableMetaData.column(l).getMissingValues().getRelation() ==
											MDNumber.Relation.UNKNOWN).toArray();
			Object[] control = new Object[missingsAtMost.length];
			Arrays.fill(control, true);
			assertArrayEquals(control, missingsAtMost);
		}

		@Test
		public void testNegativeRows() {
			MDInteger rows = new MDInteger(-5);
			rows.increaseByUnknownAmount();
			final TableMetaDataBuilder builder = new TableMetaDataBuilder(rows);
			assertTrue(builder.height().getNumber() >= 0);
		}

		@Test
		public void testDictionarySize() {
			MDInteger rows = new MDInteger(5);
			rows.reduceByUnknownAmount();
			final TableMetaDataBuilder builder = new TableMetaDataBuilder(rows);
			builder.addNominal("4", Arrays.asList("1", "2", "3", "4"), SetRelation.EQUAL, null);
			builder.add("5", new ColumnInfo(ColumnType.NOMINAL, Arrays.asList("1", "2", "3", "4", "5"),
					SetRelation.EQUAL, MDInteger.newPossible()));
			builder.addNominal("6", Arrays.asList("1", "2", "3", "4", "5", "6"), SetRelation.EQUAL, null);
			builder.add("7", new ColumnInfo(ColumnType.NOMINAL, Arrays.asList("1", "2", "3", "4", "5", "6", "7"),
					SetRelation.EQUAL, MDInteger.newPossible()));
			assertSame(SetRelation.EQUAL, builder.column("4").getValueSetRelation());
			assertSame(SetRelation.EQUAL, builder.column("5").getValueSetRelation());
			assertSame(SetRelation.SUBSET, builder.column("6").getValueSetRelation());
			assertSame(SetRelation.SUBSET, builder.column("7").getValueSetRelation());
		}

		@Test
		public void testLimitAdding() {
			Settings.setSetting(TableMetaData.SETTING_METADATA_COLUMN_LIMIT, "5");
			final TableMetaDataBuilder builder = new TableMetaDataBuilder(57);
			builder.addBoolean("boolean", "value", null, new MDInteger(0))
					.addInteger("int", new Range(2, 2), SetRelation.EQUAL, new MDInteger(0))
					.addReal("real", new Range(2, 2), SetRelation.EQUAL, new MDInteger(1))
					.addDateTime("date_time", new ObjectRange<>(Instant.EPOCH, Instant.EPOCH,
							ColumnType.DATETIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.addTime("time", new ObjectRange<>(LocalTime.NOON, LocalTime.NOON,
									ColumnType.TIME.comparator()), SetRelation.EQUAL, new MDInteger(0))
					.add("set", ColumnType.TEXTSET, new MDInteger(0))
					.add("list", ColumnType.TEXTLIST, new MDInteger(10));
			assertEquals(SetRelation.SUPERSET, builder.getColumnSetRelation());
			assertEquals(5, builder.labels().size());
			Settings.setSetting(TableMetaData.SETTING_METADATA_COLUMN_LIMIT, null);
		}

	}

	public static class RowUpdate {

		@Test
		public void reduceRowsEqualsFromEquals() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(6);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≤ 10, ≤ 3, ≤ 5, ?, ≤ 7, ?, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsEqualsFromAtMost() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.reduceByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(6);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≤ 6, = 3, = 5, ≤ 6, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsEqualsFromAtLeast() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.increaseByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(6);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≤ 10, ≤ 3, ≤ 5, ?, ≤ 7, ?, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsEqualsFromUnknown() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(MDInteger.newUnknown());
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(6);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≤ 6, = 3, = 5, ≤ 6, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsAtMostFromEquals() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(6);
			newRows.reduceByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≤ 10, ≤ 3, ≤ 5, ?, ≤ 7, ?, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsAtMostFromAtMost() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.reduceByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(6);
			newRows.reduceByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≤ 6, = 3, = 5, ≤ 6, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsAtMostFromAtLeast() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.increaseByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(6);
			newRows.reduceByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≤ 10, ≤ 3, ≤ 5, ?, ≤ 7, ?, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsAtMostFromUnknown() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(MDInteger.newUnknown());
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(6);
			newRows.reduceByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≤ 6, = 3, = 5, ≤ 6, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsAtLeastFromEquals() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(6);
			newRows.increaseByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsAtLeastFromAtMost() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.reduceByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(6);
			newRows.increaseByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsAtLeastFromAtLeast() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.increaseByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(6);
			newRows.increaseByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsAtLeastFromUnknown() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(MDInteger.newUnknown());
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(6);
			newRows.increaseByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		private void addMissings(TableMetaDataBuilder builder) {
			final MDInteger mdInteger = new MDInteger(7);
			mdInteger.increaseByUnknownAmount();
			final MDInteger mdInteger2 = new MDInteger(7);
			mdInteger2.reduceByUnknownAmount();
			final MDInteger mdInteger3 = new MDInteger(5);
			mdInteger3.reduceByUnknownAmount();
			final MDInteger mdInteger4 = new MDInteger(4);
			mdInteger4.increaseByUnknownAmount();
			builder.addReal("real", null, SetRelation.EQUAL, new MDInteger(10))
					.addReal("real2", null, SetRelation.EQUAL, new MDInteger(3))
					.addInteger("int", null, SetRelation.EQUAL, new MDInteger(5))
					.addTime("time", null, SetRelation.EQUAL, mdInteger)
					.addBoolean("boolean", mdInteger2)
					.addBoolean("boolean2", mdInteger4)
					.addDateTime("d-t", null, SetRelation.EQUAL, mdInteger3);
		}

		@Test
		public void increaseRowsEqualsFromEquals() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(13);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≥ 10, ≥ 3, ≥ 5, ≥ 7, ?, ≥ 4, ?]", missings.toString());
		}

		@Test
		public void increaseRowsEqualsFromAtMost() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.reduceByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(13);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≥ 10, ≥ 3, ≥ 5, ≥ 7, ?, ≥ 4, ?]", missings.toString());
		}

		@Test
		public void increaseRowsEqualsFromAtLeast() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.increaseByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(13);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void increaseRowsEqualsFromUnknown() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(MDInteger.newUnknown());
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(13);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void increaseRowsAtMostFromEquals() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(13);
			newRows.reduceByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void increaseRowsAtMostFromAtMost() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.reduceByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(13);
			newRows.reduceByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void increaseRowsAtMostFromAtLeast() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.increaseByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(13);
			newRows.reduceByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void increaseRowsAtMostFromUnknown() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(MDInteger.newUnknown());
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(13);
			newRows.reduceByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void increaseRowsAtLeastFromEquals() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(13);
			newRows.increaseByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≥ 10, ≥ 3, ≥ 5, ≥ 7, ?, ≥ 4, ?]", missings.toString());
		}

		@Test
		public void increaseRowsAtLeastFromAtMost() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.reduceByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(13);
			newRows.increaseByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[≥ 10, ≥ 3, ≥ 5, ≥ 7, ?, ≥ 4, ?]", missings.toString());
		}

		@Test
		public void increaseRowsAtLeastFromAtLeast() {
			final MDInteger numberOfRows = new MDInteger(10);
			numberOfRows.increaseByUnknownAmount();
			TableMetaDataBuilder builder = new TableMetaDataBuilder(numberOfRows);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(13);
			newRows.increaseByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void increaseRowsAtLeastFromUnknown() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(MDInteger.newUnknown());
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			final MDInteger newRows = new MDInteger(13);
			newRows.increaseByUnknownAmount();
			builder.updateHeight(newRows);

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void reduceRowsUnknownFromEquals() {
			TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			addMissings(builder);
			final List<MDInteger> missingsBefore =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missingsBefore.toString());

			builder.updateHeight(MDInteger.newUnknown());

			final List<MDInteger> missings =
					builder.labels().stream().map(l -> builder.column(l).getMissingValues()).collect(Collectors.toList());
			assertEquals("[= 10, = 3, = 5, ≥ 7, ≤ 7, ≥ 4, ≤ 5]", missings.toString());
		}

		@Test
		public void dictChangeFromUnknown() {
			final TableMetaDataBuilder builder = new TableMetaDataBuilder(MDInteger.newUnknown());
			builder.addNominal("bla", Arrays.asList("1", "2", "3", "4", "5", "6"), SetRelation.EQUAL, MDInteger.newPossible());

			assertSame(SetRelation.EQUAL, builder.column("bla").getValueSetRelation());

			builder.updateHeight(4);
			assertSame(SetRelation.SUBSET, builder.column("bla").getValueSetRelation());
		}

		@Test
		public void dictReduce() {
			final TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			builder.addNominal("bla", Arrays.asList("1", "2", "3", "4", "5", "6"), SetRelation.EQUAL, MDInteger.newPossible());

			assertSame(SetRelation.EQUAL, builder.column("bla").getValueSetRelation());

			builder.reduceHeightByUnknownAmount();
			assertSame(SetRelation.SUBSET, builder.column("bla").getValueSetRelation());
		}

		@Test
		public void dictIncrease() {
			final TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
			builder.addNominal("bla", Arrays.asList("1", "2", "3", "4", "5", "6"), SetRelation.EQUAL, MDInteger.newPossible());

			assertSame(SetRelation.EQUAL, builder.column("bla").getValueSetRelation());

			builder.increaseHeightByUnknownAmount();
			assertSame(SetRelation.EQUAL, builder.column("bla").getValueSetRelation());
		}
	}
}
