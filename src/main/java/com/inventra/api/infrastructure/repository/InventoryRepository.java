package com.inventra.api.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.inventory.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

}
