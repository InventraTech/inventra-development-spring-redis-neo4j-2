package com.inventra.api.core.service.product;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.inventra.api.core.service.product.model.request.CreateProductRequest;
import com.inventra.api.core.service.product.model.request.LinkSupplierRequest;
import com.inventra.api.core.service.product.model.request.SetKitchenParametersRequest;
import com.inventra.api.core.service.product.model.request.UpdateProductRequest;
import com.inventra.api.core.domain.category.Category;
import com.inventra.api.core.domain.kitchen.Kitchen;
import com.inventra.api.core.domain.product.Product;
import com.inventra.api.core.domain.product.ProductKitchenParameter;
import com.inventra.api.core.domain.product.ProductKitchenParameterId;
import com.inventra.api.core.domain.product.ProductSupplier;
import com.inventra.api.core.domain.product.ProductSupplierId;
import com.inventra.api.core.domain.supplier.Supplier;
import com.inventra.api.core.domain.unit.Unit;
import com.inventra.api.infrastructure.repository.CategoryRepository;
import com.inventra.api.infrastructure.repository.KitchenRepository;
import com.inventra.api.infrastructure.repository.ProductKitchenParameterRepository;
import com.inventra.api.infrastructure.repository.ProductRepository;
import com.inventra.api.infrastructure.repository.ProductSupplierRepository;
import com.inventra.api.infrastructure.repository.SupplierRepository;
import com.inventra.api.infrastructure.repository.UnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService implements ProductUseCase {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final SupplierRepository supplierRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final ProductKitchenParameterRepository productKitchenParameterRepository;
    private final KitchenRepository kitchenRepository;

    @Override
    public Product create(CreateProductRequest request) {
        if (request.barcode() != null && repository.existsByBarcode(request.barcode())) {
            throw new RuntimeException("Já existe um produto com esse código de barras.");
        }

        Unit unit = unitRepository.findById(request.unitId())
            .orElseThrow(() -> new RuntimeException("Unidade de medida não encontrada."));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));
        }

        Product product = Product.builder()
            .name(request.name())
            .brand(request.brand())
            .category(category)
            .unit(unit)
            .barcode(request.barcode())
            .photoUrl(request.photoUrl())
            .active(true)
            .build();

        return repository.save(product);
    }

    @Override
    public Product findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produto não encontrado."));
    }

    @Override
    public Page<Product> search(String name, Integer categoryId, Boolean active, Pageable pageable) {
        return repository.search(name, categoryId, active, pageable);
    }

    @Override
    public Product update(Long id, UpdateProductRequest request) {
        Product product = findById(id);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.brand() != null) {
            product.setBrand(request.brand());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));
            product.setCategory(category);
        }
        if (request.unitId() != null) {
            Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new RuntimeException("Unidade de medida não encontrada."));
            product.setUnit(unit);
        }
        if (request.barcode() != null && !request.barcode().equals(product.getBarcode())) {
            if (repository.existsByBarcode(request.barcode())) {
                throw new RuntimeException("Já existe um produto com esse código de barras.");
            }
            product.setBarcode(request.barcode());
        }
        if (request.photoUrl() != null) {
            product.setPhotoUrl(request.photoUrl());
        }

        return repository.save(product);
    }

    @Override
    public void activate(Long id) {
        Product product = findById(id);
        product.setActive(true);
        repository.save(product);
    }

    @Override
    public void deactivate(Long id) {
        Product product = findById(id);
        product.setActive(false);
        repository.save(product);
    }

    @Override
    public void linkSupplier(Long productId, LinkSupplierRequest request) {
        Product product = findById(productId);
        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new RuntimeException("Fornecedor não encontrado."));

        ProductSupplier link = ProductSupplier.builder()
            .id(new ProductSupplierId(productId, supplier.getId()))
            .product(product)
            .supplier(supplier)
            .supplierCode(request.supplierCode())
            .referencePrice(request.referencePrice())
            .leadTimeDays(request.leadTimeDays())
            .build();

        productSupplierRepository.save(link);
    }

    @Override
    public void setKitchenParameters(Long productId, SetKitchenParametersRequest request) {
        Product product = findById(productId);
        Kitchen kitchen = kitchenRepository.findById(request.kitchenId())
            .orElseThrow(() -> new RuntimeException("Cozinha não encontrada."));

        ProductKitchenParameter parameter = ProductKitchenParameter.builder()
            .id(new ProductKitchenParameterId(productId, kitchen.getId()))
            .product(product)
            .kitchen(kitchen)
            .minStock(Objects.requireNonNullElse(request.minStock(), BigDecimal.ZERO))
            .maxStock(request.maxStock())
            .averageDailyConsumption(request.averageDailyConsumption())
            .build();

        productKitchenParameterRepository.save(parameter);
    }
}
