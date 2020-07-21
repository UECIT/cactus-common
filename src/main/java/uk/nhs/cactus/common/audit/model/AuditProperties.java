package uk.nhs.cactus.common.audit.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditProperties {

  public static final String OPERATION_TYPE = "operation";
  public static final String INTERACTION_ID = "interactionId";
  public static final String SUPPLIER_ID = "supplierId";

}
