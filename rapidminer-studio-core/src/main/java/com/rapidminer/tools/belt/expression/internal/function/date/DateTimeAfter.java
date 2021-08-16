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
package com.rapidminer.tools.belt.expression.internal.function.date;

import java.time.Instant;


/**
 * Returns {@code true} if left is a point in time after right. Returns {@code null} if left or right are {@code
 * null}.
 *
 * @author Kevin Majchrzak
 * @since 9.11
 */
public class DateTimeAfter extends Abstract2DateInputBooleanOutput {

	public DateTimeAfter() {
		super("date.date_after");
	}

	/**
	 * @return {@code true} if left is a point in time after right. Returns {@code null} if left or right are {@code
	 * null}.
	 */
	@Override
	protected Boolean compute(Instant left, Instant right) {
		if (left == null || right == null) {
			return null;
		}
		return left.isAfter(right);
	}

}
