package com.learn.lld.gramvikash.emergency.repository;

import com.learn.lld.gramvikash.emergency.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    /**
     * Finds all available drivers within a given radius (km)
     * whose vehicleType is in the provided list, using the Haversine formula.
     * Distance is calculated entirely at the DB level.
     */
    @Query(value = """
            SELECT *, (
                6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(latitude))
                    * COS(RADIANS(longitude) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
                )
            ) AS distance_km
            FROM drivers
            WHERE available = true
              AND vehicle_type IN (:vehicleTypes)
            HAVING distance_km <= :radiusKm
            ORDER BY distance_km ASC
            """, nativeQuery = true)
    List<Driver> findNearbyAvailableDriversByVehicleTypes(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("vehicleTypes") List<String> vehicleTypes);

    /**
     * Finds all available drivers within a given radius (km) regardless of vehicle type.
     */
    @Query(value = """
            SELECT *, (
                6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(latitude))
                    * COS(RADIANS(longitude) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(latitude))
                )
            ) AS distance_km
            FROM drivers
            WHERE available = true
            HAVING distance_km <= :radiusKm
            ORDER BY distance_km ASC
            """, nativeQuery = true)
    List<Driver> findNearbyAvailableDrivers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm);
}
