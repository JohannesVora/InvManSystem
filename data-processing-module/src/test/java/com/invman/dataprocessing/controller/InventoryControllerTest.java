package com.invman.dataprocessing.controller;

import com.invman.common.dto.InventoryItemDto;
import com.invman.common.dto.UpdateInventoryItemDto;
import com.invman.dataprocessing.service.MasterDataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

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
        InventoryItemDto item = new InventoryItemDto(1L, "Flour", "kg", 50.0, 20.0, 100.0, false, false);
        when(masterDataManager.getAllInventoryItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Flour"))
                .andExpect(jsonPath("$[0].unit").value("kg"));
    }

    @Test
    void createInventoryItemReturnCreated() throws Exception {
        InventoryItemDto created = new InventoryItemDto(99L, "Oat Milk", "L", 0.0, 0.0, 0.0, false, false);
        when(masterDataManager.createInventoryItem(any())).thenReturn(created);

        mockMvc.perform(post("/api/inventory-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Oat Milk", "unit": "L"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Oat Milk"))
                .andExpect(jsonPath("$.unit").value("L"));
    }

    @Test
    void updateInventoryItemReturnsUpdated() throws Exception {
        InventoryItemDto updated = new InventoryItemDto(1L, "NewName", "L", 5.0, 2.0, 10.0, false, false);
        when(masterDataManager.updateInventoryItem(any(Long.class), any())).thenReturn(updated);

        mockMvc.perform(put("/api/inventory-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"NewName","unit":"L","minStockLevel":2.0,"reorderTarget":10.0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }
}
