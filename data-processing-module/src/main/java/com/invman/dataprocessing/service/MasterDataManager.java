package com.invman.dataprocessing.service;

import com.invman.common.dto.ConnectorConfigDto;
import com.invman.common.dto.CreateInventoryItemDto;
import com.invman.common.dto.InventoryItemDto;
import com.invman.common.dto.SupplierDto;
import com.invman.common.dto.UpdateInventoryItemDto;
import com.invman.common.entity.ConnectorConfig;
import com.invman.common.entity.ConnectorType;
import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.Supplier;
import com.invman.common.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MasterDataManager {

    private final InventoryItemRepository inventoryItemRepository;
    private final SupplierRepository supplierRepository;
    private final ConnectorConfigRepository connectorConfigRepository;
    private final ConnectorTypeRepository connectorTypeRepository;
    private final AlertEvaluator alertEvaluator;
    private final SupplierProductOfferRepository offerRepository;

    public List<InventoryItemDto> getAllInventoryItems() {
        return inventoryItemRepository.findAll().stream()
                .map(item -> {
                    boolean hasPreferredOffer =
                        offerRepository.findByInventoryItemIdAndIsPreferredTrue(item.getId()).isPresent();
                    return new InventoryItemDto(
                            item.getId(),
                            item.getName(),
                            item.getUnit(),
                            item.getCachedStock(),
                            item.getMinStockLevel(),
                            item.getReorderTarget(),
                            alertEvaluator.needsReorder(item),
                            hasPreferredOffer
                    );
                })
                .toList();
    }

    @Transactional
    public InventoryItemDto createInventoryItem(CreateInventoryItemDto dto) {
        InventoryItem item = new InventoryItem();
        item.setName(dto.name());
        item.setUnit(dto.unit());
        item.setMinStockLevel(dto.minStockLevel() != null ? dto.minStockLevel() : 0.0);
        item.setReorderTarget(dto.reorderTarget() != null ? dto.reorderTarget() : 0.0);
        item.setCachedStock(0.0);
        item.setLastRecalculatedAt(null);
        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Created inventory item '{}' (id={}, unit={})", saved.getName(), saved.getId(), saved.getUnit());
        return new InventoryItemDto(
                saved.getId(),
                saved.getName(),
                saved.getUnit(),
                saved.getCachedStock(),
                saved.getMinStockLevel(),
                saved.getReorderTarget(),
                alertEvaluator.needsReorder(saved),
                false
        );
    }

    @Transactional
    public InventoryItemDto updateInventoryItem(Long id, UpdateInventoryItemDto dto) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("InventoryItem not found: " + id));
        item.setName(dto.name());
        item.setUnit(dto.unit());
        item.setMinStockLevel(dto.minStockLevel());
        item.setReorderTarget(dto.reorderTarget());
        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Updated inventory item '{}' (id={}, minStock={}, reorderTarget={})",
                saved.getName(), saved.getId(), saved.getMinStockLevel(), saved.getReorderTarget());
        boolean hasPreferredOffer =
                offerRepository.findByInventoryItemIdAndIsPreferredTrue(saved.getId()).isPresent();
        return new InventoryItemDto(saved.getId(), saved.getName(), saved.getUnit(),
                saved.getCachedStock(), saved.getMinStockLevel(), saved.getReorderTarget(),
                alertEvaluator.needsReorder(saved), hasPreferredOffer);
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
