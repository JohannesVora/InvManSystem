package com.invman.common.repository;

import com.invman.common.entity.SupplierProductOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierProductOfferRepository extends JpaRepository<SupplierProductOffer, Long> {

    Optional<SupplierProductOffer> findByInventoryItemIdAndIsPreferredTrue(Long inventoryItemId);

    List<SupplierProductOffer> findBySupplierId(Long supplierId);

    Optional<SupplierProductOffer> findBySupplierIdAndSupplierSku(Long supplierId, String supplierSku);
}
