package com.learn.lld.gramvikash.emergency.repository;

import com.learn.lld.gramvikash.emergency.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    /**
     * Finds the nearest available doctor using the Haversine formula.
     * Distance is calculated at the DB level — no in-memory filtering.
     * Returns the single closest available doctor.
     */
    @Query(value = """
            SELECT *, (
                6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(latitude))
                    * COS(RADIANS(longitude) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
                )
            ) AS distance_km
            FROM doctors
            WHERE available = true
            ORDER BY distance_km ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Doctor> findNearestAvailableDoctor(@Param("lat") double lat, @Param("lng") double lng);

    /**
     * Finds all available doctors within a given radius (km) using Haversine.
     */
    @Query(value = """
            SELECT *, (
                6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(latitude))
                    * COS(RADIANS(longitude) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
                )
            ) AS distance_km
            FROM doctors
            WHERE available = true
            HAVING distance_km <= :radiusKm
            ORDER BY distance_km ASC
            """, nativeQuery = true)
    java.util.List<Doctor> findAvailableDoctorsWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm);
}
