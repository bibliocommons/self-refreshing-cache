package com.bibliocommons.cache;

/**
 * Represents a strategy for loading values of type V provided a key of type K.
 * @param <K>
 * @param <V>
 */
public interface LoadStrategy<K, V> {

    /**
     * Performs the actual load operation to produce the value associated with key.
     * @param key - the key to load a value for
     * @return - the value associated with key
     * @throws Throwable
     */
    V load(K key) throws Throwable;
}
