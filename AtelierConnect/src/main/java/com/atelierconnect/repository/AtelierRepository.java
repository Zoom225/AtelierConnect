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

    @EntityGraph(attributePaths = {"terrain", "terrain.site", "organisateur"})
    List<Atelier> findAll();

    @EntityGraph(attributePaths = {"terrain", "terrain.site", "organisateur"})
    Optional<Atelier> findById(Long id);

    @EntityGraph(attributePaths = {"terrain", "terrain.site", "organisateur"})
    @Query("SELECT a FROM Atelier a WHERE a.typeMatch = :type AND a.statut = :statut")
    List<Atelier> findByTypeAtelierAndStatut(@Param("type") TypeAtelier type, @Param("statut") StatutAtelier statut);

    @EntityGraph(attributePaths = {"terrain", "terrain.site", "organisateur"})
    @Query("SELECT a FROM Atelier a WHERE a.terrain.site.id = :siteId")
    List<Atelier> findBySalleCentreId(@Param("siteId") Long siteId);

    @EntityGraph(attributePaths = {"terrain", "terrain.site", "organisateur"})
    List<Atelier> findByOrganisateurId(Long organisateurId);

    List<Atelier> findByDateDebutBetweenAndStatut(LocalDateTime start, LocalDateTime end, StatutAtelier statut);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Atelier m WHERE m.id = :id")
    Optional<Atelier> findByIdForUpdate(@Param("id") Long id);

    /**
     * Trouve les matchs sur un terrain donné qui chevauchent un créneau horaire spécifié,
     * en excluant un certain statut (typiquement ANNULE).
     *
     * Un chevauchement se produit si :
     * (startA < endB) et (endA > startB)
     *
     * @param terrainId L'ID du terrain.
     * @param start     La date et l'heure de début du créneau à vérifier.
     * @param end       La date et l'heure de fin du créneau à vérifier.
     * @param statut    Le statut à exclure de la recherche.
     * @return Une liste de matchs qui se chevauchent.
     */
    @Query("SELECT m FROM Atelier m WHERE m.terrain.id = :terrainId " +
           "AND m.statut <> :statut " +
           "AND m.dateDebut < :end " +
           "AND m.dateFin > :start")
    List<Atelier> findOverlappingAteliers(
            @Param("terrainId") Long terrainId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statut") StatutAtelier statut
    );
}
