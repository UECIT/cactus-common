package uk.nhs.cactus.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultJws;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@RunWith(MockitoJUnitRunner.class)
public class TokenAuthenticationServiceTest {

  @Mock
  JWTHandler jwtHandler;

  @Mock
  Clock clock;

  @InjectMocks
  TokenAuthenticationService authService;

  private HttpServletRequest bearerTokenRequest() {
    return MockMvcRequestBuilders.get("/")
        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
        .buildRequest(new MockServletContext());
  }

  private Claims validClaims() {
    Claims claims = Jwts.claims();
    claims.setSubject("user");
    claims.put("supplierId", "supplier");
    return claims;
  }

  private Claims validRolesClaims() {
    Claims claims = Jwts.claims();
    claims.setSubject("user");
    claims.put("supplierId", "supplier");
    claims.put("roles", "ADMIN,SUPER_ADMIN");
    return claims;
  }

  private Claims partialClaims() {
    Claims claims = Jwts.claims();
    claims.setSubject("user");
    return claims;
  }

  private Claims emptyClaims() {
    Claims claims = Jwts.claims();
    claims.setSubject("user");
    claims.put("supplierId", "");
    return claims;
  }

  private CactusPrincipal validPrincipal() {
    return CactusPrincipal.builder().name("user").supplierId("supplier").build();
  }

  @Test
  public void extractsBearerToken() {
    when(jwtHandler.parse("token")).thenReturn(new DefaultJws<>(null, validClaims(), null));
    Authentication auth = authService.getAuthentication(bearerTokenRequest());

    assertThat(auth).isNotNull();
    assertThat(auth.getName()).isEqualTo("user");
    assertThat(auth.getPrincipal()).isEqualTo(validPrincipal());
  }

  @Test
  public void rejectsMissingSupplier() {
    when(jwtHandler.parse("token")).thenReturn(new DefaultJws<>(null, partialClaims(), null));
    Authentication auth = authService.getAuthentication(bearerTokenRequest());

    assertThat(auth).isNull();
  }

  @Test
  public void rejectsEmptySupplier() {
    when(jwtHandler.parse("token")).thenReturn(new DefaultJws<>(null, emptyClaims(), null));
    Authentication auth = authService.getAuthentication(bearerTokenRequest());

    assertThat(auth).isNull();
  }

  @Test
  public void issuesToken() {

    when(jwtHandler.generate(JWTRequest.builder()
        .username("user")
        .supplierId("supplier")
        .secondsUntilExpiry(TokenAuthenticationService.SECONDS_UNTIL_EXPIRY)
        .build()))
        .thenReturn("token");

    MockHttpServletResponse response = new MockHttpServletResponse();
    authService.setAuthentication(response, "user", "supplier", Collections.emptyList());

    assertThat(response.containsHeader(HttpHeaders.AUTHORIZATION)).isTrue();

    String header = response.getHeader(HttpHeaders.AUTHORIZATION);
    assertThat(header).isEqualTo("Bearer token");
  }

  @Test
  public void parsesRoles() {
    when(jwtHandler.parse("token")).thenReturn(new DefaultJws<>(null, validRolesClaims(), null));
    Authentication auth = authService.getAuthentication(bearerTokenRequest());

    assertThat(auth).isNotNull();
    assertThat(auth.getAuthorities()).hasSize(2);
    assertThat(auth.getAuthorities().containsAll(List.of(
        new SimpleGrantedAuthority("ADMIN"),
        new SimpleGrantedAuthority("SUPER_ADMIN")))).isTrue();
  }

}