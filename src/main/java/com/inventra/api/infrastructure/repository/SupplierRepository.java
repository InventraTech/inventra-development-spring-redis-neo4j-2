package com.inventra.api.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.supplier.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

}
