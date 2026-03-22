package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.ProductRequest;
import com.liquorshop.inventory.dto.ProductResponse;
import com.liquorshop.inventory.entity.ProductEntity;
import com.liquorshop.inventory.exception.ResourceNotFoundException;
import com.liquorshop.inventory.mapper.ProductMapper;
import com.liquorshop.inventory.repository.BatchRepository;
import com.liquorshop.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll(boolean excludeQuantityZero) {
        return productRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .filter(p -> !excludeQuantityZero || p.getCurrentStock() > 0)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return this.productMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (request.getBarcode() != null && productRepository.existsByBarcodeAndDeletedFalse(request.getBarcode())) {
            throw new IllegalArgumentException("Barcode already exists: " + request.getBarcode());
        }
        ProductEntity entity = new ProductEntity();
        applyRequest(entity, request);
        return this.productMapper.toResponse(productRepository.save(entity));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        ProductEntity entity = findOrThrow(id);
        if (request.getBarcode() != null
                && !request.getBarcode().equals(entity.getBarcode())
                && productRepository.existsByBarcodeAndDeletedFalse(request.getBarcode())) {
            throw new IllegalArgumentException("Barcode already exists: " + request.getBarcode());
        }
        applyRequest(entity, request);
        return this.productMapper.toResponse(productRepository.save(entity));
    }

    @Transactional
    public void toggleStatus(Long id) {
        ProductEntity entity = findOrThrow(id);
        entity.setStatus("ACTIVE".equals(entity.getStatus()) ? "INACTIVE" : "ACTIVE");
        productRepository.save(entity);
    }

    // Soft delete — sets deleted=true, hidden from all frontend queries
    @Transactional
    public void delete(Long id) {
        ProductEntity entity = findOrThrow(id);
        entity.setDeleted(true);
        productRepository.save(entity);
    }

    public ProductEntity findOrThrow(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void applyRequest(ProductEntity entity, ProductRequest request) {
        entity.setName(request.getName());
        entity.setBrand(request.getBrand());
        entity.setCategory(request.getCategory());
        entity.setVolumeMl(request.getVolumeMl());
        entity.setBarcode(request.getBarcode());
        if (request.getMinStock() != null) entity.setMinStock(request.getMinStock());
        if (request.getSellingPrice() != null) entity.setSellingPrice(request.getSellingPrice());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        entity.setType(request.getType());
        entity.setMrp(request.getMrp());
        entity.setAlcoholPercentage(request.getAlcoholPercentage());
    }

}
