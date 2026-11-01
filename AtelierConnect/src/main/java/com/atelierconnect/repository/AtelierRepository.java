package com.atelierconnect.repository;

import com.atelierconnect.entity.Atelier;
import com.atelierconnect.entity.enums.StatutAtelier;
import com.atelierconnect.entity.enums.TypeAtelier;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtelierRepository extends JpaRepository<Atelier, Long> {

    @EntityGraph(attributePaths = {"salle", "salle.centre", "organisateur"})
    List<Atelier> findAll();

    @EntityGraph(attributePaths = {"salle", "salle.centre", "organisateur"})
    Optional<Atelier> findById(Long id);

    @EntityGraph(attributePaths = {"salle", "salle.centre", "organisateur"})
    @Query("SELECT a FROM Atelier a WHERE a.typeAtelier = :type AND a.statut = :statut")
    List<Atelier> findByTypeAtelierAndStatut(@Param("type") TypeAtelier type, @Param("statut") StatutAtelier statut);

    @EntityGraph(attributePaths = {"salle", "salle.centre", "organisateur"})
    @Query("SELECT a FROM Atelier a WHERE a.salle.centre.id = :centreId")
    List<Atelier> findBySalleCentreId(@Param("centreId") Long centreId);

    @EntityGraph(attributePaths = {"salle", "salle.centre", "organisateur"})
    List<Atelier> findByOrganisateurId(Long organisateurId);

    List<Atelier> findByDateDebutBetweenAndStatut(LocalDateTime start, LocalDateTime end, StatutAtelier statut);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Atelier m WHERE m.id = :id")
    Optional<Atelier> findByIdForUpdate(@Param("id") Long id);

    /**
     * Trouve les atelier sur une salle donné qui chevauchent un créneau horaire spécifié,
     * en excluant un certain statut (typiquement ANNULE).
     *
     * Un chevauchement se produit si :
     * (startA < endB) et (endA > startB)
     *
     * @param terrainId L'ID de la salle.
     * @param start     La date et l'heure de début du créneau à vérifier.
     * @param end       La date et l'heure de fin du créneau à vérifier.
     * @param statut    Le statut à exclure de la recherche.
     * @return Une liste de atelier qui se chevauchent.
     */
    @Query("SELECT m FROM Atelier m WHERE m.salle.id = :salleId " +
           "AND m.statut <> :statut " +
           "AND m.dateDebut < :end " +
           "AND m.dateFin > :start")
    List<Atelier> findOverlappingAteliers(
            @Param("salleId") Long salleId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statut") StatutAtelier statut
    );
}
