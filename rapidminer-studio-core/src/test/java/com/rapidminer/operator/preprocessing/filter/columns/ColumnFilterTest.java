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
package com.rapidminer.operator.preprocessing.filter.columns;

import static com.rapidminer.operator.preprocessing.filter.columns.TableSubsetSelectorFilter.SpecialFilterStrategy.FILTER;
import static com.rapidminer.operator.preprocessing.filter.columns.TableSubsetSelectorFilter.SpecialFilterStrategy.KEEP;
import static com.rapidminer.operator.preprocessing.filter.columns.TableSubsetSelectorFilter.SpecialFilterStrategy.REMOVE;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.Belt;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.operator.tools.TableSubsetSelector;
import com.rapidminer.tools.math.container.Range;


/**
 * Tests the default implementations of {@link TableSubsetSelectorFilter}. These are the filters used in the {@link
 * TableSubsetSelector}.
 *
 * @author Kevin Majchrzak
 */
@RunWith(Enclosed.class)
public class ColumnFilterTest {

	private static final String MISSINGS_SUFFIX = "Missing";
	private static final String SPECIAL_COLUMN = "Special";
	private static final String REAL = ValueTypeColumnFilter.TYPE_REAL;
	private static final String INTEGER = ValueTypeColumnFilter.TYPE_INTEGER;
	private static final String TIME = ValueTypeColumnFilter.TYPE_TIME;
	private static final String DATE_TIME = ValueTypeColumnFilter.TYPE_DATE_TIME;
	private static final String BINOMINAL = ValueTypeColumnFilter.TYPE_BINOMINAL;
	private static final String NON_BINOMINAL = ValueTypeColumnFilter.TYPE_NON_BINOMINAL;

	public static class FilterTable{
		@Test
		public void testAllColumnFilter() {
			Table testTable = createTable();
			assertEquals(testTable.labels(), AllColumnFilter.filterTableWithSettings(testTable,
					KEEP, false).labels());
			assertEquals(testTable.labels(), AllColumnFilter.filterTableWithSettings(testTable,
					FILTER, false).labels());
			assertEquals(removeSpecial(testTable.labels()), AllColumnFilter.filterTableWithSettings(testTable,
					REMOVE, false).labels());
			assertEquals(Collections.singletonList(SPECIAL_COLUMN), AllColumnFilter.filterTableWithSettings(testTable,
					KEEP, true).labels());
			assertEquals(Collections.emptyList(), AllColumnFilter.filterTableWithSettings(testTable,
					FILTER, true).labels());
			assertEquals(Collections.emptyList(), AllColumnFilter.filterTableWithSettings(testTable,
					REMOVE, true).labels());
		}

