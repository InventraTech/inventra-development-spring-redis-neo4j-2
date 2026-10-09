package com.inventra.api.core.service.product.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Sem photoUrl: a foto só entra pelo upload (PUT /api/products/{id}/photo), que valida o arquivo e gera
// a URL do Cloudinary — aceitar uma URL livre aqui abria espaço para links maliciosos (ex.: javascript:).
public record CreateProductRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 80)
        String brand,

        Integer categoryId,

        @NotNull
        Integer unitId,

        @Size(max = 50)
        String barcode
) {
}
