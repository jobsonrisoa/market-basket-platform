package com.jobson.market.seller_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.jobson.market.seller_service.domain.SellerStoreEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
class KafkaSellerEventPublisherIntegrationTest {

  @Container
  private static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldPublishSellerApprovedEventToKafka() throws Exception {
    SellerStoreEntity seller =
        SellerStoreEntity.create(
            "Fresh Market", UUID.randomUUID(), Instant.parse("2026-05-10T12:00:00Z"));
    seller.approve(UUID.randomUUID(), "Ready", Instant.parse("2026-05-10T13:00:00Z"));
    SellerEvent event = SellerEvent.sellerApproved(seller);
    KafkaTemplate<String, String> kafka = kafkaTemplate();
    try (KafkaConsumer<String, String> consumer = consumer()) {
      KafkaSellerEventPublisher publisher = new KafkaSellerEventPublisher(kafka, objectMapper);

      publisher.publish(event);

      ConsumerRecord<String, String> record = pollOne(consumer, event.eventType());
      JsonNode envelope = objectMapper.readTree(record.value());
      assertEquals(event.eventType(), record.topic());
      assertEquals(event.eventId().toString(), record.key());
      assertEquals(event.eventId().toString(), envelope.path("eventId").stringValue());
      assertEquals("seller.approved.v1", envelope.path("eventType").stringValue());
      assertEquals(seller.id().toString(), envelope.at("/payload/sellerId").stringValue());
      assertEquals("APPROVED", envelope.at("/payload/approvalStatus").stringValue());
    } finally {
      kafka.destroy();
    }
  }

  @Test
  void shouldPublishSellerRejectedEventToKafka() throws Exception {
    SellerStoreEntity seller =
        SellerStoreEntity.create(
            "Fresh Market", UUID.randomUUID(), Instant.parse("2026-05-10T12:00:00Z"));
    seller.reject(UUID.randomUUID(), "Needs documents", Instant.parse("2026-05-10T14:00:00Z"));
    SellerEvent event = SellerEvent.sellerRejected(seller);
    KafkaTemplate<String, String> kafka = kafkaTemplate();
    try (KafkaConsumer<String, String> consumer = consumer()) {
      KafkaSellerEventPublisher publisher = new KafkaSellerEventPublisher(kafka, objectMapper);

      publisher.publish(event);

      ConsumerRecord<String, String> record = pollOne(consumer, event.eventType());
      JsonNode envelope = objectMapper.readTree(record.value());
      assertEquals(event.eventType(), record.topic());
      assertEquals(event.eventId().toString(), record.key());
      assertEquals(event.eventId().toString(), envelope.path("eventId").stringValue());
      assertEquals("seller.rejected.v1", envelope.path("eventType").stringValue());
      assertEquals(seller.id().toString(), envelope.at("/payload/sellerId").stringValue());
      assertEquals("REJECTED", envelope.at("/payload/approvalStatus").stringValue());
    } finally {
      kafka.destroy();
    }
  }

  private KafkaTemplate<String, String> kafkaTemplate() {
    Map<String, Object> producerProperties =
        Map.of(
            "bootstrap.servers",
            KAFKA.getBootstrapServers(),
            "key.serializer",
            StringSerializer.class,
            "value.serializer",
            StringSerializer.class);
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProperties));
  }

  private KafkaConsumer<String, String> consumer() {
    Properties properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
    properties.put(
        ConsumerConfig.GROUP_ID_CONFIG, "seller-event-publisher-test-" + UUID.randomUUID());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new KafkaConsumer<>(properties);
  }

  private ConsumerRecord<String, String> pollOne(
      KafkaConsumer<String, String> consumer, String topic) {
    consumer.subscribe(List.of(topic));
    var records = consumer.poll(Duration.ofSeconds(10));
    assertFalse(records.isEmpty(), "Expected one Kafka record");
    return records.iterator().next();
  }
}
