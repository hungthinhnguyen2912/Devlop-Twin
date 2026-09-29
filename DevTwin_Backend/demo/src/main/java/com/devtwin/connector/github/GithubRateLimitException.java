package com.devtwin.connector.github;

public class GithubRateLimitException extends GithubApiException {

    public GithubRateLimitException(String message) {
        super(429, message);
    }
}
