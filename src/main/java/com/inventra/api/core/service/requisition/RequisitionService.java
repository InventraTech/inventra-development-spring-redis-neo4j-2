package com.inventra.api.core.service.requisition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventra.api.core.service.requisition.model.request.AddRequisitionItemRequest;
import com.inventra.api.core.service.requisition.model.request.CreateRequisitionRequest;
import com.inventra.api.core.service.stockbatch.StockBatchUseCase;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.requisition.Requisition;
import com.inventra.api.core.domain.requisition.RequisitionItem;
import com.inventra.api.core.domain.requisition.enums.RequisitionStatus;
import com.inventra.api.core.domain.supplier.Supplier;
import com.inventra.api.core.domain.user.User;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.ProductRepository;
import com.inventra.api.infrastructure.repository.RequisitionItemRepository;
import com.inventra.api.infrastructure.repository.RequisitionRepository;
import com.inventra.api.infrastructure.repository.SupplierRepository;
import com.inventra.api.infrastructure.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequisitionService implements RequisitionUseCase {

    private final RequisitionRepository repository;
    private final RequisitionItemRepository itemRepository;
    private final KitchenRepository kitchenRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StockBatchUseCase stockBatchUseCase;

    @Override
    public Requisition create(CreateRequisitionRequest request) {
        Kitchen kitchen = kitchenRepository.findById(request.kitchenId())
            .orElseThrow(() -> new RuntimeException("Cozinha não encontrada."));
        User requester = userRepository.findById(request.requesterId())
            .orElseThrow(() -> new RuntimeException("Usuário requisitante não encontrado."));

        Requisition requisition = Requisition.builder()
            .type(request.type())
            .origin(request.origin())
            .status(RequisitionStatus.EM_ANALISE)
            .kitchen(kitchen)
            .requester(requester)
            .build();

        return repository.save(requisition);
    }

    @Override
    public Requisition addItem(Integer requisitionId, AddRequisitionItemRequest request) {
        Requisition requisition = findEditableRequisition(requisitionId);

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new RuntimeException("Produto não encontrado."));

        Supplier suggestedSupplier = null;
        if (request.suggestedSupplierId() != null) {
            suggestedSupplier = supplierRepository.findById(request.suggestedSupplierId())
                .orElseThrow(() -> new RuntimeException("Fornecedor não encontrado."));
        }

        RequisitionItem item = RequisitionItem.builder()
            .requisition(requisition)
            .product(product)
            .suggestedSupplier(suggestedSupplier)
            .quantity(request.quantity())
            .estimatedPrice(request.estimatedPrice())
            .note(request.note())
            .build();

        itemRepository.save(item);
        return requisition;
    }

    @Override
    public Requisition removeItem(Integer requisitionId, Integer itemId) {
        Requisition requisition = findEditableRequisition(requisitionId);

        RequisitionItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item não encontrado."));
        if (!item.getRequisition().getId().equals(requisitionId)) {
            throw new RuntimeException("Item não pertence a essa requisição.");
        }

        itemRepository.delete(item);
        return requisition;
    }

    @Override
    public Requisition submit(Integer requisitionId) {
        Requisition requisition = findEditableRequisition(requisitionId);

        if (itemRepository.countByRequisitionId(requisitionId) == 0) {
            throw new RuntimeException("Requisição sem itens não pode ser enviada.");
        }

        // o enum RequisitionStatus não tem um status de rascunho separado de EM_ANALISE;
        // submit só valida que a requisição está pronta, não muda o status.
        return requisition;
    }

    @Override
    @Transactional
    public Requisition approve(Integer requisitionId, UUID approverId) {
        Requisition requisition = findEditableRequisition(requisitionId);
        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new RuntimeException("Usuário aprovador não encontrado."));

        requisition.setStatus(RequisitionStatus.APROVADO);
        requisition.setApprover(approver);
        requisition.setApprovedAt(LocalDateTime.now());
        Requisition saved = repository.save(requisition);

        List<RequisitionItem> items = itemRepository.findByRequisitionId(requisitionId);
        for (RequisitionItem item : items) {
            stockBatchUseCase.consumeForProduct(
                requisition.getKitchen().getId(), item.getProduct().getId(), item.getQuantity());
        }

        return saved;
    }

    @Override
    public Requisition reject(Integer requisitionId, String reason) {
        Requisition requisition = findEditableRequisition(requisitionId);

        requisition.setStatus(RequisitionStatus.REPROVADO);
        requisition.setReason(reason);

        return repository.save(requisition);
    }

    @Override
    public List<Requisition> listByKitchen(Integer kitchenId) {
        return repository.findByKitchenId(kitchenId);
    }

    @Override
    public List<Requisition> listByStatus(RequisitionStatus status) {
        return repository.findByStatus(status);
    }

    @Override
    public List<Requisition> listByRequester(UUID requesterId) {
        return repository.findByRequesterId(requesterId);
    }

    private Requisition findEditableRequisition(Integer requisitionId) {
        Requisition requisition = repository.findById(requisitionId)
            .orElseThrow(() -> new RuntimeException("Requisição não encontrada."));

        if (requisition.getStatus() != RequisitionStatus.EM_ANALISE) {
            throw new RuntimeException("Requisição não está mais em análise.");
        }

        return requisition;
    }
}
