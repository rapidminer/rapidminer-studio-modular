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
package com.rapidminer.tools.math.similarity.nominal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * Tests for {@link NominalDistance}.
 *
 * @author Kevin Majchrzak
 * @since 9.8
 */
public class NominalDistanceTest {

	/**
	 * Used for double equality checks.
	 */
	public static final double DELTA = 0.00001d;

	@Test
	public void testExampleSetInit() throws OperatorException {
		ExampleSet e1 = testExampleSets()[0];

		NominalDistance distance = new NominalDistance();
		assertFalse(distance.initialized);
		assertFalse(distance.isComparable);
		distance.init(e1);
		assertTrue(distance.initialized);
		assertTrue(distance.isComparable);

		for (Example r : e1) {
			assertEquals(0d, distance.calculateDistance(r, r), DELTA);
			assertEquals(0d, distance.calculateSimilarity(r, r), DELTA);
		}

		double[] manualMapping = new double[]{1.0, 0.0, 0.0};
		assertEquals(0.0, distance.calculateDistance(e1.getExample(0), manualMapping), DELTA);
		assertEquals(0.0, distance.calculateSimilarity(e1.getExample(0), manualMapping), DELTA);

		assertEquals(3.0, distance.calculateDistance(e1.getExample(1), manualMapping), DELTA);
		assertEquals(-3.0, distance.calculateSimilarity(e1.getExample(1), manualMapping), DELTA);

		assertEquals(3.0, distance.calculateDistance(e1.getExample(2), manualMapping), DELTA);
		assertEquals(-3.0, distance.calculateSimilarity(e1.getExample(2), manualMapping), DELTA);
	}

	@Test
	public void testAttributesInit() throws OperatorException {
		ExampleSet[] sets = testExampleSets();
		ExampleSet e1 = sets[0];
		ExampleSet e2 = sets[1];

		NominalDistance distance = new NominalDistance();
		assertFalse(distance.initialized);
		assertFalse(distance.isComparable);
		distance.init(e1.getAttributes(), e2.getAttributes());
		assertTrue(distance.initialized);
		assertTrue(distance.isComparable);

		assertEquals(1d, distance.calculateDistance(e1.getExample(0), e2.getExample(0)), DELTA);
		assertEquals(-1d, distance.calculateSimilarity(e1.getExample(0), e2.getExample(0)), DELTA);

		assertEquals(2d, distance.calculateDistance(e1.getExample(1), e2.getExample(1)), DELTA);
		assertEquals(-2d, distance.calculateSimilarity(e1.getExample(1), e2.getExample(1)), DELTA);

		assertEquals(1d, distance.calculateDistance(e1.getExample(2), e2.getExample(2)), DELTA);
		assertEquals(-1d, distance.calculateSimilarity(e1.getExample(2), e2.getExample(2)), DELTA);
	}

	@Test(expected = UserError.class)
	public void testUserError() throws OperatorException {
		Attribute a = AttributeFactory.createAttribute("a", Ontology.NOMINAL);
		a.getMapping().mapString("dog");
		a.getMapping().mapString("cat");
		a.getMapping().mapString("mouse");

		Attribute b = AttributeFactory.createAttribute("b", Ontology.REAL);

		ExampleSetBuilder builder = ExampleSets.from(a, b);
		builder.addRow(new double[]{0.0, 0.0});
		ExampleSet setWithNumeric = builder.build();

		DistanceMeasure distance = new NominalDistance();
		distance.init(setWithNumeric);
	}

	@Test(expected = IllegalStateException.class)
	public void testNotInitialized() {
		ExampleSet[] sets = testExampleSets();
		ExampleSet e1 = sets[0];
		DistanceMeasure distance = new NominalDistance();
		distance.calculateDistance(e1.getExample(0), new double[]{0.0, 0.0, 0.0});
	}

	@Test
	public void testNotComparable() {
		ExampleSet e1 = testExampleSets()[0];
		ExampleSet e2 = numericExampleSet();

		NominalDistance distance = new NominalDistance();
		assertFalse(distance.initialized);
		assertFalse(distance.isComparable);
		distance.init(e1.getAttributes(), e2.getAttributes());
		assertTrue(distance.initialized);
		assertFalse(distance.isComparable);

		int index = 0;
		for (Example r : e1) {
			assertEquals(Double.NaN, distance.calculateDistance(r, e2.getExample(index)), DELTA);
			assertEquals(Double.NaN, distance.calculateSimilarity(r, e2.getExample(index)), DELTA);
			index++;
		}

	}

