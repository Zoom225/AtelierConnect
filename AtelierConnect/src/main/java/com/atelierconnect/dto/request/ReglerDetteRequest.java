package com.atelierconnect.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record ReglerDetteRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal montantAttendu,
        @NotNull UUID cleRequete) {}
