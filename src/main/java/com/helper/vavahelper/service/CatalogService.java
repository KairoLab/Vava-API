package com.helper.vavahelper.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helper.vavahelper.models.Agents.body.AgentResponseDTO;
import com.helper.vavahelper.models.Lineups.body.LineupResponseDTO;
import com.helper.vavahelper.models.Skills.body.SkillDTO;
import com.helper.vavahelper.repositories.AgentsRepository;
import com.helper.vavahelper.repositories.LineupsRepository;
import com.helper.vavahelper.repositories.SkillsRepository;

/**
 * Le o catalogo (agentes, skills e lineups) do banco e guarda cada conjunto inteiro em cache.
 * Como o catalogo e pequeno e raramente muda, os controllers filtram em memoria. Assim, nenhuma
 * entrada do usuario (nome do agente, mapa etc.) consegue gerar consulta ao banco.
 * Todas as listas retornadas sao imutaveis, pois o mesmo objeto e compartilhado entre requisicoes.
 */
@Service
public class CatalogService {

    private final AgentsRepository agentsRepository;
    private final SkillsRepository skillsRepository;
    private final LineupsRepository lineupsRepository;

    public CatalogService(AgentsRepository agentsRepository,
                          SkillsRepository skillsRepository,
                          LineupsRepository lineupsRepository) {
        this.agentsRepository = agentsRepository;
        this.skillsRepository = skillsRepository;
        this.lineupsRepository = lineupsRepository;
    }

    @Cacheable(cacheNames = "agents", key = "'all'", sync = true)
    @Transactional(readOnly = true)
    public List<AgentResponseDTO> agents() {
        return agentsRepository.findAll().stream()
                .map(a -> new AgentResponseDTO(
                        a.getId(),
                        a.getName(),
                        a.getUltPoints(),
                        a.getFunction(),
                        a.getIconAgent(),
                        a.getImgAgent(),
                        a.getDescription()))
                .toList();
    }

    @Cacheable(cacheNames = "skills", key = "'all'", sync = true)
    @Transactional(readOnly = true)
    public List<SkillDTO> skills() {
        return skillsRepository.findAllWithAgent().stream()
                .map(s -> new SkillDTO(
                        s.getId(),
                        s.getIconSkill(),
                        s.getName(),
                        s.getDescription(),
                        s.getAgent().getName()))
                .toList();
    }

    @Cacheable(cacheNames = "lineups", key = "'all'", sync = true)
    @Transactional(readOnly = true)
    public List<LineupResponseDTO> lineups() {
        return lineupsRepository.findAllWithAgentAndMap().stream()
                .map(l -> new LineupResponseDTO(
                        l.getId(),
                        l.getDescription(),
                        l.getVideoUrl(),
                        l.getAgents().getName(),
                        l.getMap().getName()))
                .toList();
    }
}