		@Test
		public void testNoMissingValuesColumnFilter() {
			Table testTable = createTable();
			// create expected results
			List<String> noMissingValues = Arrays.asList(REAL, INTEGER, TIME, DATE_TIME, BINOMINAL, NON_BINOMINAL,
					SPECIAL_COLUMN);
			List<String> missingValues = new ArrayList<>(testTable.labels());
			missingValues.removeAll(noMissingValues);
			List<String> missingValuesAndSpecial = new ArrayList<>(missingValues);
			missingValuesAndSpecial.add(SPECIAL_COLUMN);
			// assert results
			assertEquals(noMissingValues, NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					KEEP, false).labels());
			assertEquals(noMissingValues, NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					FILTER, false).labels());
			assertEquals(removeSpecial(noMissingValues), NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					REMOVE, false).labels());
			assertEquals(missingValuesAndSpecial, NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					KEEP, true).labels());
			assertEquals(missingValues, NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					FILTER, true).labels());
			assertEquals(missingValues, NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					REMOVE, true).labels());
		}

		@Test
		public void testValueTypeColumnFilter() {
			Table testTable = createTable();
			// create expected results
			List<String> withOutNonBinominalButWithSpecial = new ArrayList<>(testTable.labels());
			withOutNonBinominalButWithSpecial.removeAll(Arrays.asList(NON_BINOMINAL,
					NON_BINOMINAL + MISSINGS_SUFFIX));
			List<String> withOutNonBinominal = new ArrayList<>(withOutNonBinominalButWithSpecial);
			withOutNonBinominal.remove(SPECIAL_COLUMN);
			// assert results
			assertEquals(Arrays.asList(INTEGER, BINOMINAL, INTEGER + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX,
					SPECIAL_COLUMN), ValueTypeColumnFilter.filterTableWithSettings(testTable, KEEP, false,
					INTEGER, BINOMINAL).labels());

			assertEquals(Arrays.asList(INTEGER, BINOMINAL, INTEGER + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX),
					ValueTypeColumnFilter.filterTableWithSettings(testTable, REMOVE, false,
					INTEGER, BINOMINAL).labels());

			assertEquals(Arrays.asList(TIME, BINOMINAL, TIME + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX),
					ValueTypeColumnFilter.filterTableWithSettings(testTable, FILTER, false, TIME, BINOMINAL).labels());

			assertEquals(Arrays.asList(TIME, BINOMINAL, TIME + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX),
					ValueTypeColumnFilter.filterTableWithSettings(testTable, REMOVE, false, TIME, BINOMINAL).labels());

			assertEquals(withOutNonBinominalButWithSpecial, ValueTypeColumnFilter.filterTableWithSettings(testTable,
					KEEP, true, NON_BINOMINAL).labels());

			assertEquals(withOutNonBinominal, ValueTypeColumnFilter.filterTableWithSettings(testTable,
					REMOVE, true, NON_BINOMINAL).labels());

			assertEquals(withOutNonBinominal, ValueTypeColumnFilter.filterTableWithSettings(testTable,
					FILTER, true, NON_BINOMINAL).labels());

			assertEquals(Collections.emptyList(), ValueTypeColumnFilter.filterTableWithSettings(testTable,
					FILTER, false).labels());

			assertEquals(Collections.singletonList(SPECIAL_COLUMN), ValueTypeColumnFilter.filterTableWithSettings(testTable,
					KEEP, false).labels());

			assertEquals(Arrays.asList(NON_BINOMINAL, NON_BINOMINAL + MISSINGS_SUFFIX),
					ValueTypeColumnFilter.filterTableWithSettings(testTable,
					REMOVE, false, NON_BINOMINAL).labels());
		}

		@Test
		public void testSingleColumnFilter() {
			Table testTable = createTable();
			List<String> withOutSpecial = new ArrayList<>(testTable.labels());
			withOutSpecial.remove(SPECIAL_COLUMN);
			assertEquals(Arrays.asList(REAL, SPECIAL_COLUMN), SingleColumnFilter.filterTableWithSettings(testTable,
					KEEP, false, REAL).labels());
			assertEquals(Collections.singletonList(REAL), SingleColumnFilter.filterTableWithSettings(testTable,
					FILTER, false, REAL).labels());
			assertEquals(Collections.singletonList(REAL), SingleColumnFilter.filterTableWithSettings(testTable,
					REMOVE, false, REAL).labels());
			assertEquals(testTable.labels(), SingleColumnFilter.filterTableWithSettings(testTable,
					KEEP, true, SPECIAL_COLUMN).labels());
			assertEquals(withOutSpecial, SingleColumnFilter.filterTableWithSettings(testTable,
					FILTER, true, SPECIAL_COLUMN).labels());
			assertEquals(removeSpecial(testTable.labels()), SingleColumnFilter.filterTableWithSettings(testTable,
					REMOVE, true, SPECIAL_COLUMN).labels());
			assertEquals(testTable.labels(), SingleColumnFilter.filterTableWithSettings(testTable,
					FILTER, true, null).labels());
			assertEquals(removeSpecial(testTable.labels()), SingleColumnFilter.filterTableWithSettings(testTable,
					REMOVE, true, null).labels());
		}

		@Test
		public void testSubsetColumnFilter() {
			Table testTable = createTable();
			List<String> withOutSpecialAndReal = new ArrayList<>(testTable.labels());
			withOutSpecialAndReal.remove(SPECIAL_COLUMN);
			withOutSpecialAndReal.remove(REAL);
			List<String> withOutReal = new ArrayList<>(testTable.labels());
			withOutReal.remove(REAL);
			assertEquals(Arrays.asList(REAL, SPECIAL_COLUMN), SubsetColumnFilter.filterTableWithSettings(testTable,
					KEEP, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(Collections.singletonList(REAL), SubsetColumnFilter.filterTableWithSettings(testTable,
					FILTER, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(Collections.singletonList(REAL), SubsetColumnFilter.filterTableWithSettings(testTable,
					REMOVE, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(testTable.labels(), SubsetColumnFilter.filterTableWithSettings(testTable,
					KEEP, true, new HashSet<>()).labels());
			assertEquals(removeSpecial(testTable.labels()), SubsetColumnFilter.filterTableWithSettings(testTable,
					REMOVE, true, new HashSet<>()).labels());
			assertEquals(Collections.singletonList(SPECIAL_COLUMN), SubsetColumnFilter.filterTableWithSettings(testTable,
					KEEP, false, new HashSet<>()).labels());
			assertEquals(Collections.emptyList(), SubsetColumnFilter.filterTableWithSettings(testTable,
					REMOVE, false, new HashSet<>()).labels());
			assertEquals(withOutReal, SubsetColumnFilter.filterTableWithSettings(testTable,
					KEEP, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(removeSpecial(withOutReal), SubsetColumnFilter.filterTableWithSettings(testTable,
					REMOVE, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(withOutSpecialAndReal, SubsetColumnFilter.filterTableWithSettings(testTable,
					FILTER, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(testTable.labels(), SubsetColumnFilter.filterTableWithSettings(testTable,
					FILTER, false, new HashSet<>(testTable.labels())).labels());
		}

		@Test
		public void testRegexColumnFilter() {
			Table testTable = createTable();
			List<String> withOutRealMissing = new ArrayList<>(testTable.labels());
			withOutRealMissing.remove(REAL + MISSINGS_SUFFIX);
			List<String> withOutSpecial = new ArrayList<>(testTable.labels());
			withOutSpecial.remove(SPECIAL_COLUMN);

			assertEquals(Arrays.asList(REAL, REAL + MISSINGS_SUFFIX, SPECIAL_COLUMN),
					RegexColumnFilter.filterTableWithSettings(testTable, KEEP, false,
							Pattern.compile(REAL + ".*"), null).labels());

			assertEquals(Arrays.asList(REAL, REAL + MISSINGS_SUFFIX),
					RegexColumnFilter.filterTableWithSettings(testTable, REMOVE, false,
							Pattern.compile(REAL + ".*"), null).labels());

			assertEquals(Collections.singletonList(REAL + MISSINGS_SUFFIX),
					RegexColumnFilter.filterTableWithSettings(testTable, FILTER, false,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());

			assertEquals(Collections.singletonList(REAL + MISSINGS_SUFFIX),
					RegexColumnFilter.filterTableWithSettings(testTable, REMOVE, false,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());

			assertEquals(withOutRealMissing,
					RegexColumnFilter.filterTableWithSettings(testTable, KEEP, true,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());

			assertEquals(removeSpecial(withOutRealMissing),
					RegexColumnFilter.filterTableWithSettings(testTable, REMOVE, true,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());

			assertEquals(withOutSpecial,
					RegexColumnFilter.filterTableWithSettings(testTable, FILTER, true, Pattern.compile(SPECIAL_COLUMN),
							null).labels());

			assertEquals(testTable.labels(),
					RegexColumnFilter.filterTableWithSettings(testTable, FILTER, false, null,
							null).labels());

			assertEquals(removeSpecial(testTable.labels()),
					RegexColumnFilter.filterTableWithSettings(testTable, REMOVE, false, null,
							null).labels());
		}
	}

	public static class FilterTableMetaData{
		@Test
		public void testAllColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			assertEquals(testTableMD.labels(), AllColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, false).labels());
			assertEquals(testTableMD.labels(), AllColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, false).labels());
			assertEquals(removeSpecial(testTableMD.labels()), AllColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, false).labels());
			assertEquals(Collections.singleton(SPECIAL_COLUMN),
					AllColumnFilter.filterMetaDataWithSettings(testTableMD, KEEP, true).labels());
			assertEquals(Collections.emptySet(),
					AllColumnFilter.filterMetaDataWithSettings(testTableMD, FILTER, true).labels());
			assertEquals(Collections.emptySet(),
					AllColumnFilter.filterMetaDataWithSettings(testTableMD, REMOVE, true).labels());
		}

		@Test
		public void testNoMissingValuesColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			// create expected results
			Set<String> noMissingValues = new HashSet<>(Arrays.asList(REAL, INTEGER, TIME, DATE_TIME, BINOMINAL,
					NON_BINOMINAL, SPECIAL_COLUMN));
			Set<String> missingValues = new HashSet<>(testTableMD.labels());
			missingValues.removeAll(noMissingValues);
			Set<String> missingValuesAndSpecial = new HashSet<>(missingValues);
			missingValuesAndSpecial.add(SPECIAL_COLUMN);
			// assert results
			assertEquals(noMissingValues, NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, false).labels());
			assertEquals(noMissingValues, NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, false).labels());
			assertEquals(removeSpecial(noMissingValues), NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, false).labels());
			assertEquals(missingValuesAndSpecial, NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, true).labels());
			assertEquals(missingValues, NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, true).labels());
			assertEquals(missingValues, NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, true).labels());
		}

		@Test
		public void testValueTypeColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			// create expected results
			Set<String> withOutNonBinominalButWithSpecial = new HashSet<>(testTableMD.labels());
			withOutNonBinominalButWithSpecial.removeAll(Arrays.asList(NON_BINOMINAL,
					NON_BINOMINAL + MISSINGS_SUFFIX));
			Set<String> withOutNonBinominal = new HashSet<>(withOutNonBinominalButWithSpecial);
			withOutNonBinominal.remove(SPECIAL_COLUMN);
			// assert results
			assertEquals(new HashSet<>(Arrays.asList(INTEGER, BINOMINAL, INTEGER + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX,
					SPECIAL_COLUMN)), ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, KEEP,
					false, INTEGER, BINOMINAL).labels());
			assertEquals(new HashSet<>(Arrays.asList(TIME, BINOMINAL, TIME + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX)),
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, FILTER, false,
							TIME, BINOMINAL).labels());
			assertEquals(new HashSet<>(Arrays.asList(TIME, BINOMINAL, TIME + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX)),
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, REMOVE, false,
							TIME, BINOMINAL).labels());
			assertEquals(withOutNonBinominalButWithSpecial,
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, KEEP, true,
							NON_BINOMINAL).labels());
			assertEquals(withOutNonBinominal,
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, FILTER, true,
							NON_BINOMINAL).labels());
			assertEquals(withOutNonBinominal,
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, REMOVE, true,
							NON_BINOMINAL).labels());
			assertEquals(Collections.emptySet(),
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, FILTER, false).labels());
			assertEquals(Collections.singleton(SPECIAL_COLUMN),
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, KEEP, false).labels());
			assertEquals(new HashSet<>(Arrays.asList(NON_BINOMINAL, NON_BINOMINAL + MISSINGS_SUFFIX)),
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, REMOVE, false, NON_BINOMINAL).labels());
		}

		@Test
		public void testSingleColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			Set<String> withOutSpecial = new HashSet<>(testTableMD.labels());
			withOutSpecial.remove(SPECIAL_COLUMN);
			assertEquals(new HashSet<>(Arrays.asList(REAL, SPECIAL_COLUMN)), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, false, REAL).labels());
			assertEquals(Collections.singleton(REAL), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, false, REAL).labels());
			assertEquals(Collections.singleton(REAL), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, false, REAL).labels());
			assertEquals(testTableMD.labels(), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, true, SPECIAL_COLUMN).labels());
			assertEquals(withOutSpecial, SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, true, SPECIAL_COLUMN).labels());
			assertEquals(withOutSpecial, SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, true, SPECIAL_COLUMN).labels());
			assertEquals(testTableMD.labels(), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, true, null).labels());
			assertEquals(removeSpecial(testTableMD.labels()), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, true, null).labels());
		}

		@Test
		public void testSubsetColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			Set<String> withOutSpecialAndReal = new HashSet<>(testTableMD.labels());
			withOutSpecialAndReal.remove(SPECIAL_COLUMN);
			withOutSpecialAndReal.remove(REAL);
			Set<String> withOutReal = new HashSet<>(testTableMD.labels());
			withOutReal.remove(REAL);
			assertEquals(new HashSet<>(Arrays.asList(REAL, SPECIAL_COLUMN)), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(Collections.singleton(REAL), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(Collections.singleton(REAL), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(testTableMD.labels(), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, true, new HashSet<>()).labels());
			assertEquals(removeSpecial(testTableMD.labels()), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, true, new HashSet<>()).labels());
			assertEquals(Collections.singleton(SPECIAL_COLUMN), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, false, new HashSet<>()).labels());
			assertEquals(withOutReal, SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					KEEP, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(removeSpecial(withOutReal), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(withOutSpecialAndReal, SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(testTableMD.labels(), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					FILTER, false, new HashSet<>(testTableMD.labels())).labels());
			assertEquals(removeSpecial(testTableMD.labels()), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					REMOVE, false, new HashSet<>(testTableMD.labels())).labels());
		}

		@Test
		public void testRegexColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			Set<String> withOutRealMissing = new HashSet<>(testTableMD.labels());
			withOutRealMissing.remove(REAL + MISSINGS_SUFFIX);
			Set<String> withOutSpecial = new HashSet<>(testTableMD.labels());
			withOutSpecial.remove(SPECIAL_COLUMN);
			assertEquals(new HashSet<>(Arrays.asList(REAL, REAL + MISSINGS_SUFFIX, SPECIAL_COLUMN)),
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, KEEP, false,
							Pattern.compile(REAL + ".*"), null).labels());
			assertEquals(Collections.singleton(REAL + MISSINGS_SUFFIX),
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, FILTER, false,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());
			assertEquals(withOutRealMissing,
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, KEEP, true,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());
			assertEquals(removeSpecial(withOutRealMissing),
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, REMOVE, true,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());
			assertEquals(withOutSpecial,
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, FILTER, true, Pattern.compile(SPECIAL_COLUMN),
							null).labels());
			assertEquals(withOutSpecial,
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, REMOVE, true, Pattern.compile(SPECIAL_COLUMN),
							null).labels());
			assertEquals(testTableMD.labels(),
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, FILTER, false, null,
							null).labels());
			assertEquals(removeSpecial(testTableMD.labels()),
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, REMOVE, false, null,
							null).labels());
		}
	}



	/**
	 * Creates and returns a test Table.
	 */
	private static Table createTable() {
		TableBuilder builder = Builders.newTableBuilder(10);
		// every value type
		builder.addReal(REAL, i -> i);
		builder.addInt53Bit(INTEGER, i -> i);
		builder.addTime(TIME, i -> LocalTime.now());
		builder.addDateTime(DATE_TIME, i -> Instant.now());
		builder.addBoolean(BINOMINAL, i -> i % 2 == 0 ? "true" : "false", "true");
		builder.addNominal(NON_BINOMINAL, String::valueOf);
		// every value type filled with missing values
		builder.addReal(REAL + MISSINGS_SUFFIX, i -> Double.NaN);
		builder.addInt53Bit(INTEGER + MISSINGS_SUFFIX, i -> Double.NaN);
		builder.addTime(TIME + MISSINGS_SUFFIX, i -> null);
		builder.addDateTime(DATE_TIME + MISSINGS_SUFFIX, i -> null);
		builder.addBoolean(BINOMINAL + MISSINGS_SUFFIX, i -> null, null);
		builder.addNominal(NON_BINOMINAL + MISSINGS_SUFFIX, i -> null);
		builder.addNominal(SPECIAL_COLUMN, i -> i % 2 == 0 ? "yes" : "no");
		builder.addMetaData(SPECIAL_COLUMN, ColumnRole.LABEL);
		return builder.build(Belt.defaultContext());
	}

	/**
	 * Creates and returns test TableMetaData.
	 */
	private static TableMetaData createTableMetaData() {
		TableMetaDataBuilder builder = new TableMetaDataBuilder(10);
		// every value type
		builder.addReal(REAL, new Range(0, 9), SetRelation.EQUAL, new MDInteger(0));
		builder.addInteger(INTEGER, new Range(-0.3, 9), SetRelation.EQUAL, new MDInteger(0));
		builder.addTime(TIME, null, SetRelation.EQUAL, new MDInteger(0));
		builder.addDateTime(DATE_TIME, null, SetRelation.EQUAL, new MDInteger(0));
		builder.addBoolean(BINOMINAL, "true", "false", MDInteger.newUnknown());
		builder.addNominal(NON_BINOMINAL, Collections.singleton("value"), SetRelation.SUPERSET, new MDInteger(0));
		// every value type filled with missing values
		builder.addReal(REAL + MISSINGS_SUFFIX, null, SetRelation.EQUAL, new MDInteger(10));
		builder.addInteger(INTEGER + MISSINGS_SUFFIX, new Range(0,1), SetRelation.EQUAL, new MDInteger(1));
		builder.addTime(TIME + MISSINGS_SUFFIX, null, SetRelation.EQUAL, new MDInteger(1));
		builder.addDateTime(DATE_TIME + MISSINGS_SUFFIX, null, SetRelation.EQUAL, new MDInteger(1));
		builder.addBoolean(BINOMINAL + MISSINGS_SUFFIX, new MDInteger(1));
		builder.addNominal(NON_BINOMINAL + MISSINGS_SUFFIX, Collections.singleton("value"), SetRelation.EQUAL,
				new MDInteger(1));
		builder.addNominal(SPECIAL_COLUMN, Collections.singleton("value"), SetRelation.UNKNOWN, new MDInteger());
		builder.addColumnMetaData(SPECIAL_COLUMN, ColumnRole.LABEL);
		return builder.build();
	}

	/**
	 * @return copy of the given list without SPECIAL_COLUMN
	 */
	private static List<String> removeSpecial(List<String> oldList) {
		List<String> newList = new ArrayList<>(oldList);
		newList.remove(SPECIAL_COLUMN);
		return newList;
	}

	/**
	 * @return copy of the given set without SPECIAL_COLUMN
	 */
	private static Set<String> removeSpecial(Set<String> oldSet) {
		Set<String> newSet = new HashSet<>(oldSet);
		newSet.remove(SPECIAL_COLUMN);
		return newSet;
	}
}
