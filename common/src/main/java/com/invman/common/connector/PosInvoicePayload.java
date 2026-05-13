package com.invman.common.connector;

import java.util.List;

public record PosInvoicePayload(
        String invoiceId,
        String posSource,
        List<LineItem> items
) {
    public record LineItem(String productId, double quantitySold) {}
}
