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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


/**
 * A sequential {@link com.rapidminer.core.concurrency.ConcurrencyContext ConcurrencyContext}. Handles all tasks in a
 * linear fashion if they are actually submitted/called.
 *
 * @author Jan Czogalla
 * @since 9.7.1
 */
public class SequentialConcurrencyContext extends AbstractConcurrencyContext {

	/** Dummy {@link PoolInstance wiht no parallelism} */
	private static final PoolInstance DUMMY_POOL = new PoolInstance() {

		@Override
		public ForkJoinPool getForkJoinPool() {
			return null;
		}

		@Override
		public boolean isPoolOutdated() {
			return false;
		}

		@Override
		public int getParallelism() {
			return 1;
		}

		@Override
		public int getDesiredParallelismLevel() {
			return 1;
		}
	};

	public SequentialConcurrencyContext() {
		super(DUMMY_POOL);
	}

	/**
	 * Same as {@code collectResults(internalSubmit(callables))}
	 */
	@Override
	<T> List<T> internalCall(List<Callable<T>> callables) throws ExecutionException {
		return collectResults(internalSubmit(callables));
	}

	/**
	 * Wraps the callables with {@link FakedFuture} to keep consistent with exception handling. This means that
	 * submitted callables are not executed in the background, but only when the collection comes up.
	 */
	@Override
	<T> List<Future<T>> internalSubmit(List<Callable<T>> callables) {
		return callables.stream().map(FakedFuture::new).collect(Collectors.toList());
	}

	/**
	 * {@link Future} implementation that simply executes the callable when calling {@link #get()}.
	 *
	 * @param <T> the type of the wrapped {@link Callable}
	 * @author Jan Czogalla
	 * @since 9.7.1
	 */
	private static class FakedFuture<T> implements Future<T>{
		private Callable<T> callable;

		public FakedFuture(Callable<T> callable) {
			this.callable = callable;
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			try {
				return callable.call();
			} catch (Throwable t) {
				throw new ExecutionException(new RecursiveWrapper.WrapperRuntimeException(t));
			}
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return get();
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}
	}
}
