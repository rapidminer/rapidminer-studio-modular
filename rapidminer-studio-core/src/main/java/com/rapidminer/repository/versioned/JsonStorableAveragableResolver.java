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
package com.rapidminer.repository.versioned;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rapidminer.example.AttributeWeight;
import com.rapidminer.operator.performance.AbsoluteError;
import com.rapidminer.operator.performance.AreaUnderCurve;
import com.rapidminer.operator.performance.AreaUnderCurve.Neutral;
import com.rapidminer.operator.performance.AreaUnderCurve.Optimistic;
import com.rapidminer.operator.performance.AreaUnderCurve.Pessimistic;
import com.rapidminer.operator.performance.BinaryClassificationPerformance;
import com.rapidminer.operator.performance.CorrelationCriterion;
import com.rapidminer.operator.performance.CrossEntropy;
import com.rapidminer.operator.performance.EstimatedPerformance;
import com.rapidminer.operator.performance.LenientRelativeError;
import com.rapidminer.operator.performance.LogisticLoss;
import com.rapidminer.operator.performance.MDLCriterion;
import com.rapidminer.operator.performance.Margin;
import com.rapidminer.operator.performance.MinMaxCriterion;
import com.rapidminer.operator.performance.MultiClassificationPerformance;
import com.rapidminer.operator.performance.NormalizedAbsoluteError;
import com.rapidminer.operator.performance.PredictionAverage;
import com.rapidminer.operator.performance.RankCorrelation;
import com.rapidminer.operator.performance.RelativeError;
import com.rapidminer.operator.performance.RootMeanSquaredError;
import com.rapidminer.operator.performance.RootRelativeSquaredError;
import com.rapidminer.operator.performance.SimpleClassificationError;
import com.rapidminer.operator.performance.SoftMarginLoss;
import com.rapidminer.operator.performance.SquaredCorrelationCriterion;
import com.rapidminer.operator.performance.SquaredError;
import com.rapidminer.operator.performance.StrictRelativeError;
import com.rapidminer.operator.performance.WeightedMultiClassPerformance;
import com.rapidminer.operator.performance.cost.ClassificationCostCriterion;
import com.rapidminer.operator.performance.cost.RankingCriterion;
import com.rapidminer.tools.math.Averagable;


/**
 * Used to resolve json type ids to (sub)classes for {@link Averagable}s, stored as parts of a {@link
 * com.rapidminer.operator.performance.PerformanceVector PerformanceVector}. Every (non-abstract) subclass of {@link
 * com.rapidminer.tools.math.Averagable Averagable} must be registered here if it should be writable as json.
 *
 * @author Jan Czogalla
 * @see JsonStorableIOObject
 * @since 9.10.0
 */
public class JsonStorableAveragableResolver extends JsonResolverWithRegistry<Averagable> {

	private static final Map<String, Class<? extends Averagable>> nameToClass = new ConcurrentHashMap<>();
	private static final Map<Class<? extends Averagable>, String> classToName = new ConcurrentHashMap<>();

	public static final JsonStorableAveragableResolver INSTANCE = new JsonStorableAveragableResolver();

	static {
		//register core classes here
		INSTANCE.register(AttributeWeight.class);
		INSTANCE.register(RankCorrelation.class);
		INSTANCE.register(Margin.class);
		INSTANCE.register(NormalizedAbsoluteError.class);
		INSTANCE.register(AreaUnderCurve.class);
		INSTANCE.register(Optimistic.class);
		INSTANCE.register(Neutral.class);
		INSTANCE.register(Pessimistic.class);
		INSTANCE.register(WeightedMultiClassPerformance.class);
		INSTANCE.register(MDLCriterion.class);
		INSTANCE.register(PredictionAverage.class);
		INSTANCE.register(SoftMarginLoss.class);
		INSTANCE.register(MinMaxCriterion.class);
		INSTANCE.register(RankingCriterion.class);
		INSTANCE.register(LogisticLoss.class);
		INSTANCE.register(RelativeError.class);
		INSTANCE.register(SimpleClassificationError.class);
		INSTANCE.register(LenientRelativeError.class);
		INSTANCE.register(SquaredError.class);
		INSTANCE.register(RootMeanSquaredError.class);
		INSTANCE.register(AbsoluteError.class);
		INSTANCE.register(StrictRelativeError.class);
		INSTANCE.register(CorrelationCriterion.class);
		INSTANCE.register(SquaredCorrelationCriterion.class);
		INSTANCE.register(RootRelativeSquaredError.class);
		INSTANCE.register(BinaryClassificationPerformance.class);
		INSTANCE.register(CrossEntropy.class);
		INSTANCE.register(MultiClassificationPerformance.class);
		INSTANCE.register(ClassificationCostCriterion.class);
		INSTANCE.register(EstimatedPerformance.class);
	}

	protected JsonStorableAveragableResolver() {
		super(nameToClass, classToName);
	}
}
