package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void money_sameValueSameMoney() {
        Money money1 = new Money(new BigDecimal("100"), Currency.getInstance("USD"));
        Money money2 = new Money(new BigDecimal("100"), Currency.getInstance("USD"));
        assertThat(money1).isEqualTo(money2); 
    }

    @Test
    void equality_byTradeRef() {
        Money money1 = new Money(new BigDecimal("100"), Currency.getInstance("USD"));
        Money money2 = new Money(new BigDecimal("50"), Currency.getInstance("USD"));
        Money money3 = money1.plus(money2);
        assertThat(money1.amount()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(money2.amount()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(money3.amount()).isEqualByComparingTo(new BigDecimal("150"));
    }

        @Test
    void money_currencyMismatch() {
        Money money1 = new Money(new BigDecimal("100"), Currency.getInstance("USD"));
        Money money2 = new Money(new BigDecimal("100"), Currency.getInstance("EUR"));
        assertThat(money1).isEqualTo(money2); 
    }



}
