package com.invman.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "supplier_order_lines")
@NoArgsConstructor
public class SupplierOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_order_id", nullable = false)
    private SupplierOrder supplierOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "ordered_qty", nullable = false)
    private Double orderedQty;

    @Column(name = "supplier_sku")
    private String supplierSku;
}
