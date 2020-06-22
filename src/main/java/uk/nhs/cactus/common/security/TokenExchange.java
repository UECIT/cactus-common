package uk.nhs.cactus.common.security;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Used to obtain the appropriate token to use for calling a particular url.
 * This depends on two app properties:
 * - cactus.servers: comma-separated list of urls known to accept the 'cactus' token
 *   - when not set or set to '*': assumes that all valid urls accept the 'cactus' token
 * - cactus.auth.server: url to the authentication server called for the actual token exchange
 *   - required only if cactus.servers is configured as a list of urls
 */
@Component
@RequiredArgsConstructor
public class TokenExchange {

  @Value("#{'${cactus.servers:*}' == '*' ? null : '${cactus.servers:*}'.split(',')}")
  private List<String> cactusServices;

  @Value("${cactus.auth.server:#{null}}")
  private String authServer;

  private final TokenAuthenticationService tokenAuthenticationService;
  private final RestTemplate restTemplate;

  public Optional<String> getExchangedToken(String requestUrl) {
    var cactusToken = tokenAuthenticationService.requireToken();
    if (cactusServices == null ||
        authServer == null ||
        cactusServices.stream().anyMatch(requestUrl::startsWith)) {
      return Optional.of(cactusToken);
    }

    var exchangeUri = UriComponentsBuilder.fromHttpUrl(authServer)
        .pathSegment("exchange")
        .queryParam("baseUrl", requestUrl)
        .build(true)
        .toUri();

    var request = RequestEntity.get(exchangeUri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cactusToken)
        .build();

    try {
      String token = restTemplate.exchange(request, String.class).getBody();
      return Optional.ofNullable(trimToNull(token));
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    }
  }

}