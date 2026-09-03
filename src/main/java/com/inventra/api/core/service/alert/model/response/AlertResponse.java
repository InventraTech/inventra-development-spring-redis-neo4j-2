package com.inventra.api.core.service.alert.model.response;

import java.time.LocalDateTime;

import com.inventra.api.core.domain.alert.Alert;
import com.inventra.api.core.domain.alert.enums.AlertSeverity;

public record AlertResponse(
        Integer id,
        String type,
        AlertSeverity severity,
        BatchSummary batch,
        ProductSummary product,
        KitchenSummary kitchen,
        String message,
        Boolean read,
        LocalDateTime createdAt
) {
    public record BatchSummary(Integer id, String batchNumber) {
    }

    public record ProductSummary(Integer id, String name) {
    }

    public record KitchenSummary(Integer id, String name) {
    }

    public static AlertResponse fromEntity(Alert alert) {
        var batch = alert.getBatch() != null
                ? new BatchSummary(alert.getBatch().getId(), alert.getBatch().getBatchNumber())
                : null;
        var product = alert.getProduct() != null
                ? new ProductSummary(alert.getProduct().getId(), alert.getProduct().getName())
                : null;

        return new AlertResponse(
                alert.getId(),
                alert.getType(),
                alert.getSeverity(),
                batch,
                product,
                new KitchenSummary(alert.getKitchen().getId(), alert.getKitchen().getName()),
                alert.getMessage(),
                alert.getRead(),
                alert.getCreatedAt()
        );
    }
}
