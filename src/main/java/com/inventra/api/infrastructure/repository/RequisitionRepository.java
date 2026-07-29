package com.inventra.api.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.requisition.Requisition;
import com.inventra.api.core.domain.requisition.enums.RequisitionStatus;

public interface RequisitionRepository extends JpaRepository<Requisition, Integer> {

    List<Requisition> findByKitchenId(Integer kitchenId);

    List<Requisition> findByStatus(RequisitionStatus status);

    List<Requisition> findByRequesterId(UUID requesterId);

}
