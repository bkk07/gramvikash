package com.learn.lld.gramvikash.user.repository;

import com.learn.lld.gramvikash.user.entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {
    Optional<Farmer> findByUserName(String userName);
    Optional<Farmer> findByPhoneNumber(String phoneNumber);
    boolean existsByUserName(String userName);
    boolean existsByPhoneNumber(String phoneNumber);
}
