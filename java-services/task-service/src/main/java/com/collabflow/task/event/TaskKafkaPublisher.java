package com.collabflow.task.event;

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
public class TaskKafkaPublisher {

    public static final String TOPIC_CREATED = "task.created";
    public static final String TOPIC_UPDATED = "task.updated";
    public static final String TOPIC_DELETED = "task.deleted";
    public static final String TOPIC_ASSIGNED = "task.assigned";
    public static final String TOPIC_STATUS_CHANGED = "task.status_changed";

    private static final Logger log = LoggerFactory.getLogger(TaskKafkaPublisher.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TaskKafkaPublisher(KafkaTemplate<Object, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(TaskCreatedDomainEvent event) {
        publish(TOPIC_CREATED, event.payload().taskId().toString(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdated(TaskUpdatedDomainEvent event) {
        publish(TOPIC_UPDATED, event.payload().taskId().toString(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeleted(TaskDeletedDomainEvent event) {
        publish(TOPIC_DELETED, event.payload().taskId().toString(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssigned(TaskAssignedDomainEvent event) {
        publish(TOPIC_ASSIGNED, event.payload().taskId().toString(), event.payload());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(TaskStatusChangedDomainEvent event) {
        publish(TOPIC_STATUS_CHANGED, event.payload().taskId().toString(), event.payload());
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
