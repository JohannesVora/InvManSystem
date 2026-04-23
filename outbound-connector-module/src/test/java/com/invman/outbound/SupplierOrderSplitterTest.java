package com.invman.outbound;

import com.invman.common.entity.*;
import com.invman.common.repository.SupplierProductOfferRepository;
import org.junit.jupiter.api.BeforeEach;
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
class SupplierOrderSplitterTest {

    @Mock SupplierProductOfferRepository offerRepository;
    @InjectMocks SupplierOrderSplitter splitter;

    private Supplier supplier;
    private InventoryItem item;
    private SupplierProductOffer offer;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setId(1L);
        supplier.setName("Test Supplier");

        item = new InventoryItem();
        item.setId(10L);
        item.setName("Flour");
        item.setUnit("kg");

        offer = new SupplierProductOffer();
        offer.setSupplier(supplier);
        offer.setInventoryItem(item);
        offer.setConversionFactor(25.0);
        offer.setIsPreferred(true);
        offer.setSupplierSku("FLOUR-25KG");
    }

    @Test
    void splitsLinesByPreferredSupplier() {
        ReplenishmentOrderLine line = new ReplenishmentOrderLine();
        line.setInventoryItem(item);
        line.setRequestedQty(60.0);

        when(offerRepository.findByInventoryItemIdAndIsPreferredTrue(10L))
                .thenReturn(Optional.of(offer));

        List<SupplierOrderSplitter.SupplierSplit> splits = splitter.split(List.of(line));

        assertThat(splits).hasSize(1);
        assertThat(splits.get(0).supplier().getId()).isEqualTo(1L);
        assertThat(splits.get(0).lines()).hasSize(1);
    }

    @Test
    void calculatesOrderedQtyWithCeil() {
        ReplenishmentOrderLine line = new ReplenishmentOrderLine();
        line.setInventoryItem(item);
        line.setRequestedQty(30.0);  // 30 / 25 = 1.2, ceil = 2

        when(offerRepository.findByInventoryItemIdAndIsPreferredTrue(10L))
                .thenReturn(Optional.of(offer));

        List<SupplierOrderSplitter.SupplierSplit> splits = splitter.split(List.of(line));

        assertThat(splits.get(0).lines().get(0).orderedQty()).isEqualTo(2.0);
    }

    @Test
    void skipsItemsWithNoPreferredOffer() {
        ReplenishmentOrderLine line = new ReplenishmentOrderLine();
        line.setInventoryItem(item);
        line.setRequestedQty(50.0);

        when(offerRepository.findByInventoryItemIdAndIsPreferredTrue(10L))
                .thenReturn(Optional.empty());

        List<SupplierOrderSplitter.SupplierSplit> splits = splitter.split(List.of(line));

        assertThat(splits).isEmpty();
    }

    @Test
    void groupsMultipleItemsForSameSupplier() {
        InventoryItem item2 = new InventoryItem();
        item2.setId(11L);
        item2.setName("Sugar");
        item2.setUnit("kg");

        SupplierProductOffer offer2 = new SupplierProductOffer();
        offer2.setSupplier(supplier);
        offer2.setInventoryItem(item2);
        offer2.setConversionFactor(10.0);
        offer2.setIsPreferred(true);

        ReplenishmentOrderLine line1 = new ReplenishmentOrderLine();
        line1.setInventoryItem(item);
        line1.setRequestedQty(50.0);

        ReplenishmentOrderLine line2 = new ReplenishmentOrderLine();
        line2.setInventoryItem(item2);
        line2.setRequestedQty(20.0);

        when(offerRepository.findByInventoryItemIdAndIsPreferredTrue(10L)).thenReturn(Optional.of(offer));
        when(offerRepository.findByInventoryItemIdAndIsPreferredTrue(11L)).thenReturn(Optional.of(offer2));

        List<SupplierOrderSplitter.SupplierSplit> splits = splitter.split(List.of(line1, line2));

        assertThat(splits).hasSize(1);
        assertThat(splits.get(0).lines()).hasSize(2);
    }
}
