package org.forome.utils.iterator;


public interface AIterator<T> extends AutoCloseable {

	boolean hasNext();

	T next();

	void close();
}
