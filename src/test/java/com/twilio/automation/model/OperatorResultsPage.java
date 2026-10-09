package com.twilio.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OperatorResultsPage(
    @JsonProperty("operator_results") @NotEmpty List<@Valid OperatorResultEnvelope> operatorResults
) {}
