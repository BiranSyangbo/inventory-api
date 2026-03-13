package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.PaymentRequest;
import com.liquorshop.inventory.dto.PaymentResponse;
import com.liquorshop.inventory.dto.PurchaseInput;
import com.liquorshop.inventory.dto.PurchaseLineInput;
import com.liquorshop.inventory.dto.PurchaseResponse;
import com.liquorshop.inventory.entity.BatchEntity;
import com.liquorshop.inventory.entity.ProductEntity;
import com.liquorshop.inventory.entity.PurchaseEntity;
import com.liquorshop.inventory.entity.PurchaseLineEntity;
import com.liquorshop.inventory.entity.PurchasePaymentEntity;
import com.liquorshop.inventory.entity.SupplierEntity;
import com.liquorshop.inventory.exception.ResourceNotFoundException;
import com.liquorshop.inventory.repository.BatchRepository;
import com.liquorshop.inventory.repository.ProductRepository;
import com.liquorshop.inventory.repository.PurchasePaymentRepository;
import com.liquorshop.inventory.repository.PurchaseRepository;
import com.liquorshop.inventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final PurchasePaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<PurchaseResponse> getAll() {
        return purchaseRepository.findAllWithSupplier()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getById(Long id) {
        PurchaseEntity entity = purchaseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));
        return toDetailResponse(entity);
    }

    @Transactional
    public PurchaseResponse create(PurchaseInput input) {
        // Enforce VAT bill uniqueness
        if (input.getVatBillNumber() != null && !input.getVatBillNumber().isBlank()) {
            if (purchaseRepository.existsByVatBillNumber(input.getVatBillNumber())) {
                throw new IllegalArgumentException("VAT bill number already exists: " + input.getVatBillNumber());
            }
        }

        SupplierEntity supplier = supplierRepository.findById(input.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + input.getSupplierId()));

        PurchaseEntity purchase = new PurchaseEntity();
        purchase.setSupplier(supplier);
        purchase.setVatBillNumber(input.getVatBillNumber());
        purchase.setPurchaseDate(input.getPurchaseDate() != null ? input.getPurchaseDate() : LocalDate.now());
        purchase.setInvoiceAmount(input.getInvoiceAmount());
        purchase.setVatAmount(input.getVatAmount() != null ? input.getVatAmount() : BigDecimal.ZERO);
        purchase.setDiscount(input.getDiscount() != null ? input.getDiscount() : BigDecimal.ZERO);
        purchase.setRemarks(input.getRemarks());

        purchase = purchaseRepository.save(purchase);

        for (PurchaseLineInput lineInput : input.getLines()) {
            ProductEntity product = productRepository.findById(lineInput.getProductId())
                    .filter(p -> !p.getDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + lineInput.getProductId()));

            // Auto-generate batch code if not provided
            String batchCode = lineInput.getBatchCode() != null && !lineInput.getBatchCode().isBlank()
                    ? lineInput.getBatchCode()
                    : generateBatchCode(product, purchase.getPurchaseDate());

            // Create batch
            BatchEntity batch = new BatchEntity();
            batch.setProduct(product);
            batch.setBatchCode(batchCode);
            batch.setPurchaseDate(purchase.getPurchaseDate());
            batch.setExpiryDate(lineInput.getExpiryDate());
            batch.setPurchasePrice(lineInput.getPurchasePrice());
            batch.setOriginalQuantity(lineInput.getQuantity());
            batch.setCurrentQuantity(lineInput.getQuantity());
            batch.setLocation(lineInput.getLocation());
            batch = batchRepository.save(batch);

            // Create purchase line
            PurchaseLineEntity line = new PurchaseLineEntity();
            line.setProduct(product);
            line.setBatch(batch);
            line.setQuantity(lineInput.getQuantity());
            line.setPurchasePrice(lineInput.getPurchasePrice());
            line.setVatPercent(lineInput.getVatPercent() != null ? lineInput.getVatPercent() : BigDecimal.ZERO);
            purchase.addPurchaseLine(line);

            // Update product weighted average cost
            updateWeightedAverageCost(product, lineInput.getQuantity(), lineInput.getPurchasePrice());
        }

        purchase = purchaseRepository.save(purchase);

        return toDetailResponse(purchaseRepository.findByIdWithDetails(purchase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found after save")));
    }

    @Transactional
    public PaymentResponse addPayment(Long purchaseId, PaymentRequest request) {
        PurchaseEntity purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + purchaseId));

        PurchasePaymentEntity payment = new PurchasePaymentEntity();
        payment.setPurchase(purchase);
        payment.setSupplier(purchase.getSupplier());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod().toUpperCase());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setNotes(request.getNotes());
        if (request.getPaymentDate() != null) payment.setPaymentDate(request.getPaymentDate());

        payment = paymentRepository.save(payment);
        return toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(Long purchaseId) {
        if (!purchaseRepository.existsById(purchaseId)) {
            throw new ResourceNotFoundException("Purchase not found: " + purchaseId);
        }
        return paymentRepository.findByPurchaseIdOrderByPaymentDateDesc(purchaseId)
                .stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    // ── Weighted Average Cost ────────────────────────────────────────────────

    private void updateWeightedAverageCost(ProductEntity product, int newQty, BigDecimal newPrice) {
        int existingQty = product.getCurrentStock(); // stock before this purchase line

        BigDecimal newAvg;
        if (existingQty <= 0) {
            newAvg = newPrice;
        } else {
            BigDecimal numerator = product.getAverageCost()
                    .multiply(BigDecimal.valueOf(existingQty))
                    .add(newPrice.multiply(BigDecimal.valueOf(newQty)));
            newAvg = numerator.divide(BigDecimal.valueOf(existingQty + newQty), 4, RoundingMode.HALF_UP);
        }

        product.setCurrentStock(existingQty + newQty);
        product.setAverageCost(newAvg);
        productRepository.save(product);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String generateBatchCode(ProductEntity product, LocalDate date) {
        return "BCH-" + product.getId() + "-" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private PurchaseResponse toResponse(PurchaseEntity e) {
        PurchaseResponse r = new PurchaseResponse();
        r.setId(e.getId());
        r.setSupplierId(e.getSupplier().getId());
        r.setSupplierName(e.getSupplier().getName());
        r.setVatBillNumber(e.getVatBillNumber());
        r.setPurchaseDate(e.getPurchaseDate());
        r.setInvoiceAmount(e.getInvoiceAmount());
        r.setVatAmount(e.getVatAmount());
        r.setDiscount(e.getDiscount());
        r.setRemarks(e.getRemarks());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private PurchaseResponse toDetailResponse(PurchaseEntity e) {
        PurchaseResponse r = toResponse(e);

        List<PurchaseResponse.PurchaseLineResponse> lines = e.getPurchaseLines().stream().map(pl -> {
            PurchaseResponse.PurchaseLineResponse lr = new PurchaseResponse.PurchaseLineResponse();
            lr.setId(pl.getId());
            lr.setProductId(pl.getProduct().getId());
            lr.setProductName(pl.getProduct().getName());
            lr.setBatchId(pl.getBatch().getId());
            lr.setBatchCode(pl.getBatch().getBatchCode());
            lr.setExpiryDate(pl.getBatch().getExpiryDate());
            lr.setQuantity(pl.getQuantity());
            lr.setPurchasePrice(pl.getPurchasePrice());
            lr.setVatPercent(pl.getVatPercent());
            lr.setLocation(pl.getBatch().getLocation());
            return lr;
        }).collect(Collectors.toList());

        r.setLines(lines);

        BigDecimal totalPaid = paymentRepository.sumAmountByPurchaseId(e.getId());
        r.setTotalPaid(totalPaid);
        BigDecimal invoiceTotal = e.getInvoiceAmount() != null ? e.getInvoiceAmount() : BigDecimal.ZERO;
        r.setOutstandingAmount(invoiceTotal.subtract(totalPaid));
        return r;
    }

    private PaymentResponse toPaymentResponse(PurchasePaymentEntity p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setReferenceId(p.getPurchase().getId());
        r.setPartyId(p.getSupplier().getId());
        r.setPartyName(p.getSupplier().getName());
        r.setPaymentDate(p.getPaymentDate());
        r.setAmount(p.getAmount());
        r.setPaymentMethod(p.getPaymentMethod());
        r.setReferenceNumber(p.getReferenceNumber());
        r.setNotes(p.getNotes());
        return r;
    }
}
