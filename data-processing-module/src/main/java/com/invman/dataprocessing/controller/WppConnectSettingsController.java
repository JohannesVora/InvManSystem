package com.invman.dataprocessing.controller;

import com.invman.dataprocessing.service.WppConnectSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings/wppconnect")
@RequiredArgsConstructor
public class WppConnectSettingsController {

    private final WppConnectSettingsService wppConnectSettingsService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        String token = wppConnectSettingsService.getToken();
        return ResponseEntity.ok(Map.of(
                "baseUrl", wppConnectSettingsService.getBaseUrl(),
                "secretKey", maskSecretKey(wppConnectSettingsService.getSecretKey()),
                "session", wppConnectSettingsService.getSession(),
                "tokenConfigured", !token.isBlank()
        ));
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> updateSettings(@RequestBody Map<String, String> body) {
        wppConnectSettingsService.saveSettings(
                body.getOrDefault("baseUrl", ""),
                body.getOrDefault("secretKey", ""),
                body.getOrDefault("session", "")
        );
        return ResponseEntity.ok(Map.of("message", "Settings saved"));
    }

    @PostMapping("/generate-token")
    public ResponseEntity<Map<String, String>> generateToken() {
        wppConnectSettingsService.generateToken();
        return ResponseEntity.ok(Map.of("message", "Token generated"));
    }

    @GetMapping("/qrcode")
    public ResponseEntity<Map<String, String>> getQrCode() {
        String qrCode = wppConnectSettingsService.getQrCodeBase64();
        return ResponseEntity.ok(Map.of("qrcode", qrCode));
    }

    private String maskSecretKey(String key) {
        if (key == null || key.length() <= 4) return "****";
        return key.substring(0, 2) + "****" + key.substring(key.length() - 2);
    }
}
