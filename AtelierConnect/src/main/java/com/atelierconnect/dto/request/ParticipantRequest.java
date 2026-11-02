package com.atelierconnect.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.atelierconnect.entity.enums.TypeParticipant;
import com.atelierconnect.entity.enums.TypeParticipantLegacy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantRequest {

    @NotBlank(message = "Le matricule est obligatoire.")
    private String matricule;

    @NotBlank(message = "Le nom est obligatoire.")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire.")
    private String prenom;

    @Email(message = "Le format de l'e-mail est invalide.")
    private String email;

    @com.fasterxml.jackson.annotation.JsonAlias("typeMembre")
    private TypeParticipantLegacy typeParticipant;

    private TypeParticipant categorie;

    @AssertTrue(message = "La catégorie du participant est obligatoire et doit correspondre au type historique s'il est fourni.")
    public boolean isCategorieValide() {
        return categorie != null && (typeParticipant == null || typeParticipant == categorie.toLegacyType())
                || categorie == null && typeParticipant != null;
    }

    @com.fasterxml.jackson.annotation.JsonAlias("siteId")
    private Long centreId;

    private String password;
}
