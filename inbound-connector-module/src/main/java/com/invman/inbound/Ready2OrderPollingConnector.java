package com.invman.inbound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.connector.InboundConnector;
import com.invman.common.connector.PosInvoicePayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class Ready2OrderPollingConnector implements InboundConnector {

    private static final String R2O_API_BASE = "https://api.ready2order.com/v1";

    private final RestTemplate restTemplate;
    private final PosEventMapper posEventMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public Ready2OrderPollingConnector(PosEventMapper posEventMapper, ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.posEventMapper = posEventMapper;
        this.objectMapper = objectMapper;
    }

    // Package-private constructor for testing
    Ready2OrderPollingConnector(PosEventMapper posEventMapper, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.posEventMapper = posEventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<PosInvoicePayload> fetchInvoices(String configJson) {
        try {
            JsonNode config = objectMapper.readTree(configJson);
            String accountToken = config.path("accountToken").asText("");
            String posSource = config.path("posSource").asText("READY2ORDER");
            String lastPolledAt = config.path("lastPolledAt").asText("");

            if (accountToken.isBlank()) {
                log.warn("No account token configured for ready2order polling");
                return Collections.emptyList();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accountToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(R2O_API_BASE + "/invoices");
            if (!lastPolledAt.isBlank()) {
                uriBuilder.queryParam("since", lastPolledAt);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getBody() == null) {
                return Collections.emptyList();
            }

            return posEventMapper.mapR2OResponse(response.getBody(), posSource);
        } catch (Exception e) {
            log.error("Error fetching ready2order invoices: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
