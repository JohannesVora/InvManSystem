package com.invman.dataprocessing.service;

import com.invman.common.entity.AppSetting;
import com.invman.common.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WppConnectSettingsService {


    private final AppSettingRepository appSettingRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public String getBaseUrl() {
        return getSetting("wppconnect.baseUrl", "http://wppconnect:21465");
    }

    public String getSession() {
        return getSetting("wppconnect.session", "inventory-session");
    }

    public String getSecretKey() {
        return getSetting("wppconnect.secretKey", "");
    }

    public String getToken() {
        return getSetting("wppconnect.token", "");
    }

    public void saveSettings(String baseUrl, String secretKey, String session) {
        appSettingRepository.upsert("wppconnect.baseUrl", baseUrl);
        appSettingRepository.upsert("wppconnect.secretKey", secretKey);
        appSettingRepository.upsert("wppconnect.session", session);
    }

    public void generateToken() {
        String baseUrl = getBaseUrl();
        String session = getSession();
        String secretKey = getSecretKey();

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
        log.info("WPPConnect token generated");
    }

    /**
     * Returns the current QR code if available, starts the session if CLOSED.
     * Returns empty string while the session is initializing — callers should poll.
     */
    public String getQrCodeBase64() {
        String baseUrl = getBaseUrl();
        String session = getSession();
        String token = getToken();

        String status = getSessionStatus(baseUrl, session, token);
        log.info("WPPConnect session status: {}", status);

        if ("CLOSED".equalsIgnoreCase(status)) {
            log.info("WPPConnect session CLOSED — starting session");
            startSession(baseUrl, session, token);
            return "";
        }

        String qr = fetchQrCode(baseUrl, session, token);
        return qr != null && !"null".equals(qr) ? qr : "";
    }

    private String getSessionStatus(String baseUrl, String session, String token) {
        try {
            String url = baseUrl + "/api/" + session + "/status-session";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<String, Object> response = responseEntity.getBody();

            if (response != null && response.containsKey("status")) {
                return String.valueOf(response.get("status"));
            }
        } catch (Exception e) {
            log.warn("Could not check WPPConnect session status: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    private String fetchQrCode(String baseUrl, String session, String token) {
        try {
            String url = baseUrl + "/api/" + session + "/qrcode-session";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(List.of(MediaType.IMAGE_PNG, MediaType.APPLICATION_JSON, MediaType.ALL));
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // The endpoint returns binary PNG when QR is available, JSON otherwise
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);
            byte[] body = responseEntity.getBody();
            MediaType contentType = responseEntity.getHeaders().getContentType();

            if (body != null && body.length > 100) {
                if (contentType != null && contentType.isCompatibleWith(MediaType.IMAGE_PNG)) {
                    return "data:image/png;base64," + Base64.getEncoder().encodeToString(body);
                }
                // Fallback: try to parse as JSON for legacy format
                String bodyStr = new String(body);
                if (bodyStr.contains("qrcode")) {
                    // already a data URL or base64 in JSON — handled by old path
                    log.debug("QR code response is JSON: {}", bodyStr.substring(0, Math.min(80, bodyStr.length())));
                }
            }
        } catch (Exception e) {
            log.debug("QR code poll attempt failed: {}", e.getMessage());
        }
        return null;
    }

    private void startSession(String baseUrl, String session, String token) {
        try {
            String url = baseUrl + "/api/" + session + "/start-session";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            // autoClose in ms; 600000 = 10 minutes
            Map<String, Object> body = Map.of("autoClose", 600000, "waitQrCode", false);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForObject(url, request, Map.class);
            log.info("WPPConnect session start-session called");
        } catch (Exception e) {
            log.warn("Could not start WPPConnect session: {}", e.getMessage());
        }
    }

    private String getSetting(String key, String defaultValue) {
        return appSettingRepository.findByKey(key)
                .map(AppSetting::getValue)
                .orElse(defaultValue);
    }
}
