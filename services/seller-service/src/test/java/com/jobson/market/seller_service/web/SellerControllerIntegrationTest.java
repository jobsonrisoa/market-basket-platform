package com.jobson.market.seller_service.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobson.market.seller_service.TestcontainersConfiguration;
import com.jobson.market.seller_service.persistence.SellerMembershipRepository;
import com.jobson.market.seller_service.persistence.SellerStoreRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SellerControllerIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SellerStoreRepository stores;
  @Autowired private SellerMembershipRepository memberships;

  @BeforeEach
  void cleanDatabase() {
    memberships.deleteAll();
    stores.deleteAll();
  }

  @Test
  void shouldCreateSellerAndManageMembers() throws Exception {
    UUID ownerUserId = UUID.randomUUID();
    UUID staffUserId = UUID.randomUUID();

    MvcResult createResult =
        mvc.perform(
                post("/sellers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Fresh Market","ownerUserId":"%s"}
                        """
                            .formatted(ownerUserId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Fresh Market"))
            .andExpect(jsonPath("$.ownerUserId").value(ownerUserId.toString()))
            .andExpect(jsonPath("$.approvalStatus").value("PENDING_REVIEW"))
            .andReturn();

    JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String sellerId = created.path("id").stringValue();

    mvc.perform(get("/sellers/{sellerId}", sellerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(sellerId))
        .andExpect(jsonPath("$.approvalStatus").value("PENDING_REVIEW"));

    mvc.perform(get("/sellers/{sellerId}/members", sellerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].userId").value(ownerUserId.toString()))
        .andExpect(jsonPath("$[0].role").value("OWNER"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));

    mvc.perform(
            post("/sellers/{sellerId}/members", sellerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"userId":"%s","role":"STAFF"}
                    """
                        .formatted(staffUserId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(staffUserId.toString()))
        .andExpect(jsonPath("$.role").value("STAFF"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mvc.perform(delete("/sellers/{sellerId}/members/{userId}", sellerId, staffUserId))
        .andExpect(status().isNoContent());

    mvc.perform(get("/sellers/{sellerId}/members", sellerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[1].userId").value(staffUserId.toString()))
        .andExpect(jsonPath("$[1].status").value("REMOVED"));
  }

  @Test
  void shouldReviewSeller() throws Exception {
    UUID ownerUserId = UUID.randomUUID();
    UUID reviewerUserId = UUID.randomUUID();
    MvcResult createResult =
        mvc.perform(
                post("/sellers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Fresh Market","ownerUserId":"%s"}
                        """
                            .formatted(ownerUserId)))
            .andExpect(status().isCreated())
            .andReturn();
    String sellerId =
        objectMapper
            .readTree(createResult.getResponse().getContentAsString())
            .path("id")
            .stringValue();

    mvc.perform(
            post("/sellers/{sellerId}/approve", sellerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"reviewerUserId":"%s","reviewNotes":"Ready for marketplace"}
                    """
                        .formatted(reviewerUserId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
        .andExpect(jsonPath("$.reviewedByUserId").value(reviewerUserId.toString()))
        .andExpect(jsonPath("$.reviewNotes").value("Ready for marketplace"));

    mvc.perform(
            post("/sellers/{sellerId}/reject", sellerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"reviewerUserId":"%s","reviewNotes":"Missing license"}
                    """
                        .formatted(reviewerUserId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approvalStatus").value("REJECTED"))
        .andExpect(jsonPath("$.reviewedByUserId").value(reviewerUserId.toString()))
        .andExpect(jsonPath("$.reviewNotes").value("Missing license"));
  }

  @Test
  void shouldReturnNotFoundForUnknownSeller() throws Exception {
    mvc.perform(get("/sellers/{sellerId}", UUID.randomUUID())).andExpect(status().isNotFound());
  }
}
