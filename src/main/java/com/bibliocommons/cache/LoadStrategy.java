package com.bibliocommons.cache;

/**
 * Represents a strategy for loading values of type V provided a key of type K.
 * @param <K>
 * @param <V>
 */
public interface LoadStrategy<K, V> {

    V load(K key) throws Throwable;
}
