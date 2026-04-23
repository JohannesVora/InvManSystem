package com.invman.common.entity;

import com.invman.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_transactions")
@NoArgsConstructor
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(name = "delta", nullable = false)
    private Double delta;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
