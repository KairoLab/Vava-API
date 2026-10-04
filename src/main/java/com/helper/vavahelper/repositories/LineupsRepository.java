package com.helper.vavahelper.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.helper.vavahelper.models.Lineups.Lineups;

@Repository
public interface LineupsRepository extends JpaRepository<Lineups, Long> {
    // Carrega lineup + agente + mapa em uma unica consulta (evita N+1)
    @Query("select l from Lineups l join fetch l.agents join fetch l.map")
    List<Lineups> findAllWithAgentAndMap();

    List<Lineups> findByAgents_NameIgnoreCase(String name);
    List<Lineups> findByMap_NameIgnoreCase(String name);
}
