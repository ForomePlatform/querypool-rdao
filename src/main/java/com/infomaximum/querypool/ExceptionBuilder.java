package com.infomaximum.querypool;

import com.infomaximum.database.exception.DatabaseException;

public interface ExceptionBuilder {

    RuntimeException buildDatabaseException(DatabaseException e);

    RuntimeException buildServerBusyException(String cause);

    RuntimeException buildServerOverloadedException();

    RuntimeException buildServerShutsDownException();
}
