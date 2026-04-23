package com.invman.outbound;

import com.invman.common.connector.ConnectorResult;
import com.invman.common.enums.SupplierOrderStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatusAggregatorTest {

    private final StatusAggregator aggregator = new StatusAggregator();

    @Test
    void allSuccessResultsInTransmitted() {
        List<ConnectorResult> results = List.of(
                ConnectorResult.ok("sent"),
                ConnectorResult.ok("sent")
        );
        assertThat(aggregator.aggregate(results)).isEqualTo(SupplierOrderStatus.TRANSMITTED);
    }

    @Test
    void anyFailureResultsInFailed() {
        List<ConnectorResult> results = List.of(
                ConnectorResult.ok("sent"),
                ConnectorResult.failure("error")
        );
        assertThat(aggregator.aggregate(results)).isEqualTo(SupplierOrderStatus.FAILED);
    }

    @Test
    void emptyListResultsInFailed() {
        assertThat(aggregator.aggregate(List.of())).isEqualTo(SupplierOrderStatus.FAILED);
    }

    @Test
    void nullListResultsInFailed() {
        assertThat(aggregator.aggregate(null)).isEqualTo(SupplierOrderStatus.FAILED);
    }
}
