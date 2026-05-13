package com.invman.dataprocessing.controller;

import com.invman.common.entity.InventoryTransaction;
import com.invman.common.enums.TransactionType;
import com.invman.common.repository.InventoryTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final InventoryTransactionRepository transactionRepository;

    @GetMapping
    public List<Map<String, Object>> getTransactions(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "limit", defaultValue = "200") int limit) {

        List<InventoryTransaction> txs;
        PageRequest page = PageRequest.of(0, Math.min(limit, 500));

        if (type != null && !type.isBlank()) {
            txs = transactionRepository.findByTransactionTypeOrderByCreatedAtDesc(
                    TransactionType.valueOf(type.toUpperCase()), page);
        } else {
            txs = transactionRepository.findAllByOrderByCreatedAtDesc(page);
        }

        return txs.stream().map(tx -> Map.<String, Object>of(
                "id",              tx.getId(),
                "inventoryItem",   tx.getInventoryItem().getName(),
                "unit",            tx.getInventoryItem().getUnit(),
                "type",            tx.getTransactionType().name(),
                "delta",           tx.getDelta(),
                "referenceId",     tx.getReferenceId() != null ? tx.getReferenceId() : "",
                "createdAt",       tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : ""
        )).collect(Collectors.toList());
    }
}
