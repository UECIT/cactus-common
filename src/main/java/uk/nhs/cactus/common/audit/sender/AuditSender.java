package uk.nhs.cactus.common.audit.sender;


import uk.nhs.cactus.common.audit.model.AuditSession;

public interface AuditSender {
    void sendAudit(AuditSession session);
}
