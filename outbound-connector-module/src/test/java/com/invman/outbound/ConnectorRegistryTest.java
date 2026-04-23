package com.invman.outbound;

import com.invman.common.connector.ConnectorResult;
import com.invman.common.connector.OutboundConnector;
import com.invman.common.connector.SupplierOrderPayload;
import com.invman.common.entity.ConnectorConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectorRegistryTest {

    @Test
    void registersConnectorsFromList() {
        OutboundConnector emailConnector = new TestConnector("EMAIL");
        OutboundConnector restConnector = new TestConnector("REST");

        ConnectorRegistry registry = new ConnectorRegistry(List.of(emailConnector, restConnector));

        assertThat(registry.hasConnector("EMAIL")).isTrue();
        assertThat(registry.hasConnector("REST")).isTrue();
        assertThat(registry.hasConnector("EDI")).isFalse();
    }

    @Test
    void getConnectorReturnsCorrectConnector() {
        OutboundConnector connector = new TestConnector("EMAIL");
        ConnectorRegistry registry = new ConnectorRegistry(List.of(connector));

        assertThat(registry.getConnector("EMAIL")).isSameAs(connector);
    }

    @Test
    void getConnectorThrowsForUnknownType() {
        ConnectorRegistry registry = new ConnectorRegistry(List.of());

        assertThatThrownBy(() -> registry.getConnector("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private record TestConnector(String type) implements OutboundConnector {
        @Override
        public String getType() { return type; }

        @Override
        public ConnectorResult transmit(SupplierOrderPayload payload, ConnectorConfig config) {
            return ConnectorResult.ok("test");
        }
    }
}
