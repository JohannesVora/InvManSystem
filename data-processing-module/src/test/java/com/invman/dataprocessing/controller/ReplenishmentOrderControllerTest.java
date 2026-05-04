package com.invman.dataprocessing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.dto.ReplenishmentOrderRequest;
import com.invman.common.dto.ReplenishmentOrderStatusDto;
import com.invman.common.enums.ReplenishmentOrderStatus;
import com.invman.dataprocessing.service.ReplenishmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReplenishmentOrderController.class)
class ReplenishmentOrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ReplenishmentService replenishmentService;

    @Test
    void createOrderReturnsCreatedOrder() throws Exception {
        ReplenishmentOrderStatusDto dto = new ReplenishmentOrderStatusDto(
                1L, ReplenishmentOrderStatus.DRAFT, LocalDateTime.now(), null);

        when(replenishmentService.createOrder(any())).thenReturn(dto);
        when(replenishmentService.getOrder(1L)).thenReturn(dto);

        ReplenishmentOrderRequest request = new ReplenishmentOrderRequest(
                List.of(new ReplenishmentOrderRequest.OrderLine(1L, 25.0)));

        mockMvc.perform(post("/api/orders/replenishment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getOrderReturnsOrderById() throws Exception {
        ReplenishmentOrderStatusDto dto = new ReplenishmentOrderStatusDto(
                42L, ReplenishmentOrderStatus.SUBMITTED, LocalDateTime.now(), LocalDateTime.now());

        when(replenishmentService.getOrder(42L)).thenReturn(dto);

        mockMvc.perform(get("/api/orders/replenishment/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void getOrderReturns404WhenNotFound() throws Exception {
        when(replenishmentService.getOrder(999L)).thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(get("/api/orders/replenishment/999"))
                .andExpect(status().isNotFound());
    }
}
