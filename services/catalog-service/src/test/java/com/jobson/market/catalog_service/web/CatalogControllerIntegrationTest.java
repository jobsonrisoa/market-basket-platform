package com.jobson.market.catalog_service.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobson.market.catalog_service.TestcontainersConfiguration;
import com.jobson.market.catalog_service.persistence.CategoryRepository;
import com.jobson.market.catalog_service.persistence.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ProductRepository products;
  @Autowired private CategoryRepository categories;

  @BeforeEach
  void cleanDatabase() {
    products.deleteAll();
    categories.deleteAll();
  }

  @Test
  void shouldCreateCategoriesAndManageProductLifecycle() throws Exception {
    UUID sellerId = UUID.randomUUID();

    MvcResult categoryResult =
        mvc.perform(
                post("/catalog/categories")
                    .with(sellerJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Produce\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Produce"))
            .andReturn();

    JsonNode category = objectMapper.readTree(categoryResult.getResponse().getContentAsString());
    String categoryId = category.path("id").stringValue();

    mvc.perform(get("/catalog/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(categoryId));

    mvc.perform(
            patch("/catalog/categories/{categoryId}", categoryId)
                .with(sellerJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Seasonal Produce\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Seasonal Produce"));

    MvcResult productResult =
        mvc.perform(
                post("/catalog/products")
                    .with(sellerJwtFor(sellerId, "ACTIVE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "sellerId":"%s",
                          "categoryId":"%s",
                          "name":"Organic Carrots",
                          "description":"Fresh carrots",
                          "unit":"kg",
                          "packageSize":"1 kg bag",
                          "priceAmount":7.50,
                          "currency":"USD"
                        }
                        """
                            .formatted(sellerId, categoryId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
            .andExpect(jsonPath("$.categoryId").value(categoryId))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();

    JsonNode product = objectMapper.readTree(productResult.getResponse().getContentAsString());
    String productId = product.path("id").stringValue();

    mvc.perform(get("/catalog/products/{productId}", productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(productId));

    mvc.perform(
            get("/catalog/products")
                .param("sellerId", sellerId.toString())
                .param("status", "DRAFT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(productId));

    mvc.perform(
            patch("/catalog/products/{productId}", productId)
                .with(sellerJwtFor(sellerId, "ACTIVE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "categoryId":"%s",
                      "name":"Rainbow Carrots",
                      "description":"Colorful carrots",
                      "unit":"bundle",
                      "packageSize":"6 count",
                      "priceAmount":9.25,
                      "currency":"BRL"
                    }
                    """
                        .formatted(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Rainbow Carrots"))
        .andExpect(jsonPath("$.currency").value("BRL"));

    mvc.perform(
            post("/catalog/products/{productId}/publish", productId)
                .with(sellerJwtFor(sellerId, "ACTIVE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));

    mvc.perform(
            post("/catalog/products/{productId}/unpublish", productId)
                .with(sellerJwtFor(sellerId, "ACTIVE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNPUBLISHED"));
  }

  @Test
  void shouldReturnNotFoundForMissingCategoryAndProduct() throws Exception {
    mvc.perform(get("/catalog/categories/{categoryId}", UUID.randomUUID()))
        .andExpect(status().isNotFound());

    mvc.perform(get("/catalog/products/{productId}", UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRequireSellerRoleForCatalogManagement() throws Exception {
    mvc.perform(
            post("/catalog/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Produce\"}"))
        .andExpect(status().isUnauthorized());

    mvc.perform(
            post("/catalog/categories")
                .with(customerJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Produce\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAuthorizeCatalogWritesByActiveSellerMembership() throws Exception {
    UUID sellerId = UUID.randomUUID();
    UUID otherSellerId = UUID.randomUUID();

    String categoryId = createCategory();
    String productId =
        createProduct(categoryId, sellerId, sellerJwtFor(sellerId, "ACTIVE"))
            .path("id")
            .stringValue();

    mvc.perform(
            post("/catalog/products")
                .with(sellerJwtFor(otherSellerId, "ACTIVE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson(sellerId, categoryId, "Cross Seller Carrots")))
        .andExpect(status().isForbidden());

    mvc.perform(
            patch("/catalog/products/{productId}", productId)
                .with(sellerJwtFor(sellerId, "REMOVED"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateProductJson(categoryId)))
        .andExpect(status().isForbidden());

    mvc.perform(post("/catalog/products/{productId}/publish", productId).with(adminJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PUBLISHED"));
  }

  private static RequestPostProcessor sellerJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER_OWNER"));
  }

  private static RequestPostProcessor customerJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  private static RequestPostProcessor adminJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private static RequestPostProcessor sellerJwtFor(UUID sellerId, String status) {
    return jwt()
        .jwt(
            token ->
                token.claim(
                    "seller_memberships",
                    List.of(
                        Map.of(
                            "sellerId", sellerId.toString(), "role", "OWNER", "status", status))))
        .authorities(new SimpleGrantedAuthority("ROLE_SELLER_OWNER"));
  }

  private String createCategory() throws Exception {
    MvcResult result =
        mvc.perform(
                post("/catalog/categories")
                    .with(sellerJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Produce\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .path("id")
        .stringValue();
  }

  private JsonNode createProduct(String categoryId, UUID sellerId, RequestPostProcessor jwt)
      throws Exception {
    MvcResult result =
        mvc.perform(
                post("/catalog/products")
                    .with(jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(productJson(sellerId, categoryId, "Organic Carrots")))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private static String productJson(UUID sellerId, String categoryId, String name) {
    return """
        {
          "sellerId":"%s",
          "categoryId":"%s",
          "name":"%s",
          "description":"Fresh carrots",
          "unit":"kg",
          "packageSize":"1 kg bag",
          "priceAmount":7.50,
          "currency":"USD"
        }
        """
        .formatted(sellerId, categoryId, name);
  }

  private static String updateProductJson(String categoryId) {
    return """
        {
          "categoryId":"%s",
          "name":"Rainbow Carrots",
          "description":"Colorful carrots",
          "unit":"bundle",
          "packageSize":"6 count",
          "priceAmount":9.25,
          "currency":"BRL"
        }
        """
        .formatted(categoryId);
  }
}
