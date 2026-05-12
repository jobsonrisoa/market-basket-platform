package com.jobson.market.inventory_service.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobson.market.inventory_service.TestcontainersConfiguration;
import com.jobson.market.inventory_service.persistence.InventoryReservationRepository;
import com.jobson.market.inventory_service.persistence.InventoryStockRepository;
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
class InventoryControllerIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private InventoryReservationRepository reservations;
  @Autowired private InventoryStockRepository stocks;

  @BeforeEach
  void cleanDatabase() {
    reservations.deleteAll();
    stocks.deleteAll();
  }

  @Test
  void shouldManageStockAndReservations() throws Exception {
    UUID sellerId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();

    MvcResult stockResult =
        mvc.perform(
                post("/inventory/stocks")
                    .with(sellerJwtFor(sellerId, "ACTIVE"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "sellerId":"%s",
                          "productId":"%s",
                          "onHandQuantity":25.5,
                          "unit":"kg"
                        }
                        """
                            .formatted(sellerId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sellerId").value(sellerId.toString()))
            .andExpect(jsonPath("$.productId").value(productId.toString()))
            .andExpect(jsonPath("$.availableQuantity").value(25.5))
            .andReturn();

    JsonNode stock = objectMapper.readTree(stockResult.getResponse().getContentAsString());
    String stockId = stock.path("id").stringValue();

    mvc.perform(get("/inventory/stocks/{stockId}", stockId).with(sellerJwtFor(sellerId, "ACTIVE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(stockId));

    mvc.perform(
            get("/inventory/stocks")
                .param("sellerId", sellerId.toString())
                .with(sellerJwtFor(sellerId, "ACTIVE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(stockId));

    MvcResult reservationResult =
        mvc.perform(
                post("/inventory/reservations")
                    .with(serviceJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "stockId":"%s",
                          "quantity":4.5,
                          "requestedBy":"order-service",
                          "referenceId":"order-123"
                        }
                        """
                            .formatted(stockId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.quantity").value(4.5))
            .andReturn();

    JsonNode reservation =
        objectMapper.readTree(reservationResult.getResponse().getContentAsString());
    String reservationId = reservation.path("id").stringValue();

    mvc.perform(get("/inventory/stocks/{stockId}", stockId).with(sellerJwtFor(sellerId, "ACTIVE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservedQuantity").value(4.5))
        .andExpect(jsonPath("$.availableQuantity").value(21.0));

    mvc.perform(
            post("/inventory/reservations/{reservationId}/release", reservationId)
                .with(serviceJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RELEASED"));

    mvc.perform(get("/inventory/stocks/{stockId}", stockId).with(sellerJwtFor(sellerId, "ACTIVE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservedQuantity").value(0))
        .andExpect(jsonPath("$.availableQuantity").value(25.5));
  }

  @Test
  void shouldReturnNotFoundForMissingStock() throws Exception {
    mvc.perform(get("/inventory/stocks/{stockId}", UUID.randomUUID()).with(sellerJwt()))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldRequireAuthorizedRoleForInventoryApis() throws Exception {
    mvc.perform(get("/inventory/stocks/{stockId}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());

    mvc.perform(get("/inventory/stocks/{stockId}", UUID.randomUUID()).with(customerJwt()))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldAuthorizeInventoryWritesByActiveSellerMembership() throws Exception {
    UUID sellerId = UUID.randomUUID();
    UUID otherSellerId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();

    String stockId =
        upsertStock(sellerId, productId, sellerJwtFor(sellerId, "ACTIVE")).path("id").stringValue();

    mvc.perform(
            post("/inventory/stocks")
                .with(sellerJwtFor(otherSellerId, "ACTIVE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson(sellerId, UUID.randomUUID())))
        .andExpect(status().isForbidden());

    mvc.perform(get("/inventory/stocks/{stockId}", stockId).with(sellerJwtFor(sellerId, "REMOVED")))
        .andExpect(status().isForbidden());

    mvc.perform(get("/inventory/stocks/{stockId}", stockId).with(adminJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(stockId));
  }

  private static RequestPostProcessor sellerJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_SELLER_OWNER"));
  }

  private static RequestPostProcessor serviceJwt() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE"));
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

  private JsonNode upsertStock(UUID sellerId, UUID productId, RequestPostProcessor jwt)
      throws Exception {
    MvcResult result =
        mvc.perform(
                post("/inventory/stocks")
                    .with(jwt)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(stockJson(sellerId, productId)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private static String stockJson(UUID sellerId, UUID productId) {
    return """
        {
          "sellerId":"%s",
          "productId":"%s",
          "onHandQuantity":25.5,
          "unit":"kg"
        }
        """
        .formatted(sellerId, productId);
  }
}
