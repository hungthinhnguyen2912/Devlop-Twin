package com.devtwin.connector;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RawDataStore {

    private final RawDataRepository repository;

    public RawDataStore(RawDataRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void upsert(String platform, String username, String dataType, String ref, JsonNode payload) {
        String normalizedUsername = normalize(username);
        Instant now = Instant.now();
        RawData rawData = repository
                .findByPlatformAndUsernameAndDataTypeAndRef(platform, normalizedUsername, dataType, ref)
                .map(existing -> {
                    existing.refresh(payload, now);
                    return existing;
                })
                .orElseGet(() -> new RawData(platform, normalizedUsername, dataType, ref, payload, now));

        repository.save(rawData);
    }

    @Transactional(readOnly = true)
    public boolean isFresh(String platform, String username, String dataType, String ref, Duration maxAge) {
        Instant cutoff = Instant.now().minus(maxAge);
        return repository
                .findByPlatformAndUsernameAndDataTypeAndRef(platform, normalize(username), dataType, ref)
                .map(RawData::getFetchedAt)
                .filter(fetchedAt -> fetchedAt.isAfter(cutoff))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public long count(String platform, String username, String dataType) {
        return repository.countByPlatformAndUsernameAndDataType(platform, normalize(username), dataType);
    }

    @Transactional(readOnly = true)
    public IngestStatus status(String platform, String username) {
        String normalizedUsername = normalize(username);
        List<IngestStatus.DataTypeStatus> dataTypes = repository.summarize(platform, normalizedUsername).stream()
                .map(item -> new IngestStatus.DataTypeStatus(
                        item.getDataType(),
                        item.getItemCount(),
                        item.getLastFetchedAt()
                ))
                .toList();
        long totalItems = dataTypes.stream().mapToLong(IngestStatus.DataTypeStatus::count).sum();
        Instant lastFetchedAt = dataTypes.stream()
                .map(IngestStatus.DataTypeStatus::lastFetchedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new IngestStatus(platform, normalizedUsername, totalItems, lastFetchedAt, dataTypes);
    }

    private String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
