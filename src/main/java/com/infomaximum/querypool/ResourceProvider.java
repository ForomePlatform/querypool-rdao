package com.infomaximum.querypool;


import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectEditable;

public interface ResourceProvider {

	<T extends DomainObject> ReadableResource<T> getReadableResource(Class<T> resClass);

	<T extends DomainObject & DomainObjectEditable> EditableResource<T> getEditableResource(Class<T> resClass);

	void borrowResource(Class resClass, QueryPool.LockType type);
}
