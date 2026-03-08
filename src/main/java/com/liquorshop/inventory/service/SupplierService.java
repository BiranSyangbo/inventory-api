package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.SupplierRequest;
import com.liquorshop.inventory.dto.SupplierResponse;
import com.liquorshop.inventory.entity.SupplierEntity;
import com.liquorshop.inventory.exception.ResourceNotFoundException;
import com.liquorshop.inventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<SupplierResponse> getAll() {
        return supplierRepository.findAllByOrderByNameAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getActive() {
        return supplierRepository.findAllByStatusOrderByNameAsc("ACTIVE")
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        SupplierEntity entity = new SupplierEntity();
        applyRequest(entity, request);
        return toResponse(supplierRepository.save(entity));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        SupplierEntity entity = findOrThrow(id);
        applyRequest(entity, request);
        return toResponse(supplierRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        supplierRepository.deleteById(id);
    }

    private void applyRequest(SupplierEntity entity, SupplierRequest request) {
        entity.setName(request.getName());
        entity.setContactPerson(request.getContactPerson());
        entity.setPhone(request.getPhone());
        entity.setAddress(request.getAddress());
        entity.setVatPanNumber(request.getVatPanNumber());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
    }

    private SupplierEntity findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
    }

    private SupplierResponse toResponse(SupplierEntity e) {
        SupplierResponse r = new SupplierResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setContactPerson(e.getContactPerson());
        r.setPhone(e.getPhone());
        r.setAddress(e.getAddress());
        r.setVatPanNumber(e.getVatPanNumber());
        r.setStatus(e.getStatus());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
