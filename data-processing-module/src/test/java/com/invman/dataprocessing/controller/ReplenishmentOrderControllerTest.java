package com.invman.dataprocessing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.dto.ReplenishmentOrderDetailDto;
import com.invman.common.dto.ReplenishmentOrderLineDto;
import com.invman.common.dto.ReplenishmentOrderRequest;
import com.invman.common.enums.ReplenishmentOrderStatus;
import com.invman.dataprocessing.service.ReplenishmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

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

    private ReplenishmentOrderDetailDto makeDto(Long id, ReplenishmentOrderStatus status) {
        return new ReplenishmentOrderDetailDto(id, status, LocalDateTime.now(), null, null, List.of());
    }

    @Test
    void createOrderReturnsCreatedOrder() throws Exception {
        ReplenishmentOrderDetailDto dto = makeDto(1L, ReplenishmentOrderStatus.DRAFT);

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
        ReplenishmentOrderDetailDto dto = new ReplenishmentOrderDetailDto(
                42L, ReplenishmentOrderStatus.SUBMITTED, LocalDateTime.now(), LocalDateTime.now(), null, List.of());

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

    @Test
    void getAllOrdersReturnsListWithLines() throws Exception {
        ReplenishmentOrderLineDto line = new ReplenishmentOrderLineDto(1L, 1L, "Flour", "kg", 25.0, null, null, null, null);
        ReplenishmentOrderDetailDto dto = new ReplenishmentOrderDetailDto(
                1L, ReplenishmentOrderStatus.DRAFT, LocalDateTime.now(), null, null, List.of(line));

        when(replenishmentService.getAllOrders()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/orders/replenishment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lines").isArray());
    }

    @Test
    void bookGoodsReceiptReturnsReceived() throws Exception {
        ReplenishmentOrderDetailDto dto = new ReplenishmentOrderDetailDto(
                1L, ReplenishmentOrderStatus.RECEIVED, LocalDateTime.now(), null, LocalDateTime.now(), List.of());

        when(replenishmentService.bookGoodsReceipt(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/orders/replenishment/1/goods-receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"inventoryItemId":1,"receivedQty":10.0}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void bookGoodsReceiptReturns409WhenAlreadyReceived() throws Exception {
        when(replenishmentService.bookGoodsReceipt(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/orders/replenishment/1/goods-receipt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"inventoryItemId":1,"receivedQty":10.0}]}
                                """))
                .andExpect(status().isConflict());
    }
}
