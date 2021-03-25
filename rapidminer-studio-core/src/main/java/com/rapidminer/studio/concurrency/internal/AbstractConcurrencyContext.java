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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rapidminer.Process;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.core.concurrency.ExecutionStoppedException;
import com.rapidminer.studio.internal.ProcessStoppedRuntimeException;
import com.rapidminer.tools.ValidationUtilV2;


/**
 * Simple {@link ConcurrencyContext} to be used with a single {@link Process}.
 * <p>
 * The context does not implement the submission methods for {@link ForkJoinTask}s.
 *
 * @author Jonas Wilms-Pfau (internals by Gisa Schaefer, Michael Knopf)
 * @since 9.1.0
 */
abstract class AbstractConcurrencyContext implements ConcurrencyContext {

	/** The corresponding process. */
	private final Process process;

	/** The shared {@link ForkJoinPool} pool wrapper */
	private final PoolInstance pool;

	/**
	 * Creates a new {@link ConcurrencyContext} for the given {@link Process}.
	 * <p>
	 * The context assumes that only operators that belong to the corresponding process submit tasks
	 * to this context.
	 *
	 * @param process
	 *            the corresponding process
	 */
	AbstractConcurrencyContext(Process process, LazyPool pool) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null");
		}
		if (pool == null) {
			throw new IllegalArgumentException("pool must not be null");
		}
		this.process = process;
		this.pool = pool.getInstance(this);
	}

	AbstractConcurrencyContext(PoolInstance instance) {
		this.pool = ValidationUtilV2.requireNonNull(instance, "instance");
		process = null;
	}

	@Override
	public void run(List<Runnable> runnables) throws ExecutionException, ExecutionStoppedException {
		if (runnables == null) {
			throw new IllegalArgumentException("runnables must not be null");
		}

		// nothing to do if list is empty
		if (runnables.isEmpty()) {
			return;
		}

		// check for null runnables
		for (Runnable runnable : runnables) {
			if (runnable == null) {
				throw new IllegalArgumentException("runnables must not contain null");
			}
		}

		// wrap runnables in callables
		List<Callable<Void>> callables = new ArrayList<>(runnables.size());
		for (final Runnable runnable : runnables) {
			callables.add(() -> {
				runnable.run();
				return null;
			});
		}

		// submit callables without further checks
		call(callables);
	}

	@Override
	public <T> List<T> call(List<Callable<T>> callables)
			throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
		if (callableCheck(callables)) {
			return Collections.emptyList();
		}
		return internalCall(callables);
	}

	@Override
	public <T> List<Future<T>> submit(List<Callable<T>> callables) throws IllegalArgumentException {
		if (callableCheck(callables)) {
			return Collections.emptyList();
		}

		return internalSubmit(callables);
	}

	@Override
	public <T> List<T> collectResults(List<Future<T>> futures)
			throws ExecutionException, ExecutionStoppedException, IllegalArgumentException {
		if (futures == null) {
			throw new IllegalArgumentException("futures must not be null");
		}

		// nothing to do if list is empty
		if (futures.isEmpty()) {
			return Collections.emptyList();
		}

		// check for null tasks
		for (Future<T> future : futures) {
			if (future == null) {
				throw new IllegalArgumentException("futures must not contain null");
			}
		}

		List<T> results = new ArrayList<>(futures.size());
		for (Future<T> future : futures) {
			try {
				T result = future.get();
				results.add(result);
			} catch (InterruptedException | RejectedExecutionException e) {
				// The pool's invokeAll() method calls Future.get() internally. If the process is
				// stopped by the user, these calls might be interrupted before calls to
				// checkStatus() throw an ExecutionStoppedException. Thus, we need to check the
				// current status again.
				checkStatus();
				// InterruptedExceptions are very unlikely to happen at this point, since the above
				// calls to get() will return immediately. A RejectedExectutionException is an
				// extreme corner case as well. In both cases, there is no benefit for the API user
				// if the exception is passed on directly. Thus, we can wrap it within a
				// ExecutionException which is part of the API.
				throw new ExecutionException(e);
			} catch (ExecutionException e) {
				// A ProcessStoppedRuntimeException is an internal exception thrown if the user
				// requests the process to stop (see the checkStatus() implementation of this
				// class). This exception should not be wrapped or consumed here, since it is
				// handled by the operator implementation itself.
				if (e.getCause() instanceof ProcessStoppedRuntimeException) {
					throw (ExecutionStoppedException) e.getCause();
				} else if (e.getCause() instanceof RecursiveWrapper.WrapperRuntimeException) {
					// unwrap exceptions that we wrapped ourselves in AdaptedCallable
					throw new ExecutionException(e.getCause().getCause());
				} else {
					throw e;
				}
			}
		}

		return results;
	}

	@Override
	public int getParallelism() {
		return pool.getParallelism();
	}

	@Override
	public void checkStatus() throws ExecutionStoppedException {
		if (process != null && process.shouldStop()) {
			throw new ProcessStoppedRuntimeException();
		}
	}

	@Override
	public <T> T invoke(ForkJoinTask<T> task) throws ExecutionException, ExecutionStoppedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<T> invokeAll(final List<ForkJoinTask<T>> tasks) throws ExecutionException,
			ExecutionStoppedException {
		throw new UnsupportedOperationException();
	}

	/**
	 * This method verifies and returns a JVM-wide, static FJPool. Override if a different pool behavior is needed.
	 *
	 * @return the ForkJoinPool to use for this context instance for execution.
	 */
	protected ForkJoinPool getForkJoinPool(){
		return pool.getForkJoinPool();
	}

	/**
	 * Checks if the JVM-wide, static pool needs to be re-created. Override if a different pool behavior is needed.
	 *
	 * @return {@code true} if the current pool is {@code null} or the {@link #getDesiredParallelismLevel} is not
	 * equal to the current pool parallelism otherwise {@code false}
	 */
	protected boolean isPoolOutdated() {
		return pool.isPoolOutdated();
	}

	/**
	 * Returns the desired number of cores to be used for concurrent computations for the JVM-wide, static pool. This
	 * number is always at least one and either bound by a license limit or by the user's configuration. Override if a
	 * different pool behavior is needed.
	 *
	 * @return the desired parallelism level
	 */
	protected int getDesiredParallelismLevel() {
		return pool.getDesiredParallelismLevel();
	}

	/**
	 * Actual call of the list of callables. By default submits and collects the list with the help of the {@link
	 * #getForkJoinPool()} and {@link #collectResults(List)} or through {@link RecursiveWrapper#call(List)}.
	 *
	 * @param callables the list of callables
	 * @param <T>       the type of callables
	 * @return the list of results
	 * @throws ExecutionException if an error occurs during exection
	 * @since 9.7.1
	 */
	<T> List<T> internalCall(List<Callable<T>> callables) throws ExecutionException {
		ForkJoinPool forkJoinPool = AccessController.doPrivileged(
				(PrivilegedAction<ForkJoinPool>) this::getForkJoinPool);
		// handle submissions from inside and outside the pool differently
		Thread currentThread = Thread.currentThread();
		if (currentThread instanceof ForkJoinWorkerThread
				&& ((ForkJoinWorkerThread) currentThread).getPool() == forkJoinPool) {
			return RecursiveWrapper.call(callables);
		} else {
			final List<Future<T>> futures = new ArrayList<>(callables.size());
			for (Callable<T> callable : callables) {
				futures.add(forkJoinPool.submit((ForkJoinTask<T>) new AdaptedCallable<>(callable)));
			}
			return collectResults(futures);
		}
	}

	/**
	 * Actual submission of the list of callables. By default wraps and submits the list elements with the help of the
	 * {@link #getForkJoinPool()} and {@link AdaptedCallable}.
	 *
	 * @param callables the list of callabes
	 * @param <T>       the type of callables
	 * @return the list of futures
	 * @since 9.7.1
	 */
	<T> List<Future<T>> internalSubmit(List<Callable<T>> callables) {
		ForkJoinPool forkJoinPool = AccessController.doPrivileged(
				(PrivilegedAction<ForkJoinPool>) this::getForkJoinPool);
		// handle submissions from inside and outside the pool differently
		Thread currentThread = Thread.currentThread();
		if (currentThread instanceof ForkJoinWorkerThread
				&& ((ForkJoinWorkerThread) currentThread).getPool() == forkJoinPool) {
			final List<Future<T>> futures = new ArrayList<>(callables.size());
			for (Callable<T> callable : callables) {
				futures.add(new FutureWrapper<>(new AdaptedCallable<>(callable).fork()));
			}
			return futures;
		} else {
			// submit callables without further checks
			final List<Future<T>> futures = new ArrayList<>(callables.size());
			for (Callable<T> callable : callables) {
				futures.add(new FutureWrapper<>(forkJoinPool.submit((ForkJoinTask<T>) new AdaptedCallable<>(callable))));
			}
			return futures;
		}
	}

	/**
	 * Checks the given list for {@code null} and {@code null} elements. If one of the former is the case, an {@link
	 * IllegalArgumentException} is thrown. Returns {@code true} if the list is empty, {@code false} otherwise.
	 *
	 * @param callables the list of callables to check
	 * @param <T>       the type of list elements
	 * @return {@code true} if the list is empty
	 * @throws IllegalArgumentException if the list is {@code null} or contains {@code null} elements
	 * @since 9.7.1
	 */
	private <T> boolean callableCheck(List<T> callables) {
		ValidationUtilV2.requireNonNull(callables, "callables");

		// nothing to do if list is empty
		if (callables.isEmpty()) {
			return true;
		}

		// check for null tasks
		for (T callable : callables) {
			if (callable == null) {
				throw new IllegalArgumentException("callables must not contain null");
			}
		}
		return false;
	}

	/**
	 * Wrapper for {@link Callable}s that is the same as ForkJoinTask#AdaptedCallable but wraps checked exceptions in
	 * {@link RecursiveWrapper.WrapperRuntimeException} instead of generic {@link RuntimeException} for easier
	 * unwrapping.
	 *
	 * @since 9.2
	 */
	private static final class AdaptedCallable<T> extends ForkJoinTask<T>
			implements RunnableFuture<T> {

		private static final long serialVersionUID = 23654279569L;

		private final transient Callable<? extends T> callable;
		private transient T result;

		private AdaptedCallable(Callable<? extends T> callable) {
			if (callable == null) {
				throw new NullPointerException();
			}
			this.callable = callable;
		}

		@Override
		public final T getRawResult() {
			return result;
		}

		@Override
		public final void setRawResult(T v) {
			result = v;
		}

		@Override
		public final boolean exec() {
			try {
				result = callable.call();
				return true;
			} catch (Throwable t) {
				// the ForkJoinPool rewraps all Exception with a public constructor in an Exception of the thrown type
				// which hides the stacktrace of the root cause. When wrapping all throwables we can ensure to find
				// the real root cause and present it.
				throw new RecursiveWrapper.WrapperRuntimeException(t);
			}

		}

		@Override
		public final void run() {
			super.invoke();
		}
	}

	/**
	 * Wrapper for a {@link Future} that takes care of unwrapping {@link RecursiveWrapper.WrapperRuntimeException}s.
	 *
	 * @since 9.2
	 */
	private static final class FutureWrapper<T> implements Future<T> {

		private final Future<T> wrappedFuture;

		private FutureWrapper(Future<T> wrappedFuture) {
			this.wrappedFuture = wrappedFuture;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return wrappedFuture.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return wrappedFuture.isCancelled();
		}

		@Override
		public boolean isDone() {
			return wrappedFuture.isDone();
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			try {
				return wrappedFuture.get();
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RecursiveWrapper.WrapperRuntimeException) {
					throw new ExecutionException(e.getCause().getCause());
				}
				throw e;
			}
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			try {
				return wrappedFuture.get(timeout, unit);
			} catch (ExecutionException e) {
				if (e.getCause() instanceof RecursiveWrapper.WrapperRuntimeException) {
					throw new ExecutionException(e.getCause().getCause());
				}
				throw e;
			}
		}
	}
}
