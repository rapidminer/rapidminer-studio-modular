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
package com.rapidminer.tools.belt.expression.internal.function.statistical;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests the {@link Minimum} function
 *
 * @author Jonas Wilms-Pfau
 */
public class MinimumTest {

	@Test
	public void testMinimum() {
		double smallestValue = -5d;
		double[] values = new double[]{-3d, 1d, smallestValue};
		Assert.assertEquals(smallestValue, new Minimum().compute(values), 0d);
	}

	@Test
	public void testMinimumSingleValue() {
		double smallestValue = -5d;
		double[] values = new double[]{smallestValue};
		Assert.assertEquals(smallestValue, new Minimum().compute(values), 0d);
	}

	@Test
	public void testMinimumEmpty() {
		double[] values = new double[]{};
		Assert.assertTrue(Double.isNaN(new Minimum().compute(values)));
	}

	@Test
	public void testMinimumNull() {
		double[] values = null;
		Assert.assertTrue(Double.isNaN(new Minimum().compute(values)));
	}

	@Test
	public void testMinimumNaN() {
		double[] values = new double[]{Double.NaN};
		Assert.assertTrue(Double.isNaN(new Minimum().compute(values)));
	}

	@Test
	public void testMinimumStartsWithNaN() {
		double[] values = {Double.NaN, 2d, 5d, 0d};
		Assert.assertTrue(Double.isNaN(new Minimum().compute(values)));
	}

	@Test
	public void testMinimumEndsWithNaN() {
		double[] values = {2d, 3d, -1d, Double.NaN};
		Assert.assertTrue(Double.isNaN(new Minimum().compute(values)));
	}
}