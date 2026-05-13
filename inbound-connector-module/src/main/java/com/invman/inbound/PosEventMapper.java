package com.invman.inbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.connector.PosInvoicePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PosEventMapper {

    private final ObjectMapper objectMapper;

    /**
     * Maps a ready2order invoices JSON array to a list of PosInvoicePayload.
     * Skips items with product_id == 0 (void lines) and tolerates malformed input.
     */
    public List<PosInvoicePayload> mapR2OResponse(String jsonBody, String posSource) {
        List<PosInvoicePayload> result = new ArrayList<>();
        if (jsonBody == null || jsonBody.isBlank()) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(jsonBody);

            if (root.isArray()) {
                // REST polling format: [{document_id, invoiceitems:[{product_id, invoiceitem_amount}]}]
                for (JsonNode invoice : root) {
                    PosInvoicePayload payload = mapPollingInvoice(invoice, posSource);
                    if (payload != null) result.add(payload);
                }
            } else if (root.has("resource")) {
                // Webhook format: {resource: {invoice_id, items:[{product_id, item_qty}]}}
                PosInvoicePayload payload = mapWebhookInvoice(root.path("resource"), posSource);
                if (payload != null) result.add(payload);
            } else {
                log.warn("Unrecognised ready2order payload shape, root keys: {}", root.fieldNames());
            }
        } catch (Exception e) {
            log.error("Failed to parse ready2order response: {}", e.getMessage());
        }
        return result;
    }

    private PosInvoicePayload mapPollingInvoice(JsonNode invoice, String posSource) {
        String invoiceId = invoice.path("document_id").asText("");
        if (invoiceId.isBlank()) return null;

        List<PosInvoicePayload.LineItem> items = new ArrayList<>();
        for (JsonNode line : invoice.path("invoiceitems")) {
            long productId = line.path("product_id").asLong(0);
            if (productId == 0) continue;
            double qty = line.path("invoiceitem_amount").asDouble(0.0);
            if (qty <= 0) continue;
            items.add(new PosInvoicePayload.LineItem(String.valueOf(productId), qty));
        }
        return items.isEmpty() ? null : new PosInvoicePayload(invoiceId, posSource, items);
    }

    private PosInvoicePayload mapWebhookInvoice(JsonNode invoice, String posSource) {
        long invoiceIdNum = invoice.path("invoice_id").asLong(0);
        if (invoiceIdNum == 0) return null;
        String invoiceId = String.valueOf(invoiceIdNum);

        List<PosInvoicePayload.LineItem> items = new ArrayList<>();
        for (JsonNode line : invoice.path("items")) {
            long productId = line.path("product_id").asLong(0);
            if (productId == 0) continue;
            // item_qty is the sold quantity; fall back to item_quantity
            double qty = line.has("item_qty")
                    ? line.path("item_qty").asDouble(0.0)
                    : line.path("item_quantity").asDouble(0.0);
            if (qty <= 0) continue;
            items.add(new PosInvoicePayload.LineItem(String.valueOf(productId), qty));
        }
        return items.isEmpty() ? null : new PosInvoicePayload(invoiceId, posSource, items);
    }
}
