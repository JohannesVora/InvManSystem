package com.invman.common.entity;

import com.invman.common.enums.ReplenishmentOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "replenishment_orders")
@NoArgsConstructor
public class ReplenishmentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReplenishmentOrderStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "replenishmentOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReplenishmentOrderLine> lines;
}
