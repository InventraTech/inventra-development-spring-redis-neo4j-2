package com.inventra.api.core.service.kitchenaccess.model.request;

import jakarta.validation.constraints.Size;

public record RejectKitchenAccessRequest(
        @Size(max = 255)
        String reason
) {
}
