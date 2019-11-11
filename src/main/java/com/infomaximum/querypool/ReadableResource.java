package com.infomaximum.querypool;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.filter.EmptyFilter;
import com.infomaximum.database.domainobject.filter.Filter;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.querypool.iterator.IteratorEntity;

public class ReadableResource<T extends DomainObject> {

	protected final Class<T> tClass;
	protected final ExceptionBuilder exceptionBuilder;

	ReadableResource(Class<T> tClass, ExceptionBuilder exceptionBuilder) {
		this.tClass = tClass;
		this.exceptionBuilder = exceptionBuilder;
	}

	public Class<T> getDomainClass() {
		return tClass;
	}

	public T get(long id, QueryTransaction transaction) {
		try {
			return transaction.getDBTransaction().get(tClass, id, null);
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}

	public T find(final Filter filter, QueryTransaction transaction) {
		try (com.infomaximum.database.domainobject.iterator.IteratorEntity<T> iter = transaction.getDBTransaction().find(tClass, filter, null)) {
			return iter.hasNext() ? iter.next() : null;
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}

	public IteratorEntity<T> iterator(QueryTransaction transaction) {
		try {
			return new IteratorEntity<>(
					transaction.getDBTransaction().find(tClass, EmptyFilter.INSTANCE, null),
					exceptionBuilder
			);
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}

	public IteratorEntity<T> findAll(final Filter filter, QueryTransaction transaction) {
		try {
			return new IteratorEntity<>(
					transaction.getDBTransaction().find(tClass, filter, null),
					exceptionBuilder
			);
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}

}
