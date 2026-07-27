package com.dbtraining.reconx.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class TradeRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validTradeRequest_hasNoViolations() {
        TradeRequest request = new TradeRequest(
                "EQU-20260603-0001",
                1L,
                2L,
                "EQUITY",
                "BUY",
                new BigDecimal("10"),
                new BigDecimal("100"),
                LocalDate.of(2026, 6, 3));

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void invalidTradeRequest_reportsExpectedFieldViolations() {
        TradeRequest request = new TradeRequest(
                "bad-ref",
                null,
                null,
                "",
                "HOLD",
                BigDecimal.ZERO,
                new BigDecimal("-1"),
                null);

        Set<ConstraintViolation<TradeRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                        "tradeRef",
                        "instrumentId",
                        "counterpartyId",
                        "assetClass",
                        "side",
                        "quantity",
                        "price",
                        "tradeDate");
    }
}
