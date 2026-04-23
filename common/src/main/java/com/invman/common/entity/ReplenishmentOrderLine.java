package com.invman.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "replenishment_order_lines")
@NoArgsConstructor
public class ReplenishmentOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replenishment_order_id", nullable = false)
    private ReplenishmentOrder replenishmentOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "requested_qty", nullable = false)
    private Double requestedQty;
}
