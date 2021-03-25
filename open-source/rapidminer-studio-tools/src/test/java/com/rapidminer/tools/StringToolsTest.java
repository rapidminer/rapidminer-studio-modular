/**
 * Copyright (C) 2001-2021 by RapidMiner and the contributors
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
package com.rapidminer.tools;


import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;


/**
 * Tests {@link StringTools#escape(String, char, char[])} and {@link StringTools#unescape(String, char, char[], char)}.
 *
 * @author Simon Fischer
 */
public class StringToolsTest {

	@Test
	public void testEscape() {
		assertEquals("test\\\\tost", StringTools.escape("test\\tost", '\\', new char[0]));
		assertEquals("test\\\ntost", StringTools.escape("test\ntost", '\\', new char[]{'\n'}));
		assertEquals("one\\.two\\.three\\.\\.five", StringTools.escape("one.two.three..five", '\\', new char[]{'.'}));
	}

	@Test
	public void testUnescape() {
		List<String> result = new LinkedList<>();
		result.add("test\\tost");
		assertEquals(result, StringTools.unescape("test\\\\tost", '\\', new char[]{'\\'}, (char) -1));

		result = new LinkedList<>();
		result.add("line1");
		result.add("line.two");
		result.add("");
		result.add("last.line");
		assertEquals(result, StringTools.unescape("line1.line\\.two..last\\.line", '\\', new char[0], '.'));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testException() {
		StringTools.unescape("illegal\\escape character", '\\', new char[]{'a', 'b'}, (char) -1);
	}
}
