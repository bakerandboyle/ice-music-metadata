package com.ice.music.port.out;

import com.ice.music.domain.model.AuditEvent;

/**
 * Outbound port for publishing audit events.
 *
 * The domain emits state-change events through this port.
 * POC: LoggingAuditPublisher writes structured log entries.
 * Production: SnsAuditPublisher pushes to SNS → SQS → Audit Service.
 */
public interface AuditPublisher {

    void publish(AuditEvent event);
}
