package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.*;
import com.liquorshop.inventory.entity.*;
import com.liquorshop.inventory.exception.ResourceNotFoundException;
import com.liquorshop.inventory.repository.BatchRepository;
import com.liquorshop.inventory.repository.ProductRepository;
import com.liquorshop.inventory.repository.SalePaymentRepository;
import com.liquorshop.inventory.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public List<SaleResponse> getAll() {
        return saleRepository.findAllWithCustomer()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SaleResponse getById(Long id) {
        SaleEntity entity = saleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + id));
        return toDetailResponse(entity);
    }

    @Transactional
    public SaleResponse create(SaleInput input) {
        CustomerEntity customer = null;
        if (input.getCustomerId() != null) {
            customer = customerService.findOrThrow(input.getCustomerId());
        }

        SaleEntity sale = new SaleEntity();
        sale.setCustomer(customer);
        sale.setInvoiceNumber(input.getInvoiceNumber() != null && !input.getInvoiceNumber().isBlank()
                ? input.getInvoiceNumber()
                : generateInvoiceNumber());
        sale.setSaleDate(input.getSaleDate() != null ? input.getSaleDate() : LocalDateTime.now());
        sale.setDiscount(input.getDiscount() != null ? input.getDiscount() : BigDecimal.ZERO);
        sale.setVatAmount(input.getVatAmount() != null ? input.getVatAmount() : BigDecimal.ZERO);
        sale.setPaymentStatus(input.getPaymentStatus() != null ? input.getPaymentStatus().toUpperCase() : "PAID");
        sale.setNotes(input.getNotes());
        sale = saleRepository.save(sale);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SaleItemInput itemInput : input.getItems()) {
            ProductEntity product = productRepository.findById(itemInput.getProductId())
                    .filter(p -> !p.getDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemInput.getProductId()));

            // Determine unit price: explicit override > customer template > product standard price
            BigDecimal unitPrice = resolveUnitPrice(itemInput, customer, product);

            // Snapshot weighted average cost at this moment
            BigDecimal costAtSale = product.getAverageCost();

            // Allocate stock from batches (expiry-first, then created_at)
            List<BatchEntity> batches = batchRepository.findAvailableByProductId(product.getId());

            int totalAvailable = batches.stream().mapToInt(BatchEntity::getCurrentQuantity).sum();
            int remaining = itemInput.getQuantity();

            if (totalAvailable < remaining) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product '" + product.getName() + "'. Available: " + totalAvailable);
            }

            for (BatchEntity batch : batches) {
                if (remaining <= 0) break;

                int take = Math.min(remaining, batch.getCurrentQuantity());

                SaleLineEntity line = new SaleLineEntity();
                line.setBatch(batch);
                line.setProduct(product);
                line.setQuantity(take);
                line.setUnitPrice(unitPrice);
                line.setCostPriceAtSale(costAtSale);
                line.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(take)));
                sale.addSaleLine(line);

                batch.setCurrentQuantity(batch.getCurrentQuantity() - take);
                batchRepository.save(batch);

                totalAmount = totalAmount.add(line.getLineTotal());
                remaining -= take;
            }
            product.setQuantity(product.getQuantity() - remaining);
            productRepository.save(product);
        }

        sale.setTotalAmount(totalAmount);
        sale = saleRepository.save(sale);

        // Update customer outstanding balance for CREDIT or PARTIAL sales
        if (customer != null && !"PAID".equals(sale.getPaymentStatus())) {
            customer.setOutstandingBalance(customer.getOutstandingBalance().add(totalAmount));
        }

        // Validate credit limit
        if (customer != null && customer.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
            if (customer.getOutstandingBalance().compareTo(customer.getCreditLimit()) > 0) {
                throw new IllegalArgumentException(
                        "Sale would exceed credit limit for customer '" + customer.getName() + "'");
            }
        }

        return toDetailResponse(saleRepository.findByIdWithDetails(sale.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found after save")));
    }

    @Transactional
    public PaymentResponse addPayment(Long saleId, PaymentRequest request) {
        SaleEntity sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));

        if (sale.getCustomer() == null) {
            throw new IllegalArgumentException("Walk-in sales do not support deferred payments");
        }

        CustomerEntity customer = sale.getCustomer();

        SalePaymentEntity payment = new SalePaymentEntity();
        payment.setCustomer(customer);
        payment.setSale(sale);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod().toUpperCase());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setNotes(request.getNotes());
        if (request.getPaymentDate() != null) payment.setPaymentDate(request.getPaymentDate());

        payment = salePaymentRepository.save(payment);

        // Reduce customer outstanding balance
        customer.setOutstandingBalance(customer.getOutstandingBalance().subtract(request.getAmount()));

        // Update sale payment status
        BigDecimal totalPaid = salePaymentRepository.sumAmountBySaleId(saleId);
        if (totalPaid.compareTo(sale.getTotalAmount()) >= 0) {
            sale.setPaymentStatus("PAID");
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            sale.setPaymentStatus("PARTIAL");
        }
        saleRepository.save(sale);

        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(Long saleId) {
        if (!saleRepository.existsById(saleId)) {
            throw new ResourceNotFoundException("Sale not found: " + saleId);
        }
        return salePaymentRepository.findBySaleIdOrderByPaymentDateDesc(saleId)
                .stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getCustomerPayments(Long customerId) {
        customerService.findOrThrow(customerId);
        return salePaymentRepository.findByCustomerIdOrderByPaymentDateDesc(customerId)
                .stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BigDecimal resolveUnitPrice(SaleItemInput item, CustomerEntity customer, ProductEntity product) {
        if (item.getUnitPrice() != null) return item.getUnitPrice();
        if (customer != null) {
            return customerService.findTemplatePrice(customer.getId(), product.getId())
                    .orElse(product.getSellingPrice());
        }
        return product.getSellingPrice();
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String prefix = "INV-" + year + "-";
        long count = saleRepository.countByInvoiceNumberPrefix(prefix);
        return prefix + String.format("%05d", count + 1);
    }

    private SaleResponse toResponse(SaleEntity e) {
        SaleResponse r = new SaleResponse();
        r.setId(e.getId());
        r.setCustomerId(e.getCustomer() != null ? e.getCustomer().getId() : null);
        r.setCustomerName(e.getCustomer() != null ? e.getCustomer().getName() : null);
        r.setInvoiceNumber(e.getInvoiceNumber());
        r.setSaleDate(e.getSaleDate());
        r.setTotalAmount(e.getTotalAmount());
        r.setDiscount(e.getDiscount());
        r.setVatAmount(e.getVatAmount());
        r.setPaymentStatus(e.getPaymentStatus());
        r.setNotes(e.getNotes());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private SaleResponse toDetailResponse(SaleEntity e) {
        SaleResponse r = toResponse(e);

        List<SaleResponse.SaleLineResponse> lines = e.getSaleLines().stream().map(sl -> {
            SaleResponse.SaleLineResponse lr = new SaleResponse.SaleLineResponse();
            lr.setId(sl.getId());
            lr.setProductId(sl.getProduct().getId());
            lr.setProductName(sl.getProduct().getName());
            lr.setBatchId(sl.getBatch().getId());
            lr.setBatchCode(sl.getBatch().getBatchCode());
            lr.setQuantity(sl.getQuantity());
            lr.setUnitPrice(sl.getUnitPrice());
            lr.setCostPriceAtSale(sl.getCostPriceAtSale());
            lr.setLineTotal(sl.getLineTotal());
            lr.setProfit(sl.getUnitPrice().subtract(sl.getCostPriceAtSale())
                    .multiply(BigDecimal.valueOf(sl.getQuantity())));
            return lr;
        }).collect(Collectors.toList());

        r.setLines(lines);

        BigDecimal totalPaid = salePaymentRepository.sumAmountBySaleId(e.getId());
        r.setTotalPaid(totalPaid);
        r.setOutstandingAmount(e.getTotalAmount().subtract(totalPaid));
        return r;
    }

    private PaymentResponse toPaymentResponse(SalePaymentEntity p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setReferenceId(p.getSale() != null ? p.getSale().getId() : null);
        r.setPartyId(p.getCustomer().getId());
        r.setPartyName(p.getCustomer().getName());
        r.setPaymentDate(p.getPaymentDate());
        r.setAmount(p.getAmount());
        r.setPaymentMethod(p.getPaymentMethod());
        r.setReferenceNumber(p.getReferenceNumber());
        r.setNotes(p.getNotes());
        return r;
    }
}
