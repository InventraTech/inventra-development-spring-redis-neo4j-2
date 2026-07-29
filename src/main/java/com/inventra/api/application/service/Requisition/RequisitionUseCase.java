package com.inventra.api.application.service.Requisition;

import java.util.List;
import java.util.UUID;

import com.inventra.api.application.service.Requisition.model.request.AddRequisitionItemRequest;
import com.inventra.api.application.service.Requisition.model.request.CreateRequisitionRequest;
import com.inventra.api.core.domain.requisition.Requisition;
import com.inventra.api.core.domain.requisition.enums.RequisitionStatus;

public interface RequisitionUseCase {

    Requisition create(CreateRequisitionRequest request);

    Requisition addItem(Integer requisitionId, AddRequisitionItemRequest request);

    Requisition removeItem(Integer requisitionId, Integer itemId);

    Requisition submit(Integer requisitionId);

    // define approver, approvedAt, e dispara baixa de estoque via StockBatchUseCase
    Requisition approve(Integer requisitionId, UUID approverId);

    Requisition reject(Integer requisitionId, String reason);

    List<Requisition> listByKitchen(Integer kitchenId);

    List<Requisition> listByStatus(RequisitionStatus status);

    List<Requisition> listByRequester(UUID requesterId);
}
