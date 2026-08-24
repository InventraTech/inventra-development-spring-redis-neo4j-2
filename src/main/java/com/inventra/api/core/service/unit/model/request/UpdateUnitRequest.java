package com.inventra.api.core.service.unit.model.request;

public record UpdateUnitRequest(
        String symbol,
        String description
) {
}
