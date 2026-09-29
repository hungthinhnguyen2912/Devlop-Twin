package com.devtwin.connector;

import java.time.Instant;
import java.util.List;

public record IngestStatus(
        String platform,
        String username,
        long totalItems,
        Instant lastFetchedAt,
        List<DataTypeStatus> dataTypes
) {
    public IngestStatus {
        dataTypes = List.copyOf(dataTypes);
    }

    public record DataTypeStatus(String type, long count, Instant lastFetchedAt) {
    }
}
