package com.learn.lld.gramvikash.emergency.repository;

import com.learn.lld.gramvikash.emergency.entity.EmergencyRequest;
import com.learn.lld.gramvikash.emergency.enums.EmergencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, UUID> {

    List<EmergencyRequest> findByFarmerId(Long farmerId);

    List<EmergencyRequest> findByStatus(EmergencyStatus status);

    List<EmergencyRequest> findByFarmerIdAndStatus(Long farmerId, EmergencyStatus status);

    /**
     * Counts how many emergencies of the same type have been reported
     * within a given radius (km) and within the given time window,
     * using the Haversine formula — entirely at DB level.
     */
    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT id, (
                    6371 * ACOS(
                        COS(RADIANS(:lat)) * COS(RADIANS(latitude))
                        * COS(RADIANS(longitude) - RADIANS(:lng))
                        + SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
                    )
                ) AS distance_km
                FROM emergency_requests
                WHERE emergency_type = :emergencyType
                  AND timestamp >= :since
                HAVING distance_km <= :radiusKm
            ) AS cluster_count
            """, nativeQuery = true)
    int countRecentSameTypeInRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("emergencyType") String emergencyType,
            @Param("since") LocalDateTime since);
}
