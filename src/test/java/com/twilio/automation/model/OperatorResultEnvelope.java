package com.twilio.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * One entry of {@code OperatorResults}. Twilio Intelligence operator payloads vary by
 * operator type; fields beyond the ones modeled here are ignored by {@code JsonUtil}'s
 * {@code ObjectMapper} rather than causing a hard failure (see that class for why).
 */
public record OperatorResultEnvelope(
    @JsonProperty("operator_sid") @NotBlank @Pattern(regexp = "^OR[0-9a-f]{32}$") String operatorSid,
    @JsonProperty("name") @NotBlank String name,
    @JsonProperty("operator_type") @NotBlank String operatorType,
    @JsonProperty("match_probability") @DecimalMin("0.0") @DecimalMax("1.0") Double matchProbability
) {}
