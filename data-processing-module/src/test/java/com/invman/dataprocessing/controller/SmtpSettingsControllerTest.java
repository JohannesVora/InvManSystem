package com.invman.dataprocessing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.dto.SmtpSettingsDto;
import com.invman.dataprocessing.service.SmtpSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SmtpSettingsController.class)
class SmtpSettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean SmtpSettingsService smtpSettingsService;

    @Test
    void getSmtpSettingsReturnsSettings() throws Exception {
        SmtpSettingsDto dto = new SmtpSettingsDto("smtp.example.com", 587, "user@test.com", null, "from@test.com");
        when(smtpSettingsService.getSettings()).thenReturn(dto);

        mockMvc.perform(get("/api/settings/smtp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("smtp.example.com"))
                .andExpect(jsonPath("$.port").value(587));
    }

    @Test
    void updateSmtpSettingsSavesAndReturns() throws Exception {
        SmtpSettingsDto input = new SmtpSettingsDto("smtp.gmail.com", 465, "new@test.com", "secret", "new@test.com");
        SmtpSettingsDto updated = new SmtpSettingsDto("smtp.gmail.com", 465, "new@test.com", null, "new@test.com");

        doNothing().when(smtpSettingsService).updateSettings(any());
        when(smtpSettingsService.getSettings()).thenReturn(updated);

        mockMvc.perform(put("/api/settings/smtp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value("smtp.gmail.com"));
    }
}
