package com.invman.common.entity;

import com.invman.common.enums.ConnectorExecutionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "connector_executions")
@NoArgsConstructor
public class ConnectorExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_order_id", nullable = false)
    private SupplierOrder supplierOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connector_config_id", nullable = false)
    private ConnectorConfig connectorConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConnectorExecutionStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;
}
