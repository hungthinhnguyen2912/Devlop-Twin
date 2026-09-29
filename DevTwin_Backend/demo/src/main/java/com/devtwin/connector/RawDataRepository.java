package com.devtwin.connector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RawDataRepository extends JpaRepository<RawData, Long> {

    Optional<RawData> findByPlatformAndUsernameAndDataTypeAndRef(
            String platform,
            String username,
            String dataType,
            String ref
    );

    long countByPlatformAndUsernameAndDataType(String platform, String username, String dataType);

    @Query("""
            select r.dataType as dataType, count(r) as itemCount, max(r.fetchedAt) as lastFetchedAt
            from RawData r
            where r.platform = :platform and r.username = :username
            group by r.dataType
            order by r.dataType
            """)
    List<RawDataSummary> summarize(String platform, String username);
}
