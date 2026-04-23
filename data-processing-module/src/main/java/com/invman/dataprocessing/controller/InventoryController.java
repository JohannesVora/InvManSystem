package com.invman.dataprocessing.controller;

import com.invman.common.dto.InventoryItemDto;
import com.invman.dataprocessing.service.MasterDataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
