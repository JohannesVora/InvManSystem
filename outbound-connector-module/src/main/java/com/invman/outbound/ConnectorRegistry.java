package com.invman.outbound;

import com.invman.common.connector.OutboundConnector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectorRegistry {

    private final Map<String, OutboundConnector> connectors;

    public ConnectorRegistry(List<OutboundConnector> connectorList) {
        this.connectors = connectorList.stream()
                .collect(Collectors.toMap(OutboundConnector::getType, Function.identity()));
    }

    public OutboundConnector getConnector(String type) {
        OutboundConnector connector = connectors.get(type);
        if (connector == null) {
            throw new IllegalArgumentException("No connector registered for type: " + type);
        }
        return connector;
    }

    public boolean hasConnector(String type) {
        return connectors.containsKey(type);
    }

    public Map<String, OutboundConnector> getAll() {
        return connectors;
    }
}
