package com.inventra.api.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inventra.api.core.domain.kitchen.Kitchen;

import jakarta.persistence.LockModeType;

public interface KitchenRepository extends JpaRepository<Kitchen, Integer> {

    Optional<Kitchen> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    // Serializa operações "verifica e cria" por cozinha (ex.: só um inventário OPEN por cozinha).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM Kitchen k WHERE k.id = :id")
    Optional<Kitchen> findByIdForUpdate(@Param("id") Integer id);
}
