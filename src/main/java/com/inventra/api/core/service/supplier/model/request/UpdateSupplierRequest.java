package com.inventra.api.core.service.supplier.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(
        @Size(max = 150)
        String legalName,

        @Email
        @Size(max = 150)
        String email,

        @Size(max = 20)
        String whatsapp,

        @Min(1)
        @Max(5)
        Integer rating
) {
}
