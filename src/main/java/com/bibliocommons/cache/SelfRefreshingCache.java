package com.bibliocommons.cache;

import com.google.common.base.Preconditions;
import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SelfRefreshingCache - a cache with loading semantics which supports automatic background refresh via a configurable threadpool.
 * Exceptions caught during loading will be and passed off to handleInitialException if on initial load, otherwise will be ignored.
 *
 * WARNING: Exceptions which occur during loading will delay subsequent updates to the value until the next update interval.
 * No guarantees are made to the freshness of the value.
 *
 * @param <K>
 * @param <V>
 */
public class SelfRefreshingCache<K, V> {

    private static Logger logger = Logger.getLogger(SelfRefreshingCache.class);

    protected static long MIN_REFRESH_INTERVAL = 1 * 60 * 1000;
    protected static long FAILED_INITIAL_LOAD_REFRESH_INTERVAL = 10 * 60 * 1000;

    private static final int DEFAULT_POOL_SIZE = 10;
    private static final float REFRESH_PERIOD_PROPORTION = 0.5f;

    public static float getRefreshPeriodProportion() {
        return REFRESH_PERIOD_PROPORTION;
    }

    // All instances share the same thread pool for refresh operations
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(getCacheThreadPoolSize());

    private static int getCacheThreadPoolSize() {
        String threadPoolSize = System.getProperty("self.refreshing.cache.thread.pool.size");
        if (threadPoolSize != null) {
            return new Integer(threadPoolSize);
        } else {
            return DEFAULT_POOL_SIZE;
        }
    }

    private Map<K, CacheValueProxy<K, V>> cache;

    private long refreshInterval;
    // Maximum amount of initial delay for run of first scheduled refresh. This is used to
    // effectively stagger refreshes
    private long maxInitialDelay;
    private long failedInitialLoadDelay;
    private Random random = new Random();
    protected LoadStrategy<K, V> loadStrategy;
    private V defaultValue;
    private boolean useDefaultValueForInitialLoad = false;

    /**
     * @param cacheName       - must be unique
     * @param loadStrategy    - the strategy used when performing background refreshes
     * @param refreshInterval in millis
     * @param maxInitialDelay
     * @param maxElements     - the maximum number of elements allowed in this cache
     */
    @SuppressWarnings("unchecked")
    public SelfRefreshingCache(LoadStrategy<K, V> loadStrategy, long refreshInterval, int maxElements) {
        Preconditions.checkArgument(refreshInterval >= MIN_REFRESH_INTERVAL, "refreshInterval cannot be less than 1 minute");

        this.loadStrategy = loadStrategy;
        this.refreshInterval = refreshInterval;
        this.maxInitialDelay = (long) (refreshInterval * REFRESH_PERIOD_PROPORTION);
        this.failedInitialLoadDelay = FAILED_INITIAL_LOAD_REFRESH_INTERVAL;

        cache = new ConcurrentHashMap<K, CacheValueProxy<K, V>>(new LRUMap(maxElements));
    }

    public SelfRefreshingCache(LoadStrategy<K, V> loadStrategy, long refreshInterval, int maxElements, V defaultValue) {
        this(loadStrategy, refreshInterval, maxElements, defaultValue, false);
    }

    public SelfRefreshingCache(LoadStrategy<K, V> loadStrategy, long refreshInterval, int maxElements, V defaultValue,
                               boolean useDefaultValueForInitialLoad) {
        this(loadStrategy, refreshInterval, maxElements);

        Preconditions.checkArgument(defaultValue != null, "defaultValue cannot be null");

        this.defaultValue = defaultValue;
        this.useDefaultValueForInitialLoad = useDefaultValueForInitialLoad;
    }

    /**
     * Returns the value associated with the given key.
     *
     * @param key - the key to lookup in the cache.
     * @return - value associated with the key or null if the key is not found in the cache.
     */
    public V get(K key) {

        CacheValueProxy<K, V> valueProxy = cache.get(key);

        synchronized (this) {
            if (valueProxy == null) {
                logger.debug("proxy for key: " + key + " is null, initializing..");
                valueProxy = createNewCacheValueProxyInstance(key);
                cache.put(key, valueProxy);
            }
        }

        valueProxy = cache.get(key);

        synchronized (valueProxy) {
            if (valueProxy.getValue() == null && !(valueProxy.isCachedNull())) {
                logger.debug("setting first time value for proxy with key: " + key);
                try {
                    if (useDefaultValueForInitialLoad && defaultValue != null) {
                        valueProxy.setValue(defaultValue);
                    } else {
                        valueProxy.refreshValue();
                    }
                } catch (Throwable t) {
                    logger.warn(t);
                    handleInitialFailure(key, valueProxy, t);
                }

                // setup a schedule for periodic refresh
                executor.scheduleAtFixedRate(valueProxy, getRandomDelayOffset(maxInitialDelay), refreshInterval, TimeUnit.MILLISECONDS);
            }
        }

        return valueProxy.getValue();
    }

    /**
     * Method for handling an initial failure to load and store the value associated with a given key.
     *
     * @param key
     * @param cacheValueProxy
     * @param t               - an exception to chain for root cause.
     */
    protected void handleInitialFailure(K key, CacheValueProxy<K, V> cacheValueProxy, Throwable t) {
        logger.error("exception encountered while refreshing value for proxy with key: " + key + ". Exception: " + t.getClass()
                + ", message: " + t.getMessage() + ", Load Strategy: " + loadStrategy.getClass().getName());

        if (defaultValue != null) {
            cacheValueProxy.setValue(defaultValue);
            scheduleOneTimeAggressiveRefresh(cacheValueProxy, failedInitialLoadDelay);
        } else {
            // if initialization fails, bomb out
            throw new SelfRefreshingCacheException("Could not initialize value for proxy with key: " + key, t);
        }
    }

    protected void scheduleOneTimeAggressiveRefresh(CacheValueProxy<K, V> valueProxy, long initialDelay) {
        executor.schedule(valueProxy, getRandomDelayOffset(initialDelay), TimeUnit.MILLISECONDS);
    }

    protected CacheValueProxy<K, V> createNewCacheValueProxyInstance(K key) {
        return new CacheValueProxy<K, V>(key, loadStrategy);
    }

    /**
     * Forces a refresh of the cache and returns the value
     *
     * @param key
     * @return value associated with key
     * @throws Throwable
     */
    public V getForceRefresh(K key) throws Throwable {
        CacheValueProxy<K, V> valueProxy = cache.get(key);
        if (valueProxy != null) {
            valueProxy.refreshValue();
            return valueProxy.getValue();
        } else {
            return get(key);
        }

    }

    long getRandomDelayOffset(long initialDelay) {
        return (long) (random.nextDouble() * initialDelay);
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public long getMaxInitialDelay() {
        return maxInitialDelay;
    }
}
