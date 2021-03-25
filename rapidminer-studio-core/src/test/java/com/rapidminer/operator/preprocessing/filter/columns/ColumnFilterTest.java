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
					false, false).labels());
			assertEquals(testTable.labels(), AllColumnFilter.filterTableWithSettings(testTable,
					true, false).labels());
			assertEquals(Collections.singletonList(SPECIAL_COLUMN),
					AllColumnFilter.filterTableWithSettings(testTable, false, true).labels());
			assertEquals(Collections.emptyList(),
					AllColumnFilter.filterTableWithSettings(testTable, true, true).labels());
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
					false, false).labels());
			assertEquals(noMissingValues, NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					true, false).labels());
			assertEquals(missingValuesAndSpecial, NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					false, true).labels());
			assertEquals(missingValues, NoMissingValuesColumnFilter.filterTableWithSettings(testTable,
					true, true).labels());
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
					SPECIAL_COLUMN), ValueTypeColumnFilter.filterTableWithSettings(testTable, false,
					false, INTEGER, BINOMINAL).labels());
			assertEquals(Arrays.asList(TIME, BINOMINAL, TIME + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX),
					ValueTypeColumnFilter.filterTableWithSettings(testTable, true, false,
							TIME, BINOMINAL).labels());
			assertEquals(withOutNonBinominalButWithSpecial,
					ValueTypeColumnFilter.filterTableWithSettings(testTable, false, true,
							NON_BINOMINAL).labels());
			assertEquals(withOutNonBinominal,
					ValueTypeColumnFilter.filterTableWithSettings(testTable, true, true,
							NON_BINOMINAL).labels());
			assertEquals(Collections.emptyList(),
					ValueTypeColumnFilter.filterTableWithSettings(testTable, true, false).labels());
		}

		@Test
		public void testSingleColumnFilter() {
			Table testTable = createTable();
			List<String> withOutSpecial = new ArrayList<>(testTable.labels());
			withOutSpecial.remove(SPECIAL_COLUMN);
			assertEquals(Arrays.asList(REAL, SPECIAL_COLUMN), SingleColumnFilter.filterTableWithSettings(testTable,
					false, false, REAL).labels());
			assertEquals(Collections.singletonList(REAL), SingleColumnFilter.filterTableWithSettings(testTable,
					true, false, REAL).labels());
			assertEquals(testTable.labels(), SingleColumnFilter.filterTableWithSettings(testTable,
					false, true, SPECIAL_COLUMN).labels());
			assertEquals(withOutSpecial, SingleColumnFilter.filterTableWithSettings(testTable,
					true, true, SPECIAL_COLUMN).labels());
			assertEquals(testTable.labels(), SingleColumnFilter.filterTableWithSettings(testTable,
					true, true, null).labels());
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
					false, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(Collections.singletonList(REAL), SubsetColumnFilter.filterTableWithSettings(testTable,
					true, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(testTable.labels(), SubsetColumnFilter.filterTableWithSettings(testTable,
					false, true, new HashSet<>()).labels());
			assertEquals(Collections.singletonList(SPECIAL_COLUMN), SubsetColumnFilter.filterTableWithSettings(testTable,
					false, false, new HashSet<>()).labels());
			assertEquals(withOutReal, SubsetColumnFilter.filterTableWithSettings(testTable,
					false, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(withOutSpecialAndReal, SubsetColumnFilter.filterTableWithSettings(testTable,
					true, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(testTable.labels(), SubsetColumnFilter.filterTableWithSettings(testTable,
					true, false, new HashSet<>(testTable.labels())).labels());
		}

		@Test
		public void testRegexColumnFilter() {
			Table testTable = createTable();
			List<String> withOutRealMissing = new ArrayList<>(testTable.labels());
			withOutRealMissing.remove(REAL + MISSINGS_SUFFIX);
			List<String> withOutSpecial = new ArrayList<>(testTable.labels());
			withOutSpecial.remove(SPECIAL_COLUMN);
			assertEquals(Arrays.asList(REAL, REAL + MISSINGS_SUFFIX, SPECIAL_COLUMN),
					RegexColumnFilter.filterTableWithSettings(testTable, false, false,
							Pattern.compile(REAL + ".*"), null).labels());
			assertEquals(Collections.singletonList(REAL + MISSINGS_SUFFIX),
					RegexColumnFilter.filterTableWithSettings(testTable, true, false,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());
			assertEquals(withOutRealMissing,
					RegexColumnFilter.filterTableWithSettings(testTable, false, true,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());
			assertEquals(withOutSpecial,
					RegexColumnFilter.filterTableWithSettings(testTable, true, true, Pattern.compile(SPECIAL_COLUMN),
							null).labels());
			assertEquals(testTable.labels(),
					RegexColumnFilter.filterTableWithSettings(testTable, true, false, null,
							null).labels());
		}
	}

	public static class FilterTableMetaData{
		@Test
		public void testAllColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			assertEquals(testTableMD.labels(), AllColumnFilter.filterMetaDataWithSettings(testTableMD,
					false, false).labels());
			assertEquals(testTableMD.labels(), AllColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, false).labels());
			assertEquals(Collections.singleton(SPECIAL_COLUMN),
					AllColumnFilter.filterMetaDataWithSettings(testTableMD, false, true).labels());
			assertEquals(Collections.emptySet(),
					AllColumnFilter.filterMetaDataWithSettings(testTableMD, true, true).labels());
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
					false, false).labels());
			assertEquals(noMissingValues, NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, false).labels());
			assertEquals(missingValuesAndSpecial, NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					false, true).labels());
			assertEquals(missingValues, NoMissingValuesColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, true).labels());
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
					SPECIAL_COLUMN)), ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, false,
					false, INTEGER, BINOMINAL).labels());
			assertEquals(new HashSet<>(Arrays.asList(TIME, BINOMINAL, TIME + MISSINGS_SUFFIX, BINOMINAL + MISSINGS_SUFFIX)),
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, true, false,
							TIME, BINOMINAL).labels());
			assertEquals(withOutNonBinominalButWithSpecial,
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, false, true,
							NON_BINOMINAL).labels());
			assertEquals(withOutNonBinominal,
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, true, true,
							NON_BINOMINAL).labels());
			assertEquals(Collections.emptySet(),
					ValueTypeColumnFilter.filterMetaDataWithSettings(testTableMD, true, false).labels());
		}

		@Test
		public void testSingleColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			Set<String> withOutSpecial = new HashSet<>(testTableMD.labels());
			withOutSpecial.remove(SPECIAL_COLUMN);
			assertEquals(new HashSet<>(Arrays.asList(REAL, SPECIAL_COLUMN)), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					false, false, REAL).labels());
			assertEquals(Collections.singleton(REAL), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, false, REAL).labels());
			assertEquals(testTableMD.labels(), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					false, true, SPECIAL_COLUMN).labels());
			assertEquals(withOutSpecial, SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, true, SPECIAL_COLUMN).labels());
			assertEquals(testTableMD.labels(), SingleColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, true, null).labels());
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
					false, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(Collections.singleton(REAL), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, false, new HashSet<>(Collections.singleton(REAL))).labels());
			assertEquals(testTableMD.labels(), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					false, true, new HashSet<>()).labels());
			assertEquals(Collections.singleton(SPECIAL_COLUMN), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					false, false, new HashSet<>()).labels());
			assertEquals(withOutReal, SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					false, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(withOutSpecialAndReal, SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, true, new HashSet<>(Arrays.asList(SPECIAL_COLUMN, REAL))).labels());
			assertEquals(testTableMD.labels(), SubsetColumnFilter.filterMetaDataWithSettings(testTableMD,
					true, false, new HashSet<>(testTableMD.labels())).labels());
		}

		@Test
		public void testRegexColumnFilter() {
			TableMetaData testTableMD = createTableMetaData();
			Set<String> withOutRealMissing = new HashSet<>(testTableMD.labels());
			withOutRealMissing.remove(REAL + MISSINGS_SUFFIX);
			Set<String> withOutSpecial = new HashSet<>(testTableMD.labels());
			withOutSpecial.remove(SPECIAL_COLUMN);
			assertEquals(new HashSet<>(Arrays.asList(REAL, REAL + MISSINGS_SUFFIX, SPECIAL_COLUMN)),
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, false, false,
							Pattern.compile(REAL + ".*"), null).labels());
			assertEquals(Collections.singleton(REAL + MISSINGS_SUFFIX),
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, true, false,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());
			assertEquals(withOutRealMissing,
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, false, true,
							Pattern.compile(REAL + ".*"), Pattern.compile(REAL)).labels());
			assertEquals(withOutSpecial,
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, true, true, Pattern.compile(SPECIAL_COLUMN),
							null).labels());
			assertEquals(testTableMD.labels(),
					RegexColumnFilter.filterMetaDataWithSettings(testTableMD, true, false, null,
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
	 * Creates and returns a test TableMetaData.
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
}
