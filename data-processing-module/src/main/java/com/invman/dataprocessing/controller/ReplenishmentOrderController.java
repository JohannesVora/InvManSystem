package com.invman.dataprocessing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.connector.OrderProcessor;
import com.invman.common.dto.ReplenishmentOrderRequest;
import com.invman.common.dto.ReplenishmentOrderStatusDto;
import com.invman.dataprocessing.service.ReplenishmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class ReplenishmentOrderController {

    private final ReplenishmentService replenishmentService;
    private final Optional<OrderProcessor> orderProcessor;
    private final ObjectMapper objectMapper;

    public ReplenishmentOrderController(ReplenishmentService replenishmentService,
                                        Optional<OrderProcessor> orderProcessor,
                                        ObjectMapper objectMapper) {
        this.replenishmentService = replenishmentService;
        this.orderProcessor = orderProcessor;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/replenishment")
    public ResponseEntity<ReplenishmentOrderStatusDto> createOrder(@RequestBody ReplenishmentOrderRequest request) {
        try {
            log.debug("POST /api/orders/replenishment — body: {}",
                    objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.debug("POST /api/orders/replenishment — {} line(s)", request.lines().size());
        }

        // 1. Save order in its own committed transaction
        ReplenishmentOrderStatusDto result = replenishmentService.createOrder(request);

        // 2. Transmit to suppliers in a separate transaction
        orderProcessor.ifPresent(processor -> {
            try {
                processor.process(result.id());
            } catch (Exception e) {
                log.error("Order transmission failed for order {}: {}", result.id(), e.getMessage(), e);
            }
        });

        // 3. Return the latest status
        return ResponseEntity.ok(replenishmentService.getOrder(result.id()));
    }

    @GetMapping("/replenishment/{id}")
    public ResponseEntity<ReplenishmentOrderStatusDto> getOrder(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(replenishmentService.getOrder(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
