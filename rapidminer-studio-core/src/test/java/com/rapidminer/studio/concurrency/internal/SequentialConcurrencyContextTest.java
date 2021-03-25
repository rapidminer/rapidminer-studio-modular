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
package com.rapidminer.studio.concurrency.internal;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.rapidminer.Process;
import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;


/**
 * Tests for {@link SequentialConcurrencyContext}
 *
 * @author Jan Czogalla
 * @since 9.7.1
 */
public class SequentialConcurrencyContextTest {

	private static class ThrowingCallable implements Callable<Void> {

		private final Exception exception;

		private ThrowingCallable(Exception exception) {
			this.exception = exception;
		}

		@Override
		public Void call() throws Exception {
			throw exception;
		}
	}

	/**
	 * Test that exception handling works as expected
	 */
	@Test
	public void testExceptions() {
		Exception[] exceptions = {new RuntimeException("runtime"),
				new ExecutionStoppedException("execution stopped", null),
				new ProcessStoppedRuntimeException(),
				new Exception("exception")};
		Process process = Mockito.mock(Process.class);
		Mockito.when(process.shouldStop()).thenReturn(false);
		StudioConcurrencyContext studioContext = new StudioConcurrencyContext(process);
		SequentialConcurrencyContext sequentialContext = new SequentialConcurrencyContext();
		int depth = 0;
		for (Exception exception : exceptions) {
			Exception expected = null;
			Exception actual = null;
			List<Callable<Void>> callables = Collections.singletonList(new ThrowingCallable(exception));
			try {
				studioContext.internalCall(callables);
			} catch (ExecutionException e) {
				expected = e;
			}
			try {
				sequentialContext.internalCall(callables);
			} catch (ExecutionException e) {
				actual = e;
			}
			assertEquals("difference for exception (depth " + depth + ")" + exception, expected, actual);
			depth++;
		}
	}

	private static void assertEquals(String errorMessage, Throwable expected, Throwable actual) {
		while (expected != null && actual != null) {
			Assert.assertEquals(errorMessage + ": different classes", expected.getClass(), actual.getClass());
			if (expected != expected.getCause()) {
				expected = expected.getCause();
			} else {
				expected = null;
			}
			if (actual != actual.getCause()) {
				actual = actual.getCause();
			} else {
				actual = null;
			}
		}
		if (expected != actual) {
			Assert.fail(errorMessage + ": different depth");
		}
	}
}