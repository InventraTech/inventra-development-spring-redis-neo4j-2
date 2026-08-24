package com.inventra.api.core.service.unit.model.request;

public record CreateUnitRequest(
        String symbol,
        String description
) {
}
