package com.insureflow.policyservice.repository;

import com.insureflow.policyservice.entity.Policyholder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PolicyholderRepository extends JpaRepository<Policyholder, Long> {
    Optional<Policyholder> findByEmail(String email);
    boolean existsByEmail(String email);
}
