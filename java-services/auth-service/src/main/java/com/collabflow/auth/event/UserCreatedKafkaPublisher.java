package com.collabflow.auth.event;

import com.collabflow.common.kafka.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Produces {@code user.created} only after the DB transaction commits.
 *
 * <p>Java analogy: like {@code @TransactionalEventListener} + a message queue —
 * Kafka is the bus; collab-service (Go) will consume this later for welcome notifications.
 */
@Component
public class UserCreatedKafkaPublisher {

    public static final String TOPIC = "user.created";

    private static final Logger log = LoggerFactory.getLogger(UserCreatedKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserCreatedKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedDomainEvent event) {
        DomainEvent<UserCreatedPayload> envelope = DomainEvent.of(TOPIC, event.payload());
        try {
            String json = objectMapper.writeValueAsString(envelope);
            // Key = userId → same user always hits same partition (ordering per user)
            kafkaTemplate.send(TOPIC, event.payload().userId().toString(), json);
            log.info("Published {} for userId={}", TOPIC, event.payload().userId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} for userId={}", TOPIC, event.payload().userId(), e);
        } catch (Exception e) {
            // Do not fail the HTTP response — user is already saved. Outbox pattern later if needed.
            log.error("Failed to publish {} for userId={}", TOPIC, event.payload().userId(), e);
        }
    }
}
