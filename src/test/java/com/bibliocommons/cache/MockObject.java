package com.bibliocommons.cache;

class MockObject implements MockValue {

    String value;

    public MockObject(String value) {
        this.value = value;
    }

    @Override
    public MockValue getValue() {
        return this;
    }
}
