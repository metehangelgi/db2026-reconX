package com.dbtraining.reconx.dto;

import com.dbtraining.reconx.repository.entity.Trade;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PagedResponseTest {

    @Test
    void of_mapsPageContentAndCopiesPagingMetadata() {
        Trade t1 = new Trade();
        t1.setTradeRef("ref1");
        Trade t2 = new Trade();
        t2.setTradeRef("ref2");

        PageImpl<Trade> page = new PageImpl<>(List.of(t1, t2), PageRequest.of(0, 20), 2);

        PagedResponse<String> response = PagedResponse.of(page, Trade::getTradeRef);

        assertThat(response.items()).containsExactly("ref1", "ref2");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }
}
