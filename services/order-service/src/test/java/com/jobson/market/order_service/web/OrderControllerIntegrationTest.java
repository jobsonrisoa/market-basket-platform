package com.jobson.market.order_service.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobson.market.order_service.TestcontainersConfiguration;
import com.jobson.market.order_service.application.InventoryReservationClient;
import com.jobson.market.order_service.event.OrderEvent;
import com.jobson.market.order_service.event.OrderEventPublisher;
import com.jobson.market.order_service.persistence.OrderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import({TestcontainersConfiguration.class, OrderControllerIntegrationTest.TestDoubles.class})
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OrderRepository orders;
  @Autowired private RecordingInventoryReservationClient inventory;
  @Autowired private RecordingOrderEventPublisher events;

  @BeforeEach
  void cleanDatabase() {
    orders.deleteAll();
    inventory.reset();
    events.clear();
  }

  @Test
  void shouldCreateConfirmCancelAndChangeFulfillment() throws Exception {
    UUID customerId = UUID.randomUUID();
    String orderId = createOrder(customerId).path("id").stringValue();

    mvc.perform(get("/orders/{orderId}", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DRAFT"));

    mvc.perform(get("/orders").param("customerId", customerId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(orderId));

    mvc.perform(post("/orders/{orderId}/confirm", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    mvc.perform(
            patch("/orders/{orderId}/fulfillment", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"FULFILLMENT_READY\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FULFILLMENT_READY"))
        .andExpect(jsonPath("$.fulfillmentStatus").value("FULFILLMENT_READY"));

    mvc.perform(post("/orders/{orderId}/cancel", orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    org.junit.jupiter.api.Assertions.assertEquals(1, inventory.reserveCalls);
    org.junit.jupiter.api.Assertions.assertEquals(1, inventory.releaseCalls);
    org.junit.jupiter.api.Assertions.assertEquals(
        List.of("order.confirmed.v1", "order.fulfillment_status_changed.v1"), events.eventTypes());
  }

  private JsonNode createOrder(UUID customerId) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "customerId":"%s",
                          "sellerId":"%s",
                          "productId":"%s",
                          "stockId":"%s",
                          "quantity":2.5,
                          "unit":"kg",
                          "source":"subscription-service",
                          "sourceReferenceId":"subscription-123"
                        }
                        """
                            .formatted(
                                customerId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  @TestConfiguration
  static class TestDoubles {

    @Bean
    @Primary
    RecordingInventoryReservationClient inventoryReservationClient() {
      return new RecordingInventoryReservationClient();
    }

    @Bean
    @Primary
    RecordingOrderEventPublisher orderEventPublisher() {
      return new RecordingOrderEventPublisher();
    }
  }

  static class RecordingInventoryReservationClient implements InventoryReservationClient {
    private int reserveCalls;
    private int releaseCalls;

    @Override
    public void reserve(UUID stockId, BigDecimal quantity, String referenceId) {
      reserveCalls++;
    }

    @Override
    public void release(UUID stockId, String referenceId) {
      releaseCalls++;
    }

    void reset() {
      reserveCalls = 0;
      releaseCalls = 0;
    }
  }

  static class RecordingOrderEventPublisher implements OrderEventPublisher {
    private final List<String> eventTypes = new ArrayList<>();

    @Override
    public void publish(OrderEvent event) {
      eventTypes.add(event.eventType());
    }

    List<String> eventTypes() {
      return eventTypes;
    }

    void clear() {
      eventTypes.clear();
    }
  }
}
