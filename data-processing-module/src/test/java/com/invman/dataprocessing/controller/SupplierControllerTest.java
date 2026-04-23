package com.invman.dataprocessing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.dto.ConnectorConfigDto;
import com.invman.common.dto.SupplierDto;
import com.invman.dataprocessing.service.MasterDataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SupplierController.class)
class SupplierControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean MasterDataManager masterDataManager;

    @Test
    void getSuppliersReturnsList() throws Exception {
        SupplierDto supplier = new SupplierDto(1L, "Fresh Farm", "orders@ff.com", "555-1234");
        when(masterDataManager.getAllSuppliers()).thenReturn(List.of(supplier));

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fresh Farm"));
    }

    @Test
    void getConnectorConfigReturnsConfig() throws Exception {
        ConnectorConfigDto config = new ConnectorConfigDto(1L, 1L, "EMAIL", "{}", true);
        when(masterDataManager.getConnectorConfig(1L)).thenReturn(config);

        mockMvc.perform(get("/api/suppliers/1/connector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectorTypeName").value("EMAIL"));
    }

    @Test
    void getConnectorConfigReturns404WhenNotFound() throws Exception {
        when(masterDataManager.getConnectorConfig(99L)).thenReturn(null);

        mockMvc.perform(get("/api/suppliers/99/connector"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateConnectorConfigSavesAndReturns() throws Exception {
        ConnectorConfigDto input = new ConnectorConfigDto(null, 1L, "EMAIL", "{\"recipientEmail\":\"test@test.com\"}", true);
        ConnectorConfigDto saved = new ConnectorConfigDto(1L, 1L, "EMAIL", "{\"recipientEmail\":\"test@test.com\"}", true);

        when(masterDataManager.updateConnectorConfig(eq(1L), any())).thenReturn(saved);

        mockMvc.perform(put("/api/suppliers/1/connector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
