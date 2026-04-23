package com.invman.common.repository;

import com.invman.common.entity.ConnectorConfig;
import com.invman.common.entity.ConnectorType;
import com.invman.common.entity.Supplier;
import com.invman.common.enums.ConnectorDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ConnectorConfigRepositoryTest {

    @Autowired ConnectorConfigRepository configRepository;
    @Autowired SupplierRepository supplierRepository;
    @Autowired ConnectorTypeRepository connectorTypeRepository;

    private Supplier supplier;
    private ConnectorType connectorType;

    @BeforeEach
    void setUp() {
        supplier = new Supplier();
        supplier.setName("Test Supplier");
        supplier = supplierRepository.save(supplier);

        connectorType = new ConnectorType();
        connectorType.setName("EMAIL");
        connectorType.setDirection(ConnectorDirection.OUTBOUND);
        connectorType = connectorTypeRepository.save(connectorType);
    }

    @Test
    void findsActiveConfig() {
        ConnectorConfig config = new ConnectorConfig();
        config.setSupplier(supplier);
        config.setConnectorType(connectorType);
        config.setIsActive(true);
        config.setConfigPayload("{\"recipientEmail\":\"test@example.com\"}");
        configRepository.save(config);

        Optional<ConnectorConfig> found =
                configRepository.findBySupplierIdAndIsActiveTrue(supplier.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getIsActive()).isTrue();
    }

    @Test
    void returnsEmptyWhenNoActiveConfig() {
        ConnectorConfig config = new ConnectorConfig();
        config.setSupplier(supplier);
        config.setConnectorType(connectorType);
        config.setIsActive(false);
        configRepository.save(config);

        Optional<ConnectorConfig> found =
                configRepository.findBySupplierIdAndIsActiveTrue(supplier.getId());

        assertThat(found).isEmpty();
    }
}
