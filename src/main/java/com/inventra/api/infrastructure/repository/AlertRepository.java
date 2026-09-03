package com.inventra.api.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.alert.Alert;

public interface AlertRepository extends JpaRepository<Alert, Integer> {

    List<Alert> findByKitchenId(Integer kitchenId);

    List<Alert> findByKitchenIdAndReadFalse(Integer kitchenId);

}
