package com.invman.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "connector_configs")
@NoArgsConstructor
public class ConnectorConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connector_type_id", nullable = false)
    private ConnectorType connectorType;

    @Column(name = "config_payload", columnDefinition = "TEXT")
    private String configPayload;

    @Column(name = "is_active")
    private Boolean isActive;
}
