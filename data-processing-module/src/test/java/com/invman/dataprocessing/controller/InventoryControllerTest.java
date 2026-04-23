package com.invman.dataprocessing.controller;

import com.invman.common.dto.InventoryItemDto;
import com.invman.dataprocessing.service.MasterDataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean MasterDataManager masterDataManager;

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.schema_version").value("1"));
    }

    @Test
    void inventoryEndpointReturnsItems() throws Exception {
        InventoryItemDto item = new InventoryItemDto(1L, "Flour", "kg", 50.0, 20.0, 100.0, false);
        when(masterDataManager.getAllInventoryItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Flour"))
                .andExpect(jsonPath("$[0].unit").value("kg"));
    }
}
