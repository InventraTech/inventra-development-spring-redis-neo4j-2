package com.inventra.api.core.service.kitchen.model.request;

public record CreateKitchenRequest(
        String name,
        String code,
        String address
) {
}
