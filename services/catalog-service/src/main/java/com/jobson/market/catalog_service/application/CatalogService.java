package com.jobson.market.catalog_service.application;

import com.jobson.market.catalog_service.domain.CategoryEntity;
import com.jobson.market.catalog_service.domain.ProductDetails;
import com.jobson.market.catalog_service.domain.ProductEntity;
import com.jobson.market.catalog_service.domain.ProductStatus;
import com.jobson.market.catalog_service.persistence.CategoryRepository;
import com.jobson.market.catalog_service.persistence.ProductRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {

  private final CategoryRepository categories;
  private final ProductRepository products;
  private final Clock clock;

  public CatalogService(CategoryRepository categories, ProductRepository products, Clock clock) {
    this.categories = categories;
    this.products = products;
    this.clock = clock;
  }

  @Transactional
  public CategoryEntity createCategory(String name) {
    return categories.save(CategoryEntity.create(name, clock.instant()));
  }

  @Transactional(readOnly = true)
  public List<CategoryEntity> listCategories() {
    return categories.findAllByOrderByNameAsc();
  }

  @Transactional(readOnly = true)
  public CategoryEntity getCategory(UUID categoryId) {
    return requireCategory(categoryId);
  }

  @Transactional
  public CategoryEntity renameCategory(UUID categoryId, String name) {
    CategoryEntity category = requireCategory(categoryId);
    category.rename(name, clock.instant());
    return categories.save(category);
  }

  @Transactional
  public ProductEntity createProduct(UUID sellerId, ProductDetails details) {
    requireCategory(details.categoryId());
    return products.save(ProductEntity.create(sellerId, details, clock.instant()));
  }

  @Transactional(readOnly = true)
  public ProductEntity getProduct(UUID productId) {
    return requireProduct(productId);
  }

  @Transactional(readOnly = true)
  public List<ProductEntity> listProducts(UUID sellerId, ProductStatus status) {
    if (status == null) {
      return products.findBySellerIdOrderByCreatedAtAsc(sellerId);
    }
    return products.findBySellerIdAndStatusOrderByCreatedAtAsc(sellerId, status);
  }

  @Transactional
  public ProductEntity updateProduct(UUID productId, ProductDetails details) {
    requireCategory(details.categoryId());
    ProductEntity product = requireProduct(productId);
    product.update(details, clock.instant());
    return products.save(product);
  }

  @Transactional
  public ProductEntity publishProduct(UUID productId) {
    ProductEntity product = requireProduct(productId);
    requireCategory(product.categoryId());
    product.publish(clock.instant());
    return products.save(product);
  }

  @Transactional
  public ProductEntity unpublishProduct(UUID productId) {
    ProductEntity product = requireProduct(productId);
    product.unpublish(clock.instant());
    return products.save(product);
  }

  private CategoryEntity requireCategory(UUID categoryId) {
    return categories
        .findById(categoryId)
        .orElseThrow(() -> new CategoryNotFoundException(categoryId));
  }

  private ProductEntity requireProduct(UUID productId) {
    return products.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
  }
}
