package com.padell.padell.repository;

import com.padell.padell.entity.Centre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CentreRepository extends JpaRepository<Centre, Long> {
    // La méthode standard findAll() héritée de JpaRepository est maintenant sûre
    // à utiliser grâce au chargement LAZY des collections dans l'entité Site.
    // Si un chargement spécifique est nécessaire à l'avenir, il faudra utiliser
    // @EntityGraph sur une nouvelle méthode avec un nom non conflictuel.
}
