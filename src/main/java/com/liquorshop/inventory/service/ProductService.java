package com.liquorshop.inventory.service;

import com.liquorshop.inventory.model.Product;
import com.liquorshop.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByNameAsc();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Optional<Product> updateProduct(Long id, Product productDetails) {
        return productRepository.findById(id).map(existingProduct -> {
            if (productDetails.getName() != null) {
                existingProduct.setName(productDetails.getName());
            }
            if (productDetails.getCategory() != null) {
                existingProduct.setCategory(productDetails.getCategory());
            }
            if (productDetails.getBrand() != null) {
                existingProduct.setBrand(productDetails.getBrand());
            }
            if (productDetails.getVolumeMl() != null) {
                existingProduct.setVolumeMl(productDetails.getVolumeMl());
            }
            if (productDetails.getUnit() != null) {
                existingProduct.setUnit(productDetails.getUnit());
            }
            if (productDetails.getBarcode() != null) {
                existingProduct.setBarcode(productDetails.getBarcode());
            }
            if (productDetails.getMinStock() != null) {
                existingProduct.setMinStock(productDetails.getMinStock());
            }
            return productRepository.save(existingProduct);
        });
    }

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
