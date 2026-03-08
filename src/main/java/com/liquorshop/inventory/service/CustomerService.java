package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.CustomerRequest;
import com.liquorshop.inventory.dto.CustomerResponse;
import com.liquorshop.inventory.dto.CustomerStatementResponse;
import com.liquorshop.inventory.dto.PriceTemplateRequest;
import com.liquorshop.inventory.dto.PriceTemplateResponse;
import com.liquorshop.inventory.entity.CustomerEntity;
import com.liquorshop.inventory.entity.CustomerPriceTemplateEntity;
import com.liquorshop.inventory.entity.ProductEntity;
import com.liquorshop.inventory.entity.SaleEntity;
import com.liquorshop.inventory.entity.SalePaymentEntity;
import com.liquorshop.inventory.exception.ResourceNotFoundException;
import com.liquorshop.inventory.repository.CustomerPriceTemplateRepository;
import com.liquorshop.inventory.repository.CustomerRepository;
import com.liquorshop.inventory.repository.ProductRepository;
import com.liquorshop.inventory.repository.SalePaymentRepository;
import com.liquorshop.inventory.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerPriceTemplateRepository templateRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final SalePaymentRepository salePaymentRepository;

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        return customerRepository.findAllByOrderByNameAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        CustomerEntity entity = new CustomerEntity();
        applyRequest(entity, request);
        return toResponse(customerRepository.save(entity));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        CustomerEntity entity = findOrThrow(id);
        applyRequest(entity, request);
        return toResponse(customerRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        customerRepository.deleteById(id);
    }

    // ── Price Template ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PriceTemplateResponse> getTemplate(Long customerId) {
        findOrThrow(customerId);
        return templateRepository.findByCustomerIdWithProduct(customerId)
                .stream().map(this::toTemplateResponse).collect(Collectors.toList());
    }

    @Transactional
    public PriceTemplateResponse upsertTemplateEntry(Long customerId, PriceTemplateRequest request) {
        CustomerEntity customer = findOrThrow(customerId);
        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        CustomerPriceTemplateEntity entry = templateRepository
                .findByCustomerIdAndProductId(customerId, request.getProductId())
                .orElseGet(CustomerPriceTemplateEntity::new);

        entry.setCustomer(customer);
        entry.setProduct(product);
        entry.setSellingPrice(request.getSellingPrice());
        return toTemplateResponse(templateRepository.save(entry));
    }

    @Transactional
    public void deleteTemplateEntry(Long customerId, Long productId) {
        findOrThrow(customerId);
        templateRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    // Lookup the custom price for a product for this customer; returns empty if not in template
    @Transactional(readOnly = true)
    public Optional<BigDecimal> findTemplatePrice(Long customerId, Long productId) {
        return templateRepository.findByCustomerIdAndProductId(customerId, productId)
                .map(CustomerPriceTemplateEntity::getSellingPrice);
    }

    // ── Customer Statement ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerStatementResponse getStatement(Long customerId) {
        CustomerEntity customer = findOrThrow(customerId);

        List<SaleEntity> sales = saleRepository.findByCustomerIdOrderBySaleDateDesc(customerId);
        List<SalePaymentEntity> payments = salePaymentRepository.findByCustomerIdOrderByPaymentDateDesc(customerId);

        List<CustomerStatementResponse.StatementEntry> entries = new ArrayList<>();

        for (SaleEntity sale : sales) {
            CustomerStatementResponse.StatementEntry e = new CustomerStatementResponse.StatementEntry();
            e.setDate(sale.getSaleDate());
            e.setType("SALE");
            e.setReference(sale.getInvoiceNumber());
            e.setDebit(sale.getTotalAmount());
            e.setCredit(BigDecimal.ZERO);
            entries.add(e);
        }

        for (SalePaymentEntity payment : payments) {
            CustomerStatementResponse.StatementEntry e = new CustomerStatementResponse.StatementEntry();
            e.setDate(payment.getPaymentDate());
            e.setType("PAYMENT");
            e.setReference("PAY-" + payment.getId());
            e.setPaymentMethod(payment.getPaymentMethod());
            e.setReferenceNumber(payment.getReferenceNumber());
            e.setDebit(BigDecimal.ZERO);
            e.setCredit(payment.getAmount());
            entries.add(e);
        }

        // Sort all entries by date ascending for running balance
        entries.sort(Comparator.comparing(CustomerStatementResponse.StatementEntry::getDate));

        BigDecimal running = BigDecimal.ZERO;
        for (CustomerStatementResponse.StatementEntry e : entries) {
            running = running.add(e.getDebit()).subtract(e.getCredit());
            e.setBalance(running);
        }

        CustomerStatementResponse response = new CustomerStatementResponse();
        response.setCustomerId(customer.getId());
        response.setCustomerName(customer.getName());
        response.setCreditLimit(customer.getCreditLimit());
        response.setOutstandingBalance(customer.getOutstandingBalance());
        response.setEntries(entries);
        return response;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void applyRequest(CustomerEntity entity, CustomerRequest request) {
        entity.setName(request.getName());
        entity.setPhone(request.getPhone());
        entity.setAddress(request.getAddress());
        if (request.getCreditLimit() != null) entity.setCreditLimit(request.getCreditLimit());
    }

    public CustomerEntity findOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private CustomerResponse toResponse(CustomerEntity e) {
        CustomerResponse r = new CustomerResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setPhone(e.getPhone());
        r.setAddress(e.getAddress());
        r.setCreditLimit(e.getCreditLimit());
        r.setOutstandingBalance(e.getOutstandingBalance());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private PriceTemplateResponse toTemplateResponse(CustomerPriceTemplateEntity e) {
        PriceTemplateResponse r = new PriceTemplateResponse();
        r.setId(e.getId());
        r.setCustomerId(e.getCustomer().getId());
        r.setProductId(e.getProduct().getId());
        r.setProductName(e.getProduct().getName());
        r.setProductBrand(e.getProduct().getBrand());
        r.setProductVolumeMl(e.getProduct().getVolumeMl());
        r.setSellingPrice(e.getSellingPrice());
        r.setStandardPrice(e.getProduct().getSellingPrice());
        return r;
    }
}
