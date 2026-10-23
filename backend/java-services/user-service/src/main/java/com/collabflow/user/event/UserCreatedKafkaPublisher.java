package com.collabflow.user.event;

import com.collabflow.common.kafka.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Publishes {@code user.created} after the admin-create transaction commits. */
@Component
public class UserCreatedKafkaPublisher {

    public static final String TOPIC = "user.created";

    private static final Logger log = LoggerFactory.getLogger(UserCreatedKafkaPublisher.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserCreatedKafkaPublisher(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedDomainEvent event) {
        DomainEvent<UserCreatedPayload> envelope = DomainEvent.of(TOPIC, event.payload());
        try {
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC, event.payload().userId().toString(), json);
            log.info("Published {} for userId={}", TOPIC, event.payload().userId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} for userId={}", TOPIC, event.payload().userId(), e);
        } catch (Exception e) {
            log.error("Failed to publish {} for userId={}", TOPIC, event.payload().userId(), e);
        }
    }
}
