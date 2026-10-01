package com.insureflow.policyservice.dto;

import com.insureflow.policyservice.entity.ProductType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreatePolicyRequestDTO {

    @NotNull(message = "Policyholder ID is required")
    private Long policyholderId;

    @NotNull(message = "Product type is required")
    private ProductType productType;

    @NotNull(message = "Sum insured is required")
    @DecimalMin(value = "1.00", message = "Sum insured must be greater than 0")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal sumInsured;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    public Long getPolicyholderId() { return policyholderId; }
    public void setPolicyholderId(Long policyholderId) { this.policyholderId = policyholderId; }
    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }
    public BigDecimal getSumInsured() { return sumInsured; }
    public void setSumInsured(BigDecimal sumInsured) { this.sumInsured = sumInsured; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
