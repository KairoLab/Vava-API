package com.helper.vavahelper.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.helper.vavahelper.models.Agents.Agents;
import com.helper.vavahelper.models.Skills.Skills;

@Repository
public interface SkillsRepository extends JpaRepository<Skills, Long>{
    List<Skills> findByAgent(Agents agent);

    // Carrega skill + agente em uma unica consulta (evita N+1)
    @Query("select s from Skills s join fetch s.agent")
    List<Skills> findAllWithAgent();
}
