package uk.nhs.cactus.common.audit.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.nhs.cactus.common.audit.model.AuditSession;
import uk.nhs.cactus.common.elasticsearch.ElasticSearchClient;
import uk.nhs.cactus.common.security.TokenAuthenticationService;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class LocalAuditSender implements AuditSender {

    private final ObjectMapper mapper;
    private final ElasticSearchClient elasticSearchClient;
    private final TokenAuthenticationService authenticationService;

    @Value("${service.name}")
    private String serviceName;

    @Override
    public void sendAudit(AuditSession audit) {
        var supplierId = authenticationService.requireSupplierId();
        var metadata = Map.of(
            "requestId", UUID.randomUUID(),
            "@timestamp", Instant.now().toString(),
            "@owner", serviceName);

        try {
            elasticSearchClient.store(supplierId + "-audit", audit, metadata);
        } catch (IOException e) {
            logAudit(audit);
        }
    }

    @SneakyThrows
    private void logAudit(AuditSession audit) {
        log.info("Audit server configured but cannot connect: " + mapper.writeValueAsString(audit));
    }
}
