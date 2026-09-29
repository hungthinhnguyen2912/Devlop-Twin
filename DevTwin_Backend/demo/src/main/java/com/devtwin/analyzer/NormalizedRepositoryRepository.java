package com.devtwin.analyzer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NormalizedRepositoryRepository extends JpaRepository<NormalizedRepositoryEntity, Long> {

    @EntityGraph(attributePaths = "technologies")
    List<NormalizedRepositoryEntity> findAllByPlatformAndUsername(String platform, String username);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from NormalizedRepositoryEntity repository "
            + "where repository.platform = :platform and repository.username = :username")
    int deleteSnapshot(String platform, String username);
}
