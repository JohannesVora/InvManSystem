package com.invman.common.entity;

import com.invman.common.enums.ConnectorDirection;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "connector_types")
@NoArgsConstructor
public class ConnectorType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false)
    private ConnectorDirection direction;

    @Column(name = "description")
    private String description;
}
