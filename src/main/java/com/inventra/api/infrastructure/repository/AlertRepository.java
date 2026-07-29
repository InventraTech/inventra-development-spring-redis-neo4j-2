package com.inventra.api.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.alert.Alert;

public interface AlertRepository extends JpaRepository<Alert, Integer> {

}
