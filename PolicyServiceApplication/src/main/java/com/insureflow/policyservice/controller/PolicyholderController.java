package com.insureflow.policyservice.controller;

import com.insureflow.policyservice.dto.PolicyholderRequestDTO;
import com.insureflow.policyservice.dto.PolicyholderResponseDTO;
import com.insureflow.policyservice.service.PolicyholderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policyholders")
public class PolicyholderController {

    private final PolicyholderService service;

    public PolicyholderController(PolicyholderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PolicyholderResponseDTO> create(@Valid @RequestBody PolicyholderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @GetMapping
    public List<PolicyholderResponseDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PolicyholderResponseDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public PolicyholderResponseDTO update(@PathVariable Long id, @Valid @RequestBody PolicyholderRequestDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
