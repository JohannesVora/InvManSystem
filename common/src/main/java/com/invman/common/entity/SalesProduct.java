package com.invman.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "sales_products")
@NoArgsConstructor
public class SalesProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "pos_system")
    private String posSystem;
}
