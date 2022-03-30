package com.infomaximum.querypool;

import com.infomaximum.database.anotation.Entity;
import com.infomaximum.database.domainobject.DomainObject;
import com.infomaximum.database.domainobject.DomainObjectEditable;
import com.infomaximum.database.exception.ClosedObjectException;

import java.util.HashMap;

public class ResourceProviderImpl implements ResourceProvider, AutoCloseable {

	private final ExceptionBuilder exceptionBuilder;

	private final HashMap<String, QueryPool.LockType> resources = new HashMap<>();
	private boolean closed = false;

	protected ResourceProviderImpl(ExceptionBuilder exceptionBuilder) {
		this.exceptionBuilder=exceptionBuilder;
	}

	@Override
	public <T extends DomainObject & DomainObjectEditable> EditableResource<T> getEditableResource(Class<T> resClass) {
		borrowResource(resolveReadClass(resClass), QueryPool.LockType.EXCLUSIVE);
		return new EditableResource<>(resClass, exceptionBuilder);
	}

	@Override
	public <T extends DomainObject> ReadableResource<T> getReadableResource(Class<T> resClass) {
		checkReadClass(resClass);
		borrowResource(resClass, QueryPool.LockType.SHARED);
		return new ReadableResource<>(resClass, exceptionBuilder);
	}

	@Override
	public void borrowResource(Class resClass, QueryPool.LockType type) {
		borrowResource(resClass.getName(), type);
	}

	public void borrowResource(String resource, QueryPool.LockType type) {
		check();
		appendResource(resource, type, resources);
	}

	protected HashMap<String, QueryPool.LockType> getResources() {
		check();
		return resources;
	}

	@Override
	public void close() {
		closed = true;
	}

	private void check() {
		if (closed) {
			throw new ClosedObjectException(this.getClass());
		}
	}

	private <T extends DomainObject> void checkReadClass(Class<T> resClass) {
		if (!resClass.isAnnotationPresent(Entity.class)) {
			throw new IllegalArgumentException("class-Readable " + resClass.getSimpleName() + " not contains Entity annotation");
		}
	}

	public static void appendResource(String resource, QueryPool.LockType type, HashMap<String, QueryPool.LockType> destination) {
		destination.merge(resource, type, (val1, val2) -> val1 == QueryPool.LockType.EXCLUSIVE ? val1 : val2);
	}

	private static <T extends DomainObject & DomainObjectEditable> Class<?> resolveReadClass(Class<T> editClass) {
		Class<?> readClass = editClass;
		do {
			readClass = readClass.getSuperclass();
		} while (!readClass.isAnnotationPresent(Entity.class));
		return readClass;
	}
}
