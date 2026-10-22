package com.collabflow.platform.event;

import com.collabflow.common.kafka.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrganizationKafkaPublisher {

    public static final String TOPIC_CREATED = "organization.created";
    public static final String TOPIC_MEMBER_ADDED = "organization.member_added";

    private static final Logger log = LoggerFactory.getLogger(OrganizationKafkaPublisher.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrganizationKafkaPublisher(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(OrganizationCreatedDomainEvent event) {
        publish(TOPIC_CREATED, event.payload().organizationId().toString(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberAdded(MemberAddedDomainEvent event) {
        publish(TOPIC_MEMBER_ADDED, event.payload().organizationId().toString(), event.payload());
    }

    private void publish(String topic, String key, Object data) {
        try {
            String json = objectMapper.writeValueAsString(DomainEvent.of(topic, data));
            kafkaTemplate.send(topic, key, json);
            log.info("Published {} key={}", topic, key);
        } catch (Exception e) {
            log.error("Failed to publish {} key={}", topic, key, e);
        }
    }
}
