package org.forome.querypool;

import org.forome.database.exception.DatabaseException;

public interface ExceptionBuilder {

    RuntimeException buildDatabaseException(DatabaseException e);

    RuntimeException buildServerBusyException(String cause);

    RuntimeException buildServerOverloadedException();

    RuntimeException buildServerShutsDownException();
}
