package com.invman.outbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.connector.ConnectorResult;
import com.invman.common.connector.OutboundConnector;
import com.invman.common.connector.SupplierOrderPayload;
import com.invman.common.entity.ConnectorConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConnector implements OutboundConnector {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public ConnectorResult transmit(SupplierOrderPayload payload, ConnectorConfig config) {
        try {
            JsonNode configNode = objectMapper.readTree(config.getConfigPayload());

            String recipientEmail = configNode.path("recipientEmail").asText();
            String subjectTemplate = configNode.path("subjectTemplate").asText("Order for {supplierName} on {date}");
            String bodyTemplate = configNode.path("bodyTemplate").asText("Order from {date} for {supplierName}:\n{orderLines}");

            String orderLines = payload.lines().stream()
                    .map(line -> String.format("- %s (SKU: %s) x %.2f %s",
                            line.inventoryItemName(),
                            line.supplierSku() != null ? line.supplierSku() : "N/A",
                            line.orderedQty(),
                            line.unit() != null ? line.unit() : ""))
                    .collect(Collectors.joining("\n"));

            String subject = subjectTemplate
                    .replace("{date}", payload.orderDate())
                    .replace("{supplierName}", payload.supplierName())
                    .replace("{orderLines}", orderLines);

            String body = bodyTemplate
                    .replace("{date}", payload.orderDate())
                    .replace("{supplierName}", payload.supplierName())
                    .replace("{orderLines}", orderLines);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent to {} for supplier order {}", recipientEmail, payload.supplierOrderId());
            return ConnectorResult.ok("Email sent to " + recipientEmail);

        } catch (Exception e) {
            log.error("Failed to send email for supplier order {}", payload.supplierOrderId(), e);
            return ConnectorResult.failure("Email send failed: " + e.getMessage());
        }
    }
}
