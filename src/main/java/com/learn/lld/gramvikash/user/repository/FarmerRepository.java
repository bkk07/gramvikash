package com.learn.lld.gramvikash.user.repository;

import com.learn.lld.gramvikash.user.entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {
    Optional<Farmer> findByUserName(String userName);
    Optional<Farmer> findByPhoneNumber(String phoneNumber);
    boolean existsByUserName(String userName);
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Find all active farmers within a given radius using the Haversine formula.
     * Formula is applied directly in the WHERE clause to avoid extra computed
     * columns that break JPA entity mapping.
     */
    @Query(value = """
                SELECT * FROM farmers f
                WHERE f.is_active = true
                    AND f.latitude IS NOT NULL
                    AND f.longitude IS NOT NULL
          AND (
            6371 * ACOS(
                LEAST(1.0,
                    GREATEST(-1.0,
                        COS(RADIANS(:lat)) * COS(RADIANS(f.latitude))
                        * COS(RADIANS(f.longitude) - RADIANS(:lng))
                        + SIN(RADIANS(:lat)) * SIN(RADIANS(f.latitude))
                    )
                )
            )
          ) <= :radiusKm
        """, nativeQuery = true)
    List<Farmer> findNearbyActiveFarmers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm);
}
