package com.jobson.market.catalog_service.web;

import com.jobson.market.catalog_service.application.CatalogService;
import com.jobson.market.catalog_service.domain.CategoryEntity;
import com.jobson.market.catalog_service.domain.ProductDetails;
import com.jobson.market.catalog_service.domain.ProductEntity;
import com.jobson.market.catalog_service.domain.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog")
class CatalogController {

  private final CatalogService catalog;

  CatalogController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @PostMapping("/categories")
  ResponseEntity<CategoryResponse> createCategory(
      @Valid @RequestBody CreateCategoryRequest request) {
    CategoryEntity category = catalog.createCategory(request.name());
    return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
  }

  @GetMapping("/categories")
  List<CategoryResponse> listCategories() {
    return catalog.listCategories().stream().map(CategoryResponse::from).toList();
  }

  @GetMapping("/categories/{categoryId}")
  CategoryResponse getCategory(@PathVariable UUID categoryId) {
    return CategoryResponse.from(catalog.getCategory(categoryId));
  }

  @PatchMapping("/categories/{categoryId}")
  CategoryResponse renameCategory(
      @PathVariable UUID categoryId, @Valid @RequestBody UpdateCategoryRequest request) {
    return CategoryResponse.from(catalog.renameCategory(categoryId, request.name()));
  }

  @PostMapping("/products")
  ResponseEntity<ProductResponse> createProduct(
      @Valid @RequestBody CreateProductRequest request, Authentication authentication) {
    AuthenticatedSellerAccess.requireSellerAccess(authentication, request.sellerId());
    ProductEntity product = catalog.createProduct(request.sellerId(), request.details());
    return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
  }

  @GetMapping("/products/{productId}")
  ProductResponse getProduct(@PathVariable UUID productId) {
    return ProductResponse.from(catalog.getProduct(productId));
  }

  @GetMapping("/products")
  List<ProductResponse> listProducts(
      @RequestParam UUID sellerId, @RequestParam(required = false) ProductStatus status) {
    return catalog.listProducts(sellerId, status).stream().map(ProductResponse::from).toList();
  }

  @PatchMapping("/products/{productId}")
  ProductResponse updateProduct(
      @PathVariable UUID productId,
      @Valid @RequestBody UpdateProductRequest request,
      Authentication authentication) {
    ProductEntity product = catalog.getProduct(productId);
    AuthenticatedSellerAccess.requireSellerAccess(authentication, product.sellerId());
    return ProductResponse.from(catalog.updateProduct(productId, request.details()));
  }

  @PostMapping("/products/{productId}/publish")
  ProductResponse publishProduct(@PathVariable UUID productId, Authentication authentication) {
    ProductEntity product = catalog.getProduct(productId);
    AuthenticatedSellerAccess.requireSellerAccess(authentication, product.sellerId());
    return ProductResponse.from(catalog.publishProduct(productId));
  }

  @PostMapping("/products/{productId}/unpublish")
  ProductResponse unpublishProduct(@PathVariable UUID productId, Authentication authentication) {
    ProductEntity product = catalog.getProduct(productId);
    AuthenticatedSellerAccess.requireSellerAccess(authentication, product.sellerId());
    return ProductResponse.from(catalog.unpublishProduct(productId));
  }

  record CreateCategoryRequest(@NotBlank String name) {}

  record UpdateCategoryRequest(@NotBlank String name) {}

  record CreateProductRequest(
      @NotNull UUID sellerId,
      @NotNull UUID categoryId,
      @NotBlank String name,
      String description,
      @NotBlank String unit,
      @NotBlank String packageSize,
      @NotNull @DecimalMin("0.00") BigDecimal priceAmount,
      @NotBlank String currency) {
    ProductDetails details() {
      return new ProductDetails(
          categoryId, name, description, unit, packageSize, priceAmount, currency);
    }
  }

  record UpdateProductRequest(
      @NotNull UUID categoryId,
      @NotBlank String name,
      String description,
      @NotBlank String unit,
      @NotBlank String packageSize,
      @NotNull @DecimalMin("0.00") BigDecimal priceAmount,
      @NotBlank String currency) {
    ProductDetails details() {
      return new ProductDetails(
          categoryId, name, description, unit, packageSize, priceAmount, currency);
    }
  }

  record CategoryResponse(UUID id, String name, Instant createdAt, Instant updatedAt) {
    static CategoryResponse from(CategoryEntity category) {
      return new CategoryResponse(
          category.id(), category.name(), category.createdAt(), category.updatedAt());
    }
  }

  record ProductResponse(
      UUID id,
      UUID sellerId,
      UUID categoryId,
      String name,
      String description,
      String unit,
      String packageSize,
      BigDecimal priceAmount,
      String currency,
      ProductStatus status,
      Instant createdAt,
      Instant updatedAt) {
    static ProductResponse from(ProductEntity product) {
      return new ProductResponse(
          product.id(),
          product.sellerId(),
          product.categoryId(),
          product.name(),
          product.description(),
          product.unit(),
          product.packageSize(),
          product.priceAmount(),
          product.currency(),
          product.status(),
          product.createdAt(),
          product.updatedAt());
    }
  }
}
