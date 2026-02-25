package com.learn.lld.gramvikash.user.repository;

import com.learn.lld.gramvikash.user.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {
    Optional<State> findByName(String name);
    boolean existsByName(String name);
}
