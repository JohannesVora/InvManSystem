package com.invman.inbound;

import com.invman.common.connector.InboundConnector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InboundConnectorRegistry {

    private final Map<String, InboundConnector> connectors;

    public InboundConnectorRegistry(List<InboundConnector> connectorList) {
        this.connectors = connectorList.stream()
                .collect(Collectors.toMap(InboundConnector::getType, Function.identity()));
    }

    public InboundConnector getConnector(String type) {
        InboundConnector connector = connectors.get(type);
        if (connector == null) {
            throw new IllegalArgumentException("No inbound connector registered for type: " + type);
        }
        return connector;
    }

    public boolean hasConnector(String type) {
        return connectors.containsKey(type);
    }
}
