package com.jobson.market.catalog_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jobson.market.catalog_service.TestcontainersConfiguration;
import com.jobson.market.catalog_service.domain.SellerApprovalStatus;
import com.jobson.market.catalog_service.persistence.SellerApprovalRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SellerApprovedRuntimeConsumerTest {

  private static final Path EXAMPLE =
      Path.of(
          "../seller-service/src/test/resources/contracts/seller/examples/seller-approved-v1.json");
  private static final Path REJECTED_EXAMPLE =
      Path.of(
          "../seller-service/src/test/resources/contracts/seller/examples/seller-rejected-v1.json");

  @Autowired private SellerApprovedConsumer consumer;
  @Autowired private SellerApprovalRepository sellerApprovals;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void cleanDatabase() {
    sellerApprovals.deleteAll();
  }

  @Test
  void shouldRecordSellerApprovalFromRecordedExampleIdempotently() throws Exception {
    String event = Files.readString(EXAMPLE);
    UUID sellerId =
        UUID.fromString(objectMapper.readTree(event).at("/payload/sellerId").stringValue());

    consumer.handle(event);
    consumer.handle(event);

    assertEquals(1, sellerApprovals.count());
    assertEquals(
        SellerApprovalStatus.APPROVED,
        sellerApprovals.findBySellerId(sellerId).orElseThrow().approvalStatus());
  }

  @Test
  void shouldRecordSellerRejectionFromRecordedExampleIdempotently() throws Exception {
    String event = Files.readString(REJECTED_EXAMPLE);
    UUID sellerId =
        UUID.fromString(objectMapper.readTree(event).at("/payload/sellerId").stringValue());

    consumer.handleRejected(event);
    consumer.handleRejected(event);

    assertEquals(1, sellerApprovals.count());
    assertEquals(
        SellerApprovalStatus.REJECTED,
        sellerApprovals.findBySellerId(sellerId).orElseThrow().approvalStatus());
  }
}
