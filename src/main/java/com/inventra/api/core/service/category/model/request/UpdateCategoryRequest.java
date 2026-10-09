package com.inventra.api.core.service.category.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @Size(max = 80)
        @Pattern(regexp = ".*\\S.*", message = "não pode ser vazio")
        String name,

        @Size(max = 255)
        String description
) {
}
