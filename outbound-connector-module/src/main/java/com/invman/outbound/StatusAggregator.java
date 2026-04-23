package com.invman.outbound;

import com.invman.common.connector.ConnectorResult;
import com.invman.common.enums.SupplierOrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StatusAggregator {

    public SupplierOrderStatus aggregate(List<ConnectorResult> results) {
        if (results == null || results.isEmpty()) {
            return SupplierOrderStatus.FAILED;
        }
        boolean allSuccess = results.stream().allMatch(ConnectorResult::success);
        return allSuccess ? SupplierOrderStatus.TRANSMITTED : SupplierOrderStatus.FAILED;
    }
}
