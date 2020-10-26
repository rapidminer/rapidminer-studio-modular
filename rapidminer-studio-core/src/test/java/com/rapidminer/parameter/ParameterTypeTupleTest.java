/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.parameter;

import static com.rapidminer.test_utils.RapidAssert.assertArrayEquals;
import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.rapidminer.tools.container.Pair;


/**
 * Test the parameter type tuple
 */
public class ParameterTypeTupleTest {

	@Test
	public void testParameterTypeTuple() {
		//final char internalSeparator = Parameters.PAIR_SEPARATOR;
		final char internalSeparator = '.';
		assertArrayEquals(new String[]{"fi" + internalSeparator + "rst", "sec" + internalSeparator + "ond"}, ParameterTypeTupel.transformString2Tupel("fi\\" + internalSeparator + "rst" + internalSeparator + "sec\\" + internalSeparator + "ond"));

		assertEquals("fi\\" + internalSeparator + "rst" + internalSeparator + "sec\\" + internalSeparator + "ond", ParameterTypeTupel.transformTupel2String("fi" + internalSeparator + "rst", "sec" + internalSeparator + "ond"));
		assertEquals("fi\\" + internalSeparator + "rst" + internalSeparator + "sec\\" + internalSeparator + "ond", ParameterTypeTupel.transformTupel2String(new String[]{"fi" + internalSeparator + "rst", "sec" + internalSeparator + "ond"}));
		assertEquals("fi\\" + internalSeparator + "rst" + internalSeparator + "sec\\" + internalSeparator + "ond", ParameterTypeTupel.transformTupel2String(new Pair<>("fi" + internalSeparator + "rst", "sec" + internalSeparator + "ond")));
	}
}
