package com.invman.common.entity;

import com.invman.common.enums.SupplierOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "supplier_orders")
@NoArgsConstructor
public class SupplierOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replenishment_order_id", nullable = false)
    private ReplenishmentOrder replenishmentOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupplierOrderStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "supplierOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SupplierOrderLine> lines;
}
