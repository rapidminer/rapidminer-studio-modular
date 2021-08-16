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
package com.rapidminer.tools.expression.internal.function.statistical;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests the {@link Maximum} function
 *
 * @author Jonas Wilms-Pfau
 */
public class MaximumTest {

	@Test
	public void testMaximum() {
		double biggestValue = 5d;
		double[] values = new double[]{-3d, 1d, biggestValue};
		Assert.assertEquals(biggestValue, new Maximum().compute(values), 0d);
	}

	@Test
	public void testMaximumSingleValue() {
		double biggestValue = 5d;
		double[] values = new double[]{biggestValue};
		Assert.assertEquals(biggestValue, new Maximum().compute(values), 0d);
	}

	@Test
	public void testMaximumEmpty() {
		double[] values = new double[]{};
		Assert.assertTrue(Double.isNaN(new Maximum().compute(values)));
	}

	@Test
	public void testMaximumNull() {
		double[] values = null;
		Assert.assertTrue(Double.isNaN(new Maximum().compute(values)));
	}

	@Test
	public void testMaximumNaN() {
		double[] values = new double[]{Double.NaN};
		Assert.assertTrue(Double.isNaN(new Maximum().compute(values)));
	}

	@Test
	public void testMaximumStartsWithNaN() {
		double[] values = {Double.NaN, 2d};
		Assert.assertTrue(Double.isNaN(new Maximum().compute(values)));
	}

	@Test
	public void testMaximumEndsWithNaN() {
		double[] values = {2d, Double.NaN};
		Assert.assertTrue(Double.isNaN(new Maximum().compute(values)));
	}
}