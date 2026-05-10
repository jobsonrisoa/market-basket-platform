package com.jobson.market.catalog_service.persistence;

import com.jobson.market.catalog_service.domain.CategoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

  List<CategoryEntity> findAllByOrderByNameAsc();
}
