package com.bibliocommons.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class SelfRefreshingCacheTest {

    private static final long REFRESH_PERIOD = 100;

    private MockValue mockObject = new MockObject("test");
    private long maxInitialDelay = (long) (REFRESH_PERIOD * SelfRefreshingCache.getRefreshPeriodProportion());

    @Before
    public void setUp() {

        SelfRefreshingCache.MIN_REFRESH_INTERVAL = REFRESH_PERIOD;
    }

    @Test
    public void testGet() {

        LoadStrategy<String, MockValue> mockLoadStrategy = new TestLoadStrategy();

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(mockLoadStrategy).refreshInterval(REFRESH_PERIOD).build();

        Float expectedMaxInitialDelay = (REFRESH_PERIOD * SelfRefreshingCache.getRefreshPeriodProportion());
        assertEquals((Long) cache.getMaxInitialDelay(), (Long) expectedMaxInitialDelay.longValue());

        MockValue retrievedMockObject = cache.get("testKey");
        assertNotNull(retrievedMockObject);
        assertTrue(retrievedMockObject == mockObject);

        MockValue nextFetchOfMockObject = cache.get("testKey");
        // should be same object reference
        assertTrue(retrievedMockObject == nextFetchOfMockObject);

        assertTrue(((TestLoadStrategy) cache.loadStrategy).loadCounter.get("testKey") == 1);
    }

    @Test
    public void testGetMultipleKeys() {

        LoadStrategy<String, MockValue> mockLoadStrategy = new TestLoadStrategy();

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(mockLoadStrategy).refreshInterval(REFRESH_PERIOD).build();

        Float expectedMaxInitialDelay = (REFRESH_PERIOD * SelfRefreshingCache.getRefreshPeriodProportion());
        assertEquals((Long) cache.getMaxInitialDelay(), (Long) expectedMaxInitialDelay.longValue());

        MockValue retrievedMockObject = cache.get("testKey");
        MockValue retrievedMockObjectTwo = cache.get("testKey2");

        assertNotNull(retrievedMockObject);
        assertTrue(retrievedMockObject == mockObject);

        MockValue nextFetchOfMockObject = cache.get("testKey");
        // should be same object reference
        assertTrue(retrievedMockObject == nextFetchOfMockObject);

        MockValue nextFetchOfMockObjectTwo = cache.get("testKey2");
        // should be same object reference
        assertTrue(retrievedMockObjectTwo == nextFetchOfMockObjectTwo);

        assertTrue(((TestLoadStrategy) cache.loadStrategy).loadCounter.get("testKey") == 1);
        assertTrue(((TestLoadStrategy) cache.loadStrategy).loadCounter.get("testKey2") == 1);
    }

    @Test
    public void testGetNull() throws InterruptedException {

        LoadStrategy<String, MockValue> mockLoadStrategy = new TestLoadStrategy();

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(mockLoadStrategy).refreshInterval(REFRESH_PERIOD).build();

        MockValue retrievedMockObject = cache.get("null");
        assertNull(retrievedMockObject);

        MockValue nextFetchOfMockObject = cache.get("null");
        // should be same object reference
        assertNull(nextFetchOfMockObject);
        assertTrue(((TestLoadStrategy) cache.loadStrategy).loadCounter.get("null") == 1);

        // toggle load strategy to return non-null, now sleep to ensure refresh occurred
        ((TestLoadStrategy) cache.loadStrategy).returnNull(false);
        Thread.sleep(REFRESH_PERIOD + maxInitialDelay + 100);

        nextFetchOfMockObject = cache.get("null");
        assertNotNull(nextFetchOfMockObject);
    }

    @Test
    public void testGetLoadsUpdate() throws InterruptedException {

        LoadStrategy<String, MockValue> mockLoadStrategy = new TestLoadStrategy();

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(mockLoadStrategy).refreshInterval(REFRESH_PERIOD).build();

        MockValue retrievedMockObject = cache.get("testKey");
        assertNotNull(retrievedMockObject);
        assertTrue(retrievedMockObject == mockObject);
        mockObject = new MockObject("another test");

        // now sleep to ensure refresh occurred
        Thread.sleep(REFRESH_PERIOD + maxInitialDelay + 100);

        MockValue updateFetchOfMockObject = cache.get("testKey");
        assertTrue(updateFetchOfMockObject != retrievedMockObject);
        assertTrue(updateFetchOfMockObject == mockObject);

        mockObject = new MockObject("one more test");

        // now sleep to ensure refresh occurred
        Thread.sleep(REFRESH_PERIOD + maxInitialDelay + 100);

        MockValue nextUpdateFetchOfMockObject = cache.get("testKey");
        assertTrue(nextUpdateFetchOfMockObject != updateFetchOfMockObject);
        assertTrue(nextUpdateFetchOfMockObject == mockObject);
    }

    @Test(expected = SelfRefreshingCacheException.class)
    public void testExceptionOnInitialLoad() {

        LoadStrategy<String, MockValue> loadStrategyThrowingExceptions = new LoadStrategy<String, MockValue>() {

            @Override
            public MockValue load(String key) throws Throwable {
                throw new SelfRefreshingCacheException();
            }
        };

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(loadStrategyThrowingExceptions).refreshInterval(REFRESH_PERIOD).build();

        cache.get("testKey");
    }

    @Test
    public void testExceptionOnSubsequentLoad() throws InterruptedException {

        LoadStrategy<String, MockValue> mockLoadStrategy = new TestLoadStrategy();

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(mockLoadStrategy).refreshInterval(REFRESH_PERIOD).build();

        try {
            MockValue retrievedMockObject = cache.get("testKey");
            assertNotNull(retrievedMockObject);

        } catch (Exception e) {
            fail();
        }

        mockObject = new MockValue() {

            @Override
            public MockValue getValue() {
                throw new SelfRefreshingCacheException();
            }
        };

        // now sleep to ensure refresh occurred
        Thread.sleep(REFRESH_PERIOD + maxInitialDelay + 50);

        try {
            // exception should have been eaten
            cache.get("testKey");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testGetForce() throws Throwable {

        LoadStrategy<String, MockValue> mockLoadStrategy = new TestLoadStrategy();

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(mockLoadStrategy).refreshInterval(REFRESH_PERIOD).build();

        MockValue retrievedMockObject = cache.get("testKey");
        assertNotNull(retrievedMockObject);
        assertTrue(retrievedMockObject == mockObject);
        mockObject = new MockObject("another test");

        MockValue updateFetchOfMockObject = cache.get("testKey");
        assertTrue(updateFetchOfMockObject == retrievedMockObject);
        assertTrue(updateFetchOfMockObject != mockObject);

        MockValue nextUpdateFetchOfMockObject = cache.getForceRefresh("testKey");
        assertTrue(nextUpdateFetchOfMockObject != updateFetchOfMockObject);
        assertTrue(nextUpdateFetchOfMockObject == mockObject);
    }

    @Test
    public void testGetDefaultValue() throws Throwable {

        SelfRefreshingCache.MIN_REFRESH_INTERVAL = REFRESH_PERIOD;
        MockValue defaultValue = new MockObject("default");

        LoadStrategy<String, MockValue> loadStrategyThrowingException = new LoadStrategy<String, MockValue>() {

            @Override
            public MockValue load(String key) throws Throwable {
                throw new SelfRefreshingCacheException();
            }
        };

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(loadStrategyThrowingException).refreshInterval(REFRESH_PERIOD).defaultValue(defaultValue).build();

        MockValue retrievedMockObject = cache.get("testKey");
        assertNotNull(retrievedMockObject);
        assertTrue(retrievedMockObject == defaultValue);
    }

    @Test
    public void testGetDefaultValueOnInitialLoad() throws Throwable {

        SelfRefreshingCache.MIN_REFRESH_INTERVAL = REFRESH_PERIOD;
        MockValue defaultValue = new MockObject("default");
        LoadStrategy<String, MockValue> mockLoadStrategy = new TestLoadStrategy();

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(mockLoadStrategy).refreshInterval(REFRESH_PERIOD).defaultValue(defaultValue).useDefaultValueForInitialLoad().build();

        MockValue retrievedMockObject = cache.get("testKey");
        assertNotNull(retrievedMockObject);
        assertTrue(retrievedMockObject == defaultValue);
    }

    @Test
    public void testGetRandomInitialDelay() throws IllegalArgumentException {

        LoadStrategy<String, MockValue> mockLoadStrategy = new TestLoadStrategy();

        SelfRefreshingCache.Builder<String, MockValue> builder = SelfRefreshingCache.builder();
        SelfRefreshingCache<String, MockValue> cache = builder.loadStrategy(mockLoadStrategy).refreshInterval(REFRESH_PERIOD).build();

        for (int i = 0; i < 10000; i++) {
            long randomOffset = cache.getRandomDelayOffset(cache.getMaxInitialDelay());
            assertTrue(randomOffset >= 0 && randomOffset <= 1000);
        }
    }

    class TestLoadStrategy implements LoadStrategy<String, MockValue> {
        public Map<String, Integer> loadCounter = new HashMap<String, Integer>();

        boolean returnNull = true;

        @Override
        public MockValue load(String key) throws Throwable {
            loadCounter.put(key, loadCounter.get(key) == null ? 1 : loadCounter.get(key) + 1);
            if (key.equals("null") && returnNull) return null;
            return mockObject.getValue();
        }

        public void returnNull(boolean returnNull) {
            this.returnNull = returnNull;
        }
    }
}
