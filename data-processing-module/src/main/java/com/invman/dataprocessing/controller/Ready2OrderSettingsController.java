package com.invman.dataprocessing.controller;

import com.invman.common.entity.AppSetting;
import com.invman.common.repository.AppSettingRepository;
import com.invman.inbound.InboundPollingScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class Ready2OrderSettingsController {

    private static final String R2O_WEBHOOK_URL = "https://api.ready2order.com/v1/webhook";
    private static final String R2O_WEBHOOK_EVENTS_URL = "https://api.ready2order.com/v1/webhook/events";

    private final AppSettingRepository appSettingRepository;
    private final InboundPollingScheduler inboundPollingScheduler;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/settings/r2o")
    public ResponseEntity<Map<String, String>> getSettings() {
        String[] keys = {
            "r2o.accountToken",
            "r2o.developerToken",
            "r2o.pollingIntervalMinutes",
            "r2o.posSource",
            "r2o.lastPolledAt",
            "r2o.enabled",
            "r2o.inboundMode"
        };
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : keys) {
            String value = appSettingRepository.findByKey(key)
                    .map(AppSetting::getValue).orElse("");
            if ("r2o.accountToken".equals(key) && value != null && !value.isBlank()) {
                value = "***" + value.substring(Math.max(0, value.length() - 4));
            }
            result.put(key, value != null ? value : "");
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/settings/r2o")
    public ResponseEntity<Void> updateSettings(@RequestBody Map<String, String> settings) {
        String[] updatableKeys = {"r2o.accountToken", "r2o.pollingIntervalMinutes", "r2o.enabled", "r2o.inboundMode"};
        for (String key : updatableKeys) {
            if (!settings.containsKey(key)) continue;
            String value = settings.get(key);
            if ("r2o.accountToken".equals(key) && value != null && value.startsWith("***")) continue;
            appSettingRepository.upsert(key, value != null ? value : "");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/inbound/r2o/poll")
    public ResponseEntity<Map<String, String>> triggerPoll() {
        inboundPollingScheduler.pollReady2Order();
        return ResponseEntity.ok(Map.of("status", "triggered"));
    }

    @GetMapping("/settings/r2o/webhook")
    public ResponseEntity<Map<String, Object>> getWebhookStatus() {
        String accountToken = appSettingRepository.findByKey("r2o.accountToken")
                .map(AppSetting::getValue).orElse("");
        if (accountToken.isBlank()) {
            return ResponseEntity.ok(Map.of("webhookUrl", (Object) null, "activeEvents", List.of()));
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accountToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> webhookResp = restTemplate.exchange(
                    R2O_WEBHOOK_URL, HttpMethod.GET, entity, Map.class).getBody();
            @SuppressWarnings("unchecked")
            Map<String, Object> eventsResp = restTemplate.exchange(
                    R2O_WEBHOOK_EVENTS_URL, HttpMethod.GET, entity, Map.class).getBody();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("webhookUrl", webhookResp != null ? webhookResp.get("webhookUrl") : null);
            result.put("activeEvents", eventsResp != null ? eventsResp.get("activeEvents") : List.of());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to fetch webhook status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/settings/r2o/webhook")
    public ResponseEntity<Map<String, Object>> registerWebhook(@RequestBody Map<String, String> body) {
        String webhookUrl = body.getOrDefault("webhookUrl", "").trim();
        if (webhookUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "webhookUrl is required"));
        }
        String accountToken = appSettingRepository.findByKey("r2o.accountToken")
                .map(AppSetting::getValue).orElse("");
        if (accountToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not connected to ready2order"));
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accountToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.exchange(R2O_WEBHOOK_URL, HttpMethod.PUT,
                    new HttpEntity<>(Map.of("webhookUrl", webhookUrl), headers), Map.class);

            restTemplate.exchange(R2O_WEBHOOK_EVENTS_URL, HttpMethod.PUT,
                    new HttpEntity<>(Map.of("addEvent", "invoice.created"), headers), Map.class);

            log.info("Registered ready2order webhook: {}", webhookUrl);
            return ResponseEntity.ok(Map.of("webhookUrl", webhookUrl, "activeEvents", List.of("invoice.created")));
        } catch (Exception e) {
            log.error("Failed to register webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Failed to register webhook: " + e.getMessage()));
        }
    }

    @DeleteMapping("/settings/r2o/webhook")
    public ResponseEntity<Map<String, Object>> unregisterWebhook() {
        String accountToken = appSettingRepository.findByKey("r2o.accountToken")
                .map(AppSetting::getValue).orElse("");
        if (accountToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not connected to ready2order"));
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accountToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            restTemplate.exchange(R2O_WEBHOOK_URL, HttpMethod.PUT,
                    new HttpEntity<>(Map.of("webhookUrl", ""), headers), Map.class);
            restTemplate.exchange(R2O_WEBHOOK_EVENTS_URL, HttpMethod.PUT,
                    new HttpEntity<>(Map.of("removeEvent", "invoice.created"), headers), Map.class);

            log.info("Unregistered ready2order webhook");
            return ResponseEntity.ok(Map.of("webhookUrl", (Object) null, "activeEvents", List.of()));
        } catch (Exception e) {
            log.error("Failed to unregister webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Failed to unregister webhook: " + e.getMessage()));
        }
    }
}
