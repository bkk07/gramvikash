package com.learn.lld.gramvikash.user.repository;

import com.learn.lld.gramvikash.user.entity.UserKnownField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserKnownFieldRepository extends JpaRepository<UserKnownField, Long> {

    List<UserKnownField> findByFarmerId(Long farmerId);

    Optional<UserKnownField> findByFarmerIdAndFieldName(Long farmerId, String fieldName);

    void deleteByFarmerId(Long farmerId);
}
