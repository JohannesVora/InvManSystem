package com.invman.outbound;

import com.invman.common.connector.ConnectorResult;
import com.invman.common.connector.OutboundConnector;
import com.invman.common.connector.SupplierOrderPayload;
import com.invman.common.entity.ConnectorConfig;
import org.springframework.stereotype.Component;

@Component
public class RestConnector implements OutboundConnector {

    @Override
    public String getType() {
        return "REST";
    }

    @Override
    public ConnectorResult transmit(SupplierOrderPayload payload, ConnectorConfig config) {
        throw new UnsupportedOperationException("REST connector not yet implemented");
    }
}
