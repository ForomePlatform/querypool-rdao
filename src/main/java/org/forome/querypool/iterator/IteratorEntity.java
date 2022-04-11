package org.forome.querypool.iterator;

import org.forome.database.domainobject.DomainObject;
import org.forome.database.exception.DatabaseException;
import org.forome.querypool.ExceptionBuilder;
import org.forome.utils.iterator.AIterator;

public class IteratorEntity<E extends DomainObject> implements AIterator<E> {

	private final org.forome.database.domainobject.iterator.IteratorEntity<E> ie;
	private final ExceptionBuilder exceptionBuilder;

	public IteratorEntity(
			org.forome.database.domainobject.iterator.IteratorEntity<E> ie,
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
