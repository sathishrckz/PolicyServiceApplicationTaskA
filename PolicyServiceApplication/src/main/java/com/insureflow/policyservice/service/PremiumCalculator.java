package com.insureflow.policyservice.service;

import com.insureflow.policyservice.entity.ProductType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

/**
 * Calculates annual premium = sumInsured * productRate * ageFactor.
 *
 * Product rates:
 *  MOTOR=0.05, HOME=0.03, TRAVEL=0.02, HEALTH=0.04, LIABILITY=0.06
 *
 * Age factor:
 *  18-30 = 1.0, 31-45 = 1.1, 46-60 = 1.3, 61+ = 1.5
 */
@Component
public class PremiumCalculator {

    private static final Map<ProductType, BigDecimal> PRODUCT_RATE_MAP = Map.of(
            ProductType.MOTOR,     new BigDecimal("0.05"),
            ProductType.HOME,      new BigDecimal("0.03"),
            ProductType.TRAVEL,    new BigDecimal("0.02"),
            ProductType.HEALTH,    new BigDecimal("0.04"),
            ProductType.LIABILITY, new BigDecimal("0.06")
    );

    public BigDecimal calculate(BigDecimal sumInsured, ProductType productType, LocalDate dateOfBirth) {
        BigDecimal rate = PRODUCT_RATE_MAP.get(productType);
        BigDecimal ageFactor = ageFactor(dateOfBirth);
        return sumInsured.multiply(rate).multiply(ageFactor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal ageFactor(LocalDate dob) {
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age <= 30) return new BigDecimal("1.0");
        if (age <= 45) return new BigDecimal("1.1");
        if (age <= 60) return new BigDecimal("1.3");
        return new BigDecimal("1.5");
    }
}
