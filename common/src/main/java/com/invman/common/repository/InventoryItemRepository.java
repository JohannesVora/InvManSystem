package com.invman.common.repository;

import com.invman.common.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE inventory_items
            SET cached_stock = (SELECT COALESCE(SUM(delta), 0) FROM inventory_transactions WHERE inventory_item_id = :itemId),
                last_recalculated_at = NOW()
            WHERE id = :itemId
            """, nativeQuery = true)
    void recalculateCachedStock(@Param("itemId") Long itemId);
}
