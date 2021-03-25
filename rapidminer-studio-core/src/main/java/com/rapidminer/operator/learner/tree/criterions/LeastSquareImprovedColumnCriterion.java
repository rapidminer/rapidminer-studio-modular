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
package com.rapidminer.operator.learner.tree.criterions;

/**
 * Same as {@link LeastSquareDistributionColumnCriterion} but does not divide by selection length in the benefit
 * calculation. This division is a problem for the attribute weights. From the formulas the resulting tree should be the
 * same, but due to calculating with double values it can make a small difference, which is the reason for this new
 * class and the compatibility level where it is used.
 *
 * @author Gisa Meier
 * @since 9.9
 */
public class LeastSquareImprovedColumnCriterion extends LeastSquareDistributionColumnCriterion {

	/**
	 * Called via reflection by {@link AbstractColumnCriterion#createColumnCriterion}.
	 */
	public LeastSquareImprovedColumnCriterion() {
	}

	public LeastSquareImprovedColumnCriterion(double minimalGain) {
		setMinimalGain(minimalGain);
	}

	@Override
	protected double makeFinalBenefit(double residualDifference, double totalResidual, double selectionLength) {
		return residualDifference / totalResidual;
	}
}