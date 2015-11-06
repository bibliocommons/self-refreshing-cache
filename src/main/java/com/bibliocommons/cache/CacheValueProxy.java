package com.bibliocommons.cache;

import org.apache.log4j.Logger;

/**
 * CacheValueProxy - Represents a proxy to the actual value being cached at the given key.
 * @param <K>
 * @param <V>
 */
public class CacheValueProxy<K, V> implements Runnable {

    private static Logger logger = Logger.getLogger(CacheValueProxy.class);

    protected K key;
    private V value;
    private LoadStrategy<K, V> loadStrategy;
    private boolean isCachedNull = false;

    protected CacheValueProxy(K key, LoadStrategy<K, V> loadStrategy) {
        this.key = key;
        this.loadStrategy = loadStrategy;
    }

    V getValue() {
        return value;
    }

    boolean isCachedNull() {
        return isCachedNull;
    }

    @Override
    public void run() {
        try {
            logger.debug("refreshing value for proxy with key: " + key);
            refreshValue();

        } catch (Throwable t) {
            // do nothing, eat exceptions and try again on next refresh
            logger.error("exception encountered while refreshing value for proxy with key: " + key
                    + ". Exception: " + t.getClass() + ", message: " + t.getMessage());
        }
    }

    protected void refreshValue() throws Throwable {
        doLoad();
    }

    protected void doLoad() throws Throwable {
        value = loadStrategy.load(key);

        if (value == null) {
            isCachedNull = true;
        } else {
            isCachedNull = false;
        }
    }

    void setValue(V value) {
        this.value = value;
    }

}
