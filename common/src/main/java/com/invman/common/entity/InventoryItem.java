package com.invman.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_items")
@NoArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "unit")
    private String unit;

    @Column(name = "cached_stock")
    private Double cachedStock;

    @Column(name = "min_stock_level")
    private Double minStockLevel;

    @Column(name = "reorder_target")
    private Double reorderTarget;

    @Column(name = "last_recalculated_at")
    private LocalDateTime lastRecalculatedAt;
}
