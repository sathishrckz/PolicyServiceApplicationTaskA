package com.insureflow.policyservice.service;

import com.insureflow.policyservice.dto.PolicyholderRequestDTO;
import com.insureflow.policyservice.dto.PolicyholderResponseDTO;
import com.insureflow.policyservice.entity.Policyholder;
import com.insureflow.policyservice.exception.InvalidPolicyDataException;
import com.insureflow.policyservice.exception.PolicyholderNotFoundException;
import com.insureflow.policyservice.repository.PolicyholderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PolicyholderService {

    private final PolicyholderRepository repository;

    public PolicyholderService(PolicyholderRepository repository) {
        this.repository = repository;
    }

    public PolicyholderResponseDTO create(PolicyholderRequestDTO dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new InvalidPolicyDataException("Email already exists: " + dto.getEmail());
        }
        Policyholder ph = new Policyholder();
        apply(ph, dto);
        return toResponse(repository.save(ph));
    }

    @Transactional(readOnly = true)
    public List<PolicyholderResponseDTO> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PolicyholderResponseDTO findById(Long id) {
        return toResponse(getEntity(id));
    }

    public PolicyholderResponseDTO update(Long id, PolicyholderRequestDTO dto) {
        Policyholder ph = getEntity(id);
        if (!ph.getEmail().equals(dto.getEmail()) && repository.existsByEmail(dto.getEmail())) {
            throw new InvalidPolicyDataException("Email already exists: " + dto.getEmail());
        }
        apply(ph, dto);
        return toResponse(repository.save(ph));
    }

    public void delete(Long id) {
        Policyholder ph = getEntity(id);
        repository.delete(ph);
    }

    public Policyholder getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new PolicyholderNotFoundException("Policyholder not found with id: " + id));
    }

    private void apply(Policyholder ph, PolicyholderRequestDTO dto) {
        ph.setFullName(dto.getFullName());
        ph.setEmail(dto.getEmail());
        ph.setPhone(dto.getPhone());
        ph.setDateOfBirth(dto.getDateOfBirth());
        ph.setAddress(dto.getAddress());
    }

    private PolicyholderResponseDTO toResponse(Policyholder ph) {
        PolicyholderResponseDTO r = new PolicyholderResponseDTO();
        r.setId(ph.getId());
        r.setFullName(ph.getFullName());
        r.setEmail(ph.getEmail());
        r.setPhone(ph.getPhone());
        r.setDateOfBirth(ph.getDateOfBirth());
        r.setAddress(ph.getAddress());
        r.setCreatedAt(ph.getCreatedAt());
        return r;
    }
}
