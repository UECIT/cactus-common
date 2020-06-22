package uk.nhs.cactus.common.security;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@RunWith(MockitoJUnitRunner.class)
public class TokenExchangeTest {

  private static final String CACTUS_SERVICES = "cactusServices";
  private static final String AUTH_SERVER = "authServer";

  private static final List<String> VALID_CACTUS_SERVICES = List.of("cactus-ems", "cactus-cdss");
  private static final String VALID_AUTH_SERVER = "http://cactus.auth/server";
  private static final String VALID_TOKEN = "<validToken>";
  private static final ResponseEntity<Object> VALID_RESPONSE = ResponseEntity.of(Optional.empty());

  @Mock
  private TokenAuthenticationService tokenAuthenticationService;

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private TokenExchange tokenExchange;

  @Test(expected = NullPointerException.class)
  public void getExchangedToken_withNullRequestUrl_shouldFail() {
    setField(tokenExchange, CACTUS_SERVICES, VALID_CACTUS_SERVICES);
    setField(tokenExchange, AUTH_SERVER, VALID_AUTH_SERVER);
    when(tokenAuthenticationService.requireToken()).thenReturn(VALID_TOKEN);

    tokenExchange.getExchangedToken(null);
  }

  @Test
  public void getExchangedToken_withNullAuthServer_shouldReturnCactusToken() {
    setField(tokenExchange, CACTUS_SERVICES, VALID_CACTUS_SERVICES);
    setField(tokenExchange, AUTH_SERVER, null);
    when(tokenAuthenticationService.requireToken()).thenReturn(VALID_TOKEN);

    var token = tokenExchange.getExchangedToken("cactus-ems");

    assertThat(token.get(), is(VALID_TOKEN));
  }

  @Test
  public void getExchangedToken_withNullCactusServices_shouldReturnCactusToken() {
    setField(tokenExchange, CACTUS_SERVICES, null);
    setField(tokenExchange, AUTH_SERVER, VALID_AUTH_SERVER);
    when(tokenAuthenticationService.requireToken()).thenReturn(VALID_TOKEN);

    var token = tokenExchange.getExchangedToken("cactus-ems");

    assertThat(token.get(), is(VALID_TOKEN));
  }

  @Test
  public void getExchangedToken_withCactusService_shouldReturnCactusToken() {
    setField(tokenExchange, CACTUS_SERVICES, VALID_CACTUS_SERVICES);
    setField(tokenExchange, AUTH_SERVER, VALID_AUTH_SERVER);
    when(tokenAuthenticationService.requireToken()).thenReturn(VALID_TOKEN);

    var token = tokenExchange.getExchangedToken("cactus-ems");

    assertThat(token.get(), is(VALID_TOKEN));
  }

  @Test
  public void getExchangedToken_withNonCactusServiceAndFailingAuthServer_shouldReturnEmpty() {
    setField(tokenExchange, CACTUS_SERVICES, VALID_CACTUS_SERVICES);
    setField(tokenExchange, AUTH_SERVER, VALID_AUTH_SERVER);
    when(tokenAuthenticationService.requireToken()).thenReturn(VALID_TOKEN);
    when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
        .thenThrow(HttpClientErrorException.NotFound.class);

    var token = tokenExchange.getExchangedToken("non-cactus-ems");

    assertThat(token.isEmpty(), is(true));
  }

  @Test
  public void getExchangedToken_withNonCactusService_shouldReturnNonCactusToken() {
    setField(tokenExchange, CACTUS_SERVICES, VALID_CACTUS_SERVICES);
    setField(tokenExchange, AUTH_SERVER, VALID_AUTH_SERVER);
    when(tokenAuthenticationService.requireToken()).thenReturn(VALID_TOKEN);

    var expectedRequest = RequestEntity
        .get(URI.create("http://cactus.auth/server/exchange?baseUrl=non-cactus-ems"))
        .header("Authorization", "Bearer <validToken>")
        .build();
    var validResponse = ResponseEntity.ok("<exchangedToken>");

    when(restTemplate.exchange(argThat(samePropertyValuesAs(expectedRequest)), eq(String.class)))
        .thenReturn(validResponse);

    var exchangedToken = tokenExchange.getExchangedToken("non-cactus-ems");

    assertThat(exchangedToken.get(), is("<exchangedToken>"));
  }
}