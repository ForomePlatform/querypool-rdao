package org.forome.querypool;

public abstract class Query<T> {

	public abstract void prepare(ResourceProvider resources);

	public QueryPool.Priority getPriority() {
		return QueryPool.Priority.HIGH;
	}

	public String getMaintenanceMarker() {
		return null;
	}

	public abstract T execute(QueryTransaction transaction);
}
