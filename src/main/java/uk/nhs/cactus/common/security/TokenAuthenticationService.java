package uk.nhs.cactus.common.security;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.MalformedJwtException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenAuthenticationService {

  private final JWTHandler jwtHandler;

  static final long SECONDS_UNTIL_EXPIRY = 864_000; // 10 days
  private static final String TOKEN_PREFIX = "Bearer ";
  private static final String HEADER_STRING = "Authorization";
  private static final String COMMA_SEPARATOR = ",";

  private static final String ROLES_CLAIM = "roles";
  private static final String SUPPLIER_ID_CLAIM = "supplierId";


  /**
   * Extracts the authentication from the provided request and adds it to the current Spring
   * security context
   *
   * @param request to extract authentication from
   * @see #getAuthentication(HttpServletRequest)
   */
  public void authenticateRequestContext(HttpServletRequest request) {
    Authentication authentication = getAuthentication(request);
    if (authentication != null) {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
  }

  /**
   * Extracts the currently authenticated supplier ID from the {@link SecurityContextHolder}
   *
   * @return the supplier ID from the current security context, or empty if not available
   */
  public Optional<String> getCurrentSupplierId() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getPrincipal)
        .filter(CactusPrincipal.class::isInstance)
        .map(CactusPrincipal.class::cast)
        .map(CactusPrincipal::getSupplierId);
  }

  /**
   * @param supplierId identifies the expected supplier
   * @throws {@link ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException} if the provided
   *                supplierId does not match the current request's authentication token
   * @throws {@link ca.uhn.fhir.rest.server.exceptions.AuthenticationException} if no valid
   *                authentication token is present
   */
  public void requireSupplierId(String supplierId) {
    if (requireSupplierId().equals(supplierId)) {
      throw new ForbiddenOperationException("Forbidden");
    }
  }

  /**
   * Requires that a supplier is currently authenticated
   *
   * @return the current supplierId
   * @throws {@link AuthenticationException} if not able to authenticate
   */
  public String requireSupplierId() {
    return getCurrentSupplierId().orElseThrow(AuthenticationException::new);
  }

  /**
   * Adds authentication to a response by setting the Authorization header
   *
   * @param response           add authentication token to response
   * @param username           user to be identified by token
   * @param supplierId         supplier to be identified by token
   * @param grantedAuthorities roles associated with the user
   */
  public void setAuthentication(
      HttpServletResponse response, String username, String supplierId,
      Collection<? extends GrantedAuthority> grantedAuthorities) {
    var roles = grantedAuthorities.stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toUnmodifiableList());

    String jwt = jwtHandler.generate(JWTRequest.builder()
        .username(username)
        .supplierId(supplierId)
        .roles(roles)
        .secondsUntilExpiry(SECONDS_UNTIL_EXPIRY)
        .build());
    response.addHeader(HEADER_STRING, TOKEN_PREFIX + jwt);
    response.addHeader(ROLES_CLAIM, String.join(COMMA_SEPARATOR, roles));
  }

  /**
   * Extracts authentication details from the current request headers
   *
   * @param request the request to authenticate
   * @return a Spring {@link Authentication} record containing a {@link CactusPrincipal} and {@link
   * CactusToken}
   */
  public Authentication getAuthentication(HttpServletRequest request) {
    try {
      String authHeader = request.getHeader(HEADER_STRING);
      if (StringUtils.isEmpty(authHeader)) {
        return null;
      }

      String token = authHeader.replaceAll("^" + TOKEN_PREFIX, "");
      Jws<Claims> jws = jwtHandler.parse(token);
      Claims claims = jws.getBody();
      List<? extends GrantedAuthority> roles =
          Optional.ofNullable(claims.get(ROLES_CLAIM, String.class))
              .stream()
              .map(r -> r.split(COMMA_SEPARATOR))
              .flatMap(Arrays::stream)
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toList());

      String user = claims.getSubject();
      String supplierId = claims.get(SUPPLIER_ID_CLAIM, String.class);
      if (StringUtils.isAnyBlank(user, supplierId)) {
        return null;
      }

      CactusPrincipal principal = CactusPrincipal.builder()
          .name(user)
          .supplierId(supplierId)
          .build();
      CactusToken credentials = CactusToken.builder()
          .token(token)
          .jws(jws)
          .build();
      return new PreAuthenticatedAuthenticationToken(principal, credentials, roles);
    } catch (MalformedJwtException e) {
      log.error("Authorization failed", e);
      return null;
    } catch (Exception e) {
      log.error("Authorization failed", e);
      throw e;
    }
  }
}
