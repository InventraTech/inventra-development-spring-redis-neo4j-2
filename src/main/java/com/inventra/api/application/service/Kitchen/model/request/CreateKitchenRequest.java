package com.inventra.api.application.service.Kitchen.model.request;

public record CreateKitchenRequest(
        String name,
        String code,
        String address
) {
}
