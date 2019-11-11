package com.infomaximum.querypool;

import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.utils.DefaultThreadPoolExecutor;
import com.infomaximum.utils.LockGuard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class QueryPool {

	public static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
	public static final int MAX_WORKED_EXECUTION_COUNT = MAX_THREAD_COUNT * 5;
	public static final int MAX_WAITING_EXECUTION_COUNT = MAX_THREAD_COUNT * 20;

	public enum LockType {
		SHARED, EXCLUSIVE
	}

	public enum Priority {
		LOW, HIGH
	}

	@FunctionalInterface
	public interface Callback {

		void execute(QueryPool pool);
	}

	private final ExceptionBuilder exceptionBuilder;
	private final ThreadPoolExecutor threadPool;
	private final ReentrantLock lock = new ReentrantLock();
	private final ArrayList<String> maintenanceMarkers = new ArrayList<>();
	private final ResourceMap occupiedResources = new ResourceMap();
	private final ResourceMap waitingResources = new ResourceMap();
	private final ArrayList<Callback> emptyPoolListners = new ArrayList<>();
	private volatile int highPriorityWaitingExecutionCount = 0;
	private volatile int lowPriorityWaitingExecutionCount = 0;

	public QueryPool(ExceptionBuilder exceptionBuilder, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.exceptionBuilder = exceptionBuilder;
		this.threadPool = new DefaultThreadPoolExecutor(
				MAX_THREAD_COUNT,
				MAX_THREAD_COUNT,
				0L,
				TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(MAX_WORKED_EXECUTION_COUNT),
				"ExecutionQueue",
				uncaughtExceptionHandler
		);
	}


	public <T> CompletableFuture<T> execute(DomainObjectSource domainObjectSource, Query<T> execution) {
		return execute(domainObjectSource, execution, true);
	}

	public <T> CompletableFuture<T> execute(DomainObjectSource domainObjectSource, Query<T> execution, boolean failIfPoolBusy) {
		QueryWrapper<T> executionWrapper = new QueryWrapper<>(domainObjectSource, exceptionBuilder, execution);

		try (LockGuard guard = new LockGuard(lock)) {
			if (failIfPoolBusy && isOverloaded(executionWrapper.query.getPriority())) {
				executionWrapper.future.completeExceptionally(createOverloadedException());
			} else if (isOccupiedResources(executionWrapper.resources)) {
				if (failIfPoolBusy && isMaintenance()) {
					executionWrapper.future.completeExceptionally(createMaintenanceException());
				} else {
					captureWaitingResources(executionWrapper);
				}
			} else {
				submitExecution(executionWrapper);
			}
		}

		return executionWrapper.future;
	}

	private void submitExecution(QueryWrapper<?> executionWrapper) {
		captureOccupiedResources(executionWrapper);

		try {
			threadPool.submit(() -> {
				try {
					executionWrapper.execute();
				} catch (Throwable e) {
					try (LockGuard guard = new LockGuard(lock)) {
						releaseOccupiedResources(executionWrapper);
					} catch (Throwable ignore) {
						// do nothing
					}

					throw e;
				}

				Callback[] emptyListners;
				try (LockGuard guard = new LockGuard(lock)) {
					releaseOccupiedResources(executionWrapper);
					emptyListners = getFiringEmptyPoolListners();
					trySubmitNextAvailableExecutionBy(executionWrapper.resources);
				}

				fireEmptyPoolListners(emptyListners);
			});
		} catch (RejectedExecutionException e) {
			releaseOccupiedResources(executionWrapper);
			executionWrapper.future.completeExceptionally(exceptionBuilder.buildServerShutsDownException());
		} catch (Throwable e) {
			releaseOccupiedResources(executionWrapper);
			throw e;
		}
	}

	private Callback[] getFiringEmptyPoolListners() {
		if (!occupiedResources.isEmpty() || !waitingResources.isEmpty() || emptyPoolListners.isEmpty()) {
			return null;
		}

		Callback[] listners = new Callback[emptyPoolListners.size()];
		emptyPoolListners.toArray(listners);
		return listners;
	}

	private void fireEmptyPoolListners(Callback[] listners) {
		if (listners != null) {
			for (Callback item : listners) {
				item.execute(this);
			}
		}
	}

	private void captureOccupiedResources(QueryWrapper executionWrapper) {
		appendResources(executionWrapper, occupiedResources);
		pushMaintenance(executionWrapper.query.getMaintenanceMarker());
	}

	private void releaseOccupiedResources(QueryWrapper executionWrapper) {
		popMaintenance(executionWrapper.query.getMaintenanceMarker());
		removeResources(executionWrapper, occupiedResources);
	}

	private void captureWaitingResources(QueryWrapper executionWrapper) {
		switch (executionWrapper.query.getPriority()) {
			case LOW:
				++lowPriorityWaitingExecutionCount;
				break;
			case HIGH:
				++highPriorityWaitingExecutionCount;
				break;
		}
		appendResources(executionWrapper, waitingResources);
	}

	private void releaseWaitingResources(QueryWrapper executionWrapper) {
		removeResources(executionWrapper, waitingResources);
		switch (executionWrapper.query.getPriority()) {
			case LOW:
				--lowPriorityWaitingExecutionCount;
				break;
			case HIGH:
				--highPriorityWaitingExecutionCount;
				break;
		}
	}

	private void trySubmitNextAvailableExecutionBy(Map<String, LockType> releasedResources) {
		HashSet<QueryWrapper> candidates = new HashSet<>();

		for (Map.Entry<String, LockType> res : releasedResources.entrySet()) {
			ArrayList<ExecutionLockType> value = waitingResources.get(res.getKey());
			if (value != null) {
				value.forEach(item -> candidates.add(item.executionWrapper));
			}
		}

		for (QueryWrapper<?> executionWrapper : candidates) {
			if (isOccupiedResources(executionWrapper.resources)) {
				continue;
			}

			if (isFilledThreadPool()) {
				break;
			}

			releaseWaitingResources(executionWrapper);
			submitExecution(executionWrapper);
		}
	}

	private boolean isOverloaded(Priority newPriority) {
		if (isFilledThreadPool()) {
			return true;
		}

		switch (newPriority) {
			case LOW:
				return lowPriorityWaitingExecutionCount >= MAX_WAITING_EXECUTION_COUNT;
			case HIGH:
				return highPriorityWaitingExecutionCount >= MAX_WAITING_EXECUTION_COUNT;
		}
		return false;
	}

	private boolean isFilledThreadPool() {
		return threadPool.getQueue().size() >= MAX_WORKED_EXECUTION_COUNT;
	}

	private void pushMaintenance(String marker) {
		if (marker != null) {
			maintenanceMarkers.add(marker);
		}
	}

	private void popMaintenance(String marker) {
		if (marker != null) {
			maintenanceMarkers.remove(maintenanceMarkers.size() - 1);
		}
	}

	private boolean isMaintenance() {
		return !maintenanceMarkers.isEmpty();
	}

	private boolean isOccupiedResources(final Map<String, LockType> targetResources) {
		for (HashMap.Entry<String, LockType> res : targetResources.entrySet()) {
			ArrayList<ExecutionLockType> foundValue = occupiedResources.get(res.getKey());
			if (foundValue == null || foundValue.isEmpty()) {
				continue;
			}

			if (res.getValue() == LockType.EXCLUSIVE || foundValue.get(0).lockType == LockType.EXCLUSIVE) {
				return true;
			}
		}
		return false;
	}

	private RuntimeException createMaintenanceException() {
		return exceptionBuilder.buildServerBusyException(maintenanceMarkers.get(maintenanceMarkers.size() - 1));
	}

	private RuntimeException createOverloadedException() {
		if (isMaintenance()) {
			return createMaintenanceException();
		}

		return exceptionBuilder.buildServerOverloadedException();
	}

	private static void appendResources(QueryWrapper<?> executionWrapper, ResourceMap destination) {
		for (Map.Entry<String, LockType> entry : executionWrapper.resources.entrySet()) {
			ArrayList<ExecutionLockType> foundValue = destination.get(entry.getKey());
			if (foundValue == null) {
				foundValue = new ArrayList<>();
				destination.put(entry.getKey(), foundValue);
			}

			foundValue.add(new ExecutionLockType(executionWrapper, entry.getValue()));
		}
	}

	private static void removeResources(QueryWrapper<?> executionWrapper, ResourceMap destination) {
		for (Map.Entry<String, LockType> entry : executionWrapper.resources.entrySet()) {
			ArrayList<ExecutionLockType> foundValue = destination.get(entry.getKey());
			if (foundValue == null) {
				continue;
			}

			foundValue.removeIf(item -> item.executionWrapper == executionWrapper);
			if (foundValue.isEmpty()) {
				destination.remove(entry.getKey());
			}
		}
	}

	private static class QueryWrapper<T> {

		final DomainObjectSource domainObjectSource;
		final ExceptionBuilder exceptionBuilder;
		final Query<T> query;
		final CompletableFuture<T> future;

		final Map<String, LockType> resources;

		QueryWrapper(DomainObjectSource domainObjectSource, ExceptionBuilder exceptionBuilder, Query<T> query) {
			this.domainObjectSource = domainObjectSource;
			this.exceptionBuilder = exceptionBuilder;
			this.query = query;
			this.future = new CompletableFuture<>();

			try (ResourceProviderImpl provider = new ResourceProviderImpl(exceptionBuilder)) {
				query.prepare(provider);
				this.resources = provider.getResources();
			}
		}

		void execute() {
			try (QueryTransaction transaction = new QueryTransaction(domainObjectSource, exceptionBuilder)) {
				T result = query.execute(transaction);
				transaction.commit();
				future.complete(result);
			} catch (Throwable e) {
				future.completeExceptionally(e);
			}
		}
	}

	private static class ExecutionLockType {

		final QueryWrapper executionWrapper;
		final LockType lockType;

		ExecutionLockType(QueryWrapper executionWrapper, LockType lockType) {
			this.executionWrapper = executionWrapper;
			this.lockType = lockType;
		}
	}

	private static class ResourceMap extends HashMap<String, ArrayList<ExecutionLockType>> {
	}
}
