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
package com.rapidminer.parameter;

import static com.rapidminer.test_utils.RapidAssert.assertArrayEquals;
import static junit.framework.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;


/**
 * Test the parameter type enumeration
 */
public class ParameterTypeEnumerationTest {

	@Test
	public void testParameterTypeEnumeration() {
		//final char internalRecordSeparator = Parameters.RECORD_SEPARATOR;
		final char internalRecordSeparator = ',';
		assertArrayEquals(
				new String[]{
						"fi" + internalRecordSeparator + "rst",
						"sec" + internalRecordSeparator + "ond",
						"third" + internalRecordSeparator + ""},
				ParameterTypeEnumeration.transformString2Enumeration("fi\\" + internalRecordSeparator + "rst" + internalRecordSeparator + "sec\\" + internalRecordSeparator + "ond" + internalRecordSeparator + "third\\" + internalRecordSeparator + ""));
		List<String> enumeration = new LinkedList<String>();
		enumeration.add("fi" + internalRecordSeparator + "rst");
		enumeration.add("sec" + internalRecordSeparator + "ond");
		enumeration.add("third" + internalRecordSeparator + "");
		assertEquals("fi\\" + internalRecordSeparator + "rst" + internalRecordSeparator + "sec\\" + internalRecordSeparator + "ond" + internalRecordSeparator + "third\\" + internalRecordSeparator + "", ParameterTypeEnumeration.transformEnumeration2String(enumeration));
	}
}
