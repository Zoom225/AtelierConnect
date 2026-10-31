package com.atelierconnect.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalleRequest {

    @NotBlank(message = "Le nom est obligatoire.")
    private String nom;

    @NotNull(message = "L'ID du site est obligatoire.")
    @JsonAlias("centreId")
    private Long siteId;
}
