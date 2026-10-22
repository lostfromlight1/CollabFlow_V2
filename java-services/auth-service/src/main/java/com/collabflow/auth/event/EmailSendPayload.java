package com.collabflow.auth.event;

/**
 * Kafka {@code email.send} data payload. Collab stub (then email-service) consumes this.
 */
public record EmailSendPayload(
        String to,
        String subject,
        String body,
        String template,
        String correlationId
) {
    public static EmailSendPayload of(String to, String subject, String body, String template) {
        return new EmailSendPayload(to, subject, body, template, null);
    }
}
