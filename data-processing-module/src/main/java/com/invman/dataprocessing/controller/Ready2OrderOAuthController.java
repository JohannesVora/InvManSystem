package com.invman.dataprocessing.controller;

import com.invman.common.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/settings/r2o")
@RequiredArgsConstructor
public class Ready2OrderOAuthController {

    private static final String R2O_GRANT_URL = "https://api.ready2order.com/v1/developerToken/grantAccessToken";

    private final AppSettingRepository appSettingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/request-grant")
    public ResponseEntity<Map<String, String>> requestGrant(@RequestBody Map<String, String> body) {
        String developerToken = body.getOrDefault("developerToken", "");
        String callbackUrl = body.getOrDefault("callbackUrl", "");

        if (developerToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "developerToken is required"));
        }

        appSettingRepository.upsert("r2o.developerToken", developerToken);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(developerToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of("callbackUri", callbackUrl);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(R2O_GRANT_URL, request, Map.class);

            String grantAccessUri = response != null
                    ? String.valueOf(response.getOrDefault("grantAccessUri", ""))
                    : "";

            return ResponseEntity.ok(Map.of("grantAccessUri", grantAccessUri));
        } catch (Exception e) {
            log.error("Failed to request ready2order grant access: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Failed to contact ready2order: " + e.getMessage()));
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam("accountToken") String accountToken) {
        appSettingRepository.upsert("r2o.accountToken", accountToken);
        appSettingRepository.upsert("r2o.enabled", "true");
        log.info("ready2order OAuth callback received, account token saved");
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/?tab=settings&connected=true"))
                .build();
    }
}
