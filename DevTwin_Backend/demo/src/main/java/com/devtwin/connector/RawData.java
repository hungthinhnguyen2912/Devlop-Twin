package com.devtwin.connector;

import tools.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "raw_data",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_raw_data",
                columnNames = {"platform", "username", "data_type", "ref"}
        )
)
public class RawData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType;

    @Column(nullable = false, length = 500)
    private String ref;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected RawData() {
    }

    public RawData(String platform, String username, String dataType, String ref, JsonNode payload, Instant fetchedAt) {
        this.platform = platform;
        this.username = username;
        this.dataType = dataType;
        this.ref = ref;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }

    public void refresh(JsonNode payload, Instant fetchedAt) {
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }

    public String getDataType() {
        return dataType;
    }

    public String getRef() {
        return ref;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}
