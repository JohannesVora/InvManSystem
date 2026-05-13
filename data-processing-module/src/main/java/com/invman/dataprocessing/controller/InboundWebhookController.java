package com.invman.dataprocessing.controller;

import com.invman.common.connector.PosInvoicePayload;
import com.invman.inbound.InboundOrchestrator;
import com.invman.inbound.PosEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
public class InboundWebhookController {

    private final PosEventMapper posEventMapper;
    private final InboundOrchestrator inboundOrchestrator;

    @PostMapping("/r2o/webhook")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @RequestParam(value = "posSource", defaultValue = "READY2ORDER") String posSource,
            @RequestBody String payload) {
        log.info("Received ready2order webhook payload ({} chars)", payload.length());
        List<PosInvoicePayload> invoices = posEventMapper.mapR2OResponse(payload, posSource);
        log.info("Webhook parsed {} invoice(s) with {} total line item(s)",
                invoices.size(), invoices.stream().mapToInt(i -> i.items().size()).sum());
        inboundOrchestrator.processInvoices(invoices);
        return ResponseEntity.ok(Map.of("processed", invoices.size()));
    }
}
