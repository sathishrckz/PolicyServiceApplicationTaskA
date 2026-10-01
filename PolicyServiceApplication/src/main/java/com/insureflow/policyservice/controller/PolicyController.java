package com.insureflow.policyservice.controller;

import com.insureflow.policyservice.dto.CreatePolicyRequestDTO;
import com.insureflow.policyservice.dto.PolicyResponseDTO;
import com.insureflow.policyservice.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService service;

    public PolicyController(PolicyService service) {
        this.service = service;
    }

    /** Issue new policy */
    @PostMapping
    public ResponseEntity<PolicyResponseDTO> issue(@Valid @RequestBody CreatePolicyRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.issue(dto));
    }

    @GetMapping
    public List<PolicyResponseDTO> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public PolicyResponseDTO findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/number/{policyNumber}")
    public PolicyResponseDTO findByNumber(@PathVariable String policyNumber) {
        return service.findByPolicyNumber(policyNumber);
    }

    @GetMapping("/policyholder/{policyholderId}")
    public List<PolicyResponseDTO> findByPolicyholder(@PathVariable Long policyholderId) {
        return service.findByPolicyholderId(policyholderId);
    }

    /** Renew policy (extends end_date by 1 year) */
    @PutMapping("/{id}/renew")
    public PolicyResponseDTO renew(@PathVariable Long id) {
        return service.renew(id);
    }

    /** Cancel policy */
    @DeleteMapping("/{id}")
    public PolicyResponseDTO cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
