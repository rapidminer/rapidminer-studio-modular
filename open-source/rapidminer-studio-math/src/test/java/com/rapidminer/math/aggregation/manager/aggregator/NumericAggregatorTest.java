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
package com.rapidminer.math.aggregation.manager.aggregator;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


/**
 * Test the numeric aggregators.
 *
 * @author Gisa Meier
 */
@RunWith(Enclosed.class)
public class NumericAggregatorTest {

	private static double[] random(int size) {
		double[] array = new double[size];
		Arrays.setAll(array, i -> Math.random());
		return array;
	}


	@RunWith(Parameterized.class)
	public static class Result {

		@Parameter
		public Supplier<NumericAggregator> supplier;

		@Parameter(value = 1)
		public String name;

		@Parameter(value = 2)
		public Function<double[], Double> resultCalculator;

		@Parameters(name = "{1}")
		public static Iterable<Object[]> columnImplementations() {
			return Arrays.asList(
					new Object[]{(Supplier<NumericAggregator>) MinAggregator::new, "min",
							(Function<double[], Double>) d -> Arrays.stream(d).min().getAsDouble()},
					new Object[]{(Supplier<NumericAggregator>) MaxAggregator::new, "max",
							(Function<double[], Double>) d -> Arrays.stream(d).max().getAsDouble()},
					new Object[]{(Supplier<NumericAggregator>) AverageAggregator::new, "average",
							(Function<double[], Double>) d -> Arrays.stream(d).average().getAsDouble()},
					new Object[]{(Supplier<NumericAggregator>) MedianAggregator::new, "median",
							(Function<double[], Double>) d -> new Median().evaluate(d)},
					new Object[]{(Supplier<NumericAggregator>) ProductAggregator::new, "product",
							(Function<double[], Double>) d -> Arrays.stream(d).reduce(1, (a, b) -> a * b)},
					new Object[]{(Supplier<NumericAggregator>) PercentileAggregator::new, "percentile_default",
							(Function<double[], Double>) d -> {
								Percentile percentileCalc = new Percentile();
								percentileCalc.setData(d);
								return percentileCalc.evaluate(75d);
							}},
					// 50-th percentile is the median, so cross-check with median result here
					new Object[]{(Supplier<NumericAggregator>) () -> {
						PercentileAggregator percentileAggregator = new PercentileAggregator();
						percentileAggregator.setAggregationParameter(50d);
						return percentileAggregator;
					}, "percentile_50",
							(Function<double[], Double>) d -> new Median().evaluate(d)},
					new Object[]{(Supplier<NumericAggregator>) LogProductAggregator::new, "logProduct",
							(Function<double[], Double>) d -> Arrays.stream(d).reduce(0, (a, b) -> a + Math.log(b))},
					new Object[]{(Supplier<NumericAggregator>) SumAggregator::new, "sum",
							(Function<double[], Double>) d -> Arrays.stream(d).sum()},
					new Object[]{(Supplier<NumericAggregator>) VarianceAggregator::new, "variance",
							(Function<double[], Double>) d -> new Variance().evaluate(d)},
					new Object[]{(Supplier<NumericAggregator>) StandardDeviationAggregator::new, "std",
							(Function<double[], Double>) d -> new StandardDeviation().evaluate(d)},
					new Object[]{(Supplier<NumericAggregator>) ModeAggregator::new, "mode",
							(Function<double[], Double>) d -> StatUtils.mode(d)[0]},
					new Object[]{(Supplier<NumericAggregator>) LeastAggregator::new, "least",
							(Function<double[], Double>) d -> getLeast(d)}
			);
		}

		/**
		 * A least analog to {@link StatUtils#mode}
		 */
		private static double getLeast(double[] values) {
			Frequency freq = new Frequency();

			for (int i = 0; i < values.length; ++i) {
				double value = values[i];
				if (!Double.isNaN(value)) {
					freq.addValue(value);
				}
			}

			long leastPopular = Long.MAX_VALUE;
			Iterator<Map.Entry<Comparable<?>, Long>> entryIterator = freq.entrySetIterator();

			while (entryIterator.hasNext()) {
				Long l = (Long) entryIterator.next().getValue();
				long frequency = l;
				if (frequency < leastPopular) {
					leastPopular = frequency;
				}
			}

			List<Comparable<?>> leastList = new ArrayList();
			Iterator i$ = freq.entrySetIterator();

			while (i$.hasNext()) {
				Map.Entry<Comparable<?>, Long> ent = (Map.Entry) i$.next();
				long frequency = (Long) ent.getValue();
				if (frequency == leastPopular) {
					leastList.add(ent.getKey());
				}
			}

			return (Double) leastList.get(0);
		}

