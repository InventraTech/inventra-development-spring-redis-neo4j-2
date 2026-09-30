package com.inventra.api.core.service.productqueue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventra.api.core.service.product.ProductUseCase;
import com.inventra.api.core.service.product.model.request.CreateProductRequest;
import com.inventra.api.core.service.productqueue.model.ProductRegistrationJob;
import com.inventra.api.infrastructure.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductRegistrationProcessor {
    private final ProductRepository repository;
    private final ProductUseCase productUseCase;

    /** Retomadas após commit no PostgreSQL reutilizam o produto, sem cadastrar duas vezes. */
    @Transactional
    public Integer process(ProductRegistrationJob job) {
        var request = job.request();
        return repository.findByBarcode(request.barcode()).map(product -> product.getId())
                .orElseGet(() -> productUseCase.create(new CreateProductRequest(request.name(), request.brand(),
                        request.categoryId(), request.unitId(), request.barcode(), request.photoUrl())).getId());
    }
}
