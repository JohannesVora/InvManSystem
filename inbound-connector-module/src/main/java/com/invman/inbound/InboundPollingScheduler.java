package com.invman.inbound;

import com.invman.common.connector.PosInvoicePayload;
import com.invman.common.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboundPollingScheduler {

    private final InboundConnectorRegistry inboundConnectorRegistry;
    private final InboundOrchestrator inboundOrchestrator;
    private final AppSettingRepository appSettingRepository;

    @Scheduled(fixedDelay = 60000)
    public void pollReady2Order() {
        String mode = appSettingRepository.findByKey("r2o.inboundMode")
                .map(s -> s.getValue()).orElse("polling");
        if (!"polling".equals(mode)) {
            log.debug("Polling skipped — active inbound mode is '{}'", mode);
            return;
        }

        String accountToken = appSettingRepository.findByKey("r2o.accountToken")
                .map(s -> s.getValue()).orElse("");
        if (accountToken == null || accountToken.isBlank()) {
            log.debug("r2o polling skipped: no account token configured");
            return;
        }

        String lastPolledAt = appSettingRepository.findByKey("r2o.lastPolledAt")
                .map(s -> s.getValue()).orElse("");
        String posSource = appSettingRepository.findByKey("r2o.posSource")
                .map(s -> s.getValue()).orElse("READY2ORDER");

        String configJson = String.format(
                "{\"accountToken\":\"%s\",\"posSource\":\"%s\",\"lastPolledAt\":\"%s\"}",
                accountToken, posSource, lastPolledAt
        );

        String connectorType = appSettingRepository.findByKey("r2o.connectorType")
                .map(s -> s.getValue()).orElse("READY2ORDER_POLLING");
        if (!inboundConnectorRegistry.hasConnector(connectorType)) {
            log.warn("No inbound connector registered for type '{}', skipping poll", connectorType);
            return;
        }

        log.debug("Polling ready2order invoices...");
        List<PosInvoicePayload> invoices =
                inboundConnectorRegistry.getConnector(connectorType).fetchInvoices(configJson);
        if (invoices.isEmpty()) {
            log.debug("Polling ready2order: no new invoices");
        } else {
            log.info("Polling ready2order: fetched {} invoice(s)", invoices.size());
        }
        inboundOrchestrator.processInvoices(invoices);
    }
}
