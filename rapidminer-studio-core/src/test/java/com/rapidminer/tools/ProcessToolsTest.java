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
package com.rapidminer.tools;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.Process;
import com.rapidminer.TestUtils;


/**
 * Tests for the {@link ProcessTools} utility class.
 *
 * @author Jan Czogalla
 * @since 9.6
 */
public class ProcessToolsTest {

	@BeforeClass
	public static void setup() throws Exception {
		TestUtils.INSTANCE.minimalProcessUsageSetup();
	}

	/**
	 * Test that parent process propagation works as expected
	 */
	@Test
	public void testProcessParent() {
		Process process = new Process();
		assertSame(process, ProcessTools.getParentProcess(process));
		Process child = new Process();
		assertNotSame(process, child);
		ProcessTools.setParentProcess(child, process);
		assertSame(process, ProcessTools.getParentProcess(child));
		Process grandChild = new Process();
		assertNotSame(process, grandChild);
		assertNotSame(child, grandChild);
		ProcessTools.setParentProcess(grandChild, child);
		assertSame(process, ProcessTools.getParentProcess(grandChild));
	}
}