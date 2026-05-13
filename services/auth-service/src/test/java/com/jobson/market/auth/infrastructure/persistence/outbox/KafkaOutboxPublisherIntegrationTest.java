package com.jobson.market.auth.infrastructure.persistence.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobson.market.auth.domain.event.OutboxEvent;
import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
class KafkaOutboxPublisherIntegrationTest {

  @Container
  private static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldPublishPendingOutboxEventToKafkaAndMarkItPublished() throws Exception {
    User user = User.register(new Email("john@example.com"));
    OutboxEvent event = OutboxEvent.userRegistered(user);
    OutboxEventEntity entity =
        OutboxEventEntity.pending(event, objectMapper.writeValueAsString(event.payload()));
    SpringDataOutboxEventRepository events = mock(SpringDataOutboxEventRepository.class);
    when(events.findTop50ByStatusOrderByOccurredAtAsc("PENDING")).thenReturn(List.of(entity));

    KafkaTemplate<String, String> kafka = kafkaTemplate();
    try (KafkaConsumer<String, String> consumer = consumer()) {
      KafkaOutboxPublisher publisher =
          new KafkaOutboxPublisher(
              events,
              kafka,
              Clock.fixed(Instant.parse("2026-05-09T12:00:00Z"), ZoneOffset.UTC),
              objectMapper);

      publisher.publishPendingEvents();

      ConsumerRecord<String, String> consumedRecord = pollOne(consumer, event.eventType());
      JsonNode envelope = objectMapper.readTree(consumedRecord.value());

      assertEquals(event.eventType(), consumedRecord.topic());
      assertEquals(event.eventId().toString(), consumedRecord.key());
      assertEquals(event.eventId().toString(), envelope.path("eventId").stringValue());
      assertEquals(event.eventType(), envelope.path("eventType").stringValue());
      assertEquals(user.id().toString(), envelope.at("/payload/userId").stringValue());
      assertEquals(user.email().value(), envelope.at("/payload/email").stringValue());
      assertEquals("PUBLISHED", entity.status());
      verify(events).save(entity);
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
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-outbox-test-" + UUID.randomUUID());
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