	private ExampleSet numericExampleSet() {
		Attribute a1 = AttributeFactory.createAttribute("a", Ontology.NOMINAL);
		a1.getMapping().mapString("dog");
		a1.getMapping().mapString("cat");
		a1.getMapping().mapString("mouse");

		Attribute b1 = AttributeFactory.createAttribute("b", Ontology.NOMINAL);
		b1.getMapping().mapString("dog");
		b1.getMapping().mapString("cat");
		b1.getMapping().mapString("mouse");

		Attribute c1 = AttributeFactory.createAttribute("c", Ontology.REAL);

		ExampleSetBuilder builder1 = ExampleSets.from(a1, b1, c1);
		builder1.addRow(new double[]{a1.getMapping().getIndex("cat"),
				b1.getMapping().getIndex("dog"), 0.0});
		builder1.addRow(new double[]{a1.getMapping().getIndex("dog"),
				b1.getMapping().getIndex("cat"), 1.0});
		builder1.addRow(new double[]{a1.getMapping().getIndex("mouse"),
				b1.getMapping().getIndex("mouse"), 0.5});

		return builder1.build();
	}

	private ExampleSet[] testExampleSets() {
		// used to check that distance of equal values is 0 regardless of mapping
		Attribute a1 = AttributeFactory.createAttribute("a", Ontology.NOMINAL);
		a1.getMapping().mapString("dog");
		a1.getMapping().mapString("cat");
		a1.getMapping().mapString("mouse");

		Attribute a2 = AttributeFactory.createAttribute("a", Ontology.NOMINAL);
		a2.getMapping().mapString("cat");
		a2.getMapping().mapString("mouse");
		a2.getMapping().mapString("dog");

		// used to check that distance between non-equal values is 1 regardless of mapping
		Attribute b1 = AttributeFactory.createAttribute("b", Ontology.NOMINAL);
		b1.getMapping().mapString("dog");
		b1.getMapping().mapString("cat");
		b1.getMapping().mapString("mouse");

		Attribute b2 = AttributeFactory.createAttribute("b", Ontology.NOMINAL);
		b2.getMapping().mapString("cat");
		b2.getMapping().mapString("dog");
		b2.getMapping().mapString("mouse");

		// used to check that distance between values that occur in only one mapping is 1
		Attribute c1 = AttributeFactory.createAttribute("c", Ontology.NOMINAL);
		c1.getMapping().mapString("dog");
		c1.getMapping().mapString("cat");
		c1.getMapping().mapString("mouse");

		Attribute c2 = AttributeFactory.createAttribute("c", Ontology.NOMINAL);
		c2.getMapping().mapString("dog");
		c2.getMapping().mapString("cat1");
		c2.getMapping().mapString("mouse1");

		// the order of the attributes should not matter for the distance
		ExampleSetBuilder builder1 = ExampleSets.from(a1, b1, c1);
		ExampleSetBuilder builder2 = ExampleSets.from(b2, c2, a2);

		// number of non-equal value pairs: 1
		builder1.addRow(new double[]{a1.getMapping().getIndex("cat"),
				b1.getMapping().getIndex("dog"), c1.getMapping().getIndex("dog")});
		builder2.addRow(new double[]{b2.getMapping().getIndex("cat"), c2.getMapping().getIndex("dog"),
				a2.getMapping().getIndex("cat")
		});

		// number of non-equal value pairs: 2
		builder1.addRow(new double[]{a1.getMapping().getIndex("dog"),
				b1.getMapping().getIndex("cat"), c1.getMapping().getIndex("cat")});
		builder2.addRow(new double[]{b2.getMapping().getIndex("dog"), c2.getMapping().getIndex("cat1"),
				a2.getMapping().getIndex("dog")
		});

		// number of non-equal value pairs: 1
		builder1.addRow(new double[]{a1.getMapping().getIndex("mouse"),
				b1.getMapping().getIndex("mouse"), c1.getMapping().getIndex("mouse")});
		ExampleSet e1 = builder1.build();
		builder2.addRow(new double[]{b2.getMapping().getIndex("mouse"), c2.getMapping().getIndex("mouse1"),
				a2.getMapping().getIndex("mouse")
		});
		ExampleSet e2 = builder2.build();

		return new ExampleSet[]{e1, e2};
	}

}
