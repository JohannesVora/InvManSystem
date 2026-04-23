package com.invman.dataprocessing.controller;

import com.invman.common.dto.SmtpSettingsDto;
import com.invman.dataprocessing.service.SmtpSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SmtpSettingsController {

    private final SmtpSettingsService smtpSettingsService;

    @GetMapping("/smtp")
    public ResponseEntity<SmtpSettingsDto> getSmtpSettings() {
        return ResponseEntity.ok(smtpSettingsService.getSettings());
    }

    @PutMapping("/smtp")
    public ResponseEntity<SmtpSettingsDto> updateSmtpSettings(@RequestBody SmtpSettingsDto dto) {
        smtpSettingsService.updateSettings(dto);
        return ResponseEntity.ok(smtpSettingsService.getSettings());
    }
}
