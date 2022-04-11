package org.forome.querypool;


import org.forome.database.domainobject.DomainObject;
import org.forome.database.domainobject.DomainObjectEditable;

public interface ResourceProvider {

	<T extends DomainObject> ReadableResource<T> getReadableResource(Class<T> resClass);

	<T extends DomainObject & DomainObjectEditable> EditableResource<T> getEditableResource(Class<T> resClass);

	void borrowResource(Class resClass, QueryPool.LockType type);
}
