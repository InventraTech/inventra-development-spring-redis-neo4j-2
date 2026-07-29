package com.inventra.api.application.service.Product.model.request;

public record CreateProductRequest(
        String name,
        String brand,
        Integer categoryId,
        Integer unitId,
        String barcode,
        String photoUrl
) {
}
