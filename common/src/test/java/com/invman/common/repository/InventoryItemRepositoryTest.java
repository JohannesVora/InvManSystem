package com.invman.common.repository;

import com.invman.common.entity.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InventoryItemRepositoryTest {

    @Autowired
    InventoryItemRepository repository;

    @Test
    void savesAndFindsInventoryItem() {
        InventoryItem item = new InventoryItem();
        item.setName("Test Item");
        item.setUnit("kg");
        item.setCachedStock(50.0);
        item.setMinStockLevel(10.0);
        item.setReorderTarget(100.0);

        InventoryItem saved = repository.save(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void findsAllItems() {
        InventoryItem item1 = new InventoryItem();
        item1.setName("Item A");
        item1.setUnit("kg");

        InventoryItem item2 = new InventoryItem();
        item2.setName("Item B");
        item2.setUnit("liter");

        repository.save(item1);
        repository.save(item2);

        List<InventoryItem> all = repository.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void deletesItem() {
        InventoryItem item = new InventoryItem();
        item.setName("To Delete");
        item.setUnit("unit");
        InventoryItem saved = repository.save(item);

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}
