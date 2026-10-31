package com.padell.padell.repository;

import com.padell.padell.entity.Salle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TerrainRepository extends JpaRepository<Salle, Long> {
    @EntityGraph(attributePaths = "site")
    List<Salle> findBySiteId(Long siteId);
}
