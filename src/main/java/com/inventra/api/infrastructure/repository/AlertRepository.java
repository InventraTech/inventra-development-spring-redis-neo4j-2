package com.inventra.api.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.alert.Alert;

public interface AlertRepository extends JpaRepository<Alert, Integer> {

    @EntityGraph(attributePaths = {"batch", "product", "kitchen"})
    List<Alert> findByKitchenId(Integer kitchenId);

    @EntityGraph(attributePaths = {"batch", "product", "kitchen"})
    List<Alert> findByKitchenIdAndReadFalse(Integer kitchenId);
}
