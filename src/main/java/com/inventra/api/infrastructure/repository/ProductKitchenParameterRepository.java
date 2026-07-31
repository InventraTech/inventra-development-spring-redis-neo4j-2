package com.inventra.api.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.product.ProductKitchenParameter;
import com.inventra.api.core.domain.product.ProductKitchenParameterId;

public interface ProductKitchenParameterRepository extends JpaRepository<ProductKitchenParameter, ProductKitchenParameterId> {

}
