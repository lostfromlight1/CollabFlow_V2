package com.collabflow.user.event;

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
