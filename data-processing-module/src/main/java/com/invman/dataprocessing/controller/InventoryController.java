package com.invman.dataprocessing.controller;

import com.invman.common.dto.CreateInventoryItemDto;
import com.invman.common.dto.InventoryItemDto;
import com.invman.common.dto.UpdateInventoryItemDto;
import com.invman.dataprocessing.service.MasterDataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InventoryController {

    private final MasterDataManager masterDataManager;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "schema_version", "1"
        ));
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryItemDto>> getInventory() {
        return ResponseEntity.ok(masterDataManager.getAllInventoryItems());
    }

    @PostMapping("/inventory-items")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemDto createInventoryItem(@RequestBody CreateInventoryItemDto dto) {
        return masterDataManager.createInventoryItem(dto);
    }

    @PutMapping("/inventory-items/{id}")
    public InventoryItemDto updateInventoryItem(
            @PathVariable("id") Long id,
            @RequestBody UpdateInventoryItemDto dto) {
        return masterDataManager.updateInventoryItem(id, dto);
    }
}
