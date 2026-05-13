package com.invman.common.repository;

import com.invman.common.entity.InventoryTransaction;
import com.invman.common.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByInventoryItemId(Long inventoryItemId);

    boolean existsByReferenceId(String referenceId);

    List<InventoryTransaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType type, Pageable pageable);

    List<InventoryTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
