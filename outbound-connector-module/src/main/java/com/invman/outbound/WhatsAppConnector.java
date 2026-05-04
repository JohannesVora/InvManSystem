package com.invman.outbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.connector.ConnectorResult;
import com.invman.common.connector.OutboundConnector;
import com.invman.common.connector.SupplierOrderPayload;
import com.invman.common.entity.AppSetting;
import com.invman.common.entity.ConnectorConfig;
import com.invman.common.repository.AppSettingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WhatsAppConnector implements OutboundConnector {

    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public WhatsAppConnector(AppSettingRepository appSettingRepository, ObjectMapper objectMapper) {
        this(appSettingRepository, objectMapper, new RestTemplate());
    }

    // Package-private constructor for testing
    WhatsAppConnector(AppSettingRepository appSettingRepository, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.appSettingRepository = appSettingRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getType() {
        return "WHATSAPP";
    }

    @Override
    public ConnectorResult transmit(SupplierOrderPayload payload, ConnectorConfig config) {
        try {
            JsonNode configNode = objectMapper.readTree(config.getConfigPayload());
            String recipientPhone = configNode.path("recipientPhone").asText();
            String messageTemplate = configNode.path("messageTemplate").asText("");
            if (messageTemplate.isBlank()) {
                messageTemplate = "Order {date} - {supplierName}:\n\n{orderLines}";
            }

            String orderLines = payload.lines().stream()
                    .map(line -> String.format("- %s (SKU: %s) x %.2f %s",
                            line.inventoryItemName(),
                            line.supplierSku() != null ? line.supplierSku() : "N/A",
                            line.orderedQty(),
                            line.unit() != null ? line.unit() : ""))
                    .collect(Collectors.joining("\n"));

            String message = messageTemplate
                    .replace("{date}", payload.orderDate())
                    .replace("{supplierName}", payload.supplierName())
                    .replace("{orderLines}", orderLines);

            String baseUrl = getSetting("wppconnect.baseUrl", "http://wppconnect:21465");
            String session = getSetting("wppconnect.session", "inventory-session");
            String token = resolveToken();

            String normalizedPhone = recipientPhone.replaceAll("[\\s\\-]", "");
            if (normalizedPhone.startsWith("00")) normalizedPhone = normalizedPhone.substring(2);
            if (normalizedPhone.startsWith("+")) normalizedPhone = normalizedPhone.substring(1);
            log.debug("Sending WhatsApp — to={} (normalized: {}) message:\n{}", recipientPhone, normalizedPhone, message);
            String messageId = sendMessage(baseUrl, session, token, normalizedPhone, message);

            log.info("WhatsApp message sent to {} for supplier order {}", recipientPhone, payload.supplierOrderId());
            return ConnectorResult.ok("WhatsApp message sent, id=" + messageId);

        } catch (Exception e) {
            log.error("Failed to send WhatsApp message for supplier order {}", payload.supplierOrderId(), e);
            return ConnectorResult.failure("WhatsApp send failed: " + e.getMessage());
        }
    }

    private String resolveToken() {
        String token = getSetting("wppconnect.token", "");
        if (!token.isBlank()) {
            return token;
        }
        return generateAndStoreToken();
    }

    private String generateAndStoreToken() {
        String baseUrl = getSetting("wppconnect.baseUrl", "http://wppconnect:21465");
        String session = getSetting("wppconnect.session", "inventory-session");
        String secretKey = getSetting("wppconnect.secretKey", "THISISMYSECURETOKEN");

        String url = baseUrl + "/api/" + session + "/" + secretKey + "/generate-token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        String token = "";
        if (response != null && response.containsKey("token")) {
            token = String.valueOf(response.get("token"));
        }

        appSettingRepository.upsert("wppconnect.token", token);
        log.info("WPPConnect token generated and stored");
        return token;
    }

    private String sendMessage(String baseUrl, String session, String token,
                               String phone, String message) {
        String url = baseUrl + "/api/" + session + "/send-message";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        Map<String, String> body = Map.of("phone", phone, "message", message);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

        if (response != null && response.containsKey("id")) {
            return String.valueOf(response.get("id"));
        }
        return "unknown";
    }

    private String getSetting(String key, String defaultValue) {
        return appSettingRepository.findByKey(key)
                .map(AppSetting::getValue)
                .orElse(defaultValue);
    }
}
