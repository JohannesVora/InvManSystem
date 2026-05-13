package com.invman.common.repository;

import com.invman.common.entity.SalesProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesProductRepository extends JpaRepository<SalesProduct, Long> {

    Optional<SalesProduct> findByExternalIdAndPosSystem(String externalId, String posSystem);
}
