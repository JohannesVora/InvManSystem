package com.invman.outbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.connector.ConnectorResult;
import com.invman.common.connector.SupplierOrderPayload;
import com.invman.common.entity.ConnectorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConnectorTest {

    @Mock JavaMailSender mailSender;

    EmailConnector emailConnector;

    @BeforeEach
    void setUp() {
        emailConnector = new EmailConnector(mailSender, new ObjectMapper());
    }

    @Test
    void returnsEmailType() {
        assertThat(emailConnector.getType()).isEqualTo("EMAIL");
    }

    @Test
    void sendsEmailSuccessfully() {
        ConnectorConfig config = new ConnectorConfig();
        config.setConfigPayload("{\"recipientEmail\":\"orders@supplier.com\",\"subjectTemplate\":\"Order - {supplierName}\",\"bodyTemplate\":\"{orderLines}\"}");

        SupplierOrderPayload payload = new SupplierOrderPayload(
                1L, "Test Supplier", "2026-04-22",
                List.of(new SupplierOrderPayload.OrderLineItem("Flour", "FLOUR-25KG", 2.0, "kg"))
        );

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        ConnectorResult result = emailConnector.transmit(payload, config);

        assertThat(result.success()).isTrue();
        assertThat(result.message()).contains("orders@supplier.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains("orders@supplier.com");
        assertThat(sent.getSubject()).contains("Test Supplier");
    }

    @Test
    void returnsFailureOnException() {
        ConnectorConfig config = new ConnectorConfig();
        config.setConfigPayload("{\"recipientEmail\":\"orders@supplier.com\"}");

        SupplierOrderPayload payload = new SupplierOrderPayload(
                1L, "Test Supplier", "2026-04-22", List.of()
        );

        doThrow(new RuntimeException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        ConnectorResult result = emailConnector.transmit(payload, config);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Connection refused");
    }

    @Test
    void returnsFailureOnInvalidJson() {
        ConnectorConfig config = new ConnectorConfig();
        config.setConfigPayload("not-valid-json");

        SupplierOrderPayload payload = new SupplierOrderPayload(1L, "Supplier", "2026-04-22", List.of());

        ConnectorResult result = emailConnector.transmit(payload, config);

        assertThat(result.success()).isFalse();
    }
}
