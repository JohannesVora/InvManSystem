package com.invman.common.repository;

import com.invman.common.entity.ProductComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductComponentRepository extends JpaRepository<ProductComponent, Long> {
}
