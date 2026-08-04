package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.security.JwtAuthenticationFilter;
import com.dbtraining.reconx.security.JwtTokenProvider;
import com.dbtraining.reconx.security.SecurityConfig;
import com.dbtraining.reconx.service.TradeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ============================================================================
 * TICKET-ADV075 — MockMvc: authenticated (TRADER) create returns 201
 * TICKET-ADV076 — MockMvc: unauthenticated create returns 401
 * TICKET-ADV077 — MockMvc: VIEWER create returns 403
 *
 * WHAT:    Web-layer slice test of TradeController#create, with the real
 *          Spring Security filter chain (SecurityConfig + JwtAuthenticationFilter
 *          + JwtTokenProvider) imported so RBAC is actually exercised — not
 *          just mocked away.
 * HOW:     @WebMvcTest loads only the web layer; TradeService and TradeMapper
 *          are @MockBean'd so this stays a pure controller/security test.
 * ============================================================================
 */
@WebMvcTest(TradeController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
class TradeControllerMockMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TradeService tradeService;

    @MockitoBean
    private TradeMapper tradeMapper;

    private static final String VALID_BODY = """
            {
              "tradeRef": "ABC-20260101-0001",
              "instrumentId": 1,
              "counterpartyId": 1,
              "assetClass": "BOND",
              "side": "BUY",
              "quantity": 100,
              "price": 99.5,
              "tradeDate": "2026-01-01"
            }
            """;

    @Test
    @WithMockUser(roles = "TRADER")
    void create_asTrader_returns201WithLocationHeader() throws Exception {
        Trade saved = new Trade();
        ReflectionTestUtils.setField(saved, "id", 42L);

        TradeResponse response = new TradeResponse(
                42L, "ABC-20260101-0001", 1L, "UST10Y", 1L, "Acme Bank",
                "BOND", "BUY", new BigDecimal("100"), new BigDecimal("99.5"),
                LocalDate.of(2026, 1, 1), "PENDING", null, null, "US0378331005");

        when(tradeService.create(any(), any())).thenReturn(saved);
        when(tradeMapper.toResponse(any())).thenReturn(response);

        mvc.perform(post("/v1/trades")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("42")))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.tradeRef").value("ABC-20260101-0001"));
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mvc.perform(post("/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(tradeService);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void create_asViewer_returns403() throws Exception {
        mvc.perform(post("/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(tradeService);
    }
}
