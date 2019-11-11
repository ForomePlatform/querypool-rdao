package com.infomaximum.utils.iterator;


public interface AIterator<T> extends AutoCloseable {

	boolean hasNext();

	T next();

	void close();
}
