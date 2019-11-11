package com.infomaximum.querypool;

import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectEditable;
import com.infomaximum.database.exception.DatabaseException;

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
