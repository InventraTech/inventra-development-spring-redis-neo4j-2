package com.inventra.api.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.product.ProductKitchenParameter;
import com.inventra.api.core.domain.product.ProductKitchenParameterId;

public interface ProductKitchenParameterRepository extends JpaRepository<ProductKitchenParameter, ProductKitchenParameterId> {

    List<ProductKitchenParameter> findByProduct_Id(Integer productId);

}
