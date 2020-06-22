package uk.nhs.cactus.common.security;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedFhirClientFactory {

  private final TokenExchange tokenExchange;
  private final FhirContext fhirContext;

  public IGenericClient getClient(String baseUrl) {
    var client = fhirContext.newRestfulGenericClient(baseUrl);

    tokenExchange.getExchangedToken(baseUrl)
        .map(BearerTokenAuthInterceptor::new)
        .ifPresent(client::registerInterceptor);

    return client;
  }
}
