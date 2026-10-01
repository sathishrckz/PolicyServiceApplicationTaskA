package com.insureflow.policyservice.repository;

import com.insureflow.policyservice.entity.Policy;
import com.insureflow.policyservice.entity.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByPolicyholderId(Long policyholderId);

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByStatusAndEndDateBefore(PolicyStatus status, LocalDate date);

    boolean existsByPolicyNumber(String policyNumber);
}
