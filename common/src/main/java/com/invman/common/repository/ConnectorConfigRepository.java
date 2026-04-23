package com.invman.common.repository;

import com.invman.common.entity.ConnectorConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConnectorConfigRepository extends JpaRepository<ConnectorConfig, Long> {

    Optional<ConnectorConfig> findBySupplierIdAndIsActiveTrue(Long supplierId);
}
