package com.invman.dataprocessing.service;

import com.invman.common.dto.SmtpSettingsDto;
import com.invman.common.entity.AppSetting;
import com.invman.common.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Properties;

@Service
@RequiredArgsConstructor
public class SmtpSettingsService {

    private final AppSettingRepository appSettingRepository;
    private final JavaMailSenderImpl mailSender;

    @EventListener(ApplicationReadyEvent.class)
    public void loadSettingsFromDb() {
        String host = getSetting("smtp.host", null);
        if (host == null || host.isBlank()) return;
        String portStr = getSetting("smtp.port", "587");
        String username = getSetting("smtp.username", "");
        String password = getSetting("smtp.password", "");
        String from = getSetting("smtp.from", "");
        int port;
        try { port = Integer.parseInt(portStr); } catch (NumberFormatException e) { port = 587; }
        reconfigureMailSender(new SmtpSettingsDto(host, port, username, password, from));
    }

    public SmtpSettingsDto getSettings() {
        String host = getSetting("smtp.host", "");
        String portStr = getSetting("smtp.port", "587");
        String username = getSetting("smtp.username", "");
        String from = getSetting("smtp.from", "");

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            port = 587;
        }

        return new SmtpSettingsDto(host, port, username, null, from);
    }

    @Transactional
    public void updateSettings(SmtpSettingsDto dto) {
        saveSetting("smtp.host", dto.host());
        saveSetting("smtp.port", String.valueOf(dto.port()));
        saveSetting("smtp.username", dto.username());
        saveSetting("smtp.from", dto.from());
        if (dto.password() != null && !dto.password().isBlank()) {
            saveSetting("smtp.password", dto.password());
        }

        reconfigureMailSender(dto);
    }

    private void reconfigureMailSender(SmtpSettingsDto dto) {
        if (dto.host() != null) {
            mailSender.setHost(dto.host());
        }
        if (dto.port() != null) {
            mailSender.setPort(dto.port());
        }
        if (dto.username() != null) {
            mailSender.setUsername(dto.username());
        }
        if (dto.password() != null && !dto.password().isBlank()) {
            mailSender.setPassword(dto.password());
        }

        int effectivePort = dto.port() != null ? dto.port() : mailSender.getPort();
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        if (effectivePort == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.enable", "false");
        }
    }

    private String getSetting(String key, String defaultValue) {
        return appSettingRepository.findByKey(key)
                .map(AppSetting::getValue)
                .orElse(defaultValue);
    }

    private void saveSetting(String key, String value) {
        AppSetting setting = appSettingRepository.findByKey(key)
                .orElseGet(() -> {
                    AppSetting s = new AppSetting();
                    s.setKey(key);
                    return s;
                });
        setting.setValue(value);
        appSettingRepository.save(setting);
    }
}
