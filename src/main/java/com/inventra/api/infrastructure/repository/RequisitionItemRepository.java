package com.inventra.api.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventra.api.core.domain.requisition.RequisitionItem;

public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, Integer> {

    List<RequisitionItem> findByRequisitionId(Integer requisitionId);

    long countByRequisitionId(Integer requisitionId);

}
