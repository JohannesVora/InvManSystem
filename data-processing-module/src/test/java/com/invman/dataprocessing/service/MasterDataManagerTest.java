package com.invman.dataprocessing.service;

import com.invman.common.dto.InventoryItemDto;
import com.invman.common.dto.SupplierDto;
import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.Supplier;
import com.invman.common.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasterDataManagerTest {

    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock SupplierRepository supplierRepository;
    @Mock ConnectorConfigRepository connectorConfigRepository;
    @Mock ConnectorTypeRepository connectorTypeRepository;
    @Mock AlertEvaluator alertEvaluator;
    @Mock SupplierProductOfferRepository offerRepository;
    @InjectMocks MasterDataManager masterDataManager;

    @Test
    void returnsAllInventoryItemsWithReorderFlag() {
        InventoryItem item = new InventoryItem();
        item.setId(1L);
        item.setName("Flour");
        item.setUnit("kg");
        item.setCachedStock(5.0);
        item.setMinStockLevel(10.0);
        item.setReorderTarget(50.0);

        when(inventoryItemRepository.findAll()).thenReturn(List.of(item));
        when(alertEvaluator.needsReorder(item)).thenReturn(true);
        when(offerRepository.findByInventoryItemIdAndIsPreferredTrue(1L)).thenReturn(Optional.empty());

        List<InventoryItemDto> result = masterDataManager.getAllInventoryItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Flour");
        assertThat(result.get(0).needsReorder()).isTrue();
        assertThat(result.get(0).hasPreferredOffer()).isFalse();
    }

    @Test
    void returnsAllSuppliers() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Fresh Farm");
        supplier.setContactEmail("orders@freshfarm.com");

        when(supplierRepository.findAll()).thenReturn(List.of(supplier));

        List<SupplierDto> result = masterDataManager.getAllSuppliers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Fresh Farm");
    }
}
