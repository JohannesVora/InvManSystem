package com.invman.dataprocessing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            if (url == null || !url.startsWith("jdbc:sqlite")) {
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA foreign_keys=ON");
                log.info("SQLite PRAGMAs applied (WAL mode, foreign keys ON)");
            }
        } catch (Exception e) {
            log.warn("Could not apply SQLite PRAGMAs: {}", e.getMessage());
        }
    }
}
