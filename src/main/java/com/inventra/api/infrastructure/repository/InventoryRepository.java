package com.inventra.api.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.inventory.Inventory;
import com.inventra.api.core.domain.inventory.enums.InventoryStatus;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    List<Inventory> findByKitchenId(Integer kitchenId);

    boolean existsByKitchenIdAndStatus(Integer kitchenId, InventoryStatus status);

}
