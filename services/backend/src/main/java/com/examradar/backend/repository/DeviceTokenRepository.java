package com.examradar.backend.repository;

import com.examradar.backend.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findByUserIdAndIsActiveTrue(UUID userId);
    Optional<DeviceToken> findByToken(String token);
}
