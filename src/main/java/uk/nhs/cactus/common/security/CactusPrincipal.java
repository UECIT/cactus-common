package uk.nhs.cactus.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.AuthenticatedPrincipal;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
public class CactusPrincipal implements AuthenticatedPrincipal {
  private String name;
  private String supplierId;
}
