package com.learn.lld.gramvikash.schemes.repository;

import com.learn.lld.gramvikash.schemes.entity.Scheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {

    List<Scheme> findByIsActiveTrue();

    List<Scheme> findByStateIsNullAndIsActiveTrue();  // Central schemes

    List<Scheme> findByStateAndIsActiveTrue(String state);

    List<Scheme> findByCategoryAndIsActiveTrue(String category);

    List<Scheme> findByStateAndCategoryAndIsActiveTrue(String state, String category);

    List<Scheme> findByStateIsNullAndCategoryAndIsActiveTrue(String category);

    Optional<Scheme> findBySchemeCode(String schemeCode);

    boolean existsBySchemeCode(String schemeCode);
}
