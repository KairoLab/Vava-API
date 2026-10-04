package com.helper.vavahelper.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.helper.vavahelper.models.Agents.Agents;

@Repository
public interface AgentsRepository extends JpaRepository<Agents, Integer>{
    Agents findByName(String name);
}
