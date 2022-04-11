package org.forome.querypool;

import org.forome.database.domainobject.DomainObject;
import org.forome.database.domainobject.DomainObjectEditable;
import org.forome.database.exception.DatabaseException;

public class EditableResource<T extends DomainObject & DomainObjectEditable> extends ReadableResource<T> {

	EditableResource(Class<T> tClass, ExceptionBuilder exceptionBuilder) {
		super(tClass, exceptionBuilder);
	}

	public T create(QueryTransaction transaction) {
		try {
			return transaction.getDBTransaction().create(tClass);
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}

	public void save(T newObj, QueryTransaction transaction) {
		try {
			transaction.getDBTransaction().save(newObj);
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}

	public void remove(T obj, QueryTransaction transaction) {
		try {
			transaction.getDBTransaction().remove(obj);
		} catch (DatabaseException e) {
			throw exceptionBuilder.buildDatabaseException(e);
		}
	}


}
