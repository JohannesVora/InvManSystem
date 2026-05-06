package com.invman.dataprocessing.service;

import com.invman.common.dto.*;
import com.invman.common.entity.*;
import com.invman.common.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SupplierOfferService {

    private final SupplierProductOfferRepository offerRepo;
    private final SupplierRepository supplierRepo;
    private final InventoryItemRepository itemRepo;
    private final SalesProductRepository salesProductRepo;
    private final ProductComponentRepository componentRepo;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /** Returns all offers for the given supplier. */
    public List<SupplierOfferDto> getOffersBySupplierId(Long supplierId) {
        return offerRepo.findBySupplierId(supplierId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Returns a single offer by id, or 404. */
    public SupplierOfferDto getOffer(Long id) {
        return toDto(loadOffer(id));
    }

    /** Creates a new offer for the given supplier. */
    @Transactional
    public SupplierOfferDto createOffer(Long supplierId, SupplierOfferDto dto) {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found: " + supplierId));
        SupplierProductOffer offer = new SupplierProductOffer();
        offer.setSupplier(supplier);
        applyDto(offer, dto);
        return toDto(offerRepo.save(offer));
    }

    /** Updates an existing offer. */
    @Transactional
    public SupplierOfferDto updateOffer(Long id, SupplierOfferDto dto) {
        SupplierProductOffer offer = loadOffer(id);
        applyDto(offer, dto);
        return toDto(offerRepo.save(offer));
    }

    /** Deletes an offer. */
    @Transactional
    public void deleteOffer(Long id) {
        offerRepo.delete(loadOffer(id));
    }

    // ── Link ──────────────────────────────────────────────────────────────────

    /**
     * Links an offer to an inventory item.
     * If isPreferred=true, any existing preferred offer for the same item is cleared first.
     * Returns a warning when conversionFactor is 1.0 but supplier and canonical units differ.
     */
    @Transactional
    public LinkItemResponseDto linkItem(Long offerId, LinkItemRequestDto req) {
        SupplierProductOffer offer = loadOffer(offerId);
        InventoryItem item = itemRepo.findById(req.inventoryItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "InventoryItem not found: " + req.inventoryItemId()));

        boolean preferred = Boolean.TRUE.equals(req.isPreferred());
        if (preferred) {
            // Clear any existing preferred offer for this inventory item
            offerRepo.findByInventoryItemIdAndIsPreferredTrue(item.getId()).ifPresent(existing -> {
                existing.setIsPreferred(false);
                offerRepo.save(existing);
            });
        }

        offer.setInventoryItem(item);
        offer.setIsPreferred(preferred);
        offerRepo.save(offer);

        // Warn when factor=1.0 but units differ — likely a misconfiguration
        String warning = null;
        Double factor = offer.getConversionFactor();
        String packageUnit = offer.getPackageUnit();
        if (factor != null && factor == 1.0
                && packageUnit != null && !packageUnit.isBlank()
                && !packageUnit.equalsIgnoreCase(item.getUnit())) {
            warning = "Conversion factor is 1.0 but supplier unit '" + packageUnit
                    + "' \u2260 canonical unit '" + item.getUnit() + "'. Please check.";
        }

        return new LinkItemResponseDto(toDto(offer), warning);
    }

    // ── Excel Import ──────────────────────────────────────────────────────────

    /**
     * Imports supplier product offers from a Transgourmet-format .xlsx file.
     * Rows 0–3 are skipped (metadata/header); data starts at row 4.
     * Parsing stops when column 0 is blank.
     * Existing offers (matched by supplierId + supplierSku) are updated in place;
     * the inventoryItem and isPreferred links are preserved on update.
     */
    @Transactional
    public ExcelImportResultDto importFromExcel(Long supplierId, MultipartFile file) throws IOException {
        Supplier supplier = supplierRepo.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found: " + supplierId));

        int imported = 0, updated = 0, skipped = 0;
        List<String> skippedReasons = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int rowIdx = 4; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) break;

                Cell skuCell = row.getCell(0);
                if (skuCell == null || getCellString(skuCell).isBlank()) break;

                String sku = getCellString(skuCell);
                String productName = row.getCell(1) != null ? getCellString(row.getCell(1)) : null;
                String rawUnit = row.getCell(2) != null ? getCellString(row.getCell(2)) : null;
                Cell priceCell = row.getCell(7);
                Double unitPrice = (priceCell != null && priceCell.getCellType() == CellType.NUMERIC)
                        ? priceCell.getNumericCellValue()
                        : null;

                // Parse delivery unit: "KT = 12 FL" → ("KT", 12.0); plain abbrev → (abbrev, 1.0)
                String[] parsedUnit = parseDeliveryUnit(rawUnit);
                String packageUnit = parsedUnit[0];
                double conversionFactor = Double.parseDouble(parsedUnit[1]);

                // Upsert: update existing or create new
                var existing = offerRepo.findBySupplierIdAndSupplierSku(supplierId, sku);
                if (existing.isPresent()) {
                    SupplierProductOffer offer = existing.get();
                    offer.setSupplierProductName(productName);
                    offer.setPackageUnit(packageUnit);
                    offer.setConversionFactor(conversionFactor);
                    offer.setUnitPrice(unitPrice);
                    // inventoryItem and isPreferred are preserved
                    offerRepo.save(offer);
                    updated++;
                } else {
                    SupplierProductOffer offer = new SupplierProductOffer();
                    offer.setSupplier(supplier);
                    offer.setSupplierSku(sku);
                    offer.setSupplierProductName(productName);
                    offer.setPackageUnit(packageUnit);
                    offer.setConversionFactor(conversionFactor);
                    offer.setUnitPrice(unitPrice);
                    offer.setInventoryItem(null);
                    offer.setIsPreferred(false);
                    offerRepo.save(offer);
                    imported++;
                }
            }
        }

        return new ExcelImportResultDto(imported, updated, skipped, skippedReasons);
    }

    // ── Sales Products & Components ───────────────────────────────────────────

    /** Returns all sales products. */
    public List<SalesProductDto> getAllSalesProducts() {
        return salesProductRepo.findAll().stream()
                .map(sp -> new SalesProductDto(sp.getId(), sp.getExternalId(), sp.getName(), sp.getPosSystem()))
                .toList();
    }

    /** Returns all product components for the given sales product. */
    public List<ProductComponentDto> getComponentsBySalesProductId(Long salesProductId) {
        return componentRepo.findBySalesProductId(salesProductId).stream()
                .map(this::toComponentDto)
                .toList();
    }

    /**
     * Adds an inventory item as a component of a sales product.
     * Throws 409 if the mapping already exists.
     */
    @Transactional
    public ProductComponentDto addComponent(Long itemId, Long salesProductId, Double qtyRequired) {
        if (componentRepo.existsBySalesProductIdAndInventoryItemId(salesProductId, itemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This mapping already exists");
        }
        InventoryItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "InventoryItem not found: " + itemId));
        SalesProduct sp = salesProductRepo.findById(salesProductId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SalesProduct not found: " + salesProductId));
        ProductComponent component = new ProductComponent();
        component.setInventoryItem(item);
        component.setSalesProduct(sp);
        component.setQtyRequired(qtyRequired);
        return toComponentDto(componentRepo.save(component));
    }

    /** Deletes a product component by id. */
    @Transactional
    public void deleteComponent(Long componentId) {
        componentRepo.deleteById(componentId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SupplierProductOffer loadOffer(Long id) {
        return offerRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found: " + id));
    }

    /** Maps entity → DTO; null-safe for optional inventoryItem. */
    private SupplierOfferDto toDto(SupplierProductOffer o) {
        InventoryItem item = o.getInventoryItem();
        return new SupplierOfferDto(
                o.getId(),
                o.getSupplier() != null ? o.getSupplier().getId() : null,
                o.getSupplierSku(),
                o.getSupplierProductName(),
                o.getPackageUnit(),
                o.getUnitPrice(),
                o.getConversionFactor(),
                o.getIsPreferred(),
                item != null ? item.getId() : null,
                item != null ? item.getName() : null,
                item != null ? item.getUnit() : null
        );
    }

    private ProductComponentDto toComponentDto(ProductComponent c) {
        return new ProductComponentDto(
                c.getId(),
                c.getSalesProduct().getId(),
                c.getSalesProduct().getName(),
                c.getInventoryItem().getId(),
                c.getInventoryItem().getName(),
                c.getInventoryItem().getUnit(),
                c.getQtyRequired()
        );
    }

    /** Applies mutable fields from DTO to entity (does not touch supplier or inventoryItem). */
    private void applyDto(SupplierProductOffer offer, SupplierOfferDto dto) {
        offer.setSupplierSku(dto.supplierSku());
        offer.setSupplierProductName(dto.supplierProductName());
        offer.setPackageUnit(dto.packageUnit());
        offer.setUnitPrice(dto.unitPrice());
        offer.setConversionFactor(dto.conversionFactor());
        offer.setIsPreferred(Boolean.TRUE.equals(dto.isPreferred()));
    }

    /**
     * Parses a raw delivery-unit string from the Excel file.
     * "KT = 12 FL" → ["KT", "12.0"]
     * "EH"         → ["EH", "1.0"]
     * Returns a 2-element array: [packageUnit, conversionFactor].
     */
    private static final Pattern DELIVERY_UNIT_PATTERN =
            Pattern.compile("^([A-Z]+)\\s*=\\s*(\\d+(?:\\.\\d+)?)\\s+[A-Z]+$");

    private String[] parseDeliveryUnit(String raw) {
        if (raw == null || raw.isBlank()) return new String[]{"", "1.0"};
        String trimmed = raw.trim().toUpperCase();
        Matcher m = DELIVERY_UNIT_PATTERN.matcher(trimmed);
        if (m.matches()) {
            return new String[]{m.group(1), m.group(2)};
        }
        // Plain abbreviation (e.g. "EH", "KG") — factor defaults to 1.0
        return new String[]{trimmed, "1.0"};
    }

    /** Returns the string value of a cell regardless of its type. */
    private String getCellString(Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                // Numeric SKUs should be integers like 1234567 → "1234567"
                yield d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case STRING -> cell.getStringCellValue().trim();
            default -> "";
        };
    }
}
