package com.learn.lld.gramvikash.schemes.repository;

import com.learn.lld.gramvikash.schemes.entity.SchemeFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeFaqRepository extends JpaRepository<SchemeFaq, Long> {

    List<SchemeFaq> findBySchemeIdAndLanguageAndIsActiveTrueOrderByDisplayOrderAsc(Long schemeId, String language);

    List<SchemeFaq> findBySchemeIdAndIsActiveTrueOrderByDisplayOrderAsc(Long schemeId);
}
