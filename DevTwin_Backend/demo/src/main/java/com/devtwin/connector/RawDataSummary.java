package com.devtwin.connector;

import java.time.Instant;

public interface RawDataSummary {

    String getDataType();

    long getItemCount();

    Instant getLastFetchedAt();
}
