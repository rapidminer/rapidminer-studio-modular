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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.junit.Test;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MDNumber;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.math.container.ObjectRange;
import com.rapidminer.tools.math.container.Range;


/**
 * Tests the {@link ColumnInfo} and the {@link ColumnInfoBuilder}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class ColumnInfoTest {

	@Test
	public void testNominal() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(30).setDictionaryValues(Arrays.asList("a", "b",
						"c"), SetRelation.SUPERSET).build();
		assertEquals(30, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList("a", "b", "c")), false), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testNominalBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(30).setDictionaryValues(Arrays.asList("a", "b",
						"c"), SetRelation.SUBSET);
		assertEquals(30, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList("a", "b", "c")), false), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testNominalAdd() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(30).setDictionaryValues(Arrays.asList("a", "b",
						"c"), SetRelation.SUPERSET).addDictionaryValues(Arrays.asList("b", "d", "e")).build();
		assertEquals(30, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList("a", "b", "c", "d", "e")), false), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testNominalAddBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(30).setDictionaryValues(Arrays.asList("a", "b",
						"c"), SetRelation.SUPERSET).addDictionaryValues(Arrays.asList("b", "d", "e"));
		assertEquals(30, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList("a", "b", "c", "d", "e")), false), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testNominalManyValues() {
		String[] values = new String[AttributeMetaData.getMaximumNumberOfNominalValues() + 1];
		Arrays.setAll(values, i -> "val" + i);
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(0).setDictionaryValues(Arrays.asList(values),
						SetRelation.EQUAL).build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertTrue(info.getDictionary().getValueSet().size() < AttributeMetaData.getMaximumNumberOfNominalValues());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList(values).subList(0,
				values.length - 2)), true), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertTrue(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testNominalManyValuesBuilder() {
		String[] values = new String[AttributeMetaData.getMaximumNumberOfNominalValues() + 1];
		Arrays.setAll(values, i -> "val" + i);
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(0).setDictionaryValues(Arrays.asList(values),
						SetRelation.EQUAL);
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertTrue(info.getDictionary().getValueSet().size() < AttributeMetaData.getMaximumNumberOfNominalValues());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList(values).subList(0,
				values.length - 2)), true), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertTrue(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testNominalManyValuesAdd() {
		String[] values = new String[AttributeMetaData.getMaximumNumberOfNominalValues() + 1];
		Arrays.setAll(values, i -> "val" + i);
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(0).setDictionaryValues(Arrays.asList(values),
						SetRelation.EQUAL).addDictionaryValues(Arrays.asList("a", "b")).build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertTrue(info.getDictionary().getValueSet().size() < AttributeMetaData.getMaximumNumberOfNominalValues());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList(values).subList(0,
				values.length - 2)), true), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertTrue(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testNominalBeforeManyValuesAdd() {
		String[] values = new String[AttributeMetaData.getMaximumNumberOfNominalValues() - 2];
		Arrays.setAll(values, i -> "val" + i);
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(0).setDictionaryValues(Arrays.asList(values),
						SetRelation.EQUAL).addDictionaryValues(Arrays.asList("a", "b", "c")).build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertTrue(info.getDictionary().getValueSet().size() < AttributeMetaData.getMaximumNumberOfNominalValues());
		final List<String> strings = new ArrayList<>(Arrays.asList(values));
		strings.add("a");
		assertEquals(new DictionaryInfo(new HashSet<>(strings), true), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertTrue(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testBoolean() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(-1).setBooleanDictionaryValues("bla", "blup").build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.EQUAL, info.getValueSetRelation());
		assertEquals(new BooleanDictionaryInfo("bla", "blup"), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.YES, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testBooleanBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(-1).setBooleanDictionaryValues("bla", "blup");
		assertEquals(-1, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.EQUAL, info.getValueSetRelation());
		assertEquals(new BooleanDictionaryInfo("bla", "blup"), info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.YES, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testBooleanWithNull() {
		MDInteger missings = new MDInteger(-1);
		missings.increaseByUnknownAmount();
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(missings).setBooleanDictionaryValues(null, "blup").build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_LEAST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.EQUAL, info.getValueSetRelation());
		assertEquals(new BooleanDictionaryInfo(null, "blup"), info.getDictionary());
		assertEquals(Collections.singleton("blup"), info.getDictionary().getValueSet());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.YES, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testBooleanWithNullBuilder() {
		MDInteger missings = new MDInteger(-1);
		missings.increaseByUnknownAmount();
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(missings).setBooleanDictionaryValues(null, "blup");
		assertEquals(-1, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_LEAST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.EQUAL, info.getValueSetRelation());
		assertEquals(new BooleanDictionaryInfo(null, "blup"), info.getDictionary());
		assertEquals(Collections.singleton("blup"), info.getDictionary().getValueSet());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.YES, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testBooleanUnknown() {
		MDInteger missings = new MDInteger(-5);
		missings.increaseByUnknownAmount();
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(missings).setUnknownBooleanDictionary().build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_LEAST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertEquals(new BooleanDictionaryInfo(), info.getDictionary());
		assertEquals(Collections.emptySet(), info.getDictionary().getValueSet());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.YES, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testBooleanUnknownBuilder() {
		MDInteger missings = new MDInteger(-5);
		missings.increaseByUnknownAmount();
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(missings).setUnknownBooleanDictionary();
		assertEquals(-5, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_LEAST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertEquals(new BooleanDictionaryInfo(), info.getDictionary());
		assertEquals(Collections.emptySet(), info.getDictionary().getValueSet());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.YES, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testRealWithoutRange() {
		MDInteger missings = new MDInteger(-5);
		missings.reduceByUnknownAmount();
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.REAL).setMissings(missings).build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_MOST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.REAL, info.getType().get());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testRealWithoutRangeBuilder() {
		MDInteger missings = new MDInteger(-5);
		missings.reduceByUnknownAmount();
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.REAL).setMissings(missings);
		assertEquals(-5, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_MOST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.REAL, info.getType().get());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}

	@Test
	public void testReal() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.REAL)
						.setNumericRange(new Range(4.2, Double.POSITIVE_INFINITY), SetRelation.SUBSET)
						.setMissings(42).build();
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.REAL, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertTrue(info.getNumericRange().isPresent());
		assertEquals(new Range(4.2, Double.POSITIVE_INFINITY), info.getNumericRange().get());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}

	@Test
	public void testRealBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.REAL)
						.setNumericRange(new Range(4.2, Double.POSITIVE_INFINITY), SetRelation.SUBSET)
						.setMissings(42);
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.REAL, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertTrue(info.getNumericRange().isPresent());
		assertEquals(new Range(4.2, Double.POSITIVE_INFINITY), info.getNumericRange().get());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}

	@Test
	public void testRealUnion() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.REAL)
						.setNumericRange(new Range(4.2, Double.POSITIVE_INFINITY), SetRelation.SUBSET)
						.setMissings(42).rangeUnion(new Range(-5, 11))
						.addDictionaryValues(Arrays.asList("a", "b")).build();
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.REAL, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertTrue(info.getNumericRange().isPresent());
		assertEquals(new Range(-5, Double.POSITIVE_INFINITY), info.getNumericRange().get());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}

	@Test
	public void testRealUnionBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.REAL)
						.setNumericRange(new Range(4.2, Double.POSITIVE_INFINITY), SetRelation.SUBSET)
						.setMissings(42).rangeUnion(new Range(-5, 11))
						.addDictionaryValues(Arrays.asList("a", "b"));
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.REAL, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList("a", "b")), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertTrue(info.getNumericRange().isPresent());
		assertEquals(new Range(-5, Double.POSITIVE_INFINITY), info.getNumericRange().get());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}

	@Test
	public void testRealUnionNull() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.REAL)
						.setNumericRange(new Range(4.2, Double.POSITIVE_INFINITY), SetRelation.SUBSET)
						.setMissings(42).rangeUnion(null)
						.addDictionaryValues(Arrays.asList("a", "b")).build();
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.REAL, info.getType().get());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}

	@Test
	public void testInt() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.INTEGER_53_BIT)
						.setNumericRange(new Range(-5, 10), SetRelation.SUBSET)
						.setMissings(42).unknownMissings().build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.UNKNOWN, info.getMissingValues().getRelation());
		assertEquals(ColumnType.INTEGER_53_BIT, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertTrue(info.getNumericRange().isPresent());
		assertEquals(new Range(-5, 10), info.getNumericRange().get());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}

	@Test
	public void testIntBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.INTEGER_53_BIT)
						.setNumericRange(new Range(-5, 10), SetRelation.SUBSET)
						.setMissings(42).unknownMissings();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.UNKNOWN, info.getMissingValues().getRelation());
		assertEquals(ColumnType.INTEGER_53_BIT, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertTrue(info.getNumericRange().isPresent());
		assertEquals(new Range(-5, 10), info.getNumericRange().get());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}

	@Test
	public void testTime() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.TIME)
						.setMissings(42).setObjectRange(new ObjectRange<>(LocalTime.MIN, LocalTime.NOON,
						ColumnType.TIME.comparator()), SetRelation.SUBSET).increaseMissings().build();
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_LEAST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.TIME, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertTrue(info.getObjectRange(LocalTime.class).isPresent());
		assertEquals(new ObjectRange<>(LocalTime.MIN, LocalTime.NOON, ColumnType.TIME.comparator()),
				info.getObjectRange(LocalTime.class).get());
	}

	@Test
	public void testTimeBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.TIME)
						.setMissings(42).setObjectRange(new ObjectRange<>(LocalTime.MIN, LocalTime.NOON,
						ColumnType.TIME.comparator()), SetRelation.SUBSET).increaseMissings();
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_LEAST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.TIME, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertTrue(info.getObjectRange(LocalTime.class).isPresent());
		assertEquals(new ObjectRange<>(LocalTime.MIN, LocalTime.NOON, ColumnType.TIME.comparator()),
				info.getObjectRange(LocalTime.class).get());
	}

	@Test
	public void testDateTime() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.DATETIME)
						.setMissings(42).setObjectRange(new ObjectRange<>(Instant.EPOCH, Instant.MAX,
						ColumnType.DATETIME.comparator()), SetRelation.SUBSET).reduceMissings().build();
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_MOST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.DATETIME, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(LocalTime.class).isPresent());
		assertTrue(info.getObjectRange(Instant.class).isPresent());
		assertEquals(new ObjectRange<>(Instant.EPOCH, Instant.MAX, ColumnType.DATETIME.comparator()),
				info.getObjectRange(Instant.class).get());
	}

	@Test
	public void testDateTimeBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.DATETIME)
						.setMissings(42).setObjectRange(new ObjectRange<>(Instant.EPOCH, Instant.MAX,
						ColumnType.DATETIME.comparator()), SetRelation.SUBSET).reduceMissings();
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_MOST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.DATETIME, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(LocalTime.class).isPresent());
		assertTrue(info.getObjectRange(Instant.class).isPresent());
		assertEquals(new ObjectRange<>(Instant.EPOCH, Instant.MAX, ColumnType.DATETIME.comparator()),
				info.getObjectRange(Instant.class).get());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWrongObjectRange() {
		new ColumnInfoBuilder(ColumnType.DATETIME)
				.setMissings(42).setObjectRange(new ObjectRange<>(LocalTime.MIN, LocalTime.NOON,
				ColumnType.TIME.comparator()), SetRelation.SUBSET);

	}

	@Test
	public void testText() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.TEXT)
						.setMissings(42).setObjectRange(new ObjectRange<>("a", "z",
						String::compareTo), SetRelation.EQUAL).reduceMissings().setValueSetRelation(SetRelation.SUPERSET).build();
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_MOST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.TEXT, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(LocalTime.class).isPresent());
		assertTrue(info.getObjectRange(String.class).isPresent());
		assertEquals((new ObjectRange<>("a", "z",
				String::compareTo)), info.getObjectRange(String.class).get());
	}

	@Test
	public void testTextBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.TEXT)
						.setMissings(42).setObjectRange(new ObjectRange<>("a", "z",
						String::compareTo), SetRelation.EQUAL).reduceMissings().setValueSetRelation(SetRelation.SUPERSET);
		assertEquals(42, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_MOST, info.getMissingValues().getRelation());
		assertEquals(ColumnType.TEXT, info.getType().get());
		assertEquals(SetRelation.SUPERSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(LocalTime.class).isPresent());
		assertTrue(info.getObjectRange(String.class).isPresent());
		assertEquals((new ObjectRange<>("a", "z",
				String::compareTo)), info.getObjectRange(String.class).get());
	}

	@Test
	public void testTextList() {
		final ColumnInfo info =
				new ColumnInfoBuilder(ColumnType.TEXTLIST)
						.setMissings(null).setValueSetRelation(SetRelation.SUPERSET).mergeValueSetRelation(SetRelation.SUBSET).build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.UNKNOWN, info.getMissingValues().getRelation());
		assertEquals(ColumnType.TEXTLIST, info.getType().get());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(LocalTime.class).isPresent());
	}

	@Test
	public void testTextListBuilder() {
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder(ColumnType.TEXTLIST)
						.setMissings(null).setValueSetRelation(SetRelation.SUPERSET).mergeValueSetRelation(SetRelation.SUBSET);
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.UNKNOWN, info.getMissingValues().getRelation());
		assertEquals(ColumnType.TEXTLIST, info.getType().get());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.UNKNOWN, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(LocalTime.class).isPresent());
	}

	@Test
	public void testNullType() {
		MDInteger missings = new MDInteger(5);
		missings.increaseByUnknownAmount();
		final ColumnInfo info =
				new ColumnInfoBuilder((ColumnType)null)
						.setMissings(42).setValueSetRelation(SetRelation.SUPERSET).addMissings(missings).mergeValueSetRelation(SetRelation.SUBSET).build();
		assertEquals(47, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_LEAST, info.getMissingValues().getRelation());
		assertFalse(info.getType().isPresent());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.UNKNOWN, info.isNominal());
		assertEquals(MetaDataInfo.UNKNOWN, info.isNumeric());
		assertEquals(MetaDataInfo.UNKNOWN, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.UNKNOWN, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertNull(info.getUncheckedObjectRange());
	}

	@Test
	public void testNullTypeBuilder() {
		MDInteger missings = new MDInteger(5);
		missings.increaseByUnknownAmount();
		final ColumnInfoBuilder info =
				new ColumnInfoBuilder((ColumnType)null)
						.setMissings(42).setValueSetRelation(SetRelation.SUPERSET).addMissings(missings).mergeValueSetRelation(SetRelation.SUBSET);
		assertEquals(47, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.AT_LEAST, info.getMissingValues().getRelation());
		assertFalse(info.getType().isPresent());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false), info.getDictionary());
		assertEquals(MetaDataInfo.UNKNOWN, info.isNominal());
		assertEquals(MetaDataInfo.UNKNOWN, info.isNumeric());
		assertEquals(MetaDataInfo.UNKNOWN, info.isObject());
		assertEquals(MetaDataInfo.YES, info.hasMissingValues());
		assertEquals(MetaDataInfo.UNKNOWN, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(LocalTime.class).isPresent());
	}

	@Test
	public void testCopyAndAdd() {
		final ColumnInfo build =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(-1).setBooleanDictionaryValues("bla", "blup").build();
		final ColumnInfoBuilder info = new ColumnInfoBuilder(build).addDictionaryValues(Arrays.asList("na", "nu"));
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.EQUAL, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(new TreeSet<>(Arrays.asList("bla", "blup", "na", "nu")), false),
				info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testCopyAndSet() {
		final ColumnInfo build =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setMissings(-1).setBooleanDictionaryValues("bla", "blup").build();
		final ColumnInfo build2 =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setDictionaryValues(Arrays.asList("la", "lup", "na", "nu"),
						SetRelation.EQUAL).build();
		final ColumnInfoBuilder info = new ColumnInfoBuilder(build).setDictionary(build2.getDictionary(),
				SetRelation.SUBSET);
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.NOMINAL, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(new TreeSet<>(Arrays.asList("la", "lup", "na", "nu")), false),
				info.getDictionary());
		assertEquals(MetaDataInfo.YES, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testCopyAndSetWrongType() {
		final ColumnInfo build =
				new ColumnInfoBuilder(ColumnType.TEXT).setMissings(-1).setBooleanDictionaryValues("bla", "blup").build();
		final ColumnInfo build2 =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setDictionaryValues(Arrays.asList("la", "lup", "na", "nu"),
						SetRelation.EQUAL).build();
		final ColumnInfo info = new ColumnInfoBuilder(build).setDictionary(build2.getDictionary(),
				SetRelation.SUBSET).build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.TEXT, info.getType().get());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false),
				info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testCopyAndSetWrongTypeBuilder() {
		final ColumnInfo build =
				new ColumnInfoBuilder(ColumnType.TEXT).setMissings(-1).setBooleanDictionaryValues("bla", "blup").build();
		final ColumnInfo build2 =
				new ColumnInfoBuilder(ColumnType.NOMINAL).setDictionaryValues(Arrays.asList("la", "lup", "na", "nu"),
						SetRelation.EQUAL).build();
		final ColumnInfoBuilder info = new ColumnInfoBuilder(build).setDictionary(build2.getDictionary(),
				SetRelation.SUBSET);
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.TEXT, info.getType().get());
		assertEquals(SetRelation.SUBSET, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(new HashSet<>(Arrays.asList("la", "lup", "na", "nu")), false),
				info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.NO, info.isNumeric());
		assertEquals(MetaDataInfo.YES, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(String.class).isPresent());
	}

	@Test
	public void testCopyAndSetLegacyRange() {
		final ColumnInfo build =
				new ColumnInfoBuilder(ColumnType.REAL).setMissings(-1).setBooleanDictionaryValues("bla", "blup")
						.setNumericRange(new Range(3,5), SetRelation.SUPERSET).build();
		final ColumnInfo info = new ColumnInfoBuilder(build).setNumericRange(new Range(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), SetRelation.SUBSET).build();
		assertEquals(0, (int) info.getMissingValues().getNumber());
		assertEquals(MDNumber.Relation.EQUAL, info.getMissingValues().getRelation());
		assertEquals(ColumnType.REAL, info.getType().get());
		assertEquals(SetRelation.UNKNOWN, info.getValueSetRelation());
		assertEquals(new DictionaryInfo(Collections.emptySet(), false),
				info.getDictionary());
		assertEquals(MetaDataInfo.NO, info.isNominal());
		assertEquals(MetaDataInfo.YES, info.isNumeric());
		assertEquals(MetaDataInfo.NO, info.isObject());
		assertEquals(MetaDataInfo.NO, info.hasMissingValues());
		assertEquals(MetaDataInfo.NO, info.isAtMostBicategorical());
		assertFalse(info.getDictionary().valueSetWasShrunk());
		assertFalse(info.getNumericRange().isPresent());
		assertFalse(info.getObjectRange(Void.class).isPresent());
	}
}
