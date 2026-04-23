package com.invman.common.repository;

import com.invman.common.entity.SalesProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesProductRepository extends JpaRepository<SalesProduct, Long> {
}
