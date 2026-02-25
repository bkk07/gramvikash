package com.learn.lld.gramvikash.diagnostic.repository;

import com.learn.lld.gramvikash.diagnostic.entity.DiagnosticSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosticSessionRepository extends JpaRepository<DiagnosticSession, Long> {
    List<DiagnosticSession> findByFarmerIdOrderByCreatedAtDesc(Long farmerId);
    List<DiagnosticSession> findBySourceTypeOrderByCreatedAtDesc(String sourceType);
}
