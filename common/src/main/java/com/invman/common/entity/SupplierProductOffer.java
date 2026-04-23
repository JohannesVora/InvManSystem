package com.invman.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "supplier_product_offers")
@NoArgsConstructor
public class SupplierProductOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(name = "supplier_sku")
    private String supplierSku;

    @Column(name = "unit_price")
    private Double unitPrice;

    @Column(name = "conversion_factor")
    private Double conversionFactor;

    @Column(name = "is_preferred")
    private Boolean isPreferred;
}
