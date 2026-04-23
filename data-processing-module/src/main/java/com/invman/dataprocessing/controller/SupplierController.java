package com.invman.dataprocessing.controller;

import com.invman.common.dto.ConnectorConfigDto;
import com.invman.common.dto.SupplierDto;
import com.invman.dataprocessing.service.MasterDataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final MasterDataManager masterDataManager;

    @GetMapping
    public ResponseEntity<List<SupplierDto>> getSuppliers() {
        return ResponseEntity.ok(masterDataManager.getAllSuppliers());
    }

    @GetMapping("/{id}/connector")
    public ResponseEntity<ConnectorConfigDto> getConnectorConfig(@PathVariable("id") Long id) {
        ConnectorConfigDto config = masterDataManager.getConnectorConfig(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    @PutMapping("/{id}/connector")
    public ResponseEntity<ConnectorConfigDto> updateConnectorConfig(
            @PathVariable("id") Long id,
            @RequestBody ConnectorConfigDto dto) {
        try {
            ConnectorConfigDto result = masterDataManager.updateConnectorConfig(id, dto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
