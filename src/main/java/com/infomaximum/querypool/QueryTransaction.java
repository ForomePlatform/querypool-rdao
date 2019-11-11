package com.infomaximum.querypool;

import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.database.domainobject.Transaction;
import com.infomaximum.database.exception.DatabaseException;

public class QueryTransaction implements AutoCloseable {

    private final Transaction transaction;
    private final ExceptionBuilder exceptionBuilder;

    QueryTransaction(DomainObjectSource domainObjectSource, ExceptionBuilder exceptionBuilder) {
        transaction = domainObjectSource.buildTransaction();
        this.exceptionBuilder = exceptionBuilder;
    }

    public Transaction getDBTransaction() {
        return transaction;
    }

    void commit() {
        try {
            transaction.commit();
        } catch (DatabaseException e) {
            throw exceptionBuilder.buildDatabaseException(e);
        }
    }

    @Override
    public void close() {
        try {
            transaction.close();
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }
}
