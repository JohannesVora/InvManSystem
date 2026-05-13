package com.invman.common.repository;

import com.invman.common.entity.ProductComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductComponentRepository extends JpaRepository<ProductComponent, Long> {

    List<ProductComponent> findBySalesProductId(Long salesProductId);

    boolean existsBySalesProductIdAndInventoryItemId(Long salesProductId, Long inventoryItemId);

    void deleteBySalesProductId(Long salesProductId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM pos_product_mappings WHERE sales_product_id = :salesProductId", nativeQuery = true)
    void deleteLegacyMappingsBySalesProductId(@Param("salesProductId") Long salesProductId);
}
