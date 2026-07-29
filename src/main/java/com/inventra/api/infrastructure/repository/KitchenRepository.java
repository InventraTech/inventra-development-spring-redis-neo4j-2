package com.inventra.api.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.kitchen.Kitchen;

public interface KitchenRepository extends JpaRepository<Kitchen, Integer> {

    Optional<Kitchen> findByCode(String code);

    boolean existsByCode(String code);

    List<Kitchen> findByActiveTrue();

}
