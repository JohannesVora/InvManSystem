package com.invman.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.connector.ConnectorResult;
import com.invman.common.connector.SupplierOrderPayload;
import com.invman.common.entity.AppSetting;
import com.invman.common.entity.ConnectorConfig;
import com.invman.common.repository.AppSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppConnectorTest {

    @Mock AppSettingRepository appSettingRepository;
    @Mock RestTemplate restTemplate;

    WhatsAppConnector connector;

    @BeforeEach
    void setUp() {
        connector = new WhatsAppConnector(appSettingRepository, new ObjectMapper(), restTemplate);
    }

    private void stubSetting(String key, String value) {
        AppSetting setting = new AppSetting();
        setting.setKey(key);
        setting.setValue(value);
        when(appSettingRepository.findByKey(key)).thenReturn(Optional.of(setting));
    }

    private ConnectorConfig configWithPhone(String phone) {
        ConnectorConfig config = new ConnectorConfig();
        config.setConfigPayload("{\"recipientPhone\":\"" + phone + "\",\"messageTemplate\":\"Order {date} - {supplierName}:\\n\\n{orderLines}\"}");
        return config;
    }

    private SupplierOrderPayload samplePayload() {
        return new SupplierOrderPayload(
                1L,
                "Supplier A",
                LocalDate.now().toString(),
                List.of(new SupplierOrderPayload.OrderLineItem("Product 1", "SKU-A-001", 2.0, "bag"))
        );
    }

    @Test
    void returnsWhatsAppType() {
        assertThat(connector.getType()).isEqualTo("WHATSAPP");
    }

    @Test
    void sendsMessageSuccessfully_whenTokenAlreadyStored() {
        stubSetting("wppconnect.baseUrl", "http://wppconnect:21465");
        stubSetting("wppconnect.session", "inventory-session");
        stubSetting("wppconnect.token", "existing-token");

        @SuppressWarnings("unchecked")
        Map<String, Object> sendResponse = Map.of("id", "msg-123");
        when(restTemplate.postForObject(contains("/send-message"), any(), eq(Map.class)))
                .thenReturn(sendResponse);

        ConnectorResult result = connector.transmit(samplePayload(), configWithPhone("4366000000001"));

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("msg-123");
        verify(restTemplate, times(1)).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void generatesTokenWhenNotStored() {
        stubSetting("wppconnect.baseUrl", "http://wppconnect:21465");
        stubSetting("wppconnect.session", "inventory-session");
        stubSetting("wppconnect.secretKey", "MYSECRET");
        when(appSettingRepository.findByKey("wppconnect.token")).thenReturn(Optional.empty());

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = Map.of("token", "new-token-abc");
        @SuppressWarnings("unchecked")
        Map<String, Object> sendResponse = Map.of("id", "msg-456");

        when(restTemplate.postForObject(contains("generate-token"), any(), eq(Map.class)))
                .thenReturn(tokenResponse);
        when(restTemplate.postForObject(contains("/send-message"), any(), eq(Map.class)))
                .thenReturn(sendResponse);

        ConnectorResult result = connector.transmit(samplePayload(), configWithPhone("4366000000002"));

        assertThat(result.success()).isTrue();
        verify(appSettingRepository).upsert("wppconnect.token", "new-token-abc");
    }

    @Test
    void returnsFailureOnInvalidJson() {
        ConnectorConfig config = new ConnectorConfig();
        config.setConfigPayload("not-valid-json");

        ConnectorResult result = connector.transmit(samplePayload(), config);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("WhatsApp send failed");
    }

    @Test
    void returnsFailureWhenRestTemplateFails() {
        stubSetting("wppconnect.baseUrl", "http://wppconnect:21465");
        stubSetting("wppconnect.session", "inventory-session");
        stubSetting("wppconnect.token", "existing-token");

        when(restTemplate.postForObject(contains("/send-message"), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        ConnectorResult result = connector.transmit(samplePayload(), configWithPhone("4366000000001"));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Connection refused");
    }

    @Test
    void handlesNullSendResponse() {
        stubSetting("wppconnect.baseUrl", "http://wppconnect:21465");
        stubSetting("wppconnect.session", "inventory-session");
        stubSetting("wppconnect.token", "existing-token");

        when(restTemplate.postForObject(contains("/send-message"), any(), eq(Map.class)))
                .thenReturn(null);

        ConnectorResult result = connector.transmit(samplePayload(), configWithPhone("4366000000001"));

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("unknown");
    }
}
