package com.inventra.api.core.service.product.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @Size(max = 150)
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        String name,

        @Size(max = 80)
        String brand,

        Integer categoryId,

        Integer unitId,

        @Size(max = 50)
        String barcode
) {
}
