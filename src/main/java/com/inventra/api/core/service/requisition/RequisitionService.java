package com.inventra.api.core.service.requisition;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventra.api.core.service.requisition.model.request.AddRequisitionItemRequest;
import com.inventra.api.core.service.requisition.model.request.CreateRequisitionRequest;
import com.inventra.api.core.service.stockbatch.StockBatchUseCase;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.product.ProductSupplier;
import com.inventra.api.core.domain.product.ProductSupplierId;
import com.inventra.api.core.domain.requisition.Requisition;
import com.inventra.api.core.domain.requisition.RequisitionItem;
import com.inventra.api.core.domain.requisition.enums.RequisitionStatus;
import com.inventra.api.core.domain.requisition.enums.RequisitionType;
import com.inventra.api.core.domain.supplier.Supplier;
import com.inventra.api.core.domain.user.User;
import com.inventra.api.infrastructure.exception.BusinessRuleException;
import com.inventra.api.infrastructure.exception.ResourceNotFoundException;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.ProductRepository;
import com.inventra.api.infrastructure.repository.ProductSupplierRepository;
import com.inventra.api.infrastructure.repository.RequisitionItemRepository;
import com.inventra.api.infrastructure.repository.RequisitionRepository;
import com.inventra.api.infrastructure.repository.SupplierRepository;
import com.inventra.api.infrastructure.repository.UserRepository;
import com.inventra.api.infrastructure.security.KitchenAccessGuard;

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
    private final ProductSupplierRepository productSupplierRepository;
    private final StockBatchUseCase stockBatchUseCase;
    private final KitchenAccessGuard accessGuard;

    @Override
    public Requisition create(CreateRequisitionRequest request) {
        accessGuard.assertAccess(request.kitchenId());
        Kitchen kitchen = kitchenRepository.findById(request.kitchenId())
            .orElseThrow(() -> new ResourceNotFoundException("Cozinha não encontrada."));
        if (!Boolean.TRUE.equals(kitchen.getActive())) {
            throw new BusinessRuleException("Cozinha desativada: reative a cozinha para abrir requisições.");
        }
        // o solicitante é sempre o usuário logado (não vem do body)
        User requester = userRepository.findById(accessGuard.currentUser().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Usuário requisitante não encontrado."));

        Requisition requisition = Requisition.builder()
            .type(request.type())
            .origin(request.origin())
            .status(RequisitionStatus.UNDER_REVIEW)
            .kitchen(kitchen)
            .requester(requester)
            .build();

        return repository.save(requisition);
    }

    @Override
    public Requisition addItem(Integer requisitionId, AddRequisitionItemRequest request) {
        Requisition requisition = findEditableRequisition(requisitionId);
        assertOwnerOrSupervisor(requisition);

        Product product = productRepository.findById(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
        if (!product.isActive()) {
            throw new BusinessRuleException("Produto desativado não pode ser requisitado.");
        }

        Supplier suggestedSupplier = null;
        if (request.suggestedSupplierId() != null) {
            suggestedSupplier = supplierRepository.findById(request.suggestedSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor não encontrado."));
        }

        BigDecimal estimatedPrice = request.estimatedPrice() != null
            ? request.estimatedPrice()
            : suggestReferencePrice(request.productId(), request.suggestedSupplierId());

        RequisitionItem item = RequisitionItem.builder()
            .requisition(requisition)
            .product(product)
            .suggestedSupplier(suggestedSupplier)
            .quantity(request.quantity())
            .estimatedPrice(estimatedPrice)
            .note(request.note())
            .build();

        itemRepository.save(item);
        return requisition;
    }

    @Override
    public Requisition removeItem(Integer requisitionId, Integer itemId) {
        Requisition requisition = findEditableRequisition(requisitionId);
        assertOwnerOrSupervisor(requisition);

        RequisitionItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado."));
        if (!item.getRequisition().getId().equals(requisitionId)) {
            throw new BusinessRuleException("Item não pertence a essa requisição.");
        }

        itemRepository.delete(item);
        return requisition;
    }

    @Override
    public Requisition submit(Integer requisitionId) {
        Requisition requisition = findEditableRequisition(requisitionId);
        assertOwnerOrSupervisor(requisition);

        if (itemRepository.countByRequisitionId(requisitionId) == 0) {
            throw new BusinessRuleException("Requisição sem itens não pode ser enviada.");
        }

        // o enum RequisitionStatus não tem um status de rascunho separado de UNDER_REVIEW;
        // submit só valida que a requisição está pronta, não muda o status.
        return requisition;
    }

    @Override
    @Transactional
    public Requisition approve(Integer requisitionId) {
        Requisition requisition = findEditableRequisition(requisitionId);
        Integer kitchenId = requisition.getKitchen().getId();
        RequisitionType type = requisition.getType();
        if (itemRepository.countByRequisitionId(requisitionId) == 0) {
            throw new BusinessRuleException("Requisição sem itens não pode ser aprovada.");
        }

        // o aprovador é sempre o usuário logado (não vem do body);
        // sp_approve_requisition muda status/aprovador e o trigger trg_requisition_approval preenche approved_at
        repository.callApproveRequisition(requisitionId, accessGuard.currentUser().getId());

        // PURCHASE não mexe no estoque: a entrada acontece quando a mercadoria chega (POST /api/stock-batches),
        // já que o item de requisição não tem lote/validade pra virar um lote.
        // TRANSFER é a saída da cozinha da requisição (o destino é texto livre em origin, sem cozinha
        // vinculada); a cozinha que recebe registra a entrada dela pelo POST /api/stock-batches.
        if (type == RequisitionType.CONSUMPTION || type == RequisitionType.TRANSFER) {
            List<RequisitionItem> items = itemRepository.findByRequisitionId(requisitionId);
            for (RequisitionItem item : items) {
                stockBatchUseCase.consumeForProduct(kitchenId, item.getProduct().getId(), item.getQuantity());
            }
        }

        return reload(requisitionId);
    }

    @Override
    @Transactional
    public Requisition reject(Integer requisitionId, String reason) {
        findEditableRequisition(requisitionId);

        repository.callRejectRequisition(requisitionId, accessGuard.currentUser().getId(), reason);
        return reload(requisitionId);
    }

    @Override
    @Transactional
    public Requisition cancel(Integer requisitionId, String reason) {
        Requisition requisition = loadRequisition(requisitionId);
        assertOwnerOrSupervisor(requisition);
        // cancelar uma requisição já aprovada (estoque já baixado) é decisão do supervisor
        if (requisition.getStatus() == RequisitionStatus.APPROVED && !accessGuard.isSupervisor()) {
            throw new AccessDeniedException("Somente supervisor pode cancelar uma requisição já aprovada.");
        }

        // sp_cancel_requisition aceita UNDER_REVIEW ou APPROVED; não devolve ao estoque o que a aprovação consumiu
        repository.callCancelRequisition(requisitionId, reason);
        return reload(requisitionId);
    }

    @Override
    public List<Requisition> listByKitchen(Integer kitchenId) {
        accessGuard.assertAccess(kitchenId);
        return repository.findByKitchenId(kitchenId);
    }

    @Override
    public List<Requisition> listByStatus(RequisitionStatus status) {
        Integer kitchenId = accessGuard.currentKitchenId();
        return kitchenId == null ? List.of() : repository.findByKitchenIdAndStatus(kitchenId, status);
    }

    @Override
    public List<Requisition> listByRequester(UUID requesterId) {
        Integer kitchenId = accessGuard.currentKitchenId();
        return kitchenId == null ? List.of() : repository.findByKitchenIdAndRequesterId(kitchenId, requesterId);
    }

    @Override
    public List<RequisitionItem> listItems(Integer requisitionId) {
        loadRequisition(requisitionId);
        return itemRepository.findByRequisitionId(requisitionId);
    }

    // Sem preço informado: usa o referencePrice já vinculado (linkSupplier) como sugestão, se existir.
    private BigDecimal suggestReferencePrice(Integer productId, Integer supplierId) {
        if (supplierId == null) {
            return null;
        }
        return productSupplierRepository.findById(new ProductSupplierId(productId, supplierId))
            .map(ProductSupplier::getReferencePrice)
            .orElse(null);
    }

    private Requisition loadRequisition(Integer requisitionId) {
        Requisition requisition = repository.findById(requisitionId)
            .orElseThrow(() -> new ResourceNotFoundException("Requisição não encontrada."));
        accessGuard.assertAccess(requisition.getKitchen().getId());
        return requisition;
    }

    private Requisition reload(Integer requisitionId) {
        return repository.findById(requisitionId)
            .orElseThrow(() -> new ResourceNotFoundException("Requisição não encontrada."));
    }

    // Comprador só mexe nas próprias requisições; supervisor mexe em todas da cozinha.
    private void assertOwnerOrSupervisor(Requisition requisition) {
        boolean owner = requisition.getRequester().getId().equals(accessGuard.currentUser().getId());
        if (!owner && !accessGuard.isSupervisor()) {
            throw new AccessDeniedException("Você só pode alterar as próprias requisições.");
        }
    }

    private Requisition findEditableRequisition(Integer requisitionId) {
        Requisition requisition = loadRequisition(requisitionId);

        if (requisition.getStatus() != RequisitionStatus.UNDER_REVIEW) {
            throw new BusinessRuleException("Requisição não está mais em análise.");
        }

        return requisition;
    }
}
