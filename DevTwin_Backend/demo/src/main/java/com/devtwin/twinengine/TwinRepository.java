package com.devtwin.twinengine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TwinRepository extends JpaRepository<TwinEntity, Long> {

    Optional<TwinEntity> findByPlatformAndUsername(String platform, String username);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TwinEntity twin where twin.platform = :platform and twin.username = :username")
    int deleteSnapshot(String platform, String username);
}
