/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.gui.look.fc;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.JFileChooser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * Test class for {@link Item}. Mainly to test {@link Item#compareTo(Item)} functionality
 *
 * @author Jonas Wilms-Pfau, Jan Czogalla
 * @since 9.8
 */
public class ItemTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Test
	public void multiTestCompareToSameRobust() throws Exception {
		JFileChooser fileChooser = new JFileChooser();
		FileList list = new FileList(new FileChooserUI(fileChooser), fileChooser);
		ItemPanel panel = new ItemPanel(list);
		Item c = new Item(panel, testFolder.newFolder("c"));
		Item b = new Item(panel, testFolder.newFolder("b"));
		Item a = new Item(panel, testFolder.newFile("a"));
		for (int i = 0; i < 100; i++) {
			try {
				compareToSameRobust(c, b, a);
			} catch (Exception e) {
				throw new Exception("Error on run " + i, e);
			}
		}
	}

	/**
	 * Tests robustness of {@link Item#compareTo(Item)} method wrt. sameness. Makes sure that the
	 * {@link java.util.ComparableTimSort ComparableTimSort} does not throw an error.
	 * @param c
	 * @param b
	 * @param a
	 */
	private void compareToSameRobust(Item c, Item b, Item a) throws IOException {
		Item[] items = new Item[33];
		Arrays.setAll(items, (i) -> (i > 17) ? c : b);
		items[1] = a;
		Arrays.sort(items);
	}

	@Test
	public void provokeSamenessError() throws IOException {
		class TestItem extends Item {

			public TestItem(ItemPanel parent, File f) {
				super(parent, f);
			}

			@Override
			public int compareTo(Item other) {
				int res = super.compareTo(other);
				// old behavior
				return res == 0 ? -1 : res;
			}
		}

		JFileChooser fileChooser = new JFileChooser();
		FileList list = new FileList(new FileChooserUI(fileChooser), fileChooser);
		ItemPanel panel = new ItemPanel(list);
		Item c = new TestItem(panel, testFolder.newFolder("cs"));
		Item b = new TestItem(panel, testFolder.newFolder("bs"));
		Item a = new TestItem(panel, testFolder.newFile("as"));
		try {
			for (int i = 0; i < 100; i++) {
				compareToSameRobust(c, b, a);
			}
			fail("No error occured");
		} catch (IllegalArgumentException e) {
			if (!"Comparison method violates its general contract!".equals(e.getMessage())) {
				fail("Wrong error: " + e.getMessage());
			}
		}
	}
}