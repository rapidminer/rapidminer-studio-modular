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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeSet;

import org.junit.Test;

import com.rapidminer.operator.ports.metadata.MetaDataInfo;


/**
 * Tests the {@link DictionaryInfo} and the {@link BooleanDictionaryInfo}.
 *
 * @author Gisa Meier
 * @since 9.9.0
 */
public class DictionaryInfoTest {

	@Test
	public void testNominal() {
		final DictionaryInfo dictionaryInfo = new DictionaryInfo(new TreeSet<>(Arrays.asList("a", "b", "c")), false);
		assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), dictionaryInfo.getValueSet());
		assertEquals(MetaDataInfo.NO, dictionaryInfo.hasNegative());
		assertEquals(MetaDataInfo.NO, dictionaryInfo.hasPositive());
		assertFalse(dictionaryInfo.getNegativeValue().isPresent());
		assertFalse(dictionaryInfo.getPositiveValue().isPresent());
		assertFalse(dictionaryInfo.isBoolean());
	}


	@Test
	public void testBooleanBoth() {
		final DictionaryInfo dictionaryInfo = new BooleanDictionaryInfo("bla", "blup");
		assertEquals(new HashSet<>(Arrays.asList("bla", "blup")), dictionaryInfo.getValueSet());
		assertEquals(MetaDataInfo.YES, dictionaryInfo.hasNegative());
		assertEquals(MetaDataInfo.YES, dictionaryInfo.hasPositive());
		assertTrue(dictionaryInfo.getNegativeValue().isPresent());
		assertTrue(dictionaryInfo.getPositiveValue().isPresent());
		assertEquals("bla",dictionaryInfo.getPositiveValue().get());
		assertEquals("blup",dictionaryInfo.getNegativeValue().get());
		assertTrue(dictionaryInfo.isBoolean());
	}

	@Test
	public void testBooleanPositive() {
		final DictionaryInfo dictionaryInfo = new BooleanDictionaryInfo("bla", null);
		assertEquals(new HashSet<>(Collections.singletonList("bla")), dictionaryInfo.getValueSet());
		assertEquals(MetaDataInfo.NO, dictionaryInfo.hasNegative());
		assertEquals(MetaDataInfo.YES, dictionaryInfo.hasPositive());
		assertFalse(dictionaryInfo.getNegativeValue().isPresent());
		assertTrue(dictionaryInfo.getPositiveValue().isPresent());
		assertEquals("bla",dictionaryInfo.getPositiveValue().get());
		assertTrue(dictionaryInfo.isBoolean());
	}

	@Test
	public void testBooleanNegative() {
		final DictionaryInfo dictionaryInfo = new BooleanDictionaryInfo(null, "blup");
		assertEquals(new HashSet<>(Collections.singletonList("blup")), dictionaryInfo.getValueSet());
		assertEquals(MetaDataInfo.YES, dictionaryInfo.hasNegative());
		assertEquals(MetaDataInfo.NO, dictionaryInfo.hasPositive());
		assertTrue(dictionaryInfo.getNegativeValue().isPresent());
		assertFalse(dictionaryInfo.getPositiveValue().isPresent());
		assertEquals("blup",dictionaryInfo.getNegativeValue().get());
		assertTrue(dictionaryInfo.isBoolean());
	}

	@Test
	public void testBooleanNone() {
		final DictionaryInfo dictionaryInfo = new BooleanDictionaryInfo(null, null);
		assertEquals(Collections.emptySet(), dictionaryInfo.getValueSet());
		assertEquals(MetaDataInfo.NO, dictionaryInfo.hasNegative());
		assertEquals(MetaDataInfo.NO, dictionaryInfo.hasPositive());
		assertFalse(dictionaryInfo.getNegativeValue().isPresent());
		assertFalse(dictionaryInfo.getPositiveValue().isPresent());
		assertTrue(dictionaryInfo.isBoolean());
	}

	@Test
	public void testBooleanUnknown() {
		final DictionaryInfo dictionaryInfo = new BooleanDictionaryInfo();
		assertEquals(Collections.emptySet(), dictionaryInfo.getValueSet());
		assertEquals(MetaDataInfo.UNKNOWN, dictionaryInfo.hasNegative());
		assertEquals(MetaDataInfo.UNKNOWN, dictionaryInfo.hasPositive());
		assertFalse(dictionaryInfo.getNegativeValue().isPresent());
		assertFalse(dictionaryInfo.getPositiveValue().isPresent());
		assertTrue(dictionaryInfo.isBoolean());
	}
}
