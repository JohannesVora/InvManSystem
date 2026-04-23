package com.invman.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "app_settings")
@NoArgsConstructor
public class AppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true)
    private String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String value;
}
