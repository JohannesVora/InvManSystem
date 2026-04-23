package com.invman.common.repository;

import com.invman.common.entity.PosProductMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosProductMappingRepository extends JpaRepository<PosProductMapping, Long> {
}