		@Test
		public void testSimple() {
			double[] values = random(713);
			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			assertEquals(resultCalculator.apply(values), fullValue, 1e-10);
		}

		@Test
		public void testWithPositiveInfitiy() {
			double[] values = random(713);
			values[517] = Double.POSITIVE_INFINITY;
			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			assertEquals(resultCalculator.apply(values), fullValue, 1e-10);
		}

		@Test
		public void testWithNegativeInfitiy() {
			double[] values = random(713);
			values[517] = Double.NEGATIVE_INFINITY;
			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			assertEquals(resultCalculator.apply(values), fullValue, 1e-10);
		}

		@Test
		public void testWithMaxValue() {
			double[] values = random(713);
			values[123] = Double.MAX_VALUE;
			values[517] = Double.MAX_VALUE;
			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			assertEquals(resultCalculator.apply(values), fullValue, 1e-10);
		}

		@Test
		public void testWithOneNegative() {
			double[] values = random(713);
			values[123] = -1;
			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			assertEquals(resultCalculator.apply(values), fullValue, 1e-10);
		}

		@Test
		public void testBiggerRange() {
			double[] values = new double[541];
			Random random = new Random();
			Arrays.setAll(values, i -> -500 + random.nextDouble() * 1000);


			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			assertEquals(resultCalculator.apply(values), fullValue, 1e-5);
		}

	}

	@RunWith(Parameterized.class)
	public static class Merge {

		@Parameter
		public Supplier<NumericAggregator> supplier;

		@Parameter(value = 1)
		public String name;

		@Parameters(name = "{1}")
		public static Iterable<Object[]> columnImplementations() {
			return Arrays.asList(
					new Object[]{(Supplier<NumericAggregator>) MinAggregator::new, "min"},
					new Object[]{(Supplier<NumericAggregator>) MaxAggregator::new, "max"},
					new Object[]{(Supplier<NumericAggregator>) AverageAggregator::new, "average"},
					new Object[]{(Supplier<NumericAggregator>) MedianAggregator::new, "median"},
					new Object[]{(Supplier<NumericAggregator>) ProductAggregator::new, "product"},
					new Object[]{(Supplier<NumericAggregator>) PercentileAggregator::new, "percentile"},
					new Object[]{(Supplier<NumericAggregator>) LogProductAggregator::new, "logProduct"},
					new Object[]{(Supplier<NumericAggregator>) SumAggregator::new, "sum"},
					new Object[]{(Supplier<NumericAggregator>) VarianceAggregator::new, "variance"},
					new Object[]{(Supplier<NumericAggregator>) StandardDeviationAggregator::new, "std"},
					new Object[]{(Supplier<NumericAggregator>) ModeAggregator::new, "mode"},
					new Object[]{(Supplier<NumericAggregator>) LeastAggregator::new, "least"}
			);
		}

		@Test
		public void testAggregators() {
			double[] values = random(973);
			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			Random random = new Random();
			int split = random.nextInt(values.length - 5);

			NumericAggregator aggregator2 = supplier.get();
			NumericAggregator aggregator3 = supplier.get();
			apply(values, aggregator2, 0, split);
			apply(values, aggregator3, split, values.length);
			aggregator2.merge(aggregator3);

			double mergedValue = aggregator2.getValue();

			assertEquals(fullValue, mergedValue, 1e-10);
		}

		@Test
		public void testEmptyFirst() {
			double[] values = random(973);
			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			int split = 0;

			NumericAggregator aggregator2 = supplier.get();
			NumericAggregator aggregator3 = supplier.get();
			apply(values, aggregator2, 0, split);
			apply(values, aggregator3, split, values.length);
			aggregator2.merge(aggregator3);

			double mergedValue = aggregator2.getValue();

			assertEquals(fullValue, mergedValue, 1e-10);
		}


		@Test
		public void testEmptySecond() {
			double[] values = random(973);
			NumericAggregator aggregator = supplier.get();
			apply(values, aggregator, 0, values.length);
			double fullValue = aggregator.getValue();

			int split = values.length;

			NumericAggregator aggregator2 = supplier.get();
			NumericAggregator aggregator3 = supplier.get();
			apply(values, aggregator2, 0, split);
			apply(values, aggregator3, split, values.length);
			aggregator2.merge(aggregator3);

			double mergedValue = aggregator2.getValue();

			assertEquals(fullValue, mergedValue, 1e-10);
		}

	}


	private static void apply(double[] array, NumericAggregator aggregator, int start, int end) {
		for (int i = start; i < end; i++) {
			aggregator.accept(array[i]);
		}
	}
}
