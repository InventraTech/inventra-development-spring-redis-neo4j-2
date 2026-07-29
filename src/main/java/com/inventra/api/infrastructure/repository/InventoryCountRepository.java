package com.inventra.api.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.inventory.InventoryCount;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Integer> {

}
