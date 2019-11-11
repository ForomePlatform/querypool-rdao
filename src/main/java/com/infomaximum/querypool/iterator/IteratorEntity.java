package com.infomaximum.querypool.iterator;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.querypool.ExceptionBuilder;
import com.infomaximum.utils.iterator.AIterator;

public class IteratorEntity<E extends DomainObject> implements AIterator<E> {

	private final com.infomaximum.database.domainobject.iterator.IteratorEntity<E> ie;
	private final ExceptionBuilder exceptionBuilder;

	public IteratorEntity(
			com.infomaximum.database.domainobject.iterator.IteratorEntity<E> ie,
			ExceptionBuilder exceptionBuilder
	) {
		this.ie = ie;
		this.exceptionBuilder = exceptionBuilder;
	}

	@Override
	public boolean hasNext() {
		return ie.hasNext();
	}

	@Override
	public E next() {
		try {
			return ie.next();
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}

	@Override
	public void close() {
		try {
			ie.close();
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}
}
