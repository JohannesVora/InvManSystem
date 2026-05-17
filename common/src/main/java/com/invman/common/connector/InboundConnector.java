package com.invman.common.connector;

import java.util.List;

public interface InboundConnector {
    String getType();
    List<PosInvoicePayload> fetchInvoices(String configJson);
}
