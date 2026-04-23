package com.invman.common.repository;

import com.invman.common.entity.InventoryItem;
import com.invman.common.entity.Supplier;
import com.invman.common.entity.SupplierProductOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SupplierProductOfferRepositoryTest {

    @Autowired SupplierProductOfferRepository offerRepository;
    @Autowired SupplierRepository supplierRepository;
    @Autowired InventoryItemRepository inventoryItemRepository;

    private Supplier supplier;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setName("Test Supplier");
        supplier = supplierRepository.save(supplier);

        item = new InventoryItem();
        item.setName("Flour");
        item.setUnit("kg");
        item = inventoryItemRepository.save(item);
    }

    @Test
    void findsPreferredOffer() {
        SupplierProductOffer offer = new SupplierProductOffer();
        offer.setSupplier(supplier);
        offer.setInventoryItem(item);
        offer.setIsPreferred(true);
        offer.setConversionFactor(25.0);
        offerRepository.save(offer);

        Optional<SupplierProductOffer> found =
                offerRepository.findByInventoryItemIdAndIsPreferredTrue(item.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getIsPreferred()).isTrue();
    }

    @Test
    void returnsEmptyWhenNoPreferredOffer() {
        SupplierProductOffer offer = new SupplierProductOffer();
        offer.setSupplier(supplier);
        offer.setInventoryItem(item);
        offer.setIsPreferred(false);
        offerRepository.save(offer);

        Optional<SupplierProductOffer> found =
                offerRepository.findByInventoryItemIdAndIsPreferredTrue(item.getId());

        assertThat(found).isEmpty();
    }
}
