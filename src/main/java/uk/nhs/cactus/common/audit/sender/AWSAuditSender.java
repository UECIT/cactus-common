package uk.nhs.cactus.common.audit.sender;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.nhs.cactus.common.audit.model.AuditSession;
import uk.nhs.cactus.common.security.TokenAuthenticationService;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!dev")
public class AWSAuditSender implements AuditSender {

    private static final String SENDER = "sender";
    private static final String SUPPLIER = "supplierId";

    @Value("${sqs.audit.queue}")
    private String loggingQueue;

    @Value("${service.name}")
    private String serviceName;

    private final ObjectMapper mapper;
    private final AmazonSQS sqsClient;
    private final TokenAuthenticationService authenticationService;

    @Override
    public void sendAudit(AuditSession session) {
        Preconditions.checkArgument(isNotEmpty(loggingQueue), "SQS Queue url must be provided");

        authenticationService.getCurrentSupplierId()
            .ifPresentOrElse(
                supplierId -> sendRequest(session, supplierId),
                () -> log.info("No supplier id found, not sending audit: {}", session)
            );
    }

    private void sendRequest(AuditSession session, String supplierId) {
        try {
            SendMessageRequest request = new SendMessageRequest()
                .withMessageGroupId(supplierId)
                .withMessageDeduplicationId(UUID.randomUUID().toString())
                .addMessageAttributesEntry(SENDER, stringAttribute(serviceName))
                .addMessageAttributesEntry(SUPPLIER, stringAttribute(supplierId))
                .withQueueUrl(loggingQueue)
                .withMessageBody(mapper.writeValueAsString(session));
            sqsClient.sendMessage(request);
        } catch (AmazonSQSException e) {
            if (e.getStatusCode() == 413) {
                log.warn("Audit request exceeded max size SQS can handle", e);
                //TODO: CDSCT-338 - Should we be auditing/paging audit search audits or excluding things
            }
            log.error("an error occurred sending audit session {} to SQS: {}", format(session), e.getErrorMessage());
        } catch (Exception e) {
            log.error("an error occurred sending audit session {} to SQS", format(session), e);
        }
    }

    private String format(AuditSession session) {
        final var MAX_LENGTH = 1 << 10;
        String sessionString = session.toString();
        if (sessionString.length() < MAX_LENGTH) {
            return sessionString;
        }

        return String.format("%s... (truncated from %d characters)",
            sessionString.substring(0, MAX_LENGTH), sessionString.length());
    }

    private MessageAttributeValue stringAttribute(String value) {
        return new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(value);
    }
}
