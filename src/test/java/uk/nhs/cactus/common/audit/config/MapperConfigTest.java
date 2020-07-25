package uk.nhs.cactus.common.audit.config;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import uk.nhs.cactus.common.audit.model.AuditSession;

public class MapperConfigTest {

  @Test
  public void objectMapper_readValue_canDeserialiseAudit() throws IOException {
    var auditFile = getClass().getClassLoader().getResource("exampleAudit.json");
    var auditJson = IOUtils.toString(Objects.requireNonNull(auditFile), StandardCharsets.UTF_8);

    var mapper = new MapperConfig().registryObjectMapper();

    var audit = mapper.readValue(auditJson, AuditSession.class);

    assertThat(audit.getCreatedDate(), is(Instant.parse("2020-06-11T16:36:52.587218Z")));
    assertThat(audit.getRequestUrl(), is("/case/"));
    assertThat(audit.getResponseStatus(), is("200"));
    assertThat(audit.getAdditionalProperties().get("interactionId"), is("57"));
    assertThat(audit.getEntries(), hasSize(28));
  }

}