package com.bibliocommons.cache;

public class SelfRefreshingCacheException extends RuntimeException {

    public SelfRefreshingCacheException() {
    }

    public SelfRefreshingCacheException(String s) {
        super(s);
    }

    public SelfRefreshingCacheException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
