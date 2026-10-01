package com.insureflow.policyservice.service;

import com.insureflow.policyservice.dto.CreatePolicyRequestDTO;
import com.insureflow.policyservice.dto.PolicyResponseDTO;
import com.insureflow.policyservice.entity.Policy;
import com.insureflow.policyservice.entity.PolicyStatus;
import com.insureflow.policyservice.entity.Policyholder;
import com.insureflow.policyservice.exception.InvalidPolicyDataException;
import com.insureflow.policyservice.exception.PolicyCancelledException;
import com.insureflow.policyservice.exception.PolicyExpiredException;
import com.insureflow.policyservice.exception.PolicyNotFoundException;
import com.insureflow.policyservice.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyholderService policyholderService;
    private final PremiumCalculator premiumCalculator;

    public PolicyService(PolicyRepository policyRepository,
                         PolicyholderService policyholderService,
                         PremiumCalculator premiumCalculator) {
        this.policyRepository = policyRepository;
        this.policyholderService = policyholderService;
        this.premiumCalculator = premiumCalculator;
    }

    /** Issue a new policy */
    public PolicyResponseDTO issue(CreatePolicyRequestDTO dto) {
        validateDates(dto.getStartDate(), dto.getEndDate());

        Policyholder ph = policyholderService.getEntity(dto.getPolicyholderId());

        Policy p = new Policy();
        p.setPolicyNumber(generatePolicyNumber());
        p.setProductType(dto.getProductType());
        p.setSumInsured(dto.getSumInsured());
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());
        p.setPolicyholder(ph);
        p.setStatus(PolicyStatus.ACTIVE);
        p.setAnnualPremium(premiumCalculator.calculate(
                dto.getSumInsured(), dto.getProductType(), ph.getDateOfBirth()));

        return toResponse(policyRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PolicyResponseDTO> findAll() {
        return policyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PolicyResponseDTO findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PolicyResponseDTO findByPolicyNumber(String policyNumber) {
        Policy p = policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found: " + policyNumber));
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponseDTO> findByPolicyholderId(Long policyholderId) {
        policyholderService.getEntity(policyholderId); // ensures exists
        return policyRepository.findByPolicyholderId(policyholderId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PolicyResponseDTO> findExpiredBefore(LocalDate date) {
        return policyRepository.findByStatusAndEndDateBefore(PolicyStatus.ACTIVE, date)
                .stream().map(this::toResponse).toList();
    }

    /** Renew: extend end_date by 1 year, reactivate if expired. Rejects CANCELLED. */
    public PolicyResponseDTO renew(Long id) {
        Policy p = getEntity(id);
        if (p.getStatus() == PolicyStatus.CANCELLED) {
            throw new PolicyCancelledException("Cannot renew a cancelled policy: " + p.getPolicyNumber());
        }
        LocalDate base = p.getEndDate().isAfter(LocalDate.now()) ? p.getEndDate() : LocalDate.now();
        p.setStartDate(base);
        p.setEndDate(base.plusYears(1));
        p.setStatus(PolicyStatus.ACTIVE);
        p.setAnnualPremium(premiumCalculator.calculate(
                p.getSumInsured(), p.getProductType(), p.getPolicyholder().getDateOfBirth()));
        return toResponse(policyRepository.save(p));
    }

    /** Cancel a policy */
    public PolicyResponseDTO cancel(Long id) {
        Policy p = getEntity(id);
        if (p.getStatus() == PolicyStatus.CANCELLED) {
            throw new PolicyCancelledException("Policy already cancelled: " + p.getPolicyNumber());
        }
        if (p.getStatus() == PolicyStatus.EXPIRED) {
            throw new PolicyExpiredException("Cannot cancel an expired policy: " + p.getPolicyNumber());
        }
        p.setStatus(PolicyStatus.CANCELLED);
        p.setCancelledAt(LocalDateTime.now());
        return toResponse(policyRepository.save(p));
    }

    public Policy getEntity(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found with id: " + id));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (!end.isAfter(start)) {
            throw new InvalidPolicyDataException("end_date must be after start_date");
        }
    }

    private String generatePolicyNumber() {
        String candidate;
        do {
            candidate = "POL-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        } while (policyRepository.existsByPolicyNumber(candidate));
        return candidate;
    }

    private PolicyResponseDTO toResponse(Policy p) {
        PolicyResponseDTO r = new PolicyResponseDTO();
        r.setId(p.getId());
        r.setPolicyNumber(p.getPolicyNumber());
        r.setProductType(p.getProductType());
        r.setSumInsured(p.getSumInsured());
        r.setAnnualPremium(p.getAnnualPremium());
        r.setStatus(p.getStatus());
        r.setStartDate(p.getStartDate());
        r.setEndDate(p.getEndDate());
        r.setPolicyholderId(p.getPolicyholder().getId());
        r.setPolicyholderName(p.getPolicyholder().getFullName());
        r.setCreatedAt(p.getCreatedAt());
        r.setCancelledAt(p.getCancelledAt());
        return r;
    }
}
