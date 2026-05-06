package com.invman.dataprocessing.controller;

import com.invman.common.dto.*;
import com.invman.dataprocessing.service.SupplierOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SupplierCatalogController {

    private final SupplierOfferService supplierOfferService;

    /** Returns all offers for the specified supplier. */
    @GetMapping("/suppliers/{supplierId}/offers")
    public List<SupplierOfferDto> getOffers(@PathVariable("supplierId") Long supplierId) {
        return supplierOfferService.getOffersBySupplierId(supplierId);
    }

    /** Imports supplier product offers from an uploaded .xlsx file. */
    @PostMapping("/suppliers/{supplierId}/offers/import-excel")
    public ExcelImportResultDto importExcel(
            @PathVariable("supplierId") Long supplierId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return supplierOfferService.importFromExcel(supplierId, file);
    }

    /** Returns a single offer by id. */
    @GetMapping("/supplier-offers/{id}")
    public SupplierOfferDto getOffer(@PathVariable("id") Long id) {
        return supplierOfferService.getOffer(id);
    }

    /** Creates a new offer. The supplier is determined by dto.supplierId(). */
    @PostMapping("/supplier-offers")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierOfferDto createOffer(@RequestBody SupplierOfferDto dto) {
        return supplierOfferService.createOffer(dto.supplierId(), dto);
    }

    /** Updates an existing offer. */
    @PutMapping("/supplier-offers/{id}")
    public SupplierOfferDto updateOffer(@PathVariable("id") Long id, @RequestBody SupplierOfferDto dto) {
        return supplierOfferService.updateOffer(id, dto);
    }

    /** Deletes an offer; returns 204 No Content. */
    @DeleteMapping("/supplier-offers/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable("id") Long id) {
        supplierOfferService.deleteOffer(id);
        return ResponseEntity.noContent().build();
    }

    /** Links an offer to an inventory item (optionally marks as preferred). */
    @PutMapping("/supplier-offers/{id}/link-item")
    public LinkItemResponseDto linkItem(
            @PathVariable("id") Long id,
            @RequestBody LinkItemRequestDto req) {
        return supplierOfferService.linkItem(id, req);
    }

    /** Returns all sales products. */
    @GetMapping("/sales-products")
    public List<SalesProductDto> getSalesProducts() {
        return supplierOfferService.getAllSalesProducts();
    }

    /** Returns all product components for the specified sales product. */
    @GetMapping("/sales-products/{salesProductId}/components")
    public List<ProductComponentDto> getComponents(@PathVariable("salesProductId") Long salesProductId) {
        return supplierOfferService.getComponentsBySalesProductId(salesProductId);
    }

    /** Adds a product component mapping: inventory item → sales product with required quantity. */
    @PostMapping("/inventory-items/{itemId}/components")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductComponentDto addComponent(
            @PathVariable("itemId") Long itemId,
            @RequestBody Map<String, Object> body) {
        Long salesProductId = ((Number) body.get("salesProductId")).longValue();
        Double qtyRequired = ((Number) body.get("qtyRequired")).doubleValue();
        return supplierOfferService.addComponent(itemId, salesProductId, qtyRequired);
    }

    /** Deletes a product component; returns 204 No Content. */
    @DeleteMapping("/product-components/{id}")
    public ResponseEntity<Void> deleteComponent(@PathVariable("id") Long id) {
        supplierOfferService.deleteComponent(id);
        return ResponseEntity.noContent().build();
    }
}
