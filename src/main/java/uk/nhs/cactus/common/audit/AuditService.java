package uk.nhs.cactus.common.audit;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static uk.nhs.cactus.common.audit.model.AuditProperties.SUPPLIER_ID;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.cactus.common.audit.model.AuditEntry;
import uk.nhs.cactus.common.audit.model.AuditSession;
import uk.nhs.cactus.common.audit.model.HttpRequest;
import uk.nhs.cactus.common.audit.model.HttpResponse;
import uk.nhs.cactus.common.security.TokenAuthenticationService;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

  private static final String FORWARDED_HEADER = "X-Forwarded-For";
  private static final String UNKNOWN = "<unknown>";

  private final AuditThreadStore auditThreadStore;
  private final HttpExchangeHelper exchangeHelper;
  private final TokenAuthenticationService authenticationService;

  /**
   * Start an audit entry to record an outgoing FHIR request
   * @param request the request that initiated the audit entry
   */
  public void startEntry(HttpRequest request) {
    auditThreadStore.getCurrentEntry()
        .ifPresent(entry -> {
          log.warn("Unclosed audit entry");
          auditThreadStore.removeCurrentEntry();
        });

    AuditEntry entry = AuditEntry.builder()
        .dateOfEntry(Instant.now())
        .requestBody(exchangeHelper.getBodyString(request, request.getUri()))
        .requestHeaders(exchangeHelper.getHeadersString(request))
        .requestUrl(request.getUri())
        .requestMethod(request.getMethod())
        .build();

    var session = auditThreadStore.getCurrentAuditSession()
        .orElseThrow(IllegalStateException::new)
        .toBuilder()
        .entry(entry)
        .build();
    auditThreadStore.setCurrentSession(session);
    auditThreadStore.setCurrentEntry(entry);
  }

  /**
   * End the audit entry with a response from the external FHIR server
   * @param response response from the server
   */
  public void endEntry(HttpResponse response) {
    AuditEntry entry = auditThreadStore.getCurrentEntry()
        .orElseThrow(IllegalStateException::new);
    entry.setResponseStatus(String.valueOf(response.getStatus()));
    entry.setResponseBody(exchangeHelper.getBodyString(response, entry.getRequestUrl()));
    entry.setResponseHeaders(exchangeHelper.getHeadersString(response));

    auditThreadStore.removeCurrentEntry();
  }

  /**
   * Start an audit session in the current thread local
   * @param request request that initiated the audit session
   */
  public void startAuditSession(HttpRequest request) {
    auditThreadStore.getCurrentAuditSession()
        .ifPresent(session -> {
          log.warn("Unclosed audit session");
          auditThreadStore.removeCurrentSession();
        });

    var supplierId = authenticationService.getCurrentSupplierId().orElse(UNKNOWN);
    var requestOrigin = exchangeHelper.getHeader(request, FORWARDED_HEADER)
        .or(() -> Optional.ofNullable(trimToNull(request.getRemoteHost())))
        .orElse(UNKNOWN);

    AuditSession audit = AuditSession.builder()
        .entries(new ArrayList<>())
        .additionalProperties(new HashMap<>())
        .createdDate(Instant.now())
        .requestUrl(request.getUri())
        .requestMethod(request.getMethod())
        .requestHeaders(exchangeHelper.getHeadersString(request))
        .requestOrigin(requestOrigin)
        .additionalProperty(SUPPLIER_ID, supplierId)
        .build();

    auditThreadStore.setCurrentSession(audit);
  }

  /**
   * Complete audit session - the interaction with this service is completed
   * @param request the request that initiated the session
   * @param response the response given by this server
   * @return the completed audit session with all FHIR audits to other services
   */
  public AuditSession completeAuditSession(HttpRequest request, HttpResponse response) {
    AuditSession session = auditThreadStore.getCurrentAuditSession()
        .orElseThrow(IllegalStateException::new);

    try {
      auditThreadStore.getCurrentEntry()
          .ifPresent(entry -> {
            log.warn("Unclosed audit entry");
            auditThreadStore.removeCurrentEntry();
          });

      session.setRequestBody(exchangeHelper.getBodyString(request, request.getUri()));
      session.setResponseStatus(String.valueOf(response.getStatus()));
      session.setResponseHeaders(exchangeHelper.getHeadersString(response));
      session.setResponseBody(exchangeHelper.getBodyString(response, session.getRequestUrl()));
    } finally {
      auditThreadStore.removeCurrentSession();
    }
    return session;
  }

  public void addAuditProperty(String key, String value) {
    Objects.requireNonNull(key);

    var auditSession = auditThreadStore.getCurrentAuditSession()
        .orElseThrow(IllegalStateException::new);

    var newProperties = new HashMap<>(auditSession.getAdditionalProperties());
    newProperties.put(key, value);
    auditSession.setAdditionalProperties(newProperties);
  }

}
