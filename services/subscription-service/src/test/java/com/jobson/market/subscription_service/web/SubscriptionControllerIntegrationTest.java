package com.jobson.market.subscription_service.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobson.market.subscription_service.TestcontainersConfiguration;
import com.jobson.market.subscription_service.event.SubscriptionEvent;
import com.jobson.market.subscription_service.event.SubscriptionEventPublisher;
import com.jobson.market.subscription_service.persistence.SubscriptionPlanRepository;
import com.jobson.market.subscription_service.persistence.SubscriptionRepository;
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

@Import({
  TestcontainersConfiguration.class,
  SubscriptionControllerIntegrationTest.TestDoubles.class
})
@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionControllerIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SubscriptionRepository subscriptions;
  @Autowired private SubscriptionPlanRepository plans;
  @Autowired private RecordingSubscriptionEventPublisher events;

  @BeforeEach
  void cleanDatabase() {
    subscriptions.deleteAll();
    plans.deleteAll();
    events.clear();
  }

  @Test
  void shouldCreateListRenewAndManageSubscription() throws Exception {
    UUID customerId = UUID.randomUUID();
    String subscriptionId = createSubscription(customerId).path("id").stringValue();

    mvc.perform(get("/subscriptions").param("customerId", customerId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(subscriptionId));

    mvc.perform(
            post("/subscriptions/{subscriptionId}/pause", subscriptionId)
                .param("customerId", customerId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAUSED"));

    mvc.perform(
            post("/subscriptions/{subscriptionId}/resume", subscriptionId)
                .param("customerId", customerId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mvc.perform(
            post("/subscriptions/{subscriptionId}/skip", subscriptionId)
                .param("customerId", customerId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nextRenewalDate").value("2026-05-27"));

    mvc.perform(post("/subscriptions/renewals/due").param("renewalDate", "2026-05-27"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].nextRenewalDate").value("2026-06-03"));

    mvc.perform(
            post("/subscriptions/{subscriptionId}/cancel", subscriptionId)
                .param("customerId", customerId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    org.junit.jupiter.api.Assertions.assertEquals(
        List.of("subscription.renewal_due.v1", "subscription.renewal_due.v1"), events.eventTypes());
  }

  private JsonNode createSubscription(UUID customerId) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/subscriptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "customerId":"%s",
                          "sellerId":"%s",
                          "productId":"%s",
                          "stockId":"%s",
                          "basketSize":"SMALL",
                          "cadence":"WEEKLY",
                          "quantity":2.5,
                          "unit":"kg",
                          "nextRenewalDate":"2026-05-20"
                        }
                        """
                            .formatted(
                                customerId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.currentDraftOrderId").exists())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  @TestConfiguration
  static class TestDoubles {

    @Bean
    @Primary
    RecordingSubscriptionEventPublisher subscriptionEventPublisher() {
      return new RecordingSubscriptionEventPublisher();
    }
  }

  static class RecordingSubscriptionEventPublisher implements SubscriptionEventPublisher {
    private final List<String> eventTypes = new ArrayList<>();

    @Override
    public void publish(SubscriptionEvent event) {
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
