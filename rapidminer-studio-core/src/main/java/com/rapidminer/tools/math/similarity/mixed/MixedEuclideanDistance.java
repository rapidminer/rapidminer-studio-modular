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
package com.rapidminer.tools.math.similarity.mixed;

import java.util.HashMap;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.tools.container.Pair;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasures;


/**
 * Euclidean distance for numerical and nominal values. For nomimal values, a distance of one is
 * accounted if both values are not the same. Note: In most cases, you must normalize the numerical
 * values, to obtain sound results.
 *
 * @author Sebastian Land, Michael Wurst
 */
public class MixedEuclideanDistance extends DistanceMeasure {

	private static final long serialVersionUID = 536655587492508882L;

	/**
	 * These mappings are used to translate the nominal attribute's mappings to a common mapping so that they become
	 * comparable.
	 */
	private Map<Integer, Map<Double, Integer>> indexMappingSet1;
	private Map<Integer, Map<Double, Integer>> indexMappingSet2;

	/**
	 * True iff this object has been initialized (needs to be done before calling calculateDistance).
	 */
	boolean initialized = false;

	/**
	 * After initialization this indicates whether the type, number and names of the attributes match.
	 */
	boolean isComparable = false;

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		if (!initialized) {
			throw new IllegalStateException("MixedEuclideanDistance is not initialized properly");
		}
		if (!isComparable) {
			return Double.NaN;
		}

		double sum = 0.0;
		int counter = 0;
		for (int i = 0; i < value1.length; i++) {
			if ((!Double.isNaN(value1[i])) && (!Double.isNaN(value2[i]))) {
				if (isNominal(i)) {
					if (!nominalsAreEqual(value1[i], value2[i], i)) {
						sum += 1.0;
					}
				} else {
					double diff = value1[i] - value2[i];
					sum += diff * diff;
				}
				counter++;
			}
		}
		if (counter > 0) {
			return Math.sqrt(sum);
		} else {
			return Double.NaN;
		}
	}

	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		return -calculateDistance(value1, value2);
	}

	@Override
	public DistanceMeasureConfig init(Attributes firstSetAttributes, Attributes secondSetAttributes) {
		DistanceMeasureConfig config = super.init(firstSetAttributes, secondSetAttributes);
		if(config.isMatching()) {
			init(config.getFirstSetAttributes(), config.getSecondSetAttributes());
		} else {
			isComparable = false;
		}
		initialized = true;
		return config;
	}

	@Override
	public String toString() {
		return "Mixed euclidean distance";
	}

	/**
	 * Fills indexMappingSet1 and indexMappingSet2. These mappings are used to translate the attribute mappings to a
	 * common mapping so that they become comparable. Also checks that nominal values are not compared with numerical
	 * values.
	 *
	 * @since 9.8
	 */
	private void init(Attribute[] attributes1, Attribute[] attributes2) {
		indexMappingSet1 = new HashMap<>();
		indexMappingSet2 = new HashMap<>();
		int length = attributes1.length;

		for (int i = 0; i < length; i++) {
			Attribute attribute1 = attributes1[i];
			Attribute attribute2 = attributes2[i];
			if(attribute1.isNominal() && attribute2.isNominal()) {
				Pair<Map<Double, Integer>, Map<Double, Integer>> mappings
						= DistanceMeasures.createCommonMapping(attribute1, attribute2);
				indexMappingSet1.put(i, mappings.getFirst());
				indexMappingSet2.put(i, mappings.getSecond());
			} else if(attribute1.isNominal() || attribute2.isNominal()){
				// cannot compare nominal with numerical value
				indexMappingSet1.clear();
				indexMappingSet2.clear();
				isComparable = false;
				return;
			}
		}

		isComparable = true;
	}

	/**
	 * True iff the attribute with the given index is nominal.
	 *
	 * @since 9.8
	 */
	private boolean isNominal(int i) {
		return indexMappingSet1.containsKey(i);
	}

	/**
	 * True iff the nominal values corresponding to the given indices are equal.
	 *
	 * @since 9.8
	 */
	private boolean nominalsAreEqual(double index, double index2, int i) {
		Map<Double, Integer> indexMapping1 = indexMappingSet1.get(i);
		Map<Double, Integer> indexMapping2 = indexMappingSet2.get(i);
		if (indexMapping1.containsKey(index) && indexMapping2.containsKey(index2)) {
			double commonValue1 = indexMapping1.get(index);
			double commonValue2 = indexMapping2.get(index2);
			return commonValue1 == commonValue2;
		}
		return false;
	}
}
