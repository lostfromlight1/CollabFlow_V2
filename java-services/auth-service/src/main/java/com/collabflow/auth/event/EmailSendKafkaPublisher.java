package com.collabflow.auth.event;

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

/** Publishes {@code email.send} after commit — collab stub consumes. */
@Component
public class EmailSendKafkaPublisher {

    public static final String TOPIC = "email.send";

    private static final Logger log = LoggerFactory.getLogger(EmailSendKafkaPublisher.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EmailSendKafkaPublisher(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailSend(EmailSendDomainEvent event) {
        DomainEvent<EmailSendPayload> envelope = DomainEvent.of(TOPIC, event.payload());
        try {
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(TOPIC, event.payload().to(), json);
            log.info("Published {} to={} template={}", TOPIC, event.payload().to(), event.payload().template());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {}", TOPIC, e);
        } catch (Exception e) {
            log.error("Failed to publish {}", TOPIC, e);
        }
    }
}
