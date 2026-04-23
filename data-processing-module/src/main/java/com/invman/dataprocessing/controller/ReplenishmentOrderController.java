package com.invman.dataprocessing.controller;

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

    public ReplenishmentOrderController(ReplenishmentService replenishmentService,
                                        Optional<OrderProcessor> orderProcessor) {
        this.replenishmentService = replenishmentService;
        this.orderProcessor = orderProcessor;
    }

    @PostMapping("/replenishment")
    public ResponseEntity<ReplenishmentOrderStatusDto> createOrder(@RequestBody ReplenishmentOrderRequest request) {
        // 1. Save order in its own committed transaction
        ReplenishmentOrderStatusDto result = replenishmentService.createOrder(request);

        // 2. Transmit to suppliers in a separate transaction (mail failures won't roll back the order)
        orderProcessor.ifPresent(processor -> {
            try {
                processor.process(result.id());
            } catch (Exception e) {
                log.error("Order transmission failed for order {}: {}", result.id(), e.getMessage(), e);
            }
        });

        // 3. Return the latest status (may now be SUBMITTED if transmission succeeded)
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
