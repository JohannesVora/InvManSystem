package com.invman.common.connector;

import com.invman.common.entity.ConnectorConfig;

public interface OutboundConnector {

    String getType();

    ConnectorResult transmit(SupplierOrderPayload payload, ConnectorConfig config);
}
