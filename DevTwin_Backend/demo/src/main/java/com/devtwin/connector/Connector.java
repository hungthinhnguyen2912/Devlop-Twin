package com.devtwin.connector;

public interface Connector {

    String platform();

    RawFetchResult fetch(String username);
}
