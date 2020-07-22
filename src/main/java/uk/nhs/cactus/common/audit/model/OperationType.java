package uk.nhs.cactus.common.audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum OperationType {

  @JsonProperty("Service Search") SERVICE_SEARCH,
  @JsonProperty("Encounter") ENCOUNTER,
  @JsonProperty("Is Valid") IS_VALID,
  @JsonProperty("Check Services") CHECK_SERVICES,
  @JsonProperty("Encounter Report") ENCOUNTER_REPORT,
  @JsonProperty("Handover") HANDOVER;

  public String getName() {
    return name().toLowerCase();
  }

}