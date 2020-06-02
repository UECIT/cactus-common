package uk.nhs.cactus.common.security;

import java.security.Principal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data
public class CactusPrincipal implements Principal {

  private String name;
  private String supplierId;
}
