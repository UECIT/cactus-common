package uk.nhs.cactus.common.security;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticatedFhirClientFactoryTest {

  @Mock
  private TokenExchange tokenExchange;

  @Mock
  private FhirContext fhirContext;

  @InjectMocks
  private AuthenticatedFhirClientFactory clientFactory;

  @Test
  public void getClient() {
    var url = "validUrl";
    var mockClient = mock(IGenericClient.class);
    when(fhirContext.newRestfulGenericClient(url)).thenReturn(mockClient);
    when(tokenExchange.getExchangedToken(url)).thenReturn(Optional.of("correctToken"));

    var client = clientFactory.getClient(url);

    assertThat(client, is(mockClient));
    verify(mockClient)
        .registerInterceptor(argThat(hasProperty("token", is("correctToken"))));
  }
}