package com.invman.dataprocessing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invman.common.dto.SalesProductDto;
import com.invman.common.entity.AppSetting;
import com.invman.common.entity.SalesProduct;
import com.invman.common.repository.AppSettingRepository;
import com.invman.common.repository.ProductComponentRepository;
import com.invman.common.repository.SalesProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sales-products")
@RequiredArgsConstructor
public class SalesProductController {

    private static final String R2O_PRODUCTS_URL = "https://api.ready2order.com/v1/products";

    private final SalesProductRepository salesProductRepository;
    private final ProductComponentRepository productComponentRepository;
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalesProductDto create(@RequestBody Map<String, String> body) {
        SalesProduct sp = new SalesProduct();
        sp.setExternalId(body.getOrDefault("externalId", "").trim());
        sp.setName(body.getOrDefault("name", "").trim());
        sp.setPosSystem(body.getOrDefault("posSystem", "MANUAL").trim());
        SalesProduct saved = salesProductRepository.save(sp);
        log.info("Created sales product '{}' (id={}, externalId={}, posSystem={})",
                saved.getName(), saved.getId(), saved.getExternalId(), saved.getPosSystem());
        return toDto(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        productComponentRepository.deleteLegacyMappingsBySalesProductId(id);
        productComponentRepository.deleteBySalesProductId(id);
        salesProductRepository.deleteById(id);
        log.info("Deleted sales product id={}", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import-r2o")
    public ResponseEntity<Map<String, Object>> importFromR2O() {
        String accountToken = appSettingRepository.findByKey("r2o.accountToken")
                .map(AppSetting::getValue).orElse("");
        if (accountToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not connected to ready2order"));
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accountToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            int imported = 0, updated = 0;
            int page = 1;
            while (true) {
                String url = R2O_PRODUCTS_URL + "?limit=100&page=" + page;
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                JsonNode products = objectMapper.readTree(response.getBody());
                if (!products.isArray() || products.isEmpty()) break;

                for (JsonNode p : products) {
                    long productId = p.path("product_id").asLong(0);
                    if (productId == 0) continue;
                    String externalId = String.valueOf(productId);
                    String name = p.path("product_name").asText("").trim();
                    if (name.isBlank()) continue;

                    Optional<SalesProduct> existing = salesProductRepository
                            .findByExternalIdAndPosSystem(externalId, "READY2ORDER");
                    SalesProduct sp = existing.orElseGet(SalesProduct::new);
                    sp.setExternalId(externalId);
                    sp.setName(name);
                    sp.setPosSystem("READY2ORDER");
                    salesProductRepository.save(sp);
                    if (existing.isPresent()) updated++; else imported++;
                }
                if (products.size() < 100) break;
                page++;
            }
            log.info("Imported {} / updated {} sales products from ready2order", imported, updated);
            return ResponseEntity.ok(Map.of("imported", imported, "updated", updated));
        } catch (Exception e) {
            log.error("Failed to import from ready2order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/import-csv")
    public ResponseEntity<Map<String, Object>> importFromCsv(@RequestParam("file") MultipartFile file) {
        int imported = 0, updated = 0, skipped = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                if (firstLine) {
                    firstLine = false;
                    String lower = line.toLowerCase();
                    if (lower.startsWith("name") || lower.startsWith("external")) continue; // skip header
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 2) { skipped++; continue; }
                String name      = parts[0].trim().replaceAll("^\"|\"$", "");
                String externalId = parts[1].trim().replaceAll("^\"|\"$", "");
                String posSystem  = parts.length >= 3 ? parts[2].trim().replaceAll("^\"|\"$", "") : "MANUAL";
                if (name.isBlank() || externalId.isBlank()) { skipped++; continue; }

                Optional<SalesProduct> existing = salesProductRepository
                        .findByExternalIdAndPosSystem(externalId, posSystem);
                SalesProduct sp = existing.orElseGet(SalesProduct::new);
                sp.setExternalId(externalId);
                sp.setName(name);
                sp.setPosSystem(posSystem);
                salesProductRepository.save(sp);
                if (existing.isPresent()) updated++; else imported++;
            }
            log.info("CSV import: {} imported, {} updated, {} skipped", imported, updated, skipped);
            return ResponseEntity.ok(Map.of("imported", imported, "updated", updated, "skipped", skipped));
        } catch (Exception e) {
            log.error("CSV import failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private SalesProductDto toDto(SalesProduct sp) {
        return new SalesProductDto(sp.getId(), sp.getExternalId(), sp.getName(), sp.getPosSystem());
    }
}
