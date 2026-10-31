package com.padell.padell.repository;

import com.padell.padell.entity.Salle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {
    @EntityGraph(attributePaths = "site")
    @Query("SELECT s FROM Salle s WHERE s.site.id = :centreId")
    List<Salle> findByCentreId(@Param("centreId") Long centreId);
}
