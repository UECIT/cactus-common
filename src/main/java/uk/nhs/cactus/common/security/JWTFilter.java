package uk.nhs.cactus.common.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component
@RequiredArgsConstructor
public class JWTFilter extends GenericFilterBean {

  public static final String SUPPLIER = "supplier";

  private final TokenAuthenticationService authService;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    authService.authenticateRequestContext((HttpServletRequest) request);
    var mdcContext = authService.getCurrentSupplierId().map(id -> MDC.putCloseable(SUPPLIER, id));

    filterChain.doFilter(request, response);

    mdcContext.ifPresent(MDCCloseable::close);
  }
}
