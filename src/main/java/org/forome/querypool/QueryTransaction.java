package org.forome.querypool;

import org.forome.database.domainobject.DomainObjectSource;
import org.forome.database.domainobject.Transaction;
import org.forome.database.exception.DatabaseException;

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
