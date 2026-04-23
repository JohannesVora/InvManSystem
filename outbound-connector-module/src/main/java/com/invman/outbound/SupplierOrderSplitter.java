package com.invman.outbound;

import com.invman.common.entity.ReplenishmentOrderLine;
import com.invman.common.entity.Supplier;
import com.invman.common.entity.SupplierProductOffer;
import com.invman.common.repository.SupplierProductOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SupplierOrderSplitter {

    private final SupplierProductOfferRepository offerRepository;

    public record SupplierSplit(Supplier supplier, List<SplitLine> lines) {}

    public record SplitLine(
            ReplenishmentOrderLine orderLine,
            SupplierProductOffer offer,
            double orderedQty
    ) {}

    public List<SupplierSplit> split(List<ReplenishmentOrderLine> lines) {
        Map<Long, List<SplitLine>> bySupplier = new LinkedHashMap<>();
        Map<Long, Supplier> suppliers = new LinkedHashMap<>();

        for (ReplenishmentOrderLine line : lines) {
            Long itemId = line.getInventoryItem().getId();

            Optional<SupplierProductOffer> offerOpt = offerRepository
                    .findByInventoryItemIdAndIsPreferredTrue(itemId);

            if (offerOpt.isEmpty()) {
                continue; // skip items without a preferred supplier
            }

            SupplierProductOffer offer = offerOpt.get();
            Supplier supplier = offer.getSupplier();

            double conversionFactor = offer.getConversionFactor() != null
                    ? offer.getConversionFactor() : 1.0;
            double orderedQty = Math.ceil(line.getRequestedQty() / conversionFactor);

            bySupplier.computeIfAbsent(supplier.getId(), id -> new ArrayList<>())
                    .add(new SplitLine(line, offer, orderedQty));
            suppliers.put(supplier.getId(), supplier);
        }

        return bySupplier.entrySet().stream()
                .map(entry -> new SupplierSplit(suppliers.get(entry.getKey()), entry.getValue()))
                .toList();
    }
}
