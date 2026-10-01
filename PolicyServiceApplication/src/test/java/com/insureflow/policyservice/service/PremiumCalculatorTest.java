package com.insureflow.policyservice.service;

import com.insureflow.policyservice.entity.ProductType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PremiumCalculatorTest {

    private final PremiumCalculator calculator = new PremiumCalculator();

    private static final BigDecimal SUM_INSURED = new BigDecimal("100000.00");

    // ---- Product rate coverage (age factor fixed at 1.0 via age 25) ----

    @ParameterizedTest
    @EnumSource(ProductType.class)
    void calculate_appliesCorrectProductRate_forYoungAdult(ProductType productType) {
        LocalDate dob = LocalDate.now().minusYears(25); // age factor 1.0

        BigDecimal result = calculator.calculate(SUM_INSURED, productType, dob);

        BigDecimal expectedRate = switch (productType) {
            case MOTOR -> new BigDecimal("0.05");
            case HOME -> new BigDecimal("0.03");
            case TRAVEL -> new BigDecimal("0.02");
            case HEALTH -> new BigDecimal("0.04");
            case LIABILITY -> new BigDecimal("0.06");
        };
        BigDecimal expected = SUM_INSURED.multiply(expectedRate)
                .multiply(new BigDecimal("1.0"))
                .setScale(2, RoundingMode.HALF_UP);

        assertThat(result).isEqualByComparingTo(expected);
    }

    // ---- Age factor bracket coverage (product fixed to MOTOR, rate 0.05) ----

    @Test
    void calculate_appliesAgeFactor_1_0_forAge18to30() {
        LocalDate dob = LocalDate.now().minusYears(30);
        BigDecimal result = calculator.calculate(SUM_INSURED, ProductType.MOTOR, dob);
        BigDecimal expected = SUM_INSURED.multiply(new BigDecimal("0.05"))
                .multiply(new BigDecimal("1.0")).setScale(2, RoundingMode.HALF_UP);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void calculate_appliesAgeFactor_1_1_forAge31to45() {
        LocalDate dob = LocalDate.now().minusYears(45);
        BigDecimal result = calculator.calculate(SUM_INSURED, ProductType.MOTOR, dob);
        BigDecimal expected = SUM_INSURED.multiply(new BigDecimal("0.05"))
                .multiply(new BigDecimal("1.1")).setScale(2, RoundingMode.HALF_UP);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void calculate_appliesAgeFactor_1_3_forAge46to60() {
        LocalDate dob = LocalDate.now().minusYears(60);
        BigDecimal result = calculator.calculate(SUM_INSURED, ProductType.MOTOR, dob);
        BigDecimal expected = SUM_INSURED.multiply(new BigDecimal("0.05"))
                .multiply(new BigDecimal("1.3")).setScale(2, RoundingMode.HALF_UP);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void calculate_appliesAgeFactor_1_5_forAge61Plus() {
        LocalDate dob = LocalDate.now().minusYears(61);
        BigDecimal result = calculator.calculate(SUM_INSURED, ProductType.MOTOR, dob);
        BigDecimal expected = SUM_INSURED.multiply(new BigDecimal("0.05"))
                .multiply(new BigDecimal("1.5")).setScale(2, RoundingMode.HALF_UP);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void calculate_roundsHalfUp_toTwoDecimalPlaces() {
        // Choose a sum insured that forces rounding
        BigDecimal sumInsured = new BigDecimal("100.005");
        LocalDate dob = LocalDate.now().minusYears(25); // factor 1.0
        BigDecimal result = calculator.calculate(sumInsured, ProductType.TRAVEL, dob); // rate 0.02
        BigDecimal expected = sumInsured.multiply(new BigDecimal("0.02"))
                .multiply(new BigDecimal("1.0")).setScale(2, RoundingMode.HALF_UP);
        assertThat(result).isEqualByComparingTo(expected);
        assertThat(result.scale()).isEqualTo(2);
    }
}
