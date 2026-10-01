package com.insureflow.policyservice.service;

import com.insureflow.policyservice.dto.CreatePolicyRequestDTO;
import com.insureflow.policyservice.dto.PolicyResponseDTO;
import com.insureflow.policyservice.entity.Policy;
import com.insureflow.policyservice.entity.PolicyStatus;
import com.insureflow.policyservice.entity.Policyholder;
import com.insureflow.policyservice.entity.ProductType;
import com.insureflow.policyservice.exception.InvalidPolicyDataException;
import com.insureflow.policyservice.exception.PolicyCancelledException;
import com.insureflow.policyservice.exception.PolicyExpiredException;
import com.insureflow.policyservice.exception.PolicyNotFoundException;
import com.insureflow.policyservice.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private PolicyholderService policyholderService;

    @Mock
    private PremiumCalculator premiumCalculator;

    @InjectMocks
    private PolicyService policyService;

    private Policyholder policyholder;

    @BeforeEach
    void setUp() {
        policyholder = new Policyholder();
        policyholder.setId(1L);
        policyholder.setFullName("Jane Doe");
        policyholder.setDateOfBirth(LocalDate.now().minusYears(30));
    }

    private Policy buildPolicy(PolicyStatus status, LocalDate start, LocalDate end) {
        Policy p = new Policy();
        p.setId(10L);
        p.setPolicyNumber("POL-TEST0001");
        p.setProductType(ProductType.MOTOR);
        p.setSumInsured(new BigDecimal("50000.00"));
        p.setAnnualPremium(new BigDecimal("2500.00"));
        p.setStatus(status);
        p.setStartDate(start);
        p.setEndDate(end);
        p.setPolicyholder(policyholder);
        return p;
    }

    // ---------------- issue(): premium calculation ----------------

    @Test
    void issue_calculatesPremium_andSavesActivePolicy() {
        CreatePolicyRequestDTO dto = new CreatePolicyRequestDTO();
        dto.setPolicyholderId(1L);
        dto.setProductType(ProductType.MOTOR);
        dto.setSumInsured(new BigDecimal("50000.00"));
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusYears(1));

        when(policyholderService.getEntity(1L)).thenReturn(policyholder);
        when(premiumCalculator.calculate(dto.getSumInsured(), dto.getProductType(), policyholder.getDateOfBirth()))
                .thenReturn(new BigDecimal("2500.00"));
        when(policyRepository.existsByPolicyNumber(any())).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyResponseDTO response = policyService.issue(dto);

        assertThat(response.getAnnualPremium()).isEqualByComparingTo("2500.00");
        assertThat(response.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        verify(premiumCalculator).calculate(dto.getSumInsured(), dto.getProductType(), policyholder.getDateOfBirth());
        verify(policyRepository).save(any(Policy.class));
    }

    // ---------------- issue(): date validation ----------------

    @Test
    void issue_throwsInvalidPolicyDataException_whenEndDateEqualsStartDate() {
        CreatePolicyRequestDTO dto = new CreatePolicyRequestDTO();
        dto.setPolicyholderId(1L);
        dto.setProductType(ProductType.MOTOR);
        dto.setSumInsured(new BigDecimal("50000.00"));
        LocalDate date = LocalDate.now();
        dto.setStartDate(date);
        dto.setEndDate(date);

        assertThatThrownBy(() -> policyService.issue(dto))
                .isInstanceOf(InvalidPolicyDataException.class)
                .hasMessageContaining("end_date must be after start_date");

        verifyNoInteractions(policyholderService, premiumCalculator, policyRepository);
    }

    @Test
    void issue_throwsInvalidPolicyDataException_whenEndDateBeforeStartDate() {
        CreatePolicyRequestDTO dto = new CreatePolicyRequestDTO();
        dto.setPolicyholderId(1L);
        dto.setProductType(ProductType.MOTOR);
        dto.setSumInsured(new BigDecimal("50000.00"));
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> policyService.issue(dto))
                .isInstanceOf(InvalidPolicyDataException.class);
    }

    // ---------------- renew(): status-based rejection ----------------

    @Test
    void renew_throwsPolicyCancelledException_whenPolicyIsCancelled() {
        Policy cancelled = buildPolicy(PolicyStatus.CANCELLED, LocalDate.now().minusYears(1), LocalDate.now().minusDays(1));
        when(policyRepository.findById(10L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> policyService.renew(10L))
                .isInstanceOf(PolicyCancelledException.class);

        verify(policyRepository, never()).save(any());
    }

    @Test
    void renew_succeeds_whenPolicyIsExpired_reactivatesAndRecalculatesPremium() {
        LocalDate oldEnd = LocalDate.now().minusDays(5);
        Policy expired = buildPolicy(PolicyStatus.EXPIRED, oldEnd.minusYears(1), oldEnd);
        when(policyRepository.findById(10L)).thenReturn(Optional.of(expired));
        when(premiumCalculator.calculate(any(), any(), any())).thenReturn(new BigDecimal("2600.00"));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyResponseDTO response = policyService.renew(10L);

        assertThat(response.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(response.getAnnualPremium()).isEqualByComparingTo("2600.00");
        // base date should be "now" since old end date was in the past
        assertThat(response.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(response.getEndDate()).isEqualTo(LocalDate.now().plusYears(1));
    }

    @Test
    void renew_succeeds_whenPolicyIsActive_extendsFromCurrentEndDate() {
        LocalDate futureEnd = LocalDate.now().plusMonths(2);
        Policy active = buildPolicy(PolicyStatus.ACTIVE, LocalDate.now().minusMonths(10), futureEnd);
        when(policyRepository.findById(10L)).thenReturn(Optional.of(active));
        when(premiumCalculator.calculate(any(), any(), any())).thenReturn(new BigDecimal("2700.00"));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyResponseDTO response = policyService.renew(10L);

        assertThat(response.getStartDate()).isEqualTo(futureEnd);
        assertThat(response.getEndDate()).isEqualTo(futureEnd.plusYears(1));
        assertThat(response.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
    }

    // ---------------- cancel(): status-based rejection ----------------

    @Test
    void cancel_throwsPolicyCancelledException_whenAlreadyCancelled() {
        Policy cancelled = buildPolicy(PolicyStatus.CANCELLED, LocalDate.now().minusYears(1), LocalDate.now().plusYears(1));
        when(policyRepository.findById(10L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> policyService.cancel(10L))
                .isInstanceOf(PolicyCancelledException.class);

        verify(policyRepository, never()).save(any());
    }

    @Test
    void cancel_throwsPolicyExpiredException_whenPolicyIsExpired() {
        Policy expired = buildPolicy(PolicyStatus.EXPIRED, LocalDate.now().minusYears(1), LocalDate.now().minusDays(1));
        when(policyRepository.findById(10L)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> policyService.cancel(10L))
                .isInstanceOf(PolicyExpiredException.class);

        verify(policyRepository, never()).save(any());
    }

    @Test
    void cancel_succeeds_whenPolicyIsActive_setsStatusAndCancelledAt() {
        Policy active = buildPolicy(PolicyStatus.ACTIVE, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(11));
        when(policyRepository.findById(10L)).thenReturn(Optional.of(active));
        when(policyRepository.save(any(Policy.class))).thenAnswer(inv -> inv.getArgument(0));

        PolicyResponseDTO response = policyService.cancel(10L);

        assertThat(response.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(response.getCancelledAt()).isNotNull();
        assertThat(response.getCancelledAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    // ---------------- getEntity() / lookups: not found ----------------

    @Test
    void getEntity_throwsPolicyNotFoundException_whenIdDoesNotExist() {
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getEntity(99L))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void renew_throwsPolicyNotFoundException_whenPolicyMissing() {
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.renew(99L))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void cancel_throwsPolicyNotFoundException_whenPolicyMissing() {
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.cancel(99L))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    // ---------------- additional coverage: lookups ----------------

    @Test
    void findByPolicyholderId_verifiesPolicyholderExists_thenReturnsPolicies() {
        when(policyholderService.getEntity(1L)).thenReturn(policyholder);
        Policy p = buildPolicy(PolicyStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusYears(1));
        when(policyRepository.findByPolicyholderId(1L)).thenReturn(List.of(p));

        List<PolicyResponseDTO> result = policyService.findByPolicyholderId(1L);

        assertThat(result).hasSize(1);
        verify(policyholderService).getEntity(1L);
    }

    @Test
    void findExpiredBefore_returnsMatchingPolicies() {
        LocalDate cutoff = LocalDate.now();
        Policy p = buildPolicy(PolicyStatus.ACTIVE, LocalDate.now().minusYears(1), cutoff.minusDays(1));
        when(policyRepository.findByStatusAndEndDateBefore(PolicyStatus.ACTIVE, cutoff)).thenReturn(List.of(p));

        List<PolicyResponseDTO> result = policyService.findExpiredBefore(cutoff);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEndDate()).isBefore(cutoff);
    }
}
