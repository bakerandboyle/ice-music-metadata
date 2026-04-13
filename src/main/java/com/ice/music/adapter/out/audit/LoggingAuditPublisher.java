package com.ice.music.adapter.out.audit;

import com.ice.music.domain.model.AuditEvent;
import com.ice.music.port.out.AuditPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * POC audit publisher — emits structured log entries.
 *
 * Proves the emission contract. In production, this adapter is replaced
 * by an SnsAuditPublisher that pushes to the SNS → SQS → Audit Service
 * pipeline. The domain and use case layers are unchanged.
 *
 * Structured JSON logging (ECS format) means these entries are already
 * machine-parseable by ELK/Splunk/CloudWatch.
 */
@Component
public class LoggingAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    private final JsonMapper jsonMapper;

    public LoggingAuditPublisher(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void publish(AuditEvent event) {
        try {
            var payload = jsonMapper.writeValueAsString(event);
            log.info("[AUDIT] {} {}", event.eventType(), payload);
        } catch (Exception e) {
            log.error("[AUDIT] Failed to serialize audit event: {} for entity {}",
                    event.eventType(), event.entityId(), e);
        }
    }
}
