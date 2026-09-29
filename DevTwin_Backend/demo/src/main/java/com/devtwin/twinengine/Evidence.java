package com.devtwin.twinengine;

public record Evidence(
        String evidenceType,
        String repoFullName,
        String detail,
        String url
) {
}
