package uk.nhs.cactus.common.audit;

import java.util.Optional;
import org.springframework.stereotype.Component;
import uk.nhs.cactus.common.audit.model.AuditEntry;
import uk.nhs.cactus.common.audit.model.AuditSession;

@Component
public class AuditThreadStore {

  private final ThreadLocal<AuditEntry> currentEntry = new ThreadLocal<>();
  private final ThreadLocal<AuditSession> currentSession = new ThreadLocal<>();

  public Optional<AuditEntry> getCurrentEntry() {
    return Optional.ofNullable(currentEntry.get());
  }

  public void removeCurrentEntry() {
    currentEntry.remove();
  }

  public void setCurrentEntry(AuditEntry entry) {
    currentEntry.set(entry);
  }

  public Optional<AuditSession> getCurrentAuditSession() {
    return Optional.ofNullable(currentSession.get());
  }

  public void removeCurrentSession() {
    currentSession.remove();
  }

  public void setCurrentSession(AuditSession session) {
    currentSession.set(session);
  }

}
