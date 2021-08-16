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
package com.rapidminer.tools.belt.expression.internal.function.eval;

import com.rapidminer.tools.belt.expression.ExpressionContext;
import com.rapidminer.tools.belt.expression.ExpressionEvaluator;


/**
 * TODO add java doc
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class Lag extends Lead {

	public Lag() {
		super("process.lag");
	}

	@Override
	protected ExpressionEvaluator resolve(String attributeName, int offset, ExpressionContext context) {
		ExpressionEvaluator evaluator = context.getDynamicVariable(attributeName, () -> context.getIndex() - offset);
		if (evaluator == null) {
			throw new AttributeEvaluationExceptionWrapper(getFunctionName(), attributeName);
		}
		return evaluator;
	}
}
