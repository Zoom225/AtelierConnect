package com.padell.padell.dto.response;

import com.padell.padell.entity.enums.TypeMembre;
import com.padell.padell.entity.enums.TypeParticipant;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreResponse {
    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private TypeMembre typeMembre;
    private TypeParticipant categorie;
    private Long siteId;
    private String siteNom;
    private Double solde;
    private String token;
}
