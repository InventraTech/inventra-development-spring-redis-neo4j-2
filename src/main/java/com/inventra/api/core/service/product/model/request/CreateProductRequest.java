package com.inventra.api.core.service.product.model.request;

public record CreateProductRequest(
        String name,
        String brand,
        Integer categoryId,
        Integer unitId,
        String barcode,
        String photoUrl
) {
}
