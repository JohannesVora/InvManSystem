package com.invman.dataprocessing.service;

import com.invman.common.dto.ConnectorConfigDto;
import com.invman.common.dto.InventoryItemDto;
import com.invman.common.dto.SupplierDto;
import com.invman.common.entity.ConnectorConfig;
import com.invman.common.entity.ConnectorType;
import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.Supplier;
import com.invman.common.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterDataManager {

    private final InventoryItemRepository inventoryItemRepository;
    private final SupplierRepository supplierRepository;
    private final ConnectorConfigRepository connectorConfigRepository;
    private final ConnectorTypeRepository connectorTypeRepository;
    private final AlertEvaluator alertEvaluator;

    public List<InventoryItemDto> getAllInventoryItems() {
        return inventoryItemRepository.findAll().stream()
                .map(item -> new InventoryItemDto(
                        item.getId(),
                        item.getName(),
                        item.getUnit(),
                        item.getCachedStock(),
                        item.getMinStockLevel(),
                        item.getReorderTarget(),
                        alertEvaluator.needsReorder(item)
                ))
                .toList();
    }

    public List<SupplierDto> getAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(s -> new SupplierDto(s.getId(), s.getName(), s.getContactEmail(), s.getPhone()))
                .toList();
    }

    public ConnectorConfigDto getConnectorConfig(Long supplierId) {
        return connectorConfigRepository.findBySupplierIdAndIsActiveTrue(supplierId)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public ConnectorConfigDto updateConnectorConfig(Long supplierId, ConnectorConfigDto dto) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

        ConnectorConfig config = connectorConfigRepository.findBySupplierIdAndIsActiveTrue(supplierId)
                .orElseGet(() -> {
                    ConnectorConfig c = new ConnectorConfig();
                    c.setSupplier(supplier);
                    c.setIsActive(true);
                    return c;
                });

        if (dto.connectorTypeName() != null) {
            ConnectorType type = connectorTypeRepository.findByName(dto.connectorTypeName())
                    .orElseThrow(() -> new IllegalArgumentException("ConnectorType not found: " + dto.connectorTypeName()));
            config.setConnectorType(type);
        }
        if (dto.configPayload() != null) {
            config.setConfigPayload(dto.configPayload());
        }
        if (dto.isActive() != null) {
            config.setIsActive(dto.isActive());
        }

        return toDto(connectorConfigRepository.save(config));
    }

    private ConnectorConfigDto toDto(ConnectorConfig config) {
        return new ConnectorConfigDto(
                config.getId(),
                config.getSupplier().getId(),
                config.getConnectorType() != null ? config.getConnectorType().getName() : null,
                config.getConfigPayload(),
                config.getIsActive()
        );
    }
}
