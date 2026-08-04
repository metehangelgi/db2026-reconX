package com.dbtraining.reconx.audit;

import org.springframework.context.ApplicationEventPublisher;

public class AuditEventPublisher {

    private final ApplicationEventPublisher publisher;
    private final AuditProperties properties;

    public AuditEventPublisher(ApplicationEventPublisher publisher, AuditProperties properties) {
        this.publisher = publisher;
        this.properties = properties;
    }

    public void publish(String eventType, String detail) {
        publisher.publishEvent(new AuditPublishedEvent(this, properties.getTopic(), eventType, detail));
    }

    public static class AuditPublishedEvent extends org.springframework.context.ApplicationEvent {
        private final String topic;
        private final String eventType;
        private final String detail;

        public AuditPublishedEvent(Object source, String topic, String eventType, String detail) {
            super(source);
            this.topic = topic;
            this.eventType = eventType;
            this.detail = detail;
        }

        public String getTopic() { return topic; }
        public String getEventType() { return eventType; }
        public String getDetail() { return detail; }
    }
}
