package com.invman.common.connector;

import java.util.List;

public interface InboundConnector {
    List<PosInvoicePayload> fetchInvoices(String configJson);
}
